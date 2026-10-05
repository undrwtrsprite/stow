package dev.stow;

import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Stow {
    public static final String MOD_ID="stow";
    public static StowConfig config=new StowConfig();
    public static Path dataDirectory(){return FabricLoader.getInstance().getConfigDir().resolve("stow");}
    public static Logger createLogger(Class<?> type){return LoggerFactory.getLogger("stow/"+type.getSimpleName());}
    private Stow(){}
}
