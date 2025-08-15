package org.lby123165.scroll_whell_command.client.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.IntConsumer;

public class ColorPickerScreen extends Screen {
    private final Screen parent;
    private final IntConsumer onPick;
    private final ModernTextRenderer modernRenderer;

    private enum Section { RGBA, HSB }
    private Section currentSection = Section.HSB;
    private enum Mode { PALETTE, BARS }
    private Mode currentMode = Mode.PALETTE;

    private int a, r, g, b;
    private double h, s, v; // H:0-360, S/V:0-1
    private int initialColor;
    
    // UI elements
    private TextFieldWidget hexInputField;
    private ButtonWidget resetButton;
    private ButtonWidget paletteModeButton;
    private ButtonWidget barsModeButton;
    private ClickableWidget[] presetButtons;

    // Palette and Hue bar geometry
    private int contentX, contentY, contentW;
    private int paletteX, paletteY, paletteW, paletteH;
    private int hueX, hueY, hueW, hueH;
    private boolean draggingPalette = false;
    private boolean draggingHue = false;
    // Alpha bar (palette mode)
    private int alphaX, alphaY, alphaW, alphaH;
    private boolean draggingAlpha = false;
    // Bars mode
    private int barsX, barsY, barsW, barsH;
    private int draggingBar = -1; // 0=R,1=G,2=B

    public ColorPickerScreen(Screen parent, int initialArgb, IntConsumer onPick) {
        super(Text.translatable("screen.scroll_whell_command.color_picker"));
        this.parent = parent;
        this.onPick = onPick;
        this.modernRenderer = new ModernTextRenderer();
        this.initialColor = initialArgb;
        this.a = (initialArgb >>> 24) & 0xFF;
        this.r = (initialArgb >>> 16) & 0xFF;
        this.g = (initialArgb >>> 8) & 0xFF;
        this.b = (initialArgb) & 0xFF;
        rgbToHsv();
    }

