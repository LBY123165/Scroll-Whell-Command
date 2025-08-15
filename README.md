# Scroll Whell Command (Fabric 1.20.3-1.20.6)

一个仿APEX风格的轮盘快捷命令
*（温馨提示：本README为AI生成）*
---

## ✨ Features 功能

- 自定义命令项（单条或多子命令序列）
- 每个子命令独立延时（毫秒），默认 0ms
- 轮盘分页（默认每页 10，支持 8–12 可调），显示翻页指示与 Page X/Y 文本
- 扇区高亮与精确角度选择；单条命令时任意方向可选
- 纯文本扇区布局，文本居中对齐，分割线与中央圆形提示区
- 可选“背景调暗/透明度”覆盖层（可配置）
- 高级颜色选择器（RGBA/HSB 切换、实时预览、Hex 显示）

---

## 📦 Installation 安装

1. 安装 Fabric Loader 与 Fabric API（对应 1.20.6）。
2. 下载本模组的发布版 JAR（例如 `build/libs/Scroll-Whell-Command-0.2.jar` 或 GitHub Releases 最新版本）。
3. 将 JAR 放入 `.minecraft/mods/` 目录。
4. 启动游戏。

---

## 🏗 Build from Source 源码构建

- 需要 JDK 21（1.20.5/1.20.6 要求 Java 21）。
- 使用项目自带的 Gradle Wrapper：
  - Windows: `gradlew.bat build`
  - macOS/Linux: `./gradlew build`
- 产物位置：`build/libs/`（例如 `Scroll-Whell-Command-0.2.jar`）。

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

## 🤝 Contributing 参与贡献

欢迎提交 Issue 与 PR。
---

## ✅ Compatibility 兼容性
  
  - 当前目标版本：Minecraft 1.20.6（Fabric）
  - 计划测试范围：1.20.1–1.20.6。若发现不兼容，将提供相应适配或独立构建。

## 📜 License 许可
  
  - License: GPL-3.0-or-later
