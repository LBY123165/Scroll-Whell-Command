package org.lby123165.scroll_whell_command.client.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A scrollable option list similar to Sodium's options rows, rendering label/description on the left
 * and an interactive control (slider/button/toggle) aligned on the right.
 */
public class OptionListWidget extends ElementListWidget<OptionListWidget.OptionEntry> {

    private final ModernTextRenderer renderer = new ModernTextRenderer();
    private int topEdge;
    private int bottomEdge;
    private int compatX = 0;
    public boolean visible = true; // compatibility flag for older screens

    public OptionListWidget(MinecraftClient client, int width, int height, int y, int itemHeight) {
        super(client, width, height, y, y + height, itemHeight);
        this.topEdge = y;
        this.bottomEdge = y + height;
        this.compatX = 0;
        // Do not draw vanilla dirt background
        this.setRenderBackground(false);
    }

    public void setOptions(List<OptionSpec> specs) {
        this.clearEntries();
        for (OptionSpec spec : specs) {
            this.addEntry(new OptionEntry(spec));
        }
    }

    // --- Compatibility helpers for 1.20.1/1.20.2 screens ---
    public void setX(int x) { this.compatX = x; }

    public int getX() { return this.compatX; }

    public void setY(int y) {
        int currentHeight = this.bottomEdge - this.topEdge;
        this.topEdge = y;
        this.bottomEdge = y + currentHeight;
    }

    public int getY() { return this.topEdge; }

    public void setHeight(int newHeight) { this.bottomEdge = this.topEdge + newHeight; }

    // Respect our visibility flag by gating rendering
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!this.visible) return;
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        OptionEntry entry = this.getEntryAtPosition(mouseX, mouseY);
        if (entry != null) {
            ClickableWidget ctl = entry.getControl();
            if (ctl != null && ctl.mouseClicked(mouseX, mouseY, button)) {
                clearOtherControlFocus(entry);
                this.setFocused(entry);
                try { ctl.setFocused(true); } catch (Throwable ignored) {}
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        OptionEntry focused = this.getFocused();
        if (focused != null) {
            ClickableWidget ctl = focused.getControl();
            if (ctl != null && ctl.mouseReleased(mouseX, mouseY, button)) {
                try { ctl.setFocused(false); } catch (Throwable ignored) {}
                return true;
            }
        }
        OptionEntry hovered = this.getEntryAtPosition(mouseX, mouseY);
        if (hovered != null) {
            ClickableWidget ctl = hovered.getControl();
            if (ctl != null && ctl.mouseReleased(mouseX, mouseY, button)) return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        OptionEntry focused = this.getFocused();
        if (focused != null) {
            ClickableWidget ctl = focused.getControl();
            if (ctl != null && ctl.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
        }
        OptionEntry hovered = this.getEntryAtPosition(mouseX, mouseY);
        if (hovered != null) {
            ClickableWidget ctl = hovered.getControl();
            if (ctl != null && ctl.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Try focused row first
        OptionEntry focused = this.getFocused();
        if (focused != null) {
            ClickableWidget ctl = focused.getControl();
            if (ctl != null && ctl.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        // Fallback: try hovered entry's control
        OptionEntry hovered = this.getEntryAtPosition(this.client.mouse.getX() * 1.0 / this.client.getWindow().getScaleFactor(),
                                                     this.client.mouse.getY() * 1.0 / this.client.getWindow().getScaleFactor());
        if (hovered != null) {
            ClickableWidget ctl = hovered.getControl();
            if (ctl != null && ctl.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        OptionEntry focused = this.getFocused();
        if (focused != null) {
            ClickableWidget ctl = focused.getControl();
            if (ctl != null && ctl.charTyped(chr, modifiers)) return true;
        }
        OptionEntry hovered = this.getEntryAtPosition(this.client.mouse.getX() * 1.0 / this.client.getWindow().getScaleFactor(),
                                                     this.client.mouse.getY() * 1.0 / this.client.getWindow().getScaleFactor());
        if (hovered != null) {
            ClickableWidget ctl = hovered.getControl();
            if (ctl != null && ctl.charTyped(chr, modifiers)) return true;
        }
        return super.charTyped(chr, modifiers);
    }

    // Ensure only the intended control keeps focus
    private void clearOtherControlFocus(OptionEntry keep) {
        for (OptionEntry e : this.children()) {
            if (e != keep) {
                ClickableWidget other = e.getControl();
                if (other != null) {
                    try { other.setFocused(false); } catch (Throwable ignored) {}
                }
            }
        }
    }

    // Controls are already laid out during render(). Keep this as a safe no-op.
    private void layoutControlForIndex(ClickableWidget ctl, int index) {
        // Intentionally left blank; rely on last render() positioning to avoid desync.
    }

    public void clearMyEntries() {
        this.clearEntries();
    }

    @Override
    public int getRowWidth() {
        // Leave some inner padding
        return this.width - 12;
    }

    @Override
    public int getRowLeft() {
        // Respect the explicit X set from the parent screen
        return this.compatX;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.visible && mouseX >= this.compatX && mouseX < this.compatX + this.width && mouseY >= this.topEdge && mouseY < this.bottomEdge;
    }

    // Not all versions expose this as an overridable method; keep it public without @Override
    public int getScrollbarPositionX() {
        // Align scrollbar to the right edge of this widget's bounds
        return this.compatX + this.width - 6;
    }

    public static class OptionSpec {
        public final Text label;
        public final Text description; // can be Text.empty()
        public final Supplier<ClickableWidget> controlSupplier;

        public OptionSpec(Text label, Text description, Supplier<ClickableWidget> controlSupplier) {
            this.label = label;
            this.description = description == null ? Text.empty() : description;
            this.controlSupplier = controlSupplier;
        }
    }

    public class OptionEntry extends ElementListWidget.Entry<OptionEntry> {
        private final OptionSpec spec;
        private final ClickableWidget control;

        public OptionEntry(OptionSpec spec) {
            this.spec = spec;
            this.control = spec.controlSupplier.get();
        }

        public ClickableWidget getControl() {
            return control;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int padding = 6;
            int controlW = Math.min(160, Math.max(100, entryWidth / 3));
            int controlH = entryHeight - 8;

            // Position control on the right
            int controlX = x + entryWidth - controlW - padding;
            int controlY = y + (entryHeight - controlH) / 2;

            // Try to position the control widget
            try {
                this.control.setWidth(controlW);
                this.control.setX(controlX);
                this.control.setY(controlY);
            } catch (Throwable ignored) {}

            // Draw label and optional description
            renderer.drawText(context, spec.label.getString(), x + padding, y + 6, 0xFFFFFF, false, 1.0f);
            if (!spec.description.getString().isEmpty()) {
                renderer.drawText(context, spec.description.getString(), x + padding, y + 6 + 10, 0xAAAAAA, false, 0.9f);
            }

            // Ensure control is added to screen children (handled via children())
            this.control.render(context, mouseX, mouseY, tickDelta);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.control.mouseClicked(mouseX, mouseY, button)) return true;
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (this.control.mouseReleased(mouseX, mouseY, button)) return true;
            return super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            if (this.control.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
            if (this.control.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) return true;
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return List.of(control);
        }

        @Override
        public List<? extends Element> children() {
            return List.of(control);
        }
    }
}
