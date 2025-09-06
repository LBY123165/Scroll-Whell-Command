package org.lby123165.scroll_whell_command.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import org.lby123165.scroll_whell_command.client.config.ConfigManager;
import org.lby123165.scroll_whell_command.client.exec.CommandScheduler;
import org.lby123165.scroll_whell_command.client.input.KeyBindings;
import org.lby123165.scroll_whell_command.client.render.RadialMenuOverlay;
import org.lby123165.scroll_whell_command.client.ui.CommandEditorScreen;
import org.lby123165.scroll_whell_command.client.ui.RadialMenuScreen;

public class Scroll_whell_commandClient implements ClientModInitializer {

    private final RadialMenuOverlay radialMenuOverlay = new RadialMenuOverlay();
    // Edge-detect the open key to avoid flicker and repeat when holding a keyboard key
    private boolean openKeyDownPrev = false;

    @Override
    public void onInitializeClient() {
        // Load config
        ConfigManager.load();

        // Init scheduler
        CommandScheduler.init();

        // Register keybindings
        KeyBindings.register();

        // Register the HUD renderer for our overlay
        HudRenderCallback.EVENT.register(radialMenuOverlay);

        // Handle all input logic in the client tick
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        if (client.player == null) return;

        // Advance scheduler each tick
        CommandScheduler.tick(client);

        boolean openKeyDown = KeyBindings.OPEN_WHEEL.isPressed();

        // If any screen (e.g., chat) is open, do not open the overlay; if already open, cancel and close
        if (client.currentScreen != null && RadialMenuOverlay.isVisible()) {
            RadialMenuOverlay.hideAndCancel();
        }

        // Handle the radial menu logic if it's visible
        if (RadialMenuOverlay.isVisible()) {
            // Close on release edge only (prevents flicker)
            if (!openKeyDown && openKeyDownPrev) {
                RadialMenuOverlay.hideAndExecute();
            }
            // Note: Mouse clicks and scrolls are handled via MouseMixin.

            // Handle paging via keybinds while overlay is active (since it's not an active Screen)
            while (KeyBindings.PAGE_NEXT.wasPressed()) {
                if (RadialMenuOverlay.getActiveMenu() != null) {
                    RadialMenuOverlay.getActiveMenu().nextPage();
                }
            }
            while (KeyBindings.PAGE_PREV.wasPressed()) {
                if (RadialMenuOverlay.getActiveMenu() != null) {
                    RadialMenuOverlay.getActiveMenu().prevPage();
                }
            }

        } else {
            // The menu is not open, open only on press edge and only when no screen is open
            if (openKeyDown && !openKeyDownPrev && client.currentScreen == null) {
                RadialMenuOverlay.show();
            }
        }

        // Handle opening the settings screen (this can still be a normal screen)
        while (KeyBindings.OPEN_SETTINGS.wasPressed()) {
            if (!(client.currentScreen instanceof CommandEditorScreen)) {
                client.setScreen(new CommandEditorScreen(null));
            }
        }

        // Update previous state at end of tick
        openKeyDownPrev = openKeyDown;
    }
}
