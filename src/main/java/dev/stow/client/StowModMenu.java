package dev.stow.client;
import com.terraformersmc.modmenu.api.*;
import dev.stow.client.stow.SettingsScreen;
public final class StowModMenu implements ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory(){return SettingsScreen::open;}
}
