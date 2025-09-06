package org.lby123165.scroll_whell_command.client.ui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
 
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.lby123165.scroll_whell_command.client.config.ConfigData;
import org.lby123165.scroll_whell_command.client.config.ConfigManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandEditorScreen extends Screen {
    private final Screen parent;
    private final ModernTextRenderer modernRenderer;

    private List<ConfigData.CommandItem> items = new ArrayList<>();
    private CommandListWidget commandListWidget;
    private boolean loadedOnce = false;

    // Right-side detail panel widgets
    private TextFieldWidget labelField;
    
    private TextFieldWidget singleCommandField;
    // Removed update/delete sub-command buttons per new UX
    private ButtonWidget rightAddButton;
    private ButtonWidget rightUpdateButton;
    private ButtonWidget rightDeleteButton;
    private ButtonWidget rightUpButton;
    private ButtonWidget rightDownButton;
    private TextFieldWidget subCommandInput;
    private TextFieldWidget delayInput;
    private SubCommandListWidget subCommandListWidget;
    private net.minecraft.client.gui.widget.TextWidget delayUnitLabel;
    private net.minecraft.client.gui.widget.MultilineTextWidget hintWidget;
    // UX state: 默认隐藏子命令编辑区，点击“添加子命令”后显示
    private boolean showSubEditor = false;
    // Layout cache
    private int rightPanelX;
    private int rightPanelY;
    private int rightPanelW;
    private int rightPanelH;
    private boolean verticalLayout = false;
    private int singleFieldY;
    private int subEditorTop; // Y of subcommand input row top, used to size list above it
    // Dynamic metrics
    private int fieldHeight = 20;
    private int buttonHeight = 20;
    private int vGap = 6;
    private String lastSelectedId = null;

    // 预留底部全局按钮区域（“完成/设置”这一行）高度
    private int bottomBarsReserve() { return 30 + vGap; } // 与 buttonY = height - 30 保持一致

    private ConfigData.CommandItem selectedItem;

    public CommandEditorScreen(Screen parent) {
        super(Text.translatable("screen.scroll_whell_command.command_editor"));
        this.parent = parent;
        this.modernRenderer = new ModernTextRenderer();
    }

    @Override
    public void renderBackground(DrawContext context) {
        // No-op to fully remove the vanilla dirt background
    }

    @Override
    public boolean shouldPause() {
        // Keep world rendering; avoids vanilla paused dirt background
        return false;
    }

    @Override
    protected void init() {
        super.init();
        this.clearChildren();

        if (!loadedOnce) {
            ConfigData cfg = ConfigManager.get();
            this.items = new ArrayList<>(cfg.commands);
            loadedOnce = true;
        }

        int listX = 20;
        int listY = 32;
        int margin = 20;
        int gap = 12;
        int availableW = this.width - margin * 2;
        // Switch to vertical layout if width is too small for two columns
        verticalLayout = availableW < 700; // breakpoint

        int listWidth;
        int listHeight;
        int actionButtonY;
        if (!verticalLayout) {
            // Horizontal: list on the left, details on the right
            listWidth = Math.min(360, Math.max(220, (int)(availableW * 0.42)));
            // Reserve space for left action buttons and bottom bar to prevent overlap
            int leftActionHeight = buttonHeight; // one row of buttons on the left list
            int listBottomReserve = bottomBarsReserve() + leftActionHeight + vGap;
            listHeight = Math.max(40, this.height - listY - listBottomReserve);
            this.commandListWidget = new CommandListWidget(this, this.client, listWidth, listHeight, listY, 24);
            this.commandListWidget.setX(listX);
            this.commandListWidget.setEntries(this.items);
            this.addDrawableChild(this.commandListWidget);

            // Right-side detail panel
            int panelX = listX + listWidth + gap;
            int panelWidth = Math.max(200, this.width - panelX - margin);
            this.rightPanelH = Math.max(100, this.height - listY - bottomBarsReserve());
            initDetailPanel(panelX, listY, panelWidth);

            // Left action buttons above bottom bar
            actionButtonY = this.height - bottomBarsReserve() - buttonHeight;
        } else {
            // Vertical: list on top, details below
            listWidth = availableW;
            // Give half of remaining height to the list, leaving space for action buttons between panels
            int spaceForPanels = this.height - listY - bottomBarsReserve();
            int midGap = vGap + buttonHeight + vGap; // buttons row between panels
            listHeight = Math.max(60, (spaceForPanels - midGap));
            // clamp listHeight not to exceed half to leave room for details
            listHeight = Math.min(listHeight, Math.max(80, spaceForPanels / 2));
            this.commandListWidget = new CommandListWidget(this, this.client, listWidth, listHeight, listY, 24);
            this.commandListWidget.setX(listX);
            this.commandListWidget.setEntries(this.items);
            this.addDrawableChild(this.commandListWidget);

            // Buttons between list and details
            actionButtonY = listY + listHeight + vGap;

            // Right-side detail panel becomes full-width under the list
            int panelX = margin;
            int panelY = actionButtonY + buttonHeight + vGap;
            int panelWidth = availableW;
            this.rightPanelH = Math.max(120, this.height - panelY - bottomBarsReserve());
            initDetailPanel(panelX, panelY, panelWidth);
        }

        // Auto select first item to avoid empty right panel on open (after widgets are initialized)
        if (!this.items.isEmpty()) {
            // 尝试恢复上次选中项
            if (this.lastSelectedId != null) {
                int idx = -1;
                for (int i = 0; i < items.size(); i++) {
                    if (this.lastSelectedId.equals(items.get(i).id)) { idx = i; break; }
                }
                this.commandListWidget.selectIndex(idx >= 0 ? idx : 0);
            } else {
                this.commandListWidget.selectIndex(0);
            }
        }

        // Bottom Buttons
        int buttonWidth = 80;
        int buttonY = this.height - 30;
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> saveAndBack())
                .dimensions(this.width - 20 - buttonWidth, buttonY, buttonWidth, 20).build());

        // Settings button (open full mod settings screen)
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("button.scroll_whell_command.mod_settings"), b -> openSettings())
                .dimensions(this.width - 20 - buttonWidth * 2 - 4, buttonY, buttonWidth, 20).build());

        // Place left action buttons based on layout (computed above)
        ButtonWidget addBtn = this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.scroll_whell_command.add_command"), b -> openDetailForAdd())
                .dimensions(listX, actionButtonY, 70, 20).build());
        ButtonWidget delBtn = this.addDrawableChild(ButtonWidget.builder(Text.translatable("button.scroll_whell_command.delete"), b -> deleteItem())
                .dimensions(listX + 74, actionButtonY, 70, 20).build());
        // Command list up/down
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("button.scroll_whell_command.up"), b -> moveItem(-1))
                .dimensions(listX + 148, actionButtonY, 54, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("button.scroll_whell_command.down"), b -> moveItem(1))
                .dimensions(listX + 206, actionButtonY, 54, 20).build());

        updateDetailPanel();
    }

    private void initDetailPanel(int x, int y, int width) {
        this.rightPanelX = x;
        this.rightPanelY = y;
        this.rightPanelW = width;
        int currentY = y;

        // 名称
        this.addDrawableChild(new TranslatableTextWidget(x, currentY, width, 20, Text.translatable("label.scroll_whell_command.command_label"), this.textRenderer));
        labelField = new TextFieldWidget(this.textRenderer, x, currentY + 18, width, 20, Text.translatable("field.scroll_whell_command.label"));
        labelField.setMaxLength(32767);
        labelField.setChangedListener(label -> {
            if (selectedItem != null) {
                selectedItem.label = label;
                commandListWidget.setEntries(this.items); // Refresh list to show new label
            }
        });
        this.addDrawableChild(labelField);
        currentY += 42;

        // Removed execution mode toggle per new requirement (always use chat behavior)

        // 命令
        this.addDrawableChild(new TranslatableTextWidget(x, currentY, width, 20, Text.translatable("label.scroll_whell_command.command"), this.textRenderer));
        singleCommandField = new TextFieldWidget(this.textRenderer, x, currentY + 18, width, 20, Text.translatable("field.scroll_whell_command.command"));
        singleCommandField.setMaxLength(256);
        singleCommandField.setChangedListener(cmd -> {
            if (selectedItem != null) {
                selectedItem.command = cmd;
            }
        });
        this.addDrawableChild(singleCommandField);
        // 记录单命令输入框所在 Y，用于多命令时替换为列表
        this.singleFieldY = currentY + 18;
        currentY += 42;

        // Multi-command editor (minimal): add sub-command inputs
        // 子命令编辑区移动到底部（默认隐藏，点击“添加子命令”后显示）
        // 动态计算底部区域位置（输入行 + 按钮行 + 间隔 + 全局底部栏预留）
        int panelBottom = this.rightPanelY + this.rightPanelH;
        // 在右侧面板内部，不再预留全局底栏，只保留面板内部所需的空间
        int bottomReserved = fieldHeight + buttonHeight + vGap * 2; // 输入行 + 间隔 + 按钮行
        int opTop = panelBottom - bottomReserved + vGap; // 输入行顶部相对面板底部
        // 输入：子命令
        int inputWidth = Math.max(60, width - 110);
        subCommandInput = new TextFieldWidget(this.textRenderer, x, opTop, inputWidth, 20, Text.translatable("field.scroll_whell_command.sub_command"));
        subCommandInput.setMaxLength(256);
        this.addDrawableChild(subCommandInput);
        // 输入：延迟
        int delayX = x + Math.max(0, width - 104);
        delayInput = this.addDrawableChild(new TextFieldWidget(this.textRenderer, delayX, opTop, 60, 20, Text.translatable("field.scroll_whell_command.delay_ms")));
        delayInput.setSuggestion("");
        delayInput.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.translatable("help.scroll_whell_command.delay_ms_short")));
        // 右侧单位标识
        int unitX = x + Math.max(0, width - 40);
        delayUnitLabel = this.addDrawableChild(new net.minecraft.client.gui.widget.TextWidget(unitX, opTop + 4, 30, 12, Text.literal("ms"), this.textRenderer));
        delayUnitLabel.setTextColor(0xFFAAAAAA);

        // 右侧按钮：添加/更新/删除子命令（仅多命令显示）
        // 将按钮锚定在面板底部，从下往上换行
        int baseBtnY = panelBottom - vGap - buttonHeight;
        rightAddButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("button.scroll_whell_command.add_sub"), b -> onRightAddPressed())
                .dimensions(x, baseBtnY, 120, 20).build());
        rightUpdateButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("button.scroll_whell_command.update_sub"), b -> onRightUpdatePressed())
                .dimensions(x, baseBtnY, 80, 20).build());
        rightDeleteButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("button.scroll_whell_command.delete_sub"), b -> onRightDeletePressed())
                .dimensions(x, baseBtnY, 80, 20).build());
        rightUpButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("button.scroll_whell_command.up"), b -> onRightMoveSub(-1))
                .dimensions(x, baseBtnY, 54, 20).build());
        rightDownButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("button.scroll_whell_command.down"), b -> onRightMoveSub(1))
                .dimensions(x, baseBtnY, 54, 20).build());

        // 自适应换行布局底部按钮（向上换行）
        int curX = x;
        int curY = baseBtnY;
        int maxX = x + width;
        ButtonWidget[] btns = new ButtonWidget[]{rightAddButton, rightUpdateButton, rightDeleteButton, rightUpButton, rightDownButton};
        int[] btnW = new int[]{120, 80, 80, 54, 54};
        for (int i = 0; i < btns.length; i++) {
            int w = btnW[i];
            if (curX + w > maxX) {
                // 换行到上一行
                curX = x;
                curY -= buttonHeight + vGap;
            }
            btns[i].setPosition(curX, curY);
            btns[i].setWidth(w);
            curX += w + 4;
        }
        // 根据按钮最终行，回推输入行位置到按钮之上，确保不重叠
        int topOfButtons = curY;
        int safeOpTop = Math.max(this.rightPanelY, topOfButtons - vGap - fieldHeight);
        opTop = safeOpTop;
        subCommandInput.setY(opTop);
        delayInput.setY(opTop);
        delayUnitLabel.setY(opTop + 4);
        // 记录子命令编辑区的顶部位置，供列表高度计算使用
        this.subEditorTop = opTop;

        // 提示文本（多行自动换行）
        int hintY = currentY + 34;
        net.minecraft.client.gui.widget.MultilineTextWidget multi = new net.minecraft.client.gui.widget.MultilineTextWidget(Text.translatable("text.scroll_whell_command.hint.command_or_chat"), this.textRenderer);
        multi.setMaxWidth(width);
        multi.setPosition(x, hintY);
        // MultilineTextWidget 无直接 setTextColor；采用灰色通过渲染内置颜色，保持默认以避免渲染异常
        hintWidget = this.addDrawableChild(multi);

        // 子命令列表（可选中以编辑/删除）
        // 在完成底部按钮与子编辑区定位后再创建，以便准确计算高度
        int subListY = this.singleFieldY; // 锚定到“命令”输入框位置（多命令时替换该区域）
        int subListHeight = Math.max(40, this.subEditorTop - vGap - subListY);
        this.subCommandListWidget = new SubCommandListWidget(this.client, width, subListHeight, subListY, 20, this::onSubSelected);
        this.subCommandListWidget.setX(x);
        this.addDrawableChild(this.subCommandListWidget);
    }

    public void setSelectedItem(ConfigData.CommandItem item) {
        // Before switching, save any pending changes from text fields
        if (this.selectedItem != null) {
            // No-op: update/delete functionality removed
        }

        this.selectedItem = item;
        // 每次切换选择，折叠底部子命令编辑区，避免后续点击“添加子命令”直接尝试提交空内容
        this.showSubEditor = false;
        if (this.subCommandInput != null) this.subCommandInput.setText("");
        if (this.delayInput != null) this.delayInput.setText("");
        updateDetailPanel();
    }

    private void updateDetailPanel() {
        // Safety guard: if widgets are not yet initialized, do nothing
        if (labelField == null || singleCommandField == null || subCommandInput == null || delayInput == null || rightAddButton == null) {
            return;
        }
        boolean hasSelection = this.selectedItem != null;

        // If selected is multi, hide single fields and show right add
        boolean isMulti = hasSelection && this.selectedItem.isMulti();
        // 注：不在此处自动从多命令回退为单命令，避免单命令首次迁移到多命令（只有一条子命令）时被立即回退。
        // 回退逻辑在删除路径中处理（onRightDeletePressed）。

        // Set visibility
        labelField.visible = hasSelection; // allow editing name for both single and multi
        singleCommandField.visible = hasSelection && !isMulti; // single only
        // 默认隐藏子命令输入框，仅在点击“添加子命令”后显示
        subCommandInput.visible = hasSelection && isMulti && showSubEditor;
        delayInput.visible = hasSelection && isMulti && showSubEditor;
        if (delayUnitLabel != null) delayUnitLabel.visible = hasSelection && isMulti && showSubEditor;
        // 添加子命令按钮：单/多命令下都可见，用于触发展开或添加
        rightAddButton.visible = hasSelection;
        if (rightUpdateButton != null) rightUpdateButton.visible = hasSelection && isMulti;
        if (rightDeleteButton != null) rightDeleteButton.visible = hasSelection && isMulti;
        if (rightUpButton != null) rightUpButton.visible = hasSelection && isMulti;
        if (rightDownButton != null) rightDownButton.visible = hasSelection && isMulti;
        if (subCommandListWidget != null) subCommandListWidget.visible = hasSelection && isMulti;
        // 隐藏聊天/命令提示文本，避免界面干扰
        if (hintWidget != null) hintWidget.visible = false;

        if (hasSelection) {
            // Populate data (do not clear label for multi)
            labelField.setText(selectedItem.label != null ? selectedItem.label : "");
            if (!isMulti) {
                singleCommandField.setText(selectedItem.command != null ? selectedItem.command : "");
                // Ensure any previous multi-command UI state is cleared to prevent lingering details
                if (subCommandListWidget != null) {
                    subCommandListWidget.setEntries(java.util.List.of());
                    subCommandListWidget.setSelected(null);
                }
                // Collapse sub-editor explicitly on single-command items
                this.showSubEditor = false;
            } else {
                singleCommandField.setText("");
            }
            if (isMulti && subCommandListWidget != null) {
                subCommandListWidget.setEntries(this.selectedItem.subCommands);
                // 默认不选中任何子命令
                subCommandListWidget.setSelected(null);
                // 清空编辑框
                subCommandInput.setText("");
                delayInput.setText("");
                // 保持 initDetailPanel() 计算的初始布局，不在此处动态改动位置和尺寸，避免布局错乱
            }
        } else {
            // No selection: clear fields
            labelField.setText("");
            singleCommandField.setText("");
            subCommandInput.setText("");
            delayInput.setText("");
            if (subCommandListWidget != null) {
                subCommandListWidget.setEntries(java.util.List.of());
                subCommandListWidget.setSelected(null);
            }
        }

        // 按钮可用状态：选中子命令后才能更新/删除
        if (rightUpdateButton != null) rightUpdateButton.active = hasSelection && isMulti && subCommandListWidget != null && subCommandListWidget.getSelectedOrNull() != null;
        if (rightDeleteButton != null) rightDeleteButton.active = hasSelection && isMulti && subCommandListWidget != null && subCommandListWidget.getSelectedOrNull() != null;
        if (rightUpButton != null) rightUpButton.active = hasSelection && isMulti && subCommandListWidget != null && subCommandListWidget.getSelectedOrNull() != null;
        if (rightDownButton != null) rightDownButton.active = hasSelection && isMulti && subCommandListWidget != null && subCommandListWidget.getSelectedOrNull() != null;
    }

    @Override
    public void resize(net.minecraft.client.MinecraftClient client, int width, int height) {
        // Save current UI state before resize re-inits the screen
        String prevSelectedId = (this.selectedItem != null) ? this.selectedItem.id : this.lastSelectedId;
        String prevLabel = (this.labelField != null) ? this.labelField.getText() : null;
        String prevSingleCmd = (this.singleCommandField != null) ? this.singleCommandField.getText() : null;
        String prevSub = (this.subCommandInput != null) ? this.subCommandInput.getText() : null;
        String prevDelay = (this.delayInput != null) ? this.delayInput.getText() : null;
        boolean prevShowSubEditor = this.showSubEditor;
        // Preserve selected sub-command object for later restoration
        org.lby123165.scroll_whell_command.client.config.ConfigData.SubCommand prevSelectedSub = null;
        if (this.subCommandListWidget != null && this.subCommandListWidget.getSelectedOrNull() != null) {
            prevSelectedSub = this.subCommandListWidget.getSelectedOrNull().getSubCommand();
        }
        this.lastSelectedId = prevSelectedId; // ensure init selects the same item

        super.resize(client, width, height); // This will call init()

        // Restore state if the same item is still selected
        if (prevSelectedId != null && this.selectedItem != null && prevSelectedId.equals(this.selectedItem.id)) {
            if (prevLabel != null && this.labelField != null) this.labelField.setText(prevLabel);
            if (prevSingleCmd != null && this.singleCommandField != null) this.singleCommandField.setText(prevSingleCmd);
            if (this.subCommandInput != null && prevSub != null) this.subCommandInput.setText(prevSub);
            if (this.delayInput != null && prevDelay != null) this.delayInput.setText(prevDelay);
            this.showSubEditor = prevShowSubEditor;
            updateDetailPanel();
            // Restore sub-command selection by matching the same data object
            if (prevSelectedSub != null && this.subCommandListWidget != null && this.selectedItem != null && this.selectedItem.subCommands != null) {
                int idx = -1;
                for (int i = 0; i < this.selectedItem.subCommands.size(); i++) {
                    if (this.selectedItem.subCommands.get(i) == prevSelectedSub) { idx = i; break; }
                    // Fallback match by content
                    org.lby123165.scroll_whell_command.client.config.ConfigData.SubCommand sc = this.selectedItem.subCommands.get(i);
                    if (idx < 0) {
                        String a = sc.command == null ? "" : sc.command;
                        String b = prevSelectedSub.command == null ? "" : prevSelectedSub.command;
                        Integer da = sc.delayMs == null ? 0 : sc.delayMs;
                        Integer db = prevSelectedSub.delayMs == null ? 0 : prevSelectedSub.delayMs;
                        if (a.equals(b) && da.equals(db)) idx = i;
                    }
                }
                if (idx >= 0) {
                    this.subCommandListWidget.selectIndex(idx);
                }
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Semi-transparent black background
        context.fill(0, 0, this.width, this.height, 0xA0000000);
        super.render(context, mouseX, mouseY, delta);
        String title = getTitle().getString();
        modernRenderer.drawCenteredText(context, title, this.width / 2f, 15, 0xFFFFFFFF, false, 1.2f);

        // Minimal UI: no extra headers/dividers on the right panel to avoid overlap with field labels

        // Empty state hint for command list
        if (this.items == null || this.items.isEmpty()) {
            modernRenderer.drawCenteredText(context,
                    Text.translatable("text.scroll_whell_command.no_commands").getString(),
                    this.width / 2f, this.height / 2f, 0xFFBBBBBB, false, 1.0f);
        }

        // 子命令列表已改为使用 SubCommandListWidget 渲染

    }

    private void onRightAddPressed() {
        if (this.selectedItem == null) return;
        // 若当前为单命令：首次点击执行迁移，并展开编辑区，避免原命令“消失”
        if (!selectedItem.isMulti()) {
            migrateSingleToMultiIfNeededBeforeAdd();
            selectedItem.type = "multi";
            // 刷新子命令列表，确保原命令已作为第一条子命令显示
            if (subCommandListWidget != null) {
                subCommandListWidget.setEntries(this.selectedItem.subCommands);
                subCommandListWidget.setSelected(null);
            }
            showSubEditor = true;
            updateDetailPanel();
            return;
        }
        // 多命令但编辑区未展开：先展开
        if (!showSubEditor) {
            showSubEditor = true;
            updateDetailPanel();
            return;
        }
        String cmd = subCommandInput.getText() == null ? "" : subCommandInput.getText().trim();
        if (cmd.isEmpty()) return;
        Integer delay = null;
        String dText = delayInput.getText();
        if (dText != null && !dText.trim().isEmpty()) {
            try {
                int v = Integer.parseInt(dText.trim());
                if (v < 0) v = 0;
                delay = v;
            } catch (NumberFormatException ignored) {
                // keep null
            }
        }
        org.lby123165.scroll_whell_command.client.config.ConfigData.SubCommand sc = new org.lby123165.scroll_whell_command.client.config.ConfigData.SubCommand();
        sc.command = cmd;
        sc.delayMs = delay;
        ensureSubListInitializedForAdd();
        // 如果当前是单命令且已有主命令内容，需要把主命令内容迁移为第一条子命令
        migrateSingleToMultiIfNeededBeforeAdd();
        this.selectedItem.subCommands.add(sc);
        // 根据子命令数量自动判定类型
        syncTypeFromData();
        // Refresh list display and persist
        if (subCommandListWidget != null) {
            subCommandListWidget.setEntries(this.selectedItem.subCommands);
        }
        this.commandListWidget.setEntries(this.items);
        ConfigData cfgNow = ConfigManager.get();
        cfgNow.commands = new java.util.ArrayList<>(items);
        ConfigManager.save();
        // clear inputs
        subCommandInput.setText("");
        delayInput.setText("");
        // 切换到多命令可见性
        updateDetailPanel();
    }

    private void onRightUpdatePressed() {
        if (this.selectedItem == null || !this.selectedItem.isMulti() || subCommandListWidget == null) return;
        SubCommandListWidget.SubCommandEntry entry = subCommandListWidget.getSelectedOrNull();
        if (entry == null) return;
        String cmd = subCommandInput.getText() == null ? "" : subCommandInput.getText().trim();
        if (cmd.isEmpty()) return;
        Integer delay = null;
        String dText = delayInput.getText();
        if (dText != null && !dText.trim().isEmpty()) {
            try {
                int v = Integer.parseInt(dText.trim());
                if (v < 0) v = 0;
                delay = v;
            } catch (NumberFormatException ignored) {}
        }
        ConfigData.SubCommand sc = entry.getSubCommand();
        sc.command = cmd;
        sc.delayMs = delay;
        // Refresh UI and persist
        subCommandListWidget.setEntries(this.selectedItem.subCommands);
        this.commandListWidget.setEntries(this.items);
        ConfigData cfgNow = ConfigManager.get();
        cfgNow.commands = new java.util.ArrayList<>(items);
        ConfigManager.save();
    }

    private void onRightDeletePressed() {
        if (this.selectedItem == null || !this.selectedItem.isMulti() || subCommandListWidget == null) return;
        SubCommandListWidget.SubCommandEntry entry = subCommandListWidget.getSelectedOrNull();
        if (entry == null) return;
        ConfigData.SubCommand sc = entry.getSubCommand();
        this.selectedItem.subCommands.remove(sc);
        // sync type and migrate if necessary
        syncTypeFromData();
        // 如果删除后只剩一条子命令，迁移回单命令
        migrateMultiToSingleIfNeededAfterDelete();
        // Refresh UI and persist
        subCommandListWidget.setEntries(this.selectedItem.subCommands);
        this.commandListWidget.setEntries(this.items);
        ConfigData cfgNow = ConfigManager.get();
        cfgNow.commands = new java.util.ArrayList<>(items);
        ConfigManager.save();
        // 清空编辑框
        subCommandInput.setText("");
        delayInput.setText("");
        // 更新按钮可用状态
        rightUpdateButton.active = false;
        rightDeleteButton.active = false;
        // 如果类型变为单命令，立即刷新可见性
        updateDetailPanel();
    }

    private void onSubSelected(SubCommandListWidget.SubCommandEntry entry) {
        if (entry == null) {
            rightUpdateButton.active = false;
            rightDeleteButton.active = false;
            if (rightUpButton != null) rightUpButton.active = false;
            if (rightDownButton != null) rightDownButton.active = false;
            return;
        }
        ConfigData.SubCommand sc = entry.getSubCommand();
        subCommandInput.setText(sc.command != null ? sc.command : "");
        delayInput.setText(sc.delayMs != null ? String.valueOf(sc.delayMs) : "");
        rightUpdateButton.active = true;
        rightDeleteButton.active = true;
        if (rightUpButton != null) rightUpButton.active = true;
        if (rightDownButton != null) rightDownButton.active = true;
    }

    private void onRightMoveSub(int dir) {
        if (this.selectedItem == null || !this.selectedItem.isMulti() || subCommandListWidget == null) return;
        SubCommandListWidget.SubCommandEntry entry = subCommandListWidget.getSelectedOrNull();
        if (entry == null) return;
        ConfigData.SubCommand sc = entry.getSubCommand();
        int idx = this.selectedItem.subCommands.indexOf(sc);
        int newIdx = idx + dir;
        if (idx >= 0 && newIdx >= 0 && newIdx < this.selectedItem.subCommands.size()) {
            java.util.Collections.swap(this.selectedItem.subCommands, idx, newIdx);
            subCommandListWidget.setEntries(this.selectedItem.subCommands);
            // 重新选择移动后的项
            // 简单做法：按 newIdx 再选
            SubCommandListWidget.SubCommandEntry sel = subCommandListWidget.children().get(newIdx);
            subCommandListWidget.setSelected(sel);
            // 持久化
            ConfigData cfgNow = ConfigManager.get();
            cfgNow.commands = new java.util.ArrayList<>(items);
            ConfigManager.save();
        }
    }

    private void ensureSubListInitializedForAdd() {
        if (this.selectedItem.subCommands == null) {
            this.selectedItem.subCommands = new java.util.ArrayList<>();
        }
    }

    private void migrateSingleToMultiIfNeededBeforeAdd() {
        if (!this.selectedItem.isMulti()) {
            // 如果当前为单命令，将单命令内容迁移为第一条子命令
            String base = this.selectedItem.command != null ? this.selectedItem.command.trim() : "";
            if (!base.isEmpty()) {
                ConfigData.SubCommand first = new ConfigData.SubCommand();
                first.command = base;
                first.delayMs = 0;
                this.selectedItem.subCommands.add(first);
                this.selectedItem.command = null;
            }
        }
    }

    private void migrateMultiToSingleIfNeededAfterDelete() {
        if (this.selectedItem.subCommands != null && this.selectedItem.subCommands.size() == 1) {
            ConfigData.SubCommand only = this.selectedItem.subCommands.get(0);
            this.selectedItem.command = only.command;
            this.selectedItem.subCommands.clear();
        }
    }

    public void addOrUpdateCommand(ConfigData.CommandItem updatedItem) {
        int index = -1;
        for (int i = 0; i < this.items.size(); i++) {
            if (this.items.get(i).id.equals(updatedItem.id)) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            this.items.set(index, updatedItem);
        } else {
            this.items.add(updatedItem);
        }

        this.commandListWidget.setEntries(this.items);
    }

    private void openDetailForAdd() {
        ConfigData.CommandItem newItem = new ConfigData.CommandItem();
        newItem.label = Text.translatable("text.scroll_whell_command.new_command").getString();
        this.items.add(newItem);
        this.commandListWidget.setEntries(this.items);
        this.commandListWidget.selectIndex(this.items.size() - 1);
        this.setFocused(this.labelField);
        labelField.setSelectionStart(0);
        labelField.setSelectionEnd(newItem.label.length());
    }

    private void deleteItem() {
        if (selectedItem != null) {
            items.remove(selectedItem);
            commandListWidget.setEntries(items);
            setSelectedItem(null); // Clear selection
            // Persist immediately
            ConfigData cfgNow = ConfigManager.get();
            cfgNow.commands = new ArrayList<>(items);
            ConfigManager.save();
        }
    }

    private void moveItem(int dir) {
        if (selectedItem == null) return;
        int index = items.indexOf(selectedItem);
        if (index == -1) return;

        int newIndex = index + dir;
        if (newIndex >= 0 && newIndex < items.size()) {
            Collections.swap(items, index, newIndex);
            commandListWidget.setEntries(items);
            commandListWidget.selectIndex(newIndex);
            // Persist immediately
            ConfigData cfgNow = ConfigManager.get();
            cfgNow.commands = new ArrayList<>(items);
            ConfigManager.save();
        }
    }

    private void saveAndBack() {
        ConfigData cfg = ConfigManager.get();
        cfg.commands = new ArrayList<>(items);
        ConfigManager.save();
        ConfigManager.load(); // Force reload
        close();
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            saveAndBack();
            return true;
        }

        if (commandListWidget != null) {
            if (commandListWidget.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            CommandListWidget.CommandEntry selected = commandListWidget.getSelectedOrNull();
            if (selected != null) {
                switch (keyCode) {
                    case GLFW.GLFW_KEY_DELETE:
                        deleteItem();
                        return true;
                    case GLFW.GLFW_KEY_PAGE_UP:
                        moveItem(-1);
                        return true;
                    case GLFW.GLFW_KEY_PAGE_DOWN:
                        moveItem(1);
                        return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return super.charTyped(chr, modifiers);
    }

    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

    public TextRenderer getTextRenderer() {
        return this.textRenderer;
    }

    private void syncTypeFromData() {
        if (selectedItem == null) return;
        int count = selectedItem.subCommands != null ? selectedItem.subCommands.size() : 0;
        if (count >= 2) {
            selectedItem.type = "multi";
        } else {
            selectedItem.type = "single";
        }
    }

    private void openSettings() {
        if (this.client == null) return;
        this.client.setScreen(new SettingsScreen(this));
    }
}