    @Override
    protected void init() {
        super.init();
        this.clearChildren();

        int gap = 8;
        int topY = 40;
        // Centered layout
        contentW = Math.min(540, this.width - 40);
        contentX = (this.width - contentW) / 2;
        contentY = topY;

        // Mode toggle buttons (centered above content)
        int toggleW = 80, toggleH = 20;
        int toggleY = contentY;
        int toggleGap = 6;
        int togglesTotalW = toggleW * 2 + toggleGap;
        int toggleX = contentX + (contentW - togglesTotalW) / 2;
        paletteModeButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("text.scroll_whell_command.color_mode_palette"), b -> {
            currentMode = Mode.PALETTE;
        }).dimensions(toggleX, toggleY, toggleW, toggleH).build());
        barsModeButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("text.scroll_whell_command.color_mode_bars"), b -> {
            currentMode = Mode.BARS;
        }).dimensions(toggleX + toggleW + toggleGap, toggleY, toggleW, toggleH).build());

        // Geometry for palette and hue bar
        paletteW = Math.min(240, contentW - 200);
        paletteH = paletteW; // square
        paletteX = contentX + 20;
        paletteY = toggleY + toggleH + 12;

        hueW = paletteW;
        hueH = 12;
        hueX = paletteX;
        hueY = paletteY + paletteH + gap;
        // Alpha bar under hue in palette mode
        alphaW = paletteW;
        alphaH = 12;
        alphaX = paletteX;
        alphaY = hueY + hueH + gap;

        // Bars geometry (3 stacked bars)
        barsW = paletteW;
        barsH = 12;
        barsX = paletteX;
        barsY = paletteY + 10; // will be used when in bars mode

        // Preset swatches (define before reset so we can use default)
        int[] presets = new int[]{
                0xFFFFFFFF, 0xFF000000, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF,
                0xFFFFFF00, 0xFF00FFFF, 0xFFFF00FF, 0xFF808080, 0xFF202020
        };

        // Reset to default preset (first preset) instead of user's current value
        resetButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("controls.reset"), b -> {
            setColor(presets[0]);
            // keep hex synced
            if (this.hexInputField != null) this.hexInputField.setText(String.format("#%08X", compose()));
        }).dimensions(contentX + contentW/2 - 40, alphaY + alphaH + gap, 80, 20).build());

        // Preset swatches
        presetButtons = new ClickableWidget[presets.length];
        int sw = 20, sh = 20;
        int startX = contentX + (contentW - (5 * (sw + 6) - 6)) / 2; // center 5 columns
        int startY = alphaY + alphaH + gap + 26;
        for (int i = 0; i < presets.length; i++) {
            final int col = presets[i];
            int px = startX + (i % 5) * (sw + 6);
            int py = startY + (i / 5) * (sh + 6);
            presetButtons[i] = this.addDrawableChild(new ColorSwatchWidget(px, py, sw, sh, col, () -> {
                setColor(col);
                if (this.hexInputField != null) this.hexInputField.setText(String.format("#%08X", compose()));
            }));
        }

        // --- Hex Input Field --- //
        int boxW = 160;
        int previewX = contentX + contentW - boxW - 20;
        int previewAreaY = paletteY + 20; // Area for preview box + input field
        this.hexInputField = new TextFieldWidget(this.textRenderer, previewX, previewAreaY + 64, boxW, 18, Text.empty());
        this.hexInputField.setMaxLength(9);
        this.hexInputField.setChangedListener(this::onHexColorChanged);
        this.addDrawableChild(this.hexInputField);

        // --- Bottom Buttons --- //
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> {
            if (onPick != null) onPick.accept(compose());
            close();
        }).dimensions(this.width / 2 - 105, this.height - 30, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> close())
                .dimensions(this.width / 2 + 5, this.height - 30, 100, 20).build());
    }

    private void switchSection(Section section) {
        if (this.currentSection != section) {
            this.currentSection = section;
        }
    }

    private void refreshOptions() {
        // no-op after refactor; we keep method for compatibility
    }

    // Removed OptionListWidget-based sliders

    // Removed OptionListWidget-based sliders

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);
        modernRenderer.drawCenteredText(ctx, this.getTitle().getString(), this.width / 2f, 15, 0xFFFFFFFF, false, 1.2f);

        // --- Mode indicator styling --- //
        // (Optional visual feedback can be added later)

        // --- Render Content by Mode --- //
        if (currentMode == Mode.PALETTE) {
            // --- Render Palette (S-V) --- //
            int hueColor = hsvToRgbPreview(h, 1.0, 1.0) | 0xFF000000;
            // Fill base with hue
            ctx.fill(paletteX, paletteY, paletteX + paletteW, paletteY + paletteH, hueColor);
            // White to transparent overlay (simulate saturation)
            for (int i = 0; i < paletteW; i++) {
                float t = i / (float) (paletteW - 1);
                int alpha = (int) ((1.0 - t) * 255);
                int col = (alpha << 24) | 0x00FFFFFF;
                ctx.fill(paletteX + i, paletteY, paletteX + i + 1, paletteY + paletteH, col);
            }
            // Black gradient overlay for value
            for (int j = 0; j < paletteH; j++) {
                float t = j / (float) (paletteH - 1);
                int alpha = (int) (t * 255);
                int col = (alpha << 24) | 0x00000000;
                ctx.fill(paletteX, paletteY + j, paletteX + paletteW, paletteY + j + 1, col);
            }
            // Cursor on palette
            int cx = paletteX + (int) Math.round(s * (paletteW - 1));
            int cy = paletteY + (int) Math.round((1.0 - v) * (paletteH - 1));
            drawCrosshair(ctx, cx, cy, 0xFFFFFFFF);

            // --- Render Hue Bar --- //
            for (int i = 0; i < hueW; i++) {
                double hh = (i / (double) (hueW - 1)) * 360.0;
                int col = hsvToRgbPreview(hh, 1.0, 1.0) | 0xFF000000;
                ctx.fill(hueX + i, hueY, hueX + i + 1, hueY + hueH, col);
            }
            // Hue cursor
            int hx = hueX + (int) Math.round((h / 360.0) * (hueW - 1));
            ctx.fill(hx - 1, hueY - 2, hx + 1, hueY + hueH + 2, 0xFFFFFFFF);

            // --- Render Alpha Bar --- //
            renderAlphaBar(ctx, alphaX, alphaY, alphaW, alphaH);
            // Alpha cursor
            int ax = alphaX + (int) Math.round((a / 255.0) * (alphaW - 1));
            ctx.fill(ax - 1, alphaY - 2, ax + 1, alphaY + alphaH + 2, 0xFFFFFFFF);
        } else {
            // --- Render RGB Bars --- //
            int by = paletteY; // start vertically aligned with palette area
            renderBar(ctx, barsX, by, barsW, barsH, 'R'); by += barsH + 8;
            renderBar(ctx, barsX, by, barsW, barsH, 'G'); by += barsH + 8;
            renderBar(ctx, barsX, by, barsW, barsH, 'B'); by += barsH + 8;
            renderAlphaBar(ctx, barsX, by, barsW, barsH);
        }

        // --- Render Preview Box --- //
        int previewColor = compose();
        int boxW = 120, boxH = 60;
        int boxX = contentX + contentW - boxW - 20;
        int boxY = paletteY;
        ctx.fill(boxX, boxY, boxX + boxW, boxY + boxH, previewColor);

        if (!this.hexInputField.isFocused()) {
            this.hexInputField.setText(String.format("#%08X", previewColor));
        }
    }

    private void onHexColorChanged(String hex) {
        if (!hex.startsWith("#")) return;
        hex = hex.substring(1);
        if (hex.length() != 6 && hex.length() != 8) return;

        try {
            long val = Long.parseLong(hex, 16);
            if (hex.length() == 6) { // RGB
                val = 0xFF000000 | val;
            }
            setColor((int) val);
            // UI will reflect in next render
        } catch (NumberFormatException ignored) {
            // Ignore invalid hex
        }
    }

    private void rgbToHsv() {
        double rf = r / 255.0, gf = g / 255.0, bf = b / 255.0;
        double max = Math.max(rf, Math.max(gf, bf));
        double min = Math.min(rf, Math.min(gf, bf));
        double delta = max - min;
        double hh;
        if (delta == 0) hh = 0;
        else if (max == rf) hh = 60 * (((gf - bf) / delta) % 6);
        else if (max == gf) hh = 60 * (((bf - rf) / delta) + 2);
        else hh = 60 * (((rf - gf) / delta) + 4);
        if (hh < 0) hh += 360;
        double ss = (max == 0) ? 0 : (delta / max);
        h = hh; s = ss; v = max;
    }

    private void hsvToRgb() {
        double c = v * s;
        double x = c * (1 - Math.abs(((h / 60.0) % 2) - 1));
        double m = v - c;
        double rf=0, gf=0, bf=0;
        if (h < 60) { rf = c; gf = x; bf = 0; }
        else if (h < 120) { rf = x; gf = c; bf = 0; }
        else if (h < 180) { rf = 0; gf = c; bf = x; }
        else if (h < 240) { rf = 0; gf = x; bf = c; }
        else if (h < 300) { rf = x; gf = 0; bf = c; }
        else { rf = c; gf = 0; bf = x; }
        r = (int) Math.round((rf + m) * 255.0);
        g = (int) Math.round((gf + m) * 255.0);
        b = (int) Math.round((bf + m) * 255.0);
    }

    private int hToInt() { return (int) Math.round(h); }
    private int sToInt() { return (int) Math.round(s * 100.0); }
    private int vToInt() { return (int) Math.round(v * 100.0); }

    private void setH(double nh) { h = Math.max(0, Math.min(360, nh)); }
    private void setS(double ns) { s = Math.max(0, Math.min(1, ns)); }
    private void setV(double nv) { v = Math.max(0, Math.min(1, nv)); }

    private void setColor(int color) {
        a = (color >>> 24) & 0xFF;
        r = (color >>> 16) & 0xFF;
        g = (color >>> 8) & 0xFF;
        b = (color) & 0xFF;
        rgbToHsv();
    }

    private int compose() {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    private int hsvToRgbPreview(double hh, double ss, double vv) {
        double c = vv * ss;
        double x = c * (1 - Math.abs(((hh / 60.0) % 2) - 1));
        double m = vv - c;
        double rf=0, gf=0, bf=0;
        if (hh < 60) { rf = c; gf = x; bf = 0; }
        else if (hh < 120) { rf = x; gf = c; bf = 0; }
        else if (hh < 180) { rf = 0; gf = c; bf = x; }
        else if (hh < 240) { rf = 0; gf = x; bf = c; }
        else if (hh < 300) { rf = x; gf = 0; bf = c; }
        else { rf = c; gf = 0; bf = x; }
        int rr = (int) Math.round((rf + m) * 255.0);
        int gg = (int) Math.round((gf + m) * 255.0);
        int bb = (int) Math.round((bf + m) * 255.0);
        return (rr << 16) | (gg << 8) | bb;
    }

    private void drawCrosshair(DrawContext ctx, int x, int y, int color) {
        ctx.fill(x - 5, y, x + 6, y + 1, color);
        ctx.fill(x, y - 5, x + 1, y + 6, color);
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (currentMode == Mode.PALETTE) {
                if (isInRect(mouseX, mouseY, paletteX, paletteY, paletteW, paletteH)) {
                    draggingPalette = true;
                    updateSVFromMouse(mouseX, mouseY);
                    return true;
                }
                if (isInRect(mouseX, mouseY, hueX, hueY, hueW, hueH)) {
                    draggingHue = true;
                    updateHueFromMouse(mouseX);
                    return true;
                }
                if (isInRect(mouseX, mouseY, alphaX, alphaY, alphaW, alphaH)) {
                    draggingAlpha = true;
                    updateAlphaFromMouse(mouseX);
                    return true;
                }
            } else {
                // Bars mode
                int by = paletteY;
                for (int i = 0; i < 4; i++) { // include alpha as index 3
                    if (isInRect(mouseX, mouseY, barsX, by, barsW, barsH)) {
                        draggingBar = i;
                        updateBarFromMouse(i, mouseX);
                        return true;
                    }
                    by += barsH + 8;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (currentMode == Mode.PALETTE) {
            if (draggingPalette) {
                updateSVFromMouse(mouseX, mouseY);
                return true;
            }
            if (draggingHue) {
                updateHueFromMouse(mouseX);
                return true;
            }
            if (draggingAlpha) {
                updateAlphaFromMouse(mouseX);
                return true;
            }
        } else {
            if (draggingBar >= 0) {
                updateBarFromMouse(draggingBar, mouseX);
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingPalette = false;
            draggingHue = false;
            draggingAlpha = false;
            draggingBar = -1;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isInRect(double x, double y, int rx, int ry, int rw, int rh) {
        return x >= rx && y >= ry && x < rx + rw && y < ry + rh;
    }

    private void updateSVFromMouse(double mouseX, double mouseY) {
        double nx = (mouseX - paletteX) / (double) (paletteW - 1);
        double ny = (mouseY - paletteY) / (double) (paletteH - 1);
        nx = Math.max(0.0, Math.min(1.0, nx));
        ny = Math.max(0.0, Math.min(1.0, ny));
        s = nx;
        v = 1.0 - ny;
        hsvToRgb();
        if (this.hexInputField != null && !this.hexInputField.isFocused()) {
            this.hexInputField.setText(String.format("#%08X", compose()));
        }
    }

    private void updateHueFromMouse(double mouseX) {
        double nx = (mouseX - hueX) / (double) (hueW - 1);
        nx = Math.max(0.0, Math.min(1.0, nx));
        h = nx * 360.0;
        hsvToRgb();
        if (this.hexInputField != null && !this.hexInputField.isFocused()) {
            this.hexInputField.setText(String.format("#%08X", compose()));
        }
    }

    private void updateAlphaFromMouse(double mouseX) {
        double nx = (mouseX - alphaX) / (double) (alphaW - 1);
        nx = Math.max(0.0, Math.min(1.0, nx));
        a = (int) Math.round(nx * 255.0);
        if (this.hexInputField != null && !this.hexInputField.isFocused()) {
            this.hexInputField.setText(String.format("#%08X", compose()));
        }
    }

    private void renderBar(DrawContext ctx, int x, int y, int w, int hgt, char comp) {
        for (int i = 0; i < w; i++) {
            int val = (int) Math.round((i / (double) (w - 1)) * 255.0);
            int rr = (comp == 'R') ? val : r;
            int gg = (comp == 'G') ? val : g;
            int bb = (comp == 'B') ? val : b;
            int col = 0xFF000000 | (rr << 16) | (gg << 8) | bb;
            ctx.fill(x + i, y, x + i + 1, y + hgt, col);
        }
        int curVal = (comp == 'R') ? r : (comp == 'G') ? g : b;
        int cx = x + (int) Math.round((curVal / 255.0) * (w - 1));
        ctx.fill(cx - 1, y - 2, cx + 1, y + hgt + 2, 0xFFFFFFFF);
    }

    private void updateBarFromMouse(int index, double mouseX) {
        double nx = (mouseX - barsX) / (double) (barsW - 1);
        nx = Math.max(0.0, Math.min(1.0, nx));
        int val = (int) Math.round(nx * 255.0);
        if (index == 0) r = val;
        else if (index == 1) g = val;
        else if (index == 2) b = val;
        else a = val;
        rgbToHsv();
        if (this.hexInputField != null && !this.hexInputField.isFocused()) {
            this.hexInputField.setText(String.format("#%08X", compose()));
        }
    }

    private void renderAlphaBar(DrawContext ctx, int x, int y, int w, int hgt) {
        // checkerboard background
        int size = 4;
        for (int yy = 0; yy < hgt; yy += size) {
            for (int xx = 0; xx < w; xx += size) {
                boolean dark = ((xx / size) + (yy / size)) % 2 == 0;
                int bg = dark ? 0xFFB0B0B0 : 0xFFFFFFFF;
                ctx.fill(x + xx, y + yy, x + Math.min(xx + size, w), y + Math.min(yy + size, hgt), bg);
            }
        }
        // overlay gradient from 0->255 alpha with current RGB
        for (int i = 0; i < w; i++) {
            int val = (int) Math.round((i / (double) (w - 1)) * 255.0);
            int col = (val << 24) | (r << 16) | (g << 8) | b;
            ctx.fill(x + i, y, x + i + 1, y + hgt, col);
        }
    }

    // Simple color swatch clickable widget
    private static class ColorSwatchWidget extends ClickableWidget {
        private final int color;
        private final Runnable onClick;
        public ColorSwatchWidget(int x, int y, int width, int height, int color, Runnable onClick) {
            super(x, y, width, height, Text.empty());
            this.color = color;
            this.onClick = onClick;
        }
        @Override
        public void onClick(double mouseX, double mouseY) {
            if (onClick != null) onClick.run();
        }
        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            ctx.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), color);
            int border = isHovered() ? 0xFFFFFFFF : 0xFF000000;
            // border
            ctx.fill(getX(), getY(), getX() + getWidth(), getY() + 1, border);
            ctx.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), border);
            ctx.fill(getX(), getY(), getX() + 1, getY() + getHeight(), border);
            ctx.fill(getX() + getWidth() - 1, getY(), getX() + getWidth(), getY() + getHeight(), border);
        }
        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            // no narration for simple color swatch
        }
    }
}
