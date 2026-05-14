package fr.projectrer.mtrwebmap;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Collectors;

/**
 * Pousse un fichier JSON vers GitHub via l'API Contents.
 * Utilise uniquement java.net (pas de dépendance externe).
 */
public class GitHubClient {

    private static final String API_BASE = "https://api.github.com";
    private static final int TIMEOUT_MS = 15000;

    /**
     * Envoie le contenu JSON vers GitHub.
     * Crée le fichier si absent, le met à jour sinon.
     *
     * @return true si succès
     */
    public static boolean pushData(String jsonContent) {
        String token = WebMapConfig.SERVER.githubToken.get();
        String repo  = WebMapConfig.SERVER.githubRepo.get();
        String branch = WebMapConfig.SERVER.githubBranch.get();
        String path  = WebMapConfig.SERVER.githubDataPath.get();

        if (token.isBlank() || token.equals("YOUR_GITHUB_TOKEN_HERE")) {
            MtrWebmap.LOGGER.warn("[MtrWebMap] Token GitHub non configuré. Configurer mtrwebmap-server.toml !");
            return false;
        }
        if (jsonContent == null || jsonContent.isBlank()) {
            MtrWebmap.LOGGER.warn("[MtrWebMap] Données vides, export annulé.");
            return false;
        }

        try {
            // 1. GET existing file to retrieve SHA (needed for updates)
            String existingSha = getFileSha(token, repo, branch, path);

            // 2. PUT (create or update)
            return putFile(token, repo, branch, path, jsonContent, existingSha);
        } catch (Exception e) {
            MtrWebmap.LOGGER.error("[MtrWebMap] Erreur GitHub: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Récupère le SHA actuel du fichier (null si inexistant).
     */
    private static String getFileSha(String token, String repo, String branch, String path) {
        try {
            String urlStr = API_BASE + "/repos/" + repo + "/contents/" + path + "?ref=" + branch;
            HttpURLConnection conn = openConnection(urlStr, token);
            conn.setRequestMethod("GET");
            conn.connect();

            int code = conn.getResponseCode();
            if (code == 200) {
                String body = readBody(conn);
                // Extract "sha" field from JSON (simple string match, avoids extra deps)
                String sha = extractJsonField(body, "sha");
                MtrWebmap.LOGGER.debug("[MtrWebMap] Fichier existant, SHA: {}", sha);
                return sha;
            } else if (code == 404) {
                MtrWebmap.LOGGER.info("[MtrWebMap] Fichier {} inexistant, il sera créé.", path);
                return null;
            } else {
                MtrWebmap.LOGGER.warn("[MtrWebMap] GET SHA a répondu {}", code);
                return null;
            }
        } catch (Exception e) {
            MtrWebmap.LOGGER.warn("[MtrWebMap] Impossible de lire le SHA existant: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Crée ou met à jour le fichier sur GitHub.
     */
    private static boolean putFile(String token, String repo, String branch,
                                    String path, String content, String existingSha) throws Exception {
        String urlStr = API_BASE + "/repos/" + repo + "/contents/" + path;
        HttpURLConnection conn = openConnection(urlStr, token);
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        // Encode content in Base64
        String b64 = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));

        // Build JSON body
        StringBuilder body = new StringBuilder();
        body.append("{");
        body.append("\"message\":\"chore: update MTR web map data\",");
        body.append("\"branch\":\"").append(branch).append("\",");
        body.append("\"content\":\"").append(b64).append("\"");
        if (existingSha != null && !existingSha.isBlank()) {
            body.append(",\"sha\":\"").append(existingSha).append("\"");
        }
        body.append("}");

        byte[] bodyBytes = body.toString().getBytes(StandardCharsets.UTF_8);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(bodyBytes);
        }

        int code = conn.getResponseCode();
        if (code == 200 || code == 201) {
            MtrWebmap.LOGGER.info("[MtrWebMap] ✅ Données exportées vers GitHub ({} {}).",
                code == 201 ? "créé" : "mis à jour", path);
            return true;
        } else {
            String responseBody = readBody(conn);
            MtrWebmap.LOGGER.warn("[MtrWebMap] ❌ GitHub a répondu {} : {}", code,
                responseBody.length() > 200 ? responseBody.substring(0, 200) : responseBody);
            return false;
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static HttpURLConnection openConnection(String urlStr, String token) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestProperty("Authorization", "token " + token);
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        conn.setRequestProperty("User-Agent", "MtrWebMap-Mod/1.0");
        return conn;
    }

    private static String readBody(HttpURLConnection conn) {
        try {
            java.io.InputStream is = conn.getResponseCode() >= 400
                ? conn.getErrorStream() : conn.getInputStream();
            if (is == null) return "";
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Extrait un champ simple d'un JSON sans parser la totalité.
     * Ex: extractJsonField(json, "sha") -> "abc123..."
     */
    private static String extractJsonField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key);
        if (start == -1) return null;
        start += key.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }
}
