package fr.projectrer.mtrwebmap;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class WebMapConfig {

    public static final ForgeConfigSpec SERVER_SPEC;
    public static final WebMapConfig SERVER;

    static {
        Pair<WebMapConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(WebMapConfig::new);
        SERVER_SPEC = specPair.getRight();
        SERVER = specPair.getLeft();
    }

    // GitHub settings
    public final ForgeConfigSpec.ConfigValue<String> githubToken;
    public final ForgeConfigSpec.ConfigValue<String> githubRepo;
    public final ForgeConfigSpec.ConfigValue<String> githubBranch;
    public final ForgeConfigSpec.ConfigValue<String> githubDataPath;

    // Web app settings
    public final ForgeConfigSpec.ConfigValue<String> siteUrl;
    public final ForgeConfigSpec.ConfigValue<String> serverName;

    // Export settings
    public final ForgeConfigSpec.IntValue pushIntervalSeconds;
    public final ForgeConfigSpec.ConfigValue<String> dimension;

    public WebMapConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("MTR Web Map - Configuration du serveur")
               .push("github");

        githubToken = builder
            .comment("Token GitHub Personnel (scope: repo). Créer sur https://github.com/settings/tokens")
            .define("token", "YOUR_GITHUB_TOKEN_HERE");

        githubRepo = builder
            .comment("Dépôt GitHub au format 'owner/repo' (ex: valentindecroux/project-rer-Minecraft)")
            .define("repo", "valentindecroux/project-rer-Minecraft");

        githubBranch = builder
            .comment("Branche GitHub où pousser les données — DOIT correspondre à la branche GitHub Pages")
            .define("branch", "webmap");

        githubDataPath = builder
            .comment("Chemin du fichier JSON dans le dépôt (racine de la branche webmap)")
            .define("dataPath", "data.json");

        builder.pop().push("webapp");

        siteUrl = builder
            .comment("URL du site GitHub Pages")
            .define("siteUrl", "https://valentindecroux.github.io/project-rer-Minecraft/");

        serverName = builder
            .comment("Nom du serveur affiché sur la carte (ex: ProjectRer)")
            .define("serverName", "ProjectRer");

        builder.pop().push("export");

        pushIntervalSeconds = builder
            .comment("Intervalle d'export en secondes (min: 30, recommandé: 120)")
            .defineInRange("pushIntervalSeconds", 120, 30, 3600);

        dimension = builder
            .comment("Dimension Minecraft à exporter (ex: minecraft:overworld)")
            .define("dimension", "minecraft:overworld");

        builder.pop();
    }
}
