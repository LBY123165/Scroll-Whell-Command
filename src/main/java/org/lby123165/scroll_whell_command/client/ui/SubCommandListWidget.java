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

    public SubCommandListWidget(MinecraftClient client, int width, int height, int y, int itemHeight, Consumer<SubCommandEntry> onSelectionChanged) {
        super(client, width, height, y, itemHeight);
        this.centerListVertically = false;
        this.modernRenderer = new ModernTextRenderer();
        this.onSelectionChanged = onSelectionChanged;
    }

    public void setEntries(List<ConfigData.SubCommand> subCommands) {
        this.clearEntries();
        subCommands.forEach(sc -> this.addEntry(new SubCommandEntry(this, sc)));
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
