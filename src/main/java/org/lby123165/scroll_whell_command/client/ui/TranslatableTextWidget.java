package org.lby123165.scroll_whell_command.client.ui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public class TranslatableTextWidget extends ClickableWidget {
    private final TextRenderer textRenderer;
    public final String key;

    public TranslatableTextWidget(int x, int y, int width, int height, Text message, TextRenderer textRenderer) {
        super(x, y, width, height, message);
        this.textRenderer = textRenderer;
        this.key = message.getString(); // Simplified - just use the string representation
    }

    // In 1.20.2, ClickableWidget uses renderButton. Override it here.
    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.visible) {
            context.drawTextWithShadow(this.textRenderer, this.getMessage(), this.getX(), this.getY(), 0xFFFFFFFF);
        }
    }

    // Forward-compat: expose renderWidget delegating to renderButton without @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderButton(context, mouseX, mouseY, delta);
    }

    @Override
    protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
        // No narration for static text
    }
}
