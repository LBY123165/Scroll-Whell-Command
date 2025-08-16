package org.lby123165.scroll_whell_command.client.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ElementListWidget;
import org.lby123165.scroll_whell_command.client.config.ConfigData;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class SubCommandListWidget extends ElementListWidget<SubCommandListWidget.SubCommandEntry> {

    private final ModernTextRenderer modernRenderer;
    private final Consumer<SubCommandEntry> onSelectionChanged;
    public boolean visible = true; // compatibility flag
    private int compatX = 0;
    private int topEdge;
    private int bottomEdge;

    public SubCommandListWidget(MinecraftClient client, int width, int height, int y, int itemHeight, Consumer<SubCommandEntry> onSelectionChanged) {
        super(client, width, height, y, y + height, itemHeight);
        this.centerListVertically = false;
        this.modernRenderer = new ModernTextRenderer();
        this.onSelectionChanged = onSelectionChanged;
        this.topEdge = y;
        this.bottomEdge = y + height;
        // Avoid drawing the screen's dirt background behind the list; we manage panel backgrounds ourselves
        this.setRenderBackground(false);
    }

    public void setEntries(List<ConfigData.SubCommand> subCommands) {
        this.clearEntries();
        subCommands.forEach(sc -> this.addEntry(new SubCommandEntry(this, sc)));
    }

    // --- Compatibility helpers for 1.20.1/1.20.2 screens ---
    public void setX(int x) {
        this.compatX = x;
        // Ensure ElementListWidget uses our panel bounds for rendering & scissor
        this.left = x;
        this.right = x + this.width;
    }
    public int getX() { return this.compatX; }
    public void setY(int y) {
        int currentHeight = this.bottomEdge - this.topEdge;
        this.topEdge = y;
        this.bottomEdge = y + currentHeight;
        this.top = y;
        this.bottom = y + currentHeight;
    }
    public int getY() { return this.topEdge; }
    public void setHeight(int newHeight) {
        this.bottomEdge = this.topEdge + newHeight;
        this.bottom = this.top + newHeight;
    }

    // Allow parent to adjust width during layout recalculations
    public void setWidthCompat(int newWidth) {
        this.width = newWidth;
        this.right = this.compatX + newWidth;
    }

    // Respect our visibility flag by gating rendering
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!this.visible) return;
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void setSelected(SubCommandEntry entry) {
        super.setSelected(entry);
        if (onSelectionChanged != null) {
            onSelectionChanged.accept(entry);
        }
    }

    @Override
    public int getRowWidth() {
        return this.width - 20;
    }

    @Override
    public int getRowLeft() { return this.compatX; }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.visible && mouseX >= this.compatX && mouseX < this.compatX + this.width && mouseY >= this.topEdge && mouseY < this.bottomEdge;
    }

    public int getScrollbarPositionX() { return this.compatX + this.width - 6; }

    // Helper for parent to select by index after rebuild/reflow
    public void selectIndex(int index) {
        if (index >= 0 && index < this.getEntryCount()) {
            this.setSelected(this.getEntry(index));
        }
    }

    public class SubCommandEntry extends ElementListWidget.Entry<SubCommandEntry> {
        private final SubCommandListWidget parent;
        private final ConfigData.SubCommand subCommand;

        public SubCommandEntry(SubCommandListWidget parent, ConfigData.SubCommand subCommand) {
            this.parent = parent;
            this.subCommand = subCommand;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            String commandText = subCommand.command != null ? subCommand.command : "";
            String delayText = "(" + (subCommand.delayMs != null ? subCommand.delayMs : 0) + "ms)";
            String fullText = commandText + " " + delayText;
            parent.modernRenderer.drawText(context, fullText, x + 5, y + (entryHeight - 8) / 2f, 0xFFFFFF, false, 1.0f);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                this.parent.setSelected(this);
                return true;
            }
            return false;
        }

        public ConfigData.SubCommand getSubCommand() {
            return this.subCommand;
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return List.of();
        }

        @Override
        public List<? extends Element> children() {
            return List.of();
        }
    }
}
