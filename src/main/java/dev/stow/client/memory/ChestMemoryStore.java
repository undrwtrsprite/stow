package dev.stow.client.memory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import net.minecraft.core.BlockPos;

/** A world's last-seen container contents. No game registry or ItemStack serialization is required. */
public final class ChestMemoryStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int LIMIT = 1000;
    private final Path file;
    private final Map<String, SavedChest> chests = new LinkedHashMap<>();
    private final Map<String, String> names = new HashMap<>();
    private List<MaterialGoal> goals = new ArrayList<>();
    private Set<String> selectedChests = new HashSet<>();
    private Set<String> hiddenGoals = new HashSet<>();
    private boolean customSelection;
    private final Map<String,ProjectState> projects=new LinkedHashMap<>();
    private String activeProject="default";
    private final Deque<UndoEntry> history=new ArrayDeque<>();
    private record UndoEntry(String label,Runnable restore) {}
    public record Project(String id,String name) {}
    private record SavedProject(String id,String name,List<MaterialGoal> goals,boolean customSelection,Set<String> selectedChests,Set<String> hiddenGoals) {}
    private static final class ProjectState {
        final String id;String name;boolean customSelection;
        final List<MaterialGoal> goals=new ArrayList<>();
        final Set<String> selectedChests=new HashSet<>(),hiddenGoals=new HashSet<>();
        ProjectState(String id,String name){this.id=id;this.name=name;}
        SavedProject snapshot(){return new SavedProject(id,name,List.copyOf(goals),customSelection,Set.copyOf(selectedChests),Set.copyOf(hiddenGoals));}
    }
    private final Map<String,Long> cachedCounts=new HashMap<>();
    private final Object writeLock=new Object();
    private long revision;
    private boolean dirty;

    public record Location(String dimension, int x, int y, int z, String kind) {
        public String key() { return dimension + ":" + x + "," + y + "," + z; }
        public BlockPos pos() { return new BlockPos(x, y, z); }
    }
    public record MemoryItem(String id, String name, int count) {}
    public record SavedChest(Location location, String title, long lastSeen, List<MemoryItem> items) {}
    public record SearchResult(SavedChest chest, int count, List<MemoryItem> matches) {}
    public record MaterialGoal(String itemId, int target) {}
    private record DiskData(int version, List<SavedChest> chests, Map<String,String> names,
                            List<MaterialGoal> goals, boolean customSelection, Set<String> selectedChests, Set<String> hiddenGoals, List<SavedProject> projects, String activeProject) {}

    public ChestMemoryStore(Path directory, String worldKey) throws IOException {
        file = pathFor(directory, worldKey);
        ProjectState initial=new ProjectState("default","Main build");projects.put(initial.id,initial);bind(initial);
        if (!Files.exists(file)) return;
        if (Files.size(file) > 8 * 1024 * 1024) throw new IOException("Chest memory file exceeds 8 MiB");
        try {
            DiskData data = GSON.fromJson(Files.readString(file), DiskData.class);
            if (data == null || data.version < 1 || data.version > 4 || data.chests == null) throw new IOException("Invalid chest memory format");
            for (SavedChest chest : data.chests) {
                if (!valid(chest)) continue;
                chests.put(chest.location.key(), new SavedChest(chest.location, chest.title, chest.lastSeen, List.copyOf(chest.items)));
            }
            trim();
            if(data.names != null) data.names.forEach((key,value) -> {
                if(chests.containsKey(key) && value!=null && !cleanName(value).isBlank()) names.put(key,cleanName(value));
            });
            if(data.goals != null) for(MaterialGoal goal:data.goals) {
                if(validGoal(goal) && goals.stream().noneMatch(g -> g.itemId.equals(goal.itemId)) && goals.size()<32) goals.add(goal);
            }
            customSelection=data.customSelection;
            if(data.selectedChests!=null) for(String key:data.selectedChests) if(chests.containsKey(key)) selectedChests.add(key);
            if(data.hiddenGoals!=null) for(String id:data.hiddenGoals) if(goals.stream().anyMatch(g -> g.itemId.equals(id))) hiddenGoals.add(id);
            projects.get(activeProject).customSelection=customSelection;
            if(data.version==4 && data.projects!=null && !data.projects.isEmpty()) {
                projects.clear();
                for(SavedProject saved:data.projects) {
                    if(projects.size()>=16)break;
                    if(saved==null || saved.id==null || saved.id.isBlank() || saved.id.length()>64 || saved.name==null || cleanName(saved.name).isBlank() || projects.containsKey(saved.id))continue;
                    ProjectState state=new ProjectState(saved.id,cleanName(saved.name));state.customSelection=saved.customSelection;
                    if(saved.goals!=null)for(MaterialGoal goal:saved.goals)if(validGoal(goal) && state.goals.size()<32 && state.goals.stream().noneMatch(g -> g.itemId.equals(goal.itemId)))state.goals.add(goal);
                    if(saved.selectedChests!=null)for(String key:saved.selectedChests)if(chests.containsKey(key))state.selectedChests.add(key);
                    if(saved.hiddenGoals!=null)for(String id:saved.hiddenGoals)if(state.goals.stream().anyMatch(g -> g.itemId.equals(id)))state.hiddenGoals.add(id);
                    projects.put(state.id,state);
                }
                if(projects.isEmpty())throw new IOException("No valid projects in chest memory");
                bind(projects.getOrDefault(data.activeProject,projects.values().iterator().next()));
            }
        } catch (RuntimeException e) {
            throw new IOException("Could not read chest memory", e);
        }
    }

    private void bind(ProjectState state) {
        activeProject=state.id;goals=state.goals;selectedChests=state.selectedChests;hiddenGoals=state.hiddenGoals;customSelection=state.customSelection;
    }
    public synchronized String activeProjectId(){return activeProject;}
    public synchronized String projectName(){return projects.get(activeProject).name;}
    public synchronized List<Project> projects(){return projects.values().stream().map(p -> new Project(p.id,p.name)).toList();}
    public synchronized void switchProject(String id){ProjectState state=projects.get(id);if(state==null || id.equals(activeProject))return;bind(state);changed();}
    public synchronized String createProject(String name){if(projects.size()>=16)throw new IllegalArgumentException("Maximum 16 projects");String clean=cleanName(name);if(clean.isBlank())throw new IllegalArgumentException("Choose a project name");if(projects.values().stream().anyMatch(p -> p.name.equalsIgnoreCase(clean)))throw new IllegalArgumentException("That project name is already in use");ProjectState p=new ProjectState(UUID.randomUUID().toString(),clean);projects.put(p.id,p);bind(p);changed();return p.id;}
    public synchronized void renameProject(String name){String clean=cleanName(name);if(clean.isBlank())throw new IllegalArgumentException("Choose a project name");if(projects.values().stream().anyMatch(p -> !p.id.equals(activeProject) && p.name.equalsIgnoreCase(clean)))throw new IllegalArgumentException("That project name is already in use");projects.get(activeProject).name=clean;changed();}
    public synchronized void deleteProject(){if(projects.size()<=1)return;ProjectState deleted=projects.remove(activeProject);bind(projects.values().iterator().next());pushUndo("Delete "+deleted.name,() -> {if(projects.size()<16){projects.put(deleted.id,deleted);bind(deleted);}});changed();}
    private void pushUndo(String label,Runnable restore){history.addFirst(new UndoEntry(label,restore));while(history.size()>10)history.removeLast();}
    public synchronized boolean canUndo(){return !history.isEmpty();}
    public synchronized String undoLabel(){return history.isEmpty()?"":history.getFirst().label;}
    public synchronized boolean undo(){if(history.isEmpty())return false;history.removeFirst().restore.run();changed();return true;}
    public synchronized SavedChest nearestSource(String itemId,String dimension,BlockPos player) {
        return search("",dimension,player).stream().map(SearchResult::chest).filter(c -> c.location.dimension.equals(dimension) && isIncluded(c.location) && countIn(c.location,itemId)>0).findFirst().orElse(null);
    }
    public synchronized long oldestSourceTime(String itemId){return chests.values().stream().filter(c -> isIncluded(c.location) && countIn(c.location,itemId)>0).mapToLong(SavedChest::lastSeen).min().orElse(0);}
    public static String age(long timestamp){long seconds=Math.max(0,(System.currentTimeMillis()-timestamp)/1000);return seconds<60?seconds+"s":seconds<3600?seconds/60+"m":seconds<86400?seconds/3600+"h":seconds/86400+"d";}

    public static Path pathFor(Path directory, String worldKey) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(worldKey.getBytes(StandardCharsets.UTF_8));
            return directory.resolve(HexFormat.of().formatHex(hash) + ".json");
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private static boolean valid(SavedChest chest) {
        return chest != null && chest.location != null && chest.location.dimension != null
                && chest.location.kind != null && chest.title != null && chest.items != null && chest.items.size() <= 128
                && chest.items.stream().allMatch(item -> item != null && item.id != null && item.id.contains(":")
                && item.name != null && item.count > 0 && item.count <= 1_000_000);
    }

    private void trim() {
        while (chests.size() > LIMIT) {
            String key=chests.values().stream().min(Comparator.comparingLong(SavedChest::lastSeen)).orElseThrow().location.key();
            chests.remove(key); names.remove(key); projects.values().forEach(p -> p.selectedChests.remove(key));
        }
    }

    public synchronized long revision() { return revision; }
    public synchronized SavedChest get(Location location) { return chests.get(location.key()); }
    public synchronized void remember(SavedChest chest) {
        if (!valid(chest)) throw new IllegalArgumentException("Invalid container snapshot");
        chests.put(chest.location.key(), new SavedChest(chest.location, chest.title, chest.lastSeen, List.copyOf(chest.items)));
        trim();
        changed();
    }
    public synchronized void forget(Location location) {
        String key=location.key();SavedChest previous=chests.remove(key);if(previous==null)return;
        String alias=names.remove(key);Set<String> included=new HashSet<>();
        projects.values().forEach(p -> {if(p.selectedChests.remove(key))included.add(p.id);});
        pushUndo("Forget "+(alias==null?previous.title:alias),() -> {
            // A newly opened chest is more accurate than the deleted snapshot.
            if(!chests.containsKey(key)){chests.put(key,previous);if(alias!=null)names.put(key,alias);trim();}
            if(chests.containsKey(key) && alias!=null)names.putIfAbsent(key,alias);
            if(chests.containsKey(key))included.forEach(id -> {ProjectState p=projects.get(id);if(p!=null)p.selectedChests.add(key);});
        });changed();
    }

    private void changed() { projects.get(activeProject).customSelection=customSelection;revision++; dirty=true; cachedCounts.clear(); }
    private static String cleanName(String name) {
        String clean=name.replaceAll("[\\p{Cntrl}§]", "").strip();
        return clean.substring(0,Math.min(clean.length(),48));
    }
    public synchronized String displayName(SavedChest chest) { return names.getOrDefault(chest.location.key(),chest.title); }
    public synchronized String customName(Location location) { return names.getOrDefault(location.key(),""); }
    public synchronized void rename(Location location,String name) {
        if(!chests.containsKey(location.key())) return;
        String clean=cleanName(name);
        if(clean.isBlank()) names.remove(location.key()); else names.put(location.key(),clean);
        changed();
    }
    private static boolean validGoal(MaterialGoal goal) {
        return goal!=null && goal.itemId!=null && goal.itemId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") && goal.target>0 && goal.target<=1_000_000;
    }
    public synchronized List<MaterialGoal> goals() { return List.copyOf(goals); }
    public synchronized List<MaterialGoal> visibleGoals() { return goals.stream().filter(g -> !hiddenGoals.contains(g.itemId)).toList(); }
    public synchronized boolean isGoalVisible(String itemId) { return !hiddenGoals.contains(itemId); }
    public synchronized void toggleGoalVisible(String itemId) {
        if(goals.stream().noneMatch(g -> g.itemId.equals(itemId))) return;
        if(!hiddenGoals.remove(itemId)) hiddenGoals.add(itemId);
        changed();
    }
    public synchronized void updateTarget(String itemId,int target) {
        MaterialGoal replacement=new MaterialGoal(itemId,target);
        if(!validGoal(replacement))throw new IllegalArgumentException("Invalid material goal");
        for(int i=0;i<goals.size();i++)if(goals.get(i).itemId.equals(itemId)){goals.set(i,replacement);changed();return;}
    }
    public synchronized void setGoal(String itemId,int target) {
        MaterialGoal goal=new MaterialGoal(itemId,target);
        if(!validGoal(goal)) throw new IllegalArgumentException("Invalid material goal");
        boolean existing=goals.stream().anyMatch(g -> g.itemId.equals(itemId));
        if(!existing && goals.size()>=32) throw new IllegalArgumentException("Maximum 32 material goals");
        if(!existing)hiddenGoals.remove(itemId);
        if(existing){updateTarget(itemId,target);return;}goals.add(goal);changed();
    }
    public synchronized void removeGoal(String itemId) {
        for(int i=0;i<goals.size();i++)if(goals.get(i).itemId.equals(itemId)) {
            int index=i;MaterialGoal removed=goals.remove(i);boolean hidden=hiddenGoals.remove(itemId);String project=activeProject;
            pushUndo("Remove "+itemId,() -> {ProjectState state=projects.get(project);if(state==null || state.goals.size()>=32 || state.goals.stream().anyMatch(g -> g.itemId.equals(itemId)))return;
                bind(state);goals.add(Math.min(index,goals.size()),removed);if(hidden)hiddenGoals.add(itemId);
            });changed();return;
        }
    }
    public synchronized void trackGoal(String itemId) {
        MaterialGoal goal=goals.stream().filter(g -> g.itemId.equals(itemId)).findFirst().orElse(null);
        if(goal!=null) { hiddenGoals.remove(itemId);goals.remove(goal); goals.add(0,goal); changed(); }
    }
    public synchronized boolean isIncluded(Location location) { return !customSelection || selectedChests.contains(location.key()); }
    public synchronized boolean usesAllChests() { return !customSelection; }
    public synchronized void selectAllChests(boolean all) { customSelection=!all; selectedChests.clear(); changed(); }
    public synchronized void toggleIncluded(Location location) {
        if(!chests.containsKey(location.key())) return;
        if(!customSelection) { selectedChests.addAll(chests.keySet()); customSelection=true; }
        if(!selectedChests.remove(location.key())) selectedChests.add(location.key());
        changed();
    }
    public synchronized int includedChestCount() { return customSelection?selectedChests.size():chests.size(); }
    public synchronized long chestCount(String itemId) {
        Long cached=cachedCounts.get(itemId);if(cached!=null)return cached;
        long count=0;
        for(SavedChest chest:chests.values()) if(isIncluded(chest.location))
            for(MemoryItem item:chest.items) if(item.id.equals(itemId)) count+=item.count;
        cachedCounts.put(itemId,count);return count;
    }
    public synchronized long countIn(Location location,String itemId) {
        SavedChest chest=chests.get(location.key());
        return chest==null?0:chest.items.stream().filter(i -> i.id.equals(itemId)).mapToLong(MemoryItem::count).sum();
    }

    public synchronized List<SearchResult> search(String query, String dimension, BlockPos player) {
        List<SearchResult> results = new ArrayList<>();
        for (SavedChest chest : chests.values()) {
            List<MemoryItem> matches = chest.items.stream().filter(item -> matches(item, query)).toList();
            if (!query.isBlank() && matches.isEmpty()) {
                if(!matches(new MemoryItem("stow:container",displayName(chest),1),query)) continue;
                matches=chest.items;
            }
            results.add(new SearchResult(chest, matches.stream().mapToInt(MemoryItem::count).sum(), matches));
        }
        results.sort(Comparator.<SearchResult>comparingInt(r -> r.chest.location.dimension.equals(dimension) ? 0 : 1)
                .thenComparingDouble(r -> player != null && r.chest.location.dimension.equals(dimension)
                        ? player.distSqr(r.chest.location.pos()) : Double.MAX_VALUE)
                .thenComparing(Comparator.comparingLong((SearchResult r) -> r.chest.lastSeen).reversed()));
        return List.copyOf(results);
    }

    public static boolean matches(MemoryItem item, String query) {
        String name = item.name.toLowerCase(Locale.ROOT);
        String id = item.id.toLowerCase(Locale.ROOT);
        for (String word : query.strip().toLowerCase(Locale.ROOT).split("\\s+")) {
            if (word.startsWith("@")) {
                if (!id.substring(0, id.indexOf(':')).contains(word.substring(1))) return false;
            } else if (!name.contains(word) && !id.contains(word)) {
                String singular = word.endsWith("s") && word.length() > 3 ? word.substring(0, word.length()-1) : word;
                if (!name.contains(singular) && !id.contains(singular)) return false;
            }
        }
        return true;
    }

    /** Called on the dedicated saver thread; the main thread never waits for disk writes. */
    public void save() throws IOException {
        synchronized(writeLock){saveSnapshot();}
    }
    private void saveSnapshot() throws IOException {
        DiskData data;
        long writingRevision;
        synchronized (this) {
            if (!dirty) return;
            data = new DiskData(4, List.copyOf(chests.values()),Map.copyOf(names),List.copyOf(goals),customSelection,Set.copyOf(selectedChests),Set.copyOf(hiddenGoals),projects.values().stream().map(ProjectState::snapshot).toList(),activeProject);
            writingRevision = revision;
        }
        Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temporary, GSON.toJson(data));
        try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException e) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        synchronized (this) { if (revision == writingRevision) dirty = false; }
    }
}
