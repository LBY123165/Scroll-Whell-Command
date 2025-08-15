package org.lby123165.scroll_whell_command.mixin;

import net.minecraft.client.Mouse;
import net.minecraft.client.MinecraftClient;
import org.lby123165.scroll_whell_command.client.render.RadialMenuOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (RadialMenuOverlay.isVisible()) {
            // We only care about press events
            if (action == 1) { // 1 for press
                if (button == 0) { // 0 for left-click
                    RadialMenuOverlay.hideAndExecute();
                    ci.cancel(); // Consume the event
                } else if (button == 1) { // 1 for right-click
                    RadialMenuOverlay.hideAndCancel();
                    ci.cancel(); // Consume the event
                }
            }
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (RadialMenuOverlay.isVisible()) {
            if (vertical > 0) {
                RadialMenuOverlay.getActiveMenu().nextPage();
            } else if (vertical < 0) {
                RadialMenuOverlay.getActiveMenu().prevPage();
            }
            ci.cancel(); // Consume the event
        }
    }
}
