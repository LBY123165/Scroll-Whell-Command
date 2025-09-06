package org.lby123165.scroll_whell_command.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Util;
import org.lby123165.scroll_whell_command.client.ui.RadialMenuScreen;

public class RadialMenuOverlay implements HudRenderCallback {
    private static RadialMenuScreen activeMenu = null;
    private static long lastCloseMs = 0L;
    private static final long REOPEN_COOLDOWN_MS = 150L;

    public static boolean isVisible() {
        return activeMenu != null;
    }

    public static void show() {
        long now = Util.getMeasuringTimeMs();
        if (activeMenu == null && (now - lastCloseMs) >= REOPEN_COOLDOWN_MS) {
            MinecraftClient client = MinecraftClient.getInstance();
            activeMenu = new RadialMenuScreen();
            activeMenu.init(client, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
            client.mouse.unlockCursor();
        }
    }

    public static void hideAndExecute() {
        if (activeMenu != null) {
            activeMenu.execute();
            closeOverlay();
        }
    }

    public static void hideAndCancel() {
        if (activeMenu != null) {
            activeMenu.cancel();
            closeOverlay();
        }
    }

    private static void closeOverlay() {
        activeMenu = null;
        lastCloseMs = Util.getMeasuringTimeMs();
        MinecraftClient.getInstance().mouse.lockCursor();
    }

    public static RadialMenuScreen getActiveMenu() {
        return activeMenu;
    }

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        if (isVisible()) {
            MinecraftClient client = MinecraftClient.getInstance();
            // If any Screen (e.g., chat) is open, immediately cancel and avoid drawing to prevent flashing
            if (client.currentScreen != null) {
                hideAndCancel();
                return;
            }
            // We need to manage the render state carefully
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            double mouseX = client.mouse.getX() * (double)client.getWindow().getScaledWidth() / (double)client.getWindow().getWidth();
            double mouseY = client.mouse.getY() * (double)client.getWindow().getScaledHeight() / (double)client.getWindow().getHeight();

            activeMenu.render(drawContext, (int)mouseX, (int)mouseY, tickDelta);

            RenderSystem.disableBlend();
        }
    }
}
