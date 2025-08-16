package org.lby123165.scroll_whell_command.client.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import org.lby123165.scroll_whell_command.client.config.ConfigData;
import org.lby123165.scroll_whell_command.client.config.ConfigManager;

import java.util.ArrayList;
import java.util.List;

public class SettingsScreen extends Screen {
    private final Screen parent;
    private final ModernTextRenderer modernRenderer = new ModernTextRenderer();

    private OptionListWidget optionList;

    private static double normalizeMaxPerPage(int v) {
        int min = 8, max = 12;
        if (v < min) v = min; if (v > max) v = max;
        return (double)(v - min) / (double)(max - min);
    }

    public SettingsScreen(Screen parent) {
        super(Text.translatable("screen.scroll_whell_command.settings"));
        this.parent = parent;
    }

    @Override
    public void renderBackground(DrawContext context) {
        // No-op: remove vanilla dirt background
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Semi-transparent black background
        context.fill(0, 0, this.width, this.height, 0xA0000000);
        super.render(context, mouseX, mouseY, delta);
        modernRenderer.drawCenteredText(context, this.getTitle().getString(), this.width / 2f, 15, 0xFFFFFFFF, false, 1.2f);
    }

    @Override
    public boolean shouldPause() {
        // Keep world rendering; avoids vanilla paused dirt background bars
        return false;
    }

