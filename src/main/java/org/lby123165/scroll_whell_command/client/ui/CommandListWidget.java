package org.lby123165.scroll_whell_command.client.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ElementListWidget;
import org.lby123165.scroll_whell_command.client.config.ConfigData;

import java.util.List;

public class CommandListWidget extends ElementListWidget<CommandListWidget.CommandEntry> {

    private final CommandEditorScreen parentScreen;
    private final ModernTextRenderer modernRenderer;
    public boolean visible = true; // compatibility for 1.20.1/1.20.2 screens
    private int compatX = 0;
    private int topEdge;
    private int bottomEdge;

    public CommandListWidget(CommandEditorScreen parent, MinecraftClient client, int width, int height, int y, int itemHeight) {
        super(client, width, height, y, y + height, itemHeight);
        this.parentScreen = parent;
        this.modernRenderer = new ModernTextRenderer();
        this.topEdge = y;
        this.bottomEdge = y + height;
        // Avoid drawing the vanilla dirt background behind the list; the screen renders its own background
        this.setRenderBackground(false);
    }

    public void setEntries(List<ConfigData.CommandItem> items) {
        this.clearEntries();
        items.forEach(item -> this.addEntry(new CommandEntry(item)));
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
    public void setSelected(CommandEntry entry) {
        super.setSelected(entry);
        parentScreen.setSelectedItem(entry != null ? entry.getItem() : null);
    }

    @Override
    public int getRowWidth() {
        return this.width - 20;
    }

    @Override
    public int getRowLeft() {
        return this.compatX;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.visible && mouseX >= this.compatX && mouseX < this.compatX + this.width && mouseY >= this.topEdge && mouseY < this.bottomEdge;
    }

    public int getScrollbarPositionX() {
        return this.compatX + this.width - 6;
    }

    public class CommandEntry extends ElementListWidget.Entry<CommandEntry> {
        private final ConfigData.CommandItem item;
        private long lastClickTime = 0;

        public CommandEntry(ConfigData.CommandItem item) {
            this.item = item;
        }

        public ConfigData.CommandItem getItem() {
            return item;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            boolean selected = CommandListWidget.this.getSelectedOrNull() == this;
            int bg = selected ? 0x40FFFFFF : (hovered ? 0x30FFFFFF : 0x00000000);
            if (bg != 0) {
                context.fill(x, y, x + entryWidth, y + entryHeight, bg);
            }
            String label = item.label != null && !item.label.isEmpty() ? item.label : net.minecraft.text.Text.translatable("text.scroll_whell_command.unnamed").getString();
            int color = selected ? 0xFFFFFFFF : 0xFFE0E0E0;
            float textY = y + (entryHeight - 8) / 2f;
            modernRenderer.drawText(context, label, x + 6, textY, color, false, 1.0f);

            // Badge (type) on the right
            String typeKey = item.isMulti() ? "text.scroll_whell_command.type_multi" : "text.scroll_whell_command.type_single";
            String typeText = net.minecraft.text.Text.translatable(typeKey).getString();

            int paddingX = 6;
            int paddingY = 3;
            int gap = 6;

            int typeWidth = parentScreen.getTextRenderer().getWidth(typeText) + paddingX * 2;
            int badgeHeight = 12 + paddingY * 2;
            int badgeY = y + (entryHeight - badgeHeight) / 2;

            int right = x + entryWidth - 6;
            // Draw type badge (rightmost)
            int typeX = right - typeWidth;
            context.fill(typeX, badgeY, typeX + typeWidth, badgeY + badgeHeight, 0x3344FF44);
            modernRenderer.drawCenteredText(context, typeText, typeX + typeWidth / 2f, badgeY + paddingY + 1, 0xFFD8FFD8, false, 0.9f);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                this.onPressed();
                return true;
            }
            return false;
        }

        private void onPressed() {
            CommandListWidget.this.setSelected(this);
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

    // Helper for parent to select by index after reordering
    public void selectIndex(int index) {
        if (index >= 0 && index < this.getEntryCount()) {
            this.setSelected(this.getEntry(index));
        }
    }
}
