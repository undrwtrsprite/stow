package dev.stow.client.version;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.stow.Stow;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

/** Checks public release listings without blocking the game thread or requiring API credentials. */
public final class VersionChecker {
    private static final String MODRINTH_PROJECT = "stow";
    private static final String CURSEFORGE_PROJECT = "1728619";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final long SUCCESS_RECHECK_MILLIS = Duration.ofHours(12).toMillis();
    private static final long FAILURE_RECHECK_MILLIS = Duration.ofHours(1).toMillis();
    private static final Pattern CURSEFORGE_FILE_VERSION = Pattern.compile(
            "^stow-(\\d+\\.\\d+\\.\\d+(?:\\+[0-9A-Za-z.-]+)?)\\.jar$");
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3, task -> {
        Thread thread = new Thread(task, "stow-version-check");
        thread.setDaemon(true);
        return thread;
    });
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final AtomicBoolean CHECKING = new AtomicBoolean();
    private static final AtomicReference<Update> AVAILABLE_UPDATE = new AtomicReference<>();
    private static volatile long nextCheckAt;

    private VersionChecker() {}

    public static void tick(Minecraft client) {
        if (client.player == null) return;

        long now = System.currentTimeMillis();
        if (now >= nextCheckAt && CHECKING.compareAndSet(false, true)) {
            nextCheckAt = now + FAILURE_RECHECK_MILLIS;
            checkAllSources();
        }

        Update update = AVAILABLE_UPDATE.get();
        if (update == null || update.version().equals(Stow.config.lastNotifiedVersion)) return;

        Component message = Component.translatable("stow.update.available", update.version())
                .withStyle(style -> style
                        .withColor(ChatFormatting.GREEN)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.OpenUrl(URI.create(update.url()))));
        client.gui.hud.getChat().addClientSystemMessage(message);
        Stow.config.lastNotifiedVersion = update.version();
        Stow.config.save();
    }

    private static void checkAllSources() {
        String installed = FabricLoader.getInstance().getModContainer(Stow.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("");
        String minecraftVersion = FabricLoader.getInstance().getModContainer("minecraft")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("26.3");

        CompletableFuture<SourceResult> github = CompletableFuture.supplyAsync(
                () -> checkGitHub(installed, minecraftVersion), EXECUTOR);
        CompletableFuture<SourceResult> modrinth = CompletableFuture.supplyAsync(
                () -> checkModrinth(installed, minecraftVersion), EXECUTOR);
        CompletableFuture<SourceResult> curseforge = CompletableFuture.supplyAsync(
                () -> checkCurseForge(installed, minecraftVersion), EXECUTOR);

        CompletableFuture.allOf(github, modrinth, curseforge).whenComplete((ignored, error) -> {
            if (error == null) {
                List<SourceResult> results = List.of(github.join(), modrinth.join(), curseforge.join());
                results.stream().flatMap(result -> result.update().stream())
                        .max((left, right) -> compareVersions(left.version(), right.version()))
                        .ifPresent(VersionChecker::keepNewest);
                nextCheckAt = System.currentTimeMillis() + (results.stream().allMatch(SourceResult::reachable)
                        ? SUCCESS_RECHECK_MILLIS : FAILURE_RECHECK_MILLIS);
            }
            CHECKING.set(false);
        });
    }

    private static SourceResult checkGitHub(String installed, String minecraftVersion) {
        try {
            JsonArray releases = getJson("https://api.github.com/repos/undrwtrsprite/stow/releases?per_page=100", true)
                    .getAsJsonArray();
            Optional<Update> latest = Optional.empty();
            for (JsonElement element : releases) {
                JsonObject release = element.getAsJsonObject();
                if (release.get("draft").getAsBoolean() || release.get("prerelease").getAsBoolean()) continue;
                String tag = string(release, "tag_name");
                String version = tag.replaceFirst("^[vV]", "");
                if (!supportsMinecraft(version, minecraftVersion)) continue;
                boolean hasClient = false;
                for (JsonElement asset : release.getAsJsonArray("assets")) {
                    if (("stow-" + version + ".jar").equals(string(asset.getAsJsonObject(), "name"))) hasClient = true;
                }
                if (!hasClient) continue;
                String url = "https://github.com/undrwtrsprite/stow/releases/tag/"
                        + URLEncoder.encode(tag, StandardCharsets.UTF_8);
                latest = newerUpdate(latest, installed, new Update(version, url));
            }
            return new SourceResult(true, latest);
        } catch (Exception ignored) {
            return SourceResult.unreachable();
        }
    }

    private static SourceResult checkModrinth(String installed, String minecraftVersion) {
        try {
            String endpoint = "https://api.modrinth.com/v2/project/" + MODRINTH_PROJECT
                    + "/version?game_versions=%5B%22" + URLEncoder.encode(minecraftVersion, StandardCharsets.UTF_8)
                    + "%22%5D&loaders=%5B%22fabric%22%5D";
            JsonArray versions = getJson(endpoint, false).getAsJsonArray();
            Optional<Update> latest = Optional.empty();
            for (JsonElement element : versions) {
                if (!element.isJsonObject()) continue;
                JsonObject version = element.getAsJsonObject();
                if (!"release".equals(string(version, "version_type"))) continue;
                String number = string(version, "version_number");
                String id = string(version, "id");
                if (!contains(version.getAsJsonArray("game_versions"), minecraftVersion)
                        || !contains(version.getAsJsonArray("loaders"), "fabric")
                        || !id.matches("[A-Za-z0-9]+")) continue;
                Update candidate = new Update(number, "https://modrinth.com/mod/" + MODRINTH_PROJECT + "/version/" + id);
                latest = newerUpdate(latest, installed, candidate);
            }
            return new SourceResult(true, latest);
        } catch (Exception ignored) {
            return SourceResult.unreachable();
        }
    }

    private static SourceResult checkCurseForge(String installed, String minecraftVersion) {
        try {
            // CFWidget exposes public CurseForge file metadata without shipping a private API key.
            JsonObject project = getJson("https://api.cfwidget.com/" + CURSEFORGE_PROJECT, false).getAsJsonObject();
            if (!CURSEFORGE_PROJECT.equals(string(project, "id"))) return SourceResult.unreachable();
            Optional<Update> latest = Optional.empty();
            for (JsonElement element : project.getAsJsonArray("files")) {
                JsonObject file = element.getAsJsonObject();
                if (!"release".equals(string(file, "type"))
                        || !contains(file.getAsJsonArray("versions"), minecraftVersion)
                        || !contains(file.getAsJsonArray("versions"), "Fabric")) continue;
                Matcher matcher = CURSEFORGE_FILE_VERSION.matcher(string(file, "name"));
                String id = string(file, "id");
                if (!matcher.matches() || !id.matches("[0-9]+")) continue;
                Update candidate = new Update(matcher.group(1),
                        "https://www.curseforge.com/minecraft/mc-mods/stow/files/" + id);
                latest = newerUpdate(latest, installed, candidate);
            }
            return new SourceResult(true, latest);
        } catch (Exception ignored) {
            return SourceResult.unreachable();
        }
    }

    private static JsonElement getJson(String address, boolean github) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(address))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", "undrwtrsprite/stow");
        if (github) request.header("X-GitHub-Api-Version", "2022-11-28");
        HttpResponse<String> response = HTTP.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Version service returned HTTP " + response.statusCode());
        }
        return JsonParser.parseString(response.body());
    }

    private static Optional<Update> newerUpdate(Optional<Update> previous, String installed, Update candidate) {
        return isNewerThan(candidate.version(), installed)
                && (previous.isEmpty() || compareVersions(candidate.version(), previous.get().version()) > 0)
                ? Optional.of(candidate) : previous;
    }

    private static void keepNewest(Update candidate) {
        AVAILABLE_UPDATE.updateAndGet(previous -> previous == null
                || compareVersions(candidate.version(), previous.version()) > 0 ? candidate : previous);
    }

    private static boolean supportsMinecraft(String version, String minecraftVersion) {
        return version.endsWith("+mc" + minecraftVersion);
    }

    private static boolean isNewerThan(String candidate, String installed) {
        return !installed.isBlank() && compareVersions(candidate, installed) > 0;
    }

    private static int compareVersions(String first, String second) {
        try {
            return SemanticVersion.parse(first).compareTo(SemanticVersion.parse(second));
        } catch (VersionParsingException ignored) {
            return 0;
        }
    }

    private static boolean contains(JsonArray values, String expected) {
        if (values == null) return false;
        for (JsonElement value : values) if (expected.equals(value.getAsString())) return true;
        return false;
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    private record Update(String version, String url) {}
    private record SourceResult(boolean reachable, Optional<Update> update) {
        private static SourceResult unreachable() { return new SourceResult(false, Optional.empty()); }
    }
}
