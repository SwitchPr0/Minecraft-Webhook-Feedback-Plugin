package com.example.feedbackplugin;

import org.bukkit.plugin.java.JavaPlugin;

public class FeedbackPlugin extends JavaPlugin {

    private DiscordWebhook discordWebhook;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String webhookUrl = getConfig().getString("discord.webhook-url", "");
        if (webhookUrl.isEmpty() || webhookUrl.equals("YOUR_WEBHOOK_URL_HERE")) {
            getLogger().warning("Discord webhook URL is not configured! Edit config.yml and set discord.webhook-url.");
        }

        discordWebhook = new DiscordWebhook(this, webhookUrl);

        FeedbackCommand feedbackCommand = new FeedbackCommand(this, discordWebhook);
        getCommand("feedback").setExecutor(feedbackCommand);
        getCommand("feedback").setTabCompleter(feedbackCommand);

        getLogger().info("FeedbackPlugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("FeedbackPlugin disabled.");
    }

    public DiscordWebhook getDiscordWebhook() {
        return discordWebhook;
    }
}