    @Override
    protected void init() {
        super.init();
        this.clearChildren();

        int listX = 20;
        int listY = 36;
        int listWidth = this.width - listX * 2;
        int listHeight = this.height - listY - 48;

        optionList = new OptionListWidget(this.client, listWidth, listHeight, listY, 28);
        optionList.setX(listX);
        this.addDrawableChild(optionList);

        buildOptions();

        int buttonWidth = 100;
        int buttonY = this.height - 30;
        // Restore defaults (colors + animation settings)
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("button.scroll_whell_command.restore_defaults"), b -> restoreDefaults())
                .dimensions(20, buttonY, buttonWidth, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> onDone())
                .dimensions(this.width - 20 - buttonWidth, buttonY, buttonWidth, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("button.scroll_whell_command.back"), b -> this.close())
                .dimensions(this.width - 20 - buttonWidth * 2 - 4, buttonY, buttonWidth, 20).build());
    }

    private void buildOptions() {
        ConfigData cfg = ConfigManager.get();
        List<OptionListWidget.OptionSpec> specs = new ArrayList<>();

        // Max per page slider (range 8-12)
        specs.add(new OptionListWidget.OptionSpec(
                Text.translatable("label.scroll_whell_command.max_per_page"),
                Text.translatable("tooltip.scroll_whell_command.max_per_page"),
                () -> new SliderWidget(0, 0, 150, 20, Text.empty(), normalizeMaxPerPage(cfg.settings.maxPerPage)) {
                    { this.updateMessage(); }
                    @Override
                    protected void updateMessage() {
                        this.setMessage(Text.literal(String.valueOf(getMaxPerPage())));
                    }

                    @Override
                    protected void applyValue() {
                        cfg.settings.maxPerPage = getMaxPerPage();
                        ConfigManager.save();
                        this.updateMessage();
                    }

                    private int getMaxPerPage() {
                        int min = 8, max = 12;
                        return (int) Math.round(min + this.value * (max - min));
                    }
                }
        ));

        // Base Color button
        specs.add(colorButtonSpec(
                Text.translatable("label.scroll_whell_command.base_color"),
                () -> cfg.settings.baseColor,
                c -> { cfg.settings.baseColor = c; ConfigManager.save(); }
        ));
        // Highlight Color
        specs.add(colorButtonSpec(
                Text.translatable("label.scroll_whell_command.highlight_color"),
                () -> cfg.settings.highlightColor,
                c -> { cfg.settings.highlightColor = c; ConfigManager.save(); }
        ));
        // Text Color
        specs.add(colorButtonSpec(
                Text.translatable("label.scroll_whell_command.text_color"),
                () -> cfg.settings.textColor,
                c -> { cfg.settings.textColor = c; ConfigManager.save(); }
        ));
        // Dim Background Color
        specs.add(colorButtonSpec(
                Text.translatable("label.scroll_whell_command.dim_background"),
                () -> cfg.settings.dimBackground,
                c -> { cfg.settings.dimBackground = c; ConfigManager.save(); }
        ));

        // Animation: enable toggle
        specs.add(new OptionListWidget.OptionSpec(
                Text.translatable("label.scroll_whell_command.anim_enabled"),
                Text.translatable("tooltip.scroll_whell_command.anim_enabled"),
                () -> ButtonWidget.builder(Text.translatable(cfg.settings.animEnabled ? "options.on" : "options.off"), b -> {
                    cfg.settings.animEnabled = !cfg.settings.animEnabled;
                    ConfigManager.save();
                    rebuildListLabels();
                }).size(80, 20).build()
        ));

        // Animation: duration slider (0-1000ms)
        specs.add(new OptionListWidget.OptionSpec(
                Text.translatable("label.scroll_whell_command.anim_duration"),
                Text.translatable("tooltip.scroll_whell_command.anim_duration"),
                () -> new SliderWidget(0, 0, 150, 20, Text.empty(), clamp01(cfg.settings.animDurationMs / 1000.0)) {
                    { this.updateMessage(); }
                    @Override
                    protected void updateMessage() {
                        this.setMessage(Text.literal(getDurationMs() + " ms"));
                    }

                    @Override
                    protected void applyValue() {
                        cfg.settings.animDurationMs = getDurationMs();
                        ConfigManager.save();
                        this.updateMessage();
                    }

                    private int getDurationMs() {
                        int min = 0, max = 1000;
                        return (int)Math.round(min + this.value * (max - min));
                    }
                }
        ));

        // Animation: easing mode cycler button
        specs.add(new OptionListWidget.OptionSpec(
                Text.translatable("label.scroll_whell_command.anim_easing"),
                Text.translatable("tooltip.scroll_whell_command.anim_easing"),
                () -> ButtonWidget.builder(prettyEasing(cfg.settings.animEasing), b -> {
                    cfg.settings.animEasing = nextEasing(cfg.settings.animEasing);
                    ConfigManager.save();
                    rebuildListLabels();
                }).size(150, 20).build()
        ));

        // Animation: hover activation threshold (0.0-1.0)
        specs.add(new OptionListWidget.OptionSpec(
                Text.translatable("label.scroll_whell_command.anim_hover_gate"),
                Text.translatable("tooltip.scroll_whell_command.anim_hover_gate"),
                () -> new SliderWidget(0, 0, 150, 20, Text.empty(), clamp01(cfg.settings.animHoverEnableT)) {
                    { this.updateMessage(); }
                    @Override
                    protected void updateMessage() {
                        this.setMessage(Text.literal(String.format("%.2f", getGate())));
                    }

                    @Override
                    protected void applyValue() {
                        cfg.settings.animHoverEnableT = (float)getGate();
                        ConfigManager.save();
                        this.updateMessage();
                    }

                    private double getGate() {
                        double min = 0.0, max = 1.0;
                        return min + this.value * (max - min);
                    }
                }
        ));

        optionList.setOptions(specs);
    }

    private OptionListWidget.OptionSpec colorButtonSpec(Text label, java.util.function.IntSupplier getter, java.util.function.IntConsumer setter) {
        return new OptionListWidget.OptionSpec(
                label,
                Text.empty(),
                () -> ButtonWidget.builder(Text.literal(String.format("#%08X", getter.getAsInt())), b -> {
                    int initial = getter.getAsInt();
                    this.client.setScreen(new ColorPickerScreen(this, initial, picked -> {
                        setter.accept(picked);
                        // Return to settings screen and refresh the button label
                        this.client.setScreen(this);
                        rebuildListLabels();
                    }));
                }).size(150, 20).build()
        );
    }

    private void rebuildListLabels() {
        // Rebuild to update button text showing current color hex
        buildOptions();
    }

    private void restoreDefaults() {
        ConfigData cfg = ConfigManager.get();
        ConfigData.GlobalSettings def = new ConfigData.GlobalSettings();
        // Colors
        cfg.settings.baseColor = def.baseColor;
        cfg.settings.highlightColor = def.highlightColor;
        cfg.settings.textColor = def.textColor;
        cfg.settings.dimBackground = def.dimBackground;
        // Animation-related
        cfg.settings.animEnabled = def.animEnabled;
        cfg.settings.animDurationMs = def.animDurationMs;
        cfg.settings.animEasing = def.animEasing;
        cfg.settings.animHoverEnableT = def.animHoverEnableT;
        // Persist and refresh UI
        ConfigManager.save();
        rebuildListLabels();
    }

    private static double clamp01(double v) {
        if (v < 0) return 0; if (v > 1) return 1; return v;
    }

    private static Text prettyEasing(String s) {
        String key;
        if (s == null) s = "easeOutCubic";
        switch (s) {
            case "linear" -> key = "text.scroll_whell_command.easing.linear";
            case "easeOutBack" -> key = "text.scroll_whell_command.easing.ease_out_back";
            default -> key = "text.scroll_whell_command.easing.ease_out_cubic";
        }
        return Text.translatable(key);
    }

    private static String nextEasing(String s) {
        String cur = s == null ? "easeOutCubic" : s;
        return switch (cur) {
            case "linear" -> "easeOutCubic";
            case "easeOutCubic" -> "easeOutBack";
            default -> "linear";
        };
    }

    private void onDone() {
        ConfigManager.save();
        this.close();
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}
