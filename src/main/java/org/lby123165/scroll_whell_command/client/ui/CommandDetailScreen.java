package org.lby123165.scroll_whell_command.client.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
 
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.lby123165.scroll_whell_command.client.config.ConfigData;
import org.lby123165.scroll_whell_command.client.ui.CommandEditorScreen;
import org.lby123165.scroll_whell_command.client.ui.SubCommandListWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CommandDetailScreen extends Screen {
    private final Screen parent;
    private final ConfigData.CommandItem editing;
    private final Consumer<ConfigData.CommandItem> onDone;
    private final ModernTextRenderer modernRenderer;

    // UI components
    
    private TextFieldWidget labelField;
    private TextFieldWidget singleCommandField;
    private SubCommandListWidget subCommandListWidget;
    private TextFieldWidget subCmdField;
    private TextFieldWidget subDelayField;
    private ButtonWidget updateSubButton;
    private ButtonWidget addSubButton;
    private ButtonWidget removeSubButton;
    private ButtonWidget saveButton;

    // Data
    private List<ConfigData.SubCommand> subCommands = new ArrayList<>();
    private boolean showSubEditor = false; // 默认隐藏子命令输入区，点击“添加子命令”后显示

    // layout cache
    private int startX, labelWidth, fieldWidth, topY;
    private int singleFieldY;
    private int subListWidth;

    public CommandDetailScreen(Screen parent, ConfigData.CommandItem originalItem, Consumer<ConfigData.CommandItem> onDone) {
        super(originalItem == null || originalItem.label == null || originalItem.label.isEmpty() ? Text.translatable("screen.scroll_whell_command.add_command") : Text.translatable("screen.scroll_whell_command.edit_command"));
        this.parent = parent;
        // Create a deep copy of the item to prevent modifying the original object directly.
        this.editing = new ConfigData.CommandItem(originalItem);
        this.onDone = onDone;
        this.subCommands = new ArrayList<>(this.editing.subCommands);
        this.modernRenderer = new ModernTextRenderer();
    }

    @Override
    public void renderBackground(DrawContext context) {
        // No-op: remove vanilla dirt background
    }

    @Override
    protected void init() {
        super.init();

        // Ensure type is not null for new items
        if (this.editing.type == null) {
            this.editing.type = "single";
        }

        // Ensure execution mode is not null for new items (default to chat)
        if (this.editing.executionMode == null) {
            this.editing.executionMode = "chat";
        }

        int screenWidth = this.width;
        int screenHeight = this.height;
        int buttonHeight = 20;
        int gap = 6;
        int panelWidth = Math.min(560, screenWidth - 40);
        this.startX = (screenWidth - panelWidth) / 2;
        this.topY = 66; // below title and potential tabs area
        this.fieldWidth = 260;
        this.labelWidth = panelWidth - fieldWidth - gap;
        int y = topY;

        // Removed execution mode toggle per new requirement (always use chat behavior)

        // Label field
        this.addDrawableChild(new TranslatableTextWidget(startX, y + 5, labelWidth, buttonHeight, Text.translatable("label.scroll_whell_command.command_label"), this.textRenderer));
        this.labelField = new TextFieldWidget(this.textRenderer, startX + labelWidth + gap, y, fieldWidth, buttonHeight, Text.translatable("label.scroll_whell_command.command_label"));
        this.labelField.setMaxLength(32767);
        this.labelField.setText(this.editing.label != null ? this.editing.label : "");
        this.labelField.setChangedListener(text -> updateSaveButtonState());
        this.addDrawableChild(this.labelField);
        y += buttonHeight + gap;

        // Single command field
        this.addDrawableChild(new TranslatableTextWidget(startX, y + 5, labelWidth, buttonHeight, Text.translatable("label.scroll_whell_command.command"), this.textRenderer));
        this.singleCommandField = new TextFieldWidget(this.textRenderer, startX + labelWidth + gap, y, fieldWidth, buttonHeight, Text.translatable("label.scroll_whell_command.command"));
        this.singleCommandField.setMaxLength(256);
        this.singleCommandField.setText(this.editing.command != null ? this.editing.command : "");
        this.singleCommandField.setChangedListener(text -> updateSaveButtonState());
        this.addDrawableChild(this.singleCommandField);
        this.singleFieldY = y;
        y += buttonHeight + gap * 2;

        // Sub-command list
        int subListY = y; // will be overridden in multi-mode to singleFieldY
        int subListHeight = 140;
        this.subListWidth = labelWidth + gap + fieldWidth;
        this.subCommandListWidget = new SubCommandListWidget(this.client, subListWidth, subListHeight, subListY, 20, this::onSubCommandSelected);
        this.subCommandListWidget.setX(startX);
        this.subCommandListWidget.setEntries(subCommands);
        this.addDrawableChild(this.subCommandListWidget);

        // Bottom operation area position
        int opAreaTop = this.height - 60; // above bottom buttons

        // Sub-command fields
        this.subCmdField = new TextFieldWidget(this.textRenderer, startX, opAreaTop, subListWidth - 80 - gap, buttonHeight, Text.translatable("label.scroll_whell_command.sub_command"));
        this.subCmdField.setMaxLength(256);
        this.subCmdField.setChangedListener(text -> updateSaveButtonState());
        this.addDrawableChild(subCmdField);

        this.subDelayField = new TextFieldWidget(this.textRenderer, startX + subListWidth - 80, opAreaTop, 80, buttonHeight, Text.translatable("label.scroll_whell_command.delay_ms"));
        this.subDelayField.setMaxLength(10);
        this.subDelayField.setChangedListener(text -> updateSaveButtonState());
        this.addDrawableChild(subDelayField);

        int subButtonWidth = 70;
        this.addSubButton = ButtonWidget.builder(Text.translatable("button.scroll_whell_command.add_sub"), b -> {
            // Auto-switch to multi-mode on first sub-command add
            if (!this.editing.isMulti()) {
                this.editing.type = "multi";
                updateVisibility();
            }
            this.showSubEditor = true; // 显示子命令输入框
            updateVisibility(); // 立即刷新可见性
            addSubCommand();
        }).dimensions(startX, opAreaTop + buttonHeight + gap, subButtonWidth, buttonHeight).build();
        this.addDrawableChild(this.addSubButton);
        this.updateSubButton = ButtonWidget.builder(Text.translatable("button.scroll_whell_command.update_sub"), b -> updateSubCommand())
            .dimensions(startX + subButtonWidth + gap, opAreaTop + buttonHeight + gap, subButtonWidth, buttonHeight).build();
        this.addDrawableChild(this.updateSubButton);
        this.removeSubButton = ButtonWidget.builder(Text.translatable("button.scroll_whell_command.remove_sub"), b -> removeSubCommand())
            .dimensions(startX + (subButtonWidth + gap) * 2, opAreaTop + buttonHeight + gap, subButtonWidth, buttonHeight).build();
        this.addDrawableChild(this.removeSubButton);

        // Bottom Buttons
        int buttonY = this.height - 30;
        int buttonWidth = 80;
        this.saveButton = ButtonWidget.builder(Text.translatable("button.scroll_whell_command.save"), b -> save())
            .dimensions(this.width - 20 - buttonWidth * 2 - 4, buttonY, buttonWidth, buttonHeight).build();
        this.addDrawableChild(this.saveButton);
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), b -> close())
            .dimensions(this.width - 20 - buttonWidth, buttonY, buttonWidth, buttonHeight).build());

        updateVisibility();
        updateSaveButtonState();
    }

    private void updateSaveButtonState() {
        boolean isValid = true;
        Text tooltipMessage = null;

        if (this.labelField.getText().trim().isEmpty()) {
            isValid = false;
            tooltipMessage = Text.translatable("text.scroll_whell_command.tooltip.label_empty");
        } else if (this.editing.isMulti()) {
            if (this.subCommands.isEmpty()) {
                isValid = false;
                tooltipMessage = Text.translatable("text.scroll_whell_command.tooltip.subcommands_empty");
            }
        } else { // Single command
            if (this.singleCommandField.getText().trim().isEmpty()) {
                isValid = false;
                tooltipMessage = Text.translatable("text.scroll_whell_command.tooltip.command_empty");
            }
        }

        this.saveButton.active = isValid;
        if (tooltipMessage != null) {
            this.saveButton.setTooltip(Tooltip.of(tooltipMessage));
        } else {
            this.saveButton.setTooltip(null);
        }
    }

    private void updateVisibility() {
        boolean isMulti = "multi".equals(this.editing.type);
        singleCommandField.visible = !isMulti;
        subCommandListWidget.visible = isMulti;
        // 子命令输入框仅在多命令且用户点击“添加子命令”后显示
        subCmdField.visible = isMulti && showSubEditor;
        subDelayField.visible = isMulti && showSubEditor;
        updateSubButton.visible = isMulti;
        removeSubButton.visible = isMulti;
        addSubButton.visible = true; // 单/多模式均可见，用于触发切换或添加

        // 多命令时，将列表放置到原单命令输入框位置
        if (isMulti) {
            this.subCommandListWidget.setY(this.singleFieldY);
        }
        // Update button should only be active when an item is selected
        if (isMulti) {
            updateSubButton.active = subCommandListWidget.getSelectedOrNull() != null;
        }
        updateSaveButtonState();
    }

    private void onSubCommandSelected(SubCommandListWidget.SubCommandEntry entry) {
        if (entry != null) {
            subCmdField.setText(entry.getSubCommand().command);
            subDelayField.setText(String.valueOf(entry.getSubCommand().delayMs));
        } else {
            subCmdField.setText("");
            subDelayField.setText("");
        }
        updateVisibility(); // Re-check button active state
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        String title = this.getTitle().getString();
        modernRenderer.drawCenteredText(context, title, this.width / 2f, 15, 0xFFFFFFFF, false, 1.2f);

        // Draw centered content panel background to match unified UI style
        int panelTop = 66;
        int panelW = Math.min(560, this.width - 40);
        int panelX = (this.width - panelW) / 2;
        int panelBottom = this.height - 40;
        int bg = 0x66000000;
        int border = 0x99FFFFFF;
        context.fill(panelX, panelTop, panelX + panelW, panelBottom, bg);
        // border
        context.fill(panelX, panelTop, panelX + panelW, panelTop + 1, border);
        context.fill(panelX, panelBottom - 1, panelX + panelW, panelBottom, border);
        context.fill(panelX, panelTop, panelX + 1, panelBottom, border);
        context.fill(panelX + panelW - 1, panelTop, panelX + panelW, panelBottom, border);
    }

    @Override
    public void close() {
        // Following Fabric best practices: properly return to parent screen
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    private void addSubCommand() {
        String subText = subCmdField.getText().trim();
        if (subText.isEmpty()) {
            // 首次点击只展开编辑区域，不强制输入
            return;
        }

        // If this is the first sub-command being added to a single command, convert the original command first.
        if (subCommands.isEmpty() && "single".equalsIgnoreCase(this.editing.type) && this.editing.command != null && !this.editing.command.trim().isEmpty()) {
            ConfigData.SubCommand originalAsSub = new ConfigData.SubCommand();
            originalAsSub.command = this.editing.command.trim();
            originalAsSub.delayMs = 0; // Default delay
            subCommands.add(originalAsSub);
            this.editing.command = ""; // Clear the old single command field
        }

        ConfigData.SubCommand sc = new ConfigData.SubCommand();
        sc.command = subText;
        try {
            sc.delayMs = Integer.parseInt(subDelayField.getText().trim());
        } catch (NumberFormatException ignored) {
            sc.delayMs = 0;
        }
        
        subCommands.add(sc);
        subCommandListWidget.setEntries(subCommands);
        subCmdField.setText("");
        subDelayField.setText("");
        updateSaveButtonState();
    }

    private void updateSubCommand() {
        SubCommandListWidget.SubCommandEntry selected = subCommandListWidget.getSelectedOrNull();
        if (selected == null) return;

        String cmd = subCmdField.getText().trim();
        if (cmd.isEmpty()) return;

        selected.getSubCommand().command = cmd;
        try {
            selected.getSubCommand().delayMs = Integer.parseInt(subDelayField.getText().trim());
        } catch (NumberFormatException ignored) {
            selected.getSubCommand().delayMs = 0;
        }

        // Deselect to avoid confusion and force re-selection for next edit
        subCommandListWidget.setEntries(subCommands);
        subCommandListWidget.setSelected(null);
        updateSaveButtonState();
    }

    private void removeSubCommand() {
        SubCommandListWidget.SubCommandEntry selected = subCommandListWidget.getSelectedOrNull();
        if (selected == null) return;

        subCommands.remove(selected.getSubCommand());
        subCommandListWidget.setEntries(subCommands);

        // If only one sub-command is left, revert to single command mode.
        if (subCommands.size() == 1) {
            ConfigData.SubCommand lastSub = subCommands.get(0);
            this.editing.type = "single";
            this.editing.command = lastSub.command;
            this.singleCommandField.setText(lastSub.command);
            subCommands.clear();
            updateVisibility(); // Directly update widget visibility instead of re-initializing.
        }
        updateSaveButtonState();
    }

    private void save() {
        if (!this.saveButton.active) return; // Extra check

        // Auto-determine type based on sub-commands list
        if (this.subCommands.isEmpty()) {
            this.editing.type = "single";
        } else {
            this.editing.type = "multi";
        }

        // execution mode removed: always use chat behavior at runtime
        this.editing.label = labelField.getText().trim();
        
        if ("multi".equals(this.editing.type)) {
            this.editing.command = ""; // Clear single command if multi
            this.editing.subCommands.clear();
            for (ConfigData.SubCommand sc : subCommands) {
                this.editing.subCommands.add(sc);
            }
        } else {
            this.editing.command = singleCommandField.getText().trim();
            this.editing.subCommands.clear(); // Clear multi-command field
        }
        
        this.onDone.accept(this.editing);
        close();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // The list widget handles its own clicks, so the manual logic is no longer needed.
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
