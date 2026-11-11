# 在 Windows 桌面继续

目标目录：`D:\Database\Coding\Eyedrop`。

1. 将迁移包解压到 `D:\Database\Coding`，包内顶层目录为 `Eyedrop`。
2. 在桌面 ChatGPT / Codex 中选择本地工作模式并打开该目录。此云端会话不能替你打开本机应用或写入 D 盘。
3. 在新的本地会话中使用下面的接续提示。

> 请在当前 Eyedrop 工程上继续构建。先阅读 README.md、docs/V0.2-A.md、docs/VERIFICATION.md。已有 v0.2.0-A 开发签名 APK，云端核心测试 1992 项与约束/护理测试 110 项通过；Android 模拟器运行验收未完成。优先在本机 Android 模拟器或真机安装 dist/EyeDrop-v0.2.0-A.apk，验收规则模板、预览确认、3/10分钟护理阶段、通知授权、后台护理、记录迁移。不可把构建成功称为真机验收通过。随后修复实际发现的问题。不得改写已完成记录、压缩硬间隔或自动补滴。

## 构建与测试

安装 Android Studio（含 SDK 35、Build-tools、JDK 17）。打开本目录并等待 Gradle 同步。

PowerShell 中运行：

```powershell
Set-Location 'D:\Database\Coding\Eyedrop'
.\gradlew.bat assembleDebug
# 生成 app\build\outputs\apk\debug\app-debug.apk
powershell -ExecutionPolicy Bypass -File .\test.ps1
```

如 Java 尚未配置，可在当前 PowerShell 会话临时设置 `JAVA_HOME` 指向 Android Studio 的 `jbr`（推荐 JDK 17）或独立 JDK 17，并将其 bin 加入该会话 PATH。SDK 可由 Android Studio 配置到 local.properties；该文件未包含本机路径。

签名私钥没有提交到公开仓库。云端 APK 仍使用原测试签名；本机首次构建如未私下放入 `signing/development.keystore`，将使用 Android 默认 debug 签名并自动增加 `.local` 包名，与下载的云端版并列安装，保留原应用及其数据。若日后需要同包名覆盖升级，请先私下迁移原测试密钥，不要先卸载原应用。开发密钥不用于正式发版。

## 初次安装

`dist/EyeDrop-v0.2.0-A.apk` 适用于 Android 8.0+。在手机上打开文件，按系统提示允许安装。不要先卸载已有同签名 v0.1 测试包。

界面不会预置真实用药或自动识别药名。请添加药物并主动选择相应规则模板，再按医嘱核对次数与首剂时间。
