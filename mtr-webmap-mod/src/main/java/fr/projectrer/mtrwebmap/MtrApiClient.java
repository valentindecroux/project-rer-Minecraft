package fr.projectrer.mtrwebmap;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Interroge l'API interne du mod MTR (Jetty HTTP tournant en local).
 * L'endpoint principal : GET http://localhost:{port}/mtr/api/map/stations-and-routes?dimension=...
 */
public class MtrApiClient {

    private static final int CONNECTION_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;

    /**
     * Récupère le port du serveur web interne de MTR via réflexion.
     * MTR stocke le port dans org.mtr.mod.Init.getWebserverPort().
     */
    public static int getMtrPort() {
        try {
            Class<?> initClass = Class.forName("org.mtr.mod.Init");
            // Try static method first
            try {
                Method getPort = initClass.getMethod("getWebserverPort");
                Object result = getPort.invoke(null);
                if (result instanceof Number) {
                    int port = ((Number) result).intValue();
                    if (port > 0) return port;
                }
            } catch (Exception ignored) {}

            // Try instance via getInstance() pattern
            try {
                Method getInstance = initClass.getMethod("getServer");
                // Not what we need, skip
            } catch (Exception ignored) {}

            // Fallback: scan private fields for port value
            for (java.lang.reflect.Field field : initClass.getDeclaredFields()) {
                if (field.getName().toLowerCase().contains("port") ||
                    field.getName().equals("serverPort") ||
                    field.getName().equals("webserverPort")) {
                    field.setAccessible(true);
                    Object val = field.get(null);
                    if (val instanceof Number && ((Number) val).intValue() > 0) {
                        return ((Number) val).intValue();
                    }
                }
            }
        } catch (Exception e) {
            MtrWebmap.LOGGER.warn("[MtrWebMap] Impossible de lire le port MTR via réflexion: {}", e.getMessage());
        }
        return -1;
    }

    /**
     * Récupère les données stations+routes depuis l'API MTR locale.
     * Retourne null en cas d'erreur.
     */
    public static String fetchStationsAndRoutes(int port, String dimension) {
        return get(port, "/mtr/api/map/stations-and-routes", "dimension=" + dimension);
    }

    /**
     * Récupère les départs en temps réel depuis l'API MTR locale.
     * Retourne null en cas d'erreur.
     */
    public static String fetchDepartures(int port, String dimension) {
        return get(port, "/mtr/api/map/departures", "dimension=" + dimension);
    }

    /**
     * Effectue une requête GET sur l'API MTR locale.
     */
    private static String get(int port, String path, String query) {
        if (port <= 0) {
            MtrWebmap.LOGGER.warn("[MtrWebMap] Port MTR invalide ({}), export annulé.", port);
            return null;
        }

        String urlStr = "http://localhost:" + port + path + (query != null && !query.isEmpty() ? "?" + query : "");
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECTION_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("Accept", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    return reader.lines().collect(Collectors.joining("\n"));
                }
            } else {
                MtrWebmap.LOGGER.warn("[MtrWebMap] API MTR a répondu {} pour {}", responseCode, urlStr);
                return null;
            }
        } catch (Exception e) {
            MtrWebmap.LOGGER.warn("[MtrWebMap] Erreur lors de l'appel à l'API MTR ({}): {}", urlStr, e.getMessage());
            return null;
        }
    }

    /**
     * Construit le JSON final à envoyer vers GitHub avec les métadonnées.
     */
    public static String buildExportJson(String stationsAndRoutes, String serverName, String dimension) {
        if (stationsAndRoutes == null || stationsAndRoutes.isBlank()) {
            return null;
        }

        // Wrap the MTR data with our metadata
        String escaped = stationsAndRoutes.trim();
        // If it's already a JSON object, merge our metadata around it
        if (escaped.startsWith("{")) {
            // Insert metadata fields at the beginning of the JSON object
            String meta = String.format(
                "\"_meta\":{\"serverName\":\"%s\",\"dimension\":\"%s\",\"exportedAt\":\"%s\",\"generator\":\"mtrwebmap-1.0.0\"}",
                serverName, dimension, Instant.now().toString()
            );
            return "{" + meta + "," + escaped.substring(1);
        } else {
            // Wrap entire response
            return String.format(
                "{\"_meta\":{\"serverName\":\"%s\",\"dimension\":\"%s\",\"exportedAt\":\"%s\"}," +
                "\"data\":%s}",
                serverName, dimension, Instant.now().toString(), escaped
            );
        }
    }
}
