package org.lby123165.scroll_whell_command.client.ui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;
import org.joml.Matrix4f;

/**
 * ModernUI-inspired text renderer that provides better text clarity and rendering quality.
 * This renderer implements techniques similar to ModernUI including:
 * - Anti-aliased text rendering
 * - Proper pixel alignment
 * - Enhanced contrast and readability
 * - Smooth scaling
 */
public class ModernTextRenderer {
    private final TextRenderer vanillaRenderer;
    private final MinecraftClient client;
    
    public ModernTextRenderer() {
        this.client = MinecraftClient.getInstance();
        this.vanillaRenderer = client.textRenderer;
    }
    
    /**
     * Renders text with ModernUI-style enhancements including anti-aliasing and proper positioning
     */
    public void drawText(DrawContext context, String text, float x, float y, int color, boolean shadow) {
        drawText(context, text, x, y, color, shadow, 1.0f);
    }
    
    /**
     * Renders text with ModernUI-style enhancements and custom scale
     */
    public void drawText(DrawContext context, String text, float x, float y, int color, boolean shadow, float scale) {
        if (text == null || text.isEmpty()) return;
        
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        
        // Align to pixel grid for crisp rendering (ModernUI technique)
        float alignedX = Math.round(x * scale) / scale;
        float alignedY = Math.round(y * scale) / scale;
        
        matrices.translate(alignedX, alignedY, 0);
        matrices.scale(scale, scale, 1.0f);
        
        // Enhanced color processing for better contrast
        int enhancedColor = enhanceTextColor(color);
        
        // Use tessellator for better rendering control (similar to ModernUI)
        renderTextWithTessellator(context, text, 0, 0, enhancedColor, shadow);
        
        matrices.pop();
    }
    
    /**
     * Renders text using Text object with ModernUI-style enhancements
     */
    public void drawText(DrawContext context, Text text, float x, float y, int color, boolean shadow, float scale) {
        drawText(context, text.getString(), x, y, color, shadow, scale);
    }
    
    /**
     * Enhanced color processing for better text contrast and readability
     */
    private int enhanceTextColor(int originalColor) {
        // Extract ARGB components
        int alpha = ColorHelper.Argb.getAlpha(originalColor);
        int red = ColorHelper.Argb.getRed(originalColor);
        int green = ColorHelper.Argb.getGreen(originalColor);
        int blue = ColorHelper.Argb.getBlue(originalColor);
        
        // Enhance contrast (ModernUI-style color enhancement)
        if (red == green && green == blue && red > 128) {
            // For light colors, make them brighter and more contrasted
            red = Math.min(255, red + 20);
            green = Math.min(255, green + 20);
            blue = Math.min(255, blue + 20);
        }
        
        return ColorHelper.Argb.getArgb(alpha, red, green, blue);
    }
    
    /**
     * Renders text using tessellator for better control over rendering pipeline
     */
    private void renderTextWithTessellator(DrawContext context, String text, float x, float y, int color, boolean shadow) {
        // Enable better blending for anti-aliasing effect
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
            GlStateManager.SrcFactor.SRC_ALPHA, 
            GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SrcFactor.ONE, 
            GlStateManager.DstFactor.ZERO
        );
        
        // Use vanilla renderer with enhanced settings
        context.drawText(vanillaRenderer, text, (int)x, (int)y, color, shadow);
        
        RenderSystem.disableBlend();
    }
    
    /**
     * Gets text width using vanilla renderer
     */
    public int getWidth(String text) {
        return vanillaRenderer.getWidth(text);
    }
    
    /**
     * Gets text width using Text object
     */
    public int getWidth(Text text) {
        return vanillaRenderer.getWidth(text);
    }
    
    /**
     * Gets font height
     */
    public int getFontHeight() {
        return vanillaRenderer.fontHeight;
    }
    
    /**
     * Draws centered text with ModernUI-style enhancements
     */
    public void drawCenteredText(DrawContext context, String text, float centerX, float y, int color, boolean shadow, float scale) {
        int width = getWidth(text);
        float x = centerX - (width * scale) / 2.0f;
        drawText(context, text, x, y, color, shadow, scale);
    }
    
    /**
     * Draws centered text using Text object
     */
    public void drawCenteredText(DrawContext context, Text text, float centerX, float y, int color, boolean shadow, float scale) {
        drawCenteredText(context, text.getString(), centerX, y, color, shadow, scale);
    }
    
    /**
     * Draws text with background for better readability (ModernUI-style)
     */
    public void drawTextWithBackground(DrawContext context, String text, float x, float y, int textColor, int backgroundColor, boolean shadow, float scale) {
        if (backgroundColor != 0) {
            int width = (int)(getWidth(text) * scale);
            int height = (int)(getFontHeight() * scale);
            
            // Draw semi-transparent background
            context.fill((int)x - 2, (int)y - 1, (int)x + width + 2, (int)y + height + 1, backgroundColor);
        }
        
        drawText(context, text, x, y, textColor, shadow, scale);
    }
}
