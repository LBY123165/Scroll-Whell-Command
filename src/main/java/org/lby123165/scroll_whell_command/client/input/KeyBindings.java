package org.lby123165.scroll_whell_command.client.input;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {
    public static KeyBinding OPEN_WHEEL;
    public static KeyBinding PAGE_NEXT;
    public static KeyBinding PAGE_PREV;
    public static KeyBinding OPEN_SETTINGS;

    private KeyBindings() {}

    public static void register() {
        OPEN_WHEEL = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.scroll_whell_command.open_wheel",
                InputUtil.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
                "key.categories.scroll_whell_command"
        ));

        PAGE_NEXT = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.scroll_whell_command.page_next",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_PAGE_DOWN,
                "key.categories.scroll_whell_command"
        ));

        PAGE_PREV = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.scroll_whell_command.page_prev",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_PAGE_UP,
                "key.categories.scroll_whell_command"
        ));

        OPEN_SETTINGS = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.scroll_whell_command.open_settings",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                "key.categories.scroll_whell_command"
        ));
    }
}
