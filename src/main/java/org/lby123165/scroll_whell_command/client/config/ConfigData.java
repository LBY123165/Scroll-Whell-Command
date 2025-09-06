package org.lby123165.scroll_whell_command.client.config;

import net.minecraft.client.MinecraftClient;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConfigData {
    public GlobalSettings settings = new GlobalSettings();
    public List<CommandItem> commands = new ArrayList<>();

    public static class GlobalSettings {
        public int maxPerPage = 10; 
        public int baseColor = 0x39FFFFFF; // #39FFFFFF
        public int highlightColor = 0xFF00CCFF; // #FF00CCFF
        public int textColor = 0xFFFFFFFF; // #FFFFFFFF
        public int dimBackground = 0x00FFFFFF; // #00FFFFFF
        // Radial menu animation settings
        public boolean animEnabled = true;
        public int animDurationMs = 200; // 0-1000 reasonable
        public String animEasing = "easeOutCubic"; // linear, easeOutCubic, easeOutBack
        public float animHoverEnableT = 0.6f; // 0-1: fraction of animation progress when hover becomes active
    }

    public static class CommandItem {
        public String id = UUID.randomUUID().toString();
        public String label;
        public String type = "single"; 
        public String command;
        public String executionMode = "chat"; // deprecated: UI removed; always uses chat with '/' detection
        public List<SubCommand> subCommands = new ArrayList<>();

        public CommandItem() {}

        // Copy constructor for deep copying
        public CommandItem(CommandItem other) {
            this.id = other.id;
            this.label = other.label;
            this.type = other.type;
            this.command = other.command;
            this.executionMode = other.executionMode;
            this.subCommands = new ArrayList<>();
            for (SubCommand sc : other.subCommands) {
                this.subCommands.add(new SubCommand(sc));
            }
        }

        public boolean isMulti() { return "multi".equalsIgnoreCase(type); }

        public void execute() {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;

            if (isMulti()) {
                new Thread(() -> {
                    for (SubCommand sub : subCommands) {
                        if (sub.command != null && !sub.command.trim().isEmpty()) {
                            // Always use chat behavior with '/' detection
                            runCommand(client, sub.command);
                        }
                        if (sub.delayMs != null && sub.delayMs > 0) {
                            try {
                                Thread.sleep(sub.delayMs);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }).start();
            } else {
                if (command != null && !command.trim().isEmpty()) {
                    runCommand(client, this.command);
                }
            }
        }

        private void runCommand(MinecraftClient client, String commandText) {
            if (client.player == null) return;
            String cmd = commandText.trim();

            // Unified behavior: if starts with '/', run as command; otherwise send as chat message
            if (cmd.startsWith("/")) {
                String noSlash = cmd.substring(1);
                if (noSlash.length() > 256) noSlash = noSlash.substring(0, 256);
                String finalNoSlash = noSlash;
                client.execute(() -> client.player.networkHandler.sendChatCommand(finalNoSlash));
            } else {
                if (cmd.length() > 256) cmd = cmd.substring(0, 256);
                String finalMsg = cmd;
                client.execute(() -> client.player.networkHandler.sendChatMessage(finalMsg));
            }
        }
    }

    public static class SubCommand {
        public String command;
        public Integer delayMs; 

        public SubCommand() {}

        // Copy constructor
        public SubCommand(SubCommand other) {
            this.command = other.command;
            this.delayMs = other.delayMs;
        }
    }
}
