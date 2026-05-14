package fr.projectrer.mtrwebmap;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(MtrWebmap.MOD_ID)
public class MtrWebmap {

    public static final String MOD_ID = "mtrwebmap";
    public static final Logger LOGGER = LogManager.getLogger("MtrWebMap");

    public MtrWebmap() {
        // Register Forge config
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, WebMapConfig.SERVER_SPEC, "mtrwebmap-server.toml");

        // Register event handler
        MinecraftForge.EVENT_BUS.register(new ServerEvents());

        LOGGER.info("[MtrWebMap] Mod chargé ! Utilise /map en jeu pour accéder à la carte.");
    }
}
