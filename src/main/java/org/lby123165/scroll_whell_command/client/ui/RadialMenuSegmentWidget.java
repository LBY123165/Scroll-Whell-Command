package org.lby123165.scroll_whell_command.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.lby123165.scroll_whell_command.client.config.ConfigData;

public class RadialMenuSegmentWidget extends ClickableWidget {

    private final ConfigData.CommandItem item;
    private final double startAngle, angle;
    private final int innerRadius, outerRadius;
    private final int baseColor, highlightColor, textColor;
    private boolean forcedHighlight = false;

    public RadialMenuSegmentWidget(int x, int y, int width, int height, ConfigData.CommandItem item, double startAngle, double angle, int rInner, int rOuter, ConfigData.GlobalSettings settings) {
        super(x, y, width, height, Text.literal(item.label != null ? item.label : ""));
        this.item = item;
        this.startAngle = startAngle;
        this.angle = angle;
        this.innerRadius = rInner;
        this.outerRadius = rOuter;
        this.baseColor = settings.baseColor;
        this.highlightColor = settings.highlightColor;
        this.textColor = settings.textColor;
    }

    /**
     * Returns true if the given absolute angle (0..2PI, 0 at +X, CCW) falls within this segment's angular span.
     * Radius is ignored for this check.
     */
    public boolean containsAngle(double angle) {
        // Normalize angle to 0..2PI range consistently
        double normalizedAngle = (angle % (2 * Math.PI) + 2 * Math.PI) % (2 * Math.PI);

        // Normalize segment angles as well
        double normalizedStart = (this.startAngle % (2 * Math.PI) + 2 * Math.PI) % (2 * Math.PI);
        double normalizedEnd = (this.startAngle + this.angle) % (2 * Math.PI);

        // Handle wrap-around case where the segment crosses the 0-radian axis
        if (normalizedStart > normalizedEnd) {
            return normalizedAngle >= normalizedStart || normalizedAngle < normalizedEnd;
        } else {
            return normalizedAngle >= normalizedStart && normalizedAngle < normalizedEnd;
        }
    }

    public void setForcedHighlight(boolean v) {
        this.forcedHighlight = v;
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!this.visible) return;

        float centerX = this.getX() + this.width / 2f;
        float centerY = this.getY() + this.height / 2f;
        int segColor = this.forcedHighlight ? this.highlightColor : this.baseColor;

        // Draw the segment
        drawAnnulusSlice(context, centerX, centerY, this.innerRadius, this.outerRadius, (float) this.startAngle, (float) this.angle, segColor);

        // Draw text centered in the arc
        if (this.item != null && this.item.label != null && !this.item.label.isEmpty()) {
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            String label = this.item.label;
            int textWidth = textRenderer.getWidth(label);

            // Calculate position and rotation for the text
            float midAngle = (float) (this.startAngle + this.angle / 2f);
            float textRadius = (this.innerRadius + this.outerRadius) / 2f;

            // Use the CORRECT center point for calculation
            float textX = centerX + (float) (textRadius * Math.cos(midAngle));
            float textY = centerY + (float) (textRadius * Math.sin(midAngle));

            context.getMatrices().push();
            context.getMatrices().translate(textX, textY, 0);

            // Rotate text to align with segment, but keep it readable (not upside down)
            float rotationAngle = midAngle;
            // Normalize angle to be in range [0, 2PI]
            while(rotationAngle < 0) rotationAngle += 2 * Math.PI;
            while(rotationAngle > 2 * Math.PI) rotationAngle -= 2 * Math.PI;

            if (rotationAngle > Math.PI / 2 && rotationAngle < 3 * Math.PI / 2) {
                rotationAngle += (float) Math.PI;
            }
            context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotation(rotationAngle));

            // Draw centered text
            context.drawText(textRenderer, label, -textWidth / 2, -textRenderer.fontHeight / 2, this.textColor, true);

            context.getMatrices().pop();
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        double dx = mouseX - this.getX();
        double dy = mouseY - this.getY();
        double distSq = dx * dx + dy * dy;
        if (distSq < this.innerRadius * this.innerRadius || distSq > this.outerRadius * this.outerRadius) {
            return false;
        }
        double angle = Math.atan2(dy, dx);
        if (angle < 0) angle += Math.PI * 2;
        return containsAngle(angle);
    }

    public ConfigData.CommandItem getItem() {
        return item;
    }

    public void execute() {
        this.item.execute();
    }

    private void drawAnnulusSlice(DrawContext context, float centerX, float centerY, float innerRadius, float outerRadius, float startAngle, float sweepAngle, int color) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        int steps = Math.max(1, (int) Math.ceil(Math.toDegrees(sweepAngle) / 5.0)); // 5 degrees per step
        float stepSize = sweepAngle / steps;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        float[] rgba = argbToRgba(color);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < steps; i++) {
            float sa = startAngle + i * stepSize;
            float ea = startAngle + (i + 1) * stepSize;

            Vector2f o1 = polar(centerX, centerY, outerRadius, sa);
            Vector2f o2 = polar(centerX, centerY, outerRadius, ea);
            Vector2f i1 = polar(centerX, centerY, innerRadius, sa);
            Vector2f i2 = polar(centerX, centerY, innerRadius, ea);

            buf.vertex(matrix, o1.x, o1.y, 0).color(rgba[0], rgba[1], rgba[2], rgba[3]);
            buf.vertex(matrix, i1.x, i1.y, 0).color(rgba[0], rgba[1], rgba[2], rgba[3]);
            buf.vertex(matrix, i2.x, i2.y, 0).color(rgba[0], rgba[1], rgba[2], rgba[3]);

            buf.vertex(matrix, o1.x, o1.y, 0).color(rgba[0], rgba[1], rgba[2], rgba[3]);
            buf.vertex(matrix, i2.x, i2.y, 0).color(rgba[0], rgba[1], rgba[2], rgba[3]);
            buf.vertex(matrix, o2.x, o2.y, 0).color(rgba[0], rgba[1], rgba[2], rgba[3]);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private Vector2f polar(float cx, float cy, float r, float angle) {
        return new Vector2f(cx + (float) (Math.cos(angle) * r), cy + (float) (Math.sin(angle) * r));
    }

    private float[] argbToRgba(int argb) {
        return new float[]{
                ((argb >> 16) & 0xFF) / 255f,
                ((argb >> 8) & 0xFF) / 255f,
                (argb & 0xFF) / 255f,
                ((argb >> 24) & 0xFF) / 255f
        };
    }

    @Override
    protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }
}
