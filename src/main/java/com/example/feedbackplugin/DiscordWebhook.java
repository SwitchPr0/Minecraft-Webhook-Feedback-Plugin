package com.example.feedbackplugin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

public class DiscordWebhook {

    private final FeedbackPlugin plugin;
    private String webhookUrl;
    private final HttpClient httpClient;

    public DiscordWebhook(FeedbackPlugin plugin, String webhookUrl) {
        this.plugin = plugin;
        this.webhookUrl = webhookUrl;
        this.httpClient = HttpClient.newHttpClient();
    }

    public void updateUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    /**
     * Sends feedback to the configured Discord webhook as an embed.
     *
     * @return true if successful (2xx response), false otherwise
     */
    public boolean sendFeedback(String playerName, String playerUUID, FeedbackCategory category, String message) {
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("YOUR_WEBHOOK_URL_HERE")) {
            plugin.getLogger().warning("Cannot send feedback: Discord webhook URL is not configured.");
            return false;
        }

        // Sanitize message to prevent JSON injection
        String safeMessage = message
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");

        String safeName = playerName
            .replace("\\", "\\\\")
            .replace("\"", "\\\"");

        String timestamp = Instant.now().toString();

        // Build a Discord embed JSON payload
        String payload = """
            {
              "username": "Feedback Bot",
              "avatar_url": "https://crafatar.com/avatars/%s?overlay",
              "embeds": [
                {
                  "title": "%s",
                  "description": "%s",
                  "color": %d,
                  "footer": {
                    "text": "From: %s | UUID: %s"
                  },
                  "timestamp": "%s"
                }
              ]
            }
            """.formatted(
                playerUUID.equals("N/A") ? "00000000-0000-0000-0000-000000000000" : playerUUID,
                category.getDisplayName(),
                safeMessage,
                category.getColor(),
                safeName,
                playerUUID,
                timestamp
            );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                plugin.getLogger().info("Feedback from " + playerName + " sent to Discord successfully.");
                return true;
            } else {
                plugin.getLogger().warning("Discord webhook returned status " + statusCode + ": " + response.body());
                return false;
            }

        } catch (IOException | InterruptedException e) {
            plugin.getLogger().severe("Failed to send feedback to Discord: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }
}
