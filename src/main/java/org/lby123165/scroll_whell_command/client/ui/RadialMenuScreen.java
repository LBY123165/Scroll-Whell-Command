package org.lby123165.scroll_whell_command.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import org.lby123165.scroll_whell_command.client.config.ConfigData;
import org.lby123165.scroll_whell_command.client.config.ConfigManager;
import org.lby123165.scroll_whell_command.client.exec.CommandScheduler;
import org.lby123165.scroll_whell_command.client.exec.CommandExecutor;
import org.lby123165.scroll_whell_command.client.input.KeyBindings;

import java.util.ArrayList;
import java.util.List;

public class RadialMenuScreen extends Screen {
    private List<ConfigData.CommandItem> itemsAll = new ArrayList<>();
    private final List<RadialMenuSegmentWidget> segments = new ArrayList<>();
    private int currentPage = 0;
    private RadialMenuSegmentWidget hoveredWidget = null;
    private boolean cancelled = false;

    private final ConfigData.GlobalSettings settings;
    private final ModernTextRenderer modernRenderer = new ModernTextRenderer();
    // Open animation
    private long openStartMs = Util.getMeasuringTimeMs();

    public RadialMenuScreen() {
        super(Text.translatable("screen.scroll_whell_command.radial_menu"));
        // Always reload configuration to ensure fresh color settings
        ConfigManager.load();
        ConfigData cfg = ConfigManager.get();
        this.settings = cfg.settings;
        if (cfg.commands != null) itemsAll = new ArrayList<>(cfg.commands);
    }

