package com.example.feedbackplugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class FeedbackCommand implements CommandExecutor, TabCompleter {

    private final FeedbackPlugin plugin;
    private final DiscordWebhook discordWebhook;

    // Cooldown tracking (player UUID -> last feedback time in ms)
    private final java.util.Map<java.util.UUID, Long> cooldowns = new java.util.HashMap<>();

    public FeedbackCommand(FeedbackPlugin plugin, DiscordWebhook discordWebhook) {
        this.plugin = plugin;
        this.discordWebhook = discordWebhook;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        // Check if the sender is a player or console
        boolean isPlayer = sender instanceof Player;
        Player player = isPlayer ? (Player) sender : null;

        // Must provide at least a category and message
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /feedback <bug|suggestion|general> <message>", NamedTextColor.RED));
            return true;
        }

        String categoryArg = args[0].toLowerCase();
        FeedbackCategory category;

        switch (categoryArg) {
            case "bug" -> category = FeedbackCategory.BUG;
            case "suggestion" -> category = FeedbackCategory.SUGGESTION;
            case "general" -> category = FeedbackCategory.GENERAL;
            default -> {
                sender.sendMessage(Component.text("Invalid category! Use: bug, suggestion, or general.", NamedTextColor.RED));
                return true;
            }
        }

        // Cooldown check (players only)
        if (isPlayer) {
            int cooldownSeconds = plugin.getConfig().getInt("cooldown-seconds", 30);
            java.util.UUID uuid = player.getUniqueId();
            long now = System.currentTimeMillis();

            if (cooldowns.containsKey(uuid)) {
                long elapsed = now - cooldowns.get(uuid);
                long remaining = (cooldownSeconds * 1000L) - elapsed;
                if (remaining > 0) {
                    long secondsLeft = (remaining / 1000) + 1;
                    sender.sendMessage(Component.text(
                        "Please wait " + (secondsLeft > 60 ? (secondsLeft / 60) + " minute(s)" : secondsLeft + " second(s)") + " before submitting more feedback.",
                        NamedTextColor.GOLD
                    ));
                    return true;
                }
            }

            cooldowns.put(uuid, now);
        }

        // Build the feedback message from remaining args
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        // Validate message length
        int maxLength = plugin.getConfig().getInt("max-message-length", 500);
        if (message.length() > maxLength) {
            sender.sendMessage(Component.text(
                "Your feedback is too long! Maximum " + maxLength + " characters.",
                NamedTextColor.RED
            ));
            return true;
        }

        String senderName = isPlayer ? player.getName() : "Console";
        String senderUUID = isPlayer ? player.getUniqueId().toString() : "N/A";

        // Send to Discord asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = discordWebhook.sendFeedback(senderName, senderUUID, category, message);

            // Reply back on main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    sender.sendMessage(Component.text("✅ Your feedback has been submitted! Thank you.", NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("❌ Failed to submit feedback. Please try again later.", NamedTextColor.RED));
                }
            });
        });

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("bug", "suggestion", "general").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .toList();
        }
        if (args.length == 2) {
            return List.of("<your message here>");
        }
        return List.of();
    }
}


