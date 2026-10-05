package dev.stow.client;
import com.terraformersmc.modmenu.api.*;
import dev.stow.client.memory.StowSettingsScreen;
public final class StowModMenu implements ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory(){return StowSettingsScreen::create;}
}
