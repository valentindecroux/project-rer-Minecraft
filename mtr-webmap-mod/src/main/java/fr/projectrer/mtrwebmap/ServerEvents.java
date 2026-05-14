package fr.projectrer.mtrwebmap;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Gère le cycle de vie du serveur Minecraft pour l'export MTR.
 */
public class ServerEvents {

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> exportTask;

    @SubscribeEvent
    public void onCommandsRegister(RegisterCommandsEvent event) {
        MapCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        MtrWebmap.LOGGER.info("[MtrWebMap] Serveur démarré, initialisation de l'export MTR...");

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MtrWebMap-Export");
            t.setDaemon(true);
            return t;
        });

        int interval = WebMapConfig.SERVER.pushIntervalSeconds.get();

        // Premier export après 20 secondes (laisse MTR s'initialiser)
        // Puis export périodique
        exportTask = scheduler.scheduleAtFixedRate(
            this::runExport,
            20L,          // délai initial
            interval,     // intervalle
            TimeUnit.SECONDS
        );

        MtrWebmap.LOGGER.info("[MtrWebMap] Export programmé toutes les {} secondes.", interval);
        MtrWebmap.LOGGER.info("[MtrWebMap] Tape /map en jeu pour obtenir le lien vers la carte !");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        MtrWebmap.LOGGER.info("[MtrWebMap] Arrêt du serveur, arrêt de l'export MTR.");
        if (exportTask != null) {
            exportTask.cancel(false);
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    /**
     * Effectue un export complet : récupère les données MTR et les pousse sur GitHub.
     */
    private void runExport() {
        try {
            // 1. Récupérer le port du serveur web MTR
            int port = MtrApiClient.getMtrPort();
            if (port <= 0) {
                MtrWebmap.LOGGER.warn("[MtrWebMap] Port MTR non disponible (MTR pas encore prêt ?). Prochain essai dans {} s.",
                    WebMapConfig.SERVER.pushIntervalSeconds.get());
                return;
            }

            // 2. Récupérer les données stations+routes
            String dimension = WebMapConfig.SERVER.dimension.get();
            String rawData = MtrApiClient.fetchStationsAndRoutes(port, dimension);
            if (rawData == null) {
                MtrWebmap.LOGGER.warn("[MtrWebMap] Données MTR vides ou erreur API.");
                return;
            }

            // 3. Construire le JSON final avec métadonnées
            String serverName = WebMapConfig.SERVER.serverName.get();
            String exportJson = MtrApiClient.buildExportJson(rawData, serverName, dimension);
            if (exportJson == null) {
                return;
            }

            // 4. Pousser vers GitHub
            boolean success = GitHubClient.pushData(exportJson);
            if (success) {
                MtrWebmap.LOGGER.debug("[MtrWebMap] Export réussi ({} octets).", exportJson.length());
            }

        } catch (Exception e) {
            MtrWebmap.LOGGER.error("[MtrWebMap] Erreur inattendue lors de l'export: {}", e.getMessage(), e);
        }
    }

    /**
     * Déclenche un export immédiat (utilisé par la commande /mtrexport).
     */
    public static void triggerManualExport() {
        Thread thread = new Thread(() -> {
            try {
                int port = MtrApiClient.getMtrPort();
                if (port <= 0) {
                    MtrWebmap.LOGGER.warn("[MtrWebMap] Port MTR introuvable pour l'export manuel.");
                    return;
                }
                String dimension = WebMapConfig.SERVER.dimension.get();
                String rawData = MtrApiClient.fetchStationsAndRoutes(port, dimension);
                String exportJson = MtrApiClient.buildExportJson(
                    rawData,
                    WebMapConfig.SERVER.serverName.get(),
                    dimension
                );
                if (exportJson != null) {
                    GitHubClient.pushData(exportJson);
                }
            } catch (Exception e) {
                MtrWebmap.LOGGER.error("[MtrWebMap] Erreur export manuel: {}", e.getMessage());
            }
        }, "MtrWebMap-ManualExport");
        thread.setDaemon(true);
        thread.start();
    }
}
