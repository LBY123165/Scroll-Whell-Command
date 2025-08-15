# Scroll Whell Command (Fabric 1.21.1)

- Mod ID: `scroll_whell_command`
- Environment: Client
- License: All-Rights-Reserved
- Language: 简体中文 / English

一款用于 Minecraft 的客户端 Fabric 模组：按下鼠标中键呼出“命令轮盘”，以极快的方式执行自定义命令（支持多子命令、延时、分页、颜色与样式自定义、中文界面）。

A client-side Fabric mod for Minecraft: open a radial command menu with the middle mouse button to execute custom commands quickly (multi-command sequences with per-subcommand delay, paging, color/style customization, Chinese UI).

---

## ✨ Features 功能

- 自定义命令项（单条或多子命令序列）
- 每个子命令独立延时（毫秒），默认 0ms
- 轮盘分页（默认每页 10，支持 8–12 可调），显示翻页指示与 Page X/Y 文本
- 扇区高亮与精确角度选择；单条命令时任意方向可选
- 纯文本扇区布局，文本居中对齐，分割线与中央圆形提示区
- 可选“背景调暗/透明度”覆盖层（可配置），UI 渲染遵循 1.21 最佳实践
- 高级颜色选择器（RGBA/HSB 切换、实时预览、Hex 显示）
- 现代化设置界面与命令编辑器（列表优先、键盘快捷、悬停高亮）
- 中文本地化（`zh_cn.json`），改进字体清晰度与对比度
- 不暂停游戏，开启轮盘时可继续移动；全屏角度判定，不受鼠标距离限制

---

## 🧩 Requirements 运行环境

- Minecraft: 1.21.1 (`gradle.properties:minecraft_version=1.21.1`)
- Fabric Loader: >= 0.16.10 (`loader_version=0.16.10`)
- Fabric API: 0.116.5+1.21.1
- Java: 21

---

## 📦 Installation 安装

1. 安装 Fabric Loader 与 Fabric API（对应 1.21.1）。
2. 下载本模组的发布版 JAR（例如 `build/libs/SWC-0.2.jar` 或 GitHub Releases 最新版本）。
3. 将 JAR 放入 `.minecraft/mods/` 目录。
4. 启动游戏。

---

## 🏗 Build from Source 源码构建

- 需要 JDK 21。
- 使用项目自带的 Gradle Wrapper：
  - Windows: `gradlew.bat build`
  - macOS/Linux: `./gradlew build`
- 产物位置：`build/libs/`（例如 `SWC-0.2.jar`）。

关键文件：
- `build.gradle`（使用 Fabric Loom 1.11-SNAPSHOT）
- `gradle.properties`（版本与依赖声明）
- `src/main/resources/fabric.mod.json`（入口、元数据）

---

## 🎮 Usage 使用方法

- 按下鼠标中键开启轮盘；松开中键/左键执行所选命令；右键取消。
- 翻页：PageUp / PageDown（或按配置的鼠标滚轮/热键）。
- 轮盘中央区域会动态显示键位提示（与实际按键绑定同步）。
- 选择逻辑：
  - 单条命令时：任意方向松手即可执行。
  - 多条命令时：按角度选择对应扇区（全屏角度判定）。

---

## ⚙️ Settings & Command Editor 设置与命令编辑器

- 在游戏内打开“模组设置”，进入本模组的设置界面。
- 命令编辑器采用“列表优先”工作流：新增/编辑/删除/完成按钮位于底部。
- 多命令项支持为每个子命令配置独立延时（毫秒）。
- 颜色设置支持 RGBA/HSB 切换，实时预览；支持一键恢复默认。
- 轮盘每页数量可在 8–12 之间调整（默认 10）。

---

## 🌐 Localization 本地化

- 已内置简体中文（`src/main/resources/assets/scroll_whell_command/lang/zh_cn.json`）。

---

## 📝 Changelog 变更摘要

- v0.2
  - 大规模 UI/UX 打磨：现代化设置界面、改良文本渲染、分页指示、扇区高亮与分割线
  - 高级颜色选择器（RGBA/HSB）、中文适配、即时保存与可视化
  - 输入逻辑重构：松手/左键执行、右键取消；支持移动时开启，取消背景遮罩限制
  - 命令编辑修复：新增/多命令保存逻辑稳定，避免 UI/配置不同步

---

## ⚠ Known Issues 已知问题

- 与原版一致的输入行为（带 `/` 为命令，不带 `/` 为聊天）仍在收尾调整中（见 `plan.md` 当前目标）。

---

## 🔧 Project Meta 项目信息

- 模组名称：Scroll Whell Command
- Mod ID：`scroll_whell_command`
- 入口：
  - `org.lby123165.scroll_whell_command.Scroll_whell_command`
  - `org.lby123165.scroll_whell_command.client.Scroll_whell_commandClient`

---

## 🤝 Contributing 参与贡献

欢迎提交 Issue 与 PR。请在提交前确保：
- 构建通过（`gradlew build`）。
- 遵循项目现有的 UI/交互与代码风格（Fabric 1.21 自定义 Screen 最佳实践）。

---

## 📜 License 许可

- License: All-Rights-Reserved（保留所有权利）。
- 请勿未经授权分发修改版或用于商业用途。