    @Override
    protected void init() {
        super.init();
        this.clearChildren(); // Clear old widgets
        this.segments.clear(); // FIX: Clear the segment list to ensure it only contains current page items
        // reset animation on init
        this.openStartMs = Util.getMeasuringTimeMs();

        int cx = this.width / 2;
        int cy = this.height / 2;
        int radiusOuter = Math.min(this.width, this.height) / 3;
        int radiusInner = (int) (radiusOuter * 0.45f);

        int total = itemsAll.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) settings.maxPerPage));
        currentPage = Math.max(0, Math.min(currentPage, totalPages - 1));
        int start = currentPage * settings.maxPerPage;
        int end = Math.min(start + settings.maxPerPage, total);
        List<ConfigData.CommandItem> pageItems = itemsAll.subList(start, end);

        int n = pageItems.size();
        if (n == 0) return;

        double anglePer = Math.toRadians(360.0 / n);
        double currentAngle = -Math.PI / 2 - anglePer / 2; // Start at the top

        for (ConfigData.CommandItem item : pageItems) {
            RadialMenuSegmentWidget segment = new RadialMenuSegmentWidget(
                cx, cy, 0, 0, // Position and size are handled by the widget logic itself
                item,
                currentAngle,
                anglePer,
                radiusInner,
                radiusOuter,
                this.settings
            );
            this.addDrawableChild(segment);
            this.segments.add(segment);
            currentAngle += anglePer;
        }
    }

    @Override
    public boolean shouldPause() {
        // This is the key fix: Returning false prevents the game from pausing,
        // which allows the world to be rendered behind the menu, creating the transparent effect.
        return false;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (this.client == null || this.client.world == null) {
            return;
        }
        // Opening animation easing
        float t = getOpenT();
        float et = applyEasing(t);
        // Dim background fade-in
        int dim = withAlpha(settings.dimBackground, (int)(((settings.dimBackground >>> 24) & 0xFF) * t));
        ctx.fill(0, 0, this.width, this.height, dim);

        int cx = this.width / 2;
        int cy = this.height / 2;
        int baseOuter = Math.min(this.width, this.height) / 3;
        int radiusOuter = Math.max(1, (int) (baseOuter * et));
        int segCount = 0;
        for (var child : children()) if (child instanceof RadialMenuSegmentWidget) segCount++;

        // Draw separator lines if there are multiple segments
        if (segCount > 1) {
            double anglePer = Math.toRadians(360.0 / segCount);
            double baseAngle = -Math.PI / 2 - anglePer / 2; // Align with segment start
            for (int i = 0; i < segCount; i++) {
                double lineAngle = baseAngle + i * anglePer;
                float x2 = cx + (float) (radiusOuter * Math.cos(lineAngle));
                float y2 = cy + (float) (radiusOuter * Math.sin(lineAngle));
                drawLine(ctx, cx, cy, (int)x2, (int)y2, 0xFFFFFFFF);
            }
        }

        // Update hovered segment based on the corrected angle logic
        updateHoveredSegment(mouseX, mouseY);

        // Let the framework render all the segment widgets
        super.render(ctx, mouseX, mouseY, delta);

        // Draw the central circle overlay (scale with animation)
        drawFilledCircle(ctx, cx, cy, Math.max(1, (int) (radiusOuter * 0.45f)), 0x90000000); // Semi-transparent black

        // Draw center hints and other overlays
        drawCenterHints(ctx);

        int total = itemsAll.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) settings.maxPerPage));

        // Page indicator
        if (totalPages > 1) {
            String pageText = Text.translatable("text.scroll_whell_command.page_indicator", currentPage + 1, totalPages).getString();
            modernRenderer.drawText(ctx, pageText, (width - modernRenderer.getWidth(pageText)) / 2f, this.height / 2f + radiusOuter + 20, 0xFFFFFFFF, false, 1.1f);
            if (currentPage > 0) modernRenderer.drawText(ctx, "‹", (width - modernRenderer.getWidth(pageText)) / 2f - 20, this.height / 2f + radiusOuter + 20, 0xFFAAFFAA, false, 1.1f);
            if (currentPage < totalPages - 1) modernRenderer.drawText(ctx, "›", (width + modernRenderer.getWidth(pageText)) / 2f + 10, this.height / 2f + radiusOuter + 20, 0xFFAAFFAA, false, 1.1f);
        }

        if (total == 0) {
            String tip = Text.translatable("text.scroll_whell_command.no_commands").getString();
            modernRenderer.drawText(ctx, tip, this.width / 2f - modernRenderer.getWidth(tip) / 2f, this.height / 2f, 0xFFFFFFFF, false, 1.1f);
        }
    }

    private void drawCenterHints(DrawContext ctx) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int baseOuter = Math.min(this.width, this.height) / 3;
        int radiusOuter = Math.max(1, (int) (baseOuter * applyEasing(getOpenT())));
        int radiusInner = (int) (radiusOuter * 0.45f);

        // Draw the center label (selected command name or key hints)
        if (this.hoveredWidget != null) {
            String label = this.hoveredWidget.getMessage().getString();
            modernRenderer.drawText(ctx, label, this.width / 2f - modernRenderer.getWidth(label) / 2f, this.height / 2f - 6, 0xFFFFFFFF, false, 1.1f);
        } else {
            // New: Show keybind hints in the center
            Text cancelHint = Text.translatable("text.scroll_whell_command.cancel_hint", Text.literal("ESC"));
            Text pageHint = Text.translatable("text.scroll_whell_command.paging_hint", KeyBindings.PAGE_PREV.getBoundKeyLocalizedText(), KeyBindings.PAGE_NEXT.getBoundKeyLocalizedText());
            modernRenderer.drawText(ctx, cancelHint, this.width / 2f - modernRenderer.getWidth(cancelHint.getString()) / 2f, this.height / 2f - 12, 0xFFCCCCCC, false, 1.0f);
            modernRenderer.drawText(ctx, pageHint, this.width / 2f - modernRenderer.getWidth(pageHint.getString()) / 2f, this.height / 2f + 2, 0xFFCCCCCC, false, 1.0f);
        }
    }

    private void updateHoveredSegment(int mouseX, int mouseY) {
        this.hoveredWidget = null;
        int cx = this.width / 2;
        int cy = this.height / 2;

        double dx = mouseX - cx;
        double dy = mouseY - cy;
        int baseOuter = Math.min(this.width, this.height) / 3;
        int radiusOuter = Math.max(1, (int) (baseOuter * applyEasing(getOpenT())));
        int radiusInner = (int) (radiusOuter * 0.45f);
        double distSq = dx * dx + dy * dy;
        // APEX 风格：中心空白区域作为取消区，进入该区域不选中任何分段
        float hoverGate = (settings != null && settings.animHoverEnableT >= 0f && settings.animHoverEnableT <= 1f) ? settings.animHoverEnableT : 0.6f;
        if (distSq <= (long) radiusInner * radiusInner || getOpenT() < hoverGate) { // 动画早期不选中
            for (RadialMenuSegmentWidget segment : segments) segment.setForcedHighlight(false);
            return;
        }

        double angle = Math.toDegrees(Math.atan2(dy, dx));
        if (angle < 0) {
            angle += 360;
        }

        int segmentCount = segments.size();
        double anglePerSegment = 360.0 / segmentCount;

        double startOffset = -90 - (anglePerSegment / 2.0);
        double adjustedAngle = angle - startOffset;
        if (adjustedAngle < 0) {
            adjustedAngle += 360;
        }

        int hoveredIndex = (int) (adjustedAngle / anglePerSegment) % segmentCount;

        if (hoveredIndex >= 0 && hoveredIndex < segmentCount) {
            hoveredWidget = segments.get(hoveredIndex);
            for (int i = 0; i < segments.size(); i++) {
                segments.get(i).setForcedHighlight(i == hoveredIndex);
            }
        } else {
            hoveredWidget = null;
            for (RadialMenuSegmentWidget segment : segments) {
                segment.setForcedHighlight(false);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount < 0) nextPage();
        if (verticalAmount > 0) prevPage();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyBindings.PAGE_NEXT.matchesKey(keyCode, scanCode)) {
            nextPage();
            return true;
        }
        if (KeyBindings.PAGE_PREV.matchesKey(keyCode, scanCode)) {
            prevPage();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            cancelled = true;
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) { // Right click cancels
            cancelled = true;
            close();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 2) { // Middle mouse release-to-execute
            int cx = this.width / 2;
            int cy = this.height / 2;
            double dx = mouseX - cx;
            double dy = mouseY - cy;
            int baseOuter = Math.min(this.width, this.height) / 3;
            int radiusOuter = Math.max(1, (int) (baseOuter * applyEasing(getOpenT())));
            int radiusInner = (int) (radiusOuter * 0.45f);
            double distSq = dx * dx + dy * dy;

            if (distSq <= (long) radiusInner * radiusInner) {
                // 光标在中心空白区域：取消并关闭
                cancelled = true;
                close();
            } else {
                if (!cancelled && this.hoveredWidget != null) {
                    execute();
                }
                close();
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        super.close();
    }

    public void execute() {
        if (this.hoveredWidget != null) {
            this.hoveredWidget.execute();
        }
    }

    public void cancel() {
        // No action needed, just closing.
    }

    public void nextPage() {
        int total = itemsAll.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) settings.maxPerPage));

        if (totalPages > 1) {
            this.currentPage = (this.currentPage + 1) % totalPages;
            refreshScreen();
        }
    }

    public void prevPage() {
        int total = itemsAll.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) settings.maxPerPage));

        if (totalPages > 1) {
            this.currentPage = (this.currentPage - 1 + totalPages) % totalPages;
            refreshScreen();
        }
    }

    private void refreshScreen() {
        // In overlay mode, we are not the active Minecraft screen. Simply re-init to rebuild segments.
        this.init();
    }

    private void executeItem(ConfigData.CommandItem item) {
        if (item == null) return;
        if (item.isMulti() && item.subCommands != null && !item.subCommands.isEmpty()) {
            int accMs = 0;
            for (ConfigData.SubCommand sc : item.subCommands) {
                final String cmd = sc.command;
                int delay = (sc.delayMs != null) ? sc.delayMs : 0;
                accMs += Math.max(0, delay);
                int ticks = CommandScheduler.msToTicks(accMs);
                CommandScheduler.schedule(() -> CommandExecutor.execute(cmd), ticks);
            }
        } else if (item.command != null && !item.command.isEmpty()) {
            CommandScheduler.schedule(() -> CommandExecutor.execute(item.command), 0);
        }
    }

    private void drawFilledCircle(DrawContext context, int centerX, int centerY, int radius, int color) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        float[] rgba = argbToRgba(color);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

        buf.vertex(matrix, centerX, centerY, 0).color(rgba[0], rgba[1], rgba[2], rgba[3]);
        for (int i = 0; i <= 360; i += 10) { // 10 degrees per step is smooth enough
            double angle = Math.toRadians(i);
            float x = centerX + (float) (radius * Math.cos(angle));
            float y = centerY + (float) (radius * Math.sin(angle));
            buf.vertex(matrix, x, y, 0).color(rgba[0], rgba[1], rgba[2], rgba[3]);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void drawLine(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        float[] rgba = argbToRgba(color);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        buf.vertex(matrix, x1, y1, 0).color(rgba[0], rgba[1], rgba[2], rgba[3]);
        buf.vertex(matrix, x2, y2, 0).color(rgba[0], rgba[1], rgba[2], rgba[3]);

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private float[] argbToRgba(int argb) {
        return new float[]{
            ((argb >> 16) & 0xFF) / 255.0f,
            ((argb >> 8) & 0xFF) / 255.0f,
            (argb & 0xFF) / 255.0f,
            ((argb >> 24) & 0xFF) / 255.0f
        };
    }

    private float getOpenT() {
        if (settings == null || !settings.animEnabled) return 1f;
        int dur = (settings.animDurationMs > 0 && settings.animDurationMs <= 2000) ? settings.animDurationMs : 200;
        long elapsed = Util.getMeasuringTimeMs() - openStartMs;
        if (elapsed <= 0) return 0f;
        return Math.min(1f, elapsed / (float) dur);
    }

    private float applyEasing(float t) {
        if (settings == null) return easeOutCubic(t);
        String mode = settings.animEasing == null ? "easeOutCubic" : settings.animEasing;
        return switch (mode) {
            case "linear" -> t;
            case "easeOutBack" -> easeOutBack(t);
            default -> easeOutCubic(t);
        };
    }

    private float easeOutCubic(float x) {
        float inv = 1f - x;
        return 1f - inv * inv * inv;
    }

    private float easeOutBack(float x) {
        // s ~ 1.70158f standard back overshoot
        float s = 1.70158f;
        float inv = x - 1f;
        return 1f + inv * inv * ((s + 1f) * inv + s);
    }

    private int withAlpha(int argb, int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        return (a << 24) | (argb & 0x00FFFFFF);
    }
}
