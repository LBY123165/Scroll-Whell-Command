package org.lby123165.scroll_whell_command.client.exec;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

public final class CommandExecutor {
    private CommandExecutor() {}

    public static void execute(String raw) {
        if (raw == null || raw.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.player.networkHandler == null) return;
        ClientPlayNetworkHandler nh = mc.player.networkHandler;

        String s = raw.trim();
        if (s.isEmpty()) return;

        if (s.startsWith("/")) {
            // strip leading '/' for sendChatCommand
            String noSlash = s.substring(1);
            if (noSlash.length() > 256) noSlash = noSlash.substring(0, 256);
            nh.sendChatCommand(noSlash);
        } else {
            if (s.length() > 256) s = s.substring(0, 256);
            nh.sendChatMessage(s);
        }
    }
}
