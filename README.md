# EyeDrop Manager · 滴答护眼 v0.2.0-A

Android 本地眼药水计划与护理原型，最低 Android 8.0 / API 26，目标 API 35；无账号、无网络权限。个人配置及记录保存在应用私有存储，系统备份关闭。

本版加入三类规则模板、硬约束排程、冲突预览确认、实际打卡后的护理计时、前台护理阶段通知，以及计划/实际时间轴。每天滴药闹钟尚未提供。完整范围见 docs/V0.2-A.md。

## 下载

- [Android 测试安装包](https://github.com/Xeyus/Eyedrop/raw/refs/heads/main/dist/EyeDrop-v0.2.0-A.apk)
- [Windows 源码迁移包](https://github.com/Xeyus/Eyedrop/raw/refs/heads/main/downloads/Eyedrop-v0.2A-Windows.zip)
- [本地接续说明](docs/CONTINUE.md)

## 安装与迁移

测试安装包：dist/EyeDrop-v0.2.0-A.apk。Windows 接续步骤见 docs/CONTINUE.md，目标目录为 D:\Database\Coding\Eyedrop。

下载的云端 APK 与本会话 v0.1 使用同一测试签名，支持覆盖升级。先安装覆盖，不要卸载或清除数据。旧记录在 schema v2 中保留，新增原计划与 epoch 元数据。签名私钥未上传到此公开仓库。

## 构建

标准 Android Gradle 工程：AGP 8.7.3、Gradle Wrapper 8.9、JDK 17、SDK 35。Windows 运行 gradlew.bat assembleDebug。若本地私下提供 signing/development.keystore，则使用该测试密钥；公开仓库不包含私钥。未提供时使用 Android 默认 debug 签名，包名增加 .local，与云端 APK 并列安装，避免为换签名卸载而丢失原数据。

云端实际使用官方 Android SDK + ECJ 直接构建：先 ./setup-tools.sh，再 ./test.sh 与 ./build.sh。工具目录由 EYEDROP_TOOLS 指定，默认 /workspace/work/tools；云端脚本适用于 Linux x86_64，依赖 Java 17+ 运行时、keytool、curl、unzip、zip。

Windows 单独运行核心测试：powershell -ExecutionPolicy Bypass -File .\test.ps1；需要 JDK 17 与首次下载测试 JSON 库的网络访问。JSON 库仅供 JVM 测试，不进入 APK；应用使用 Android 内置 JSON。

## 验收

1. 设置 08:00–22:00，依次添加环孢素模板（2次，首剂08:00）、施图伦模板（3次）、人工泪液模板（5次或6次）。每次确认前先核对预览。
2. 环孢素应为08:00与20:00；人工泪液须与它们前后相隔至少15分钟；其他任务不可落入施图伦护理窗口。
3. 改短作息到不能容纳12小时，应显示未安排与冲突，不压缩间隔。
4. 手工改为冲突时刻，界面应显示药名、冲突时间与违反规则，并提供重新安排预览。
5. 施图伦“已实际使用”打卡后，护理从实际时间开始。允许通知；前3分钟按压，随后继续闭眼，10分钟结束。切到后台及重开应用验证阶段通知和计时恢复。
6. 漏滴仅记漏滴；过期任务保留待处理。删除或暂停药物后，已有使用与护理记录保留。
7. 在同签名v0.1已有数据上覆盖安装，检查旧药物及原计划/实际使用时间保留。

不要用真实用药代替软件验收：只在实际已经使用后记录，不因测试提前或重复滴药。

## 已知验证范围

构建、签名与核心自动测试结果见 docs/VERIFICATION.md。未在真机验证，Windows Gradle入口与后台护理通知仍待本机验收。系统强制停止、通知关闭或电池限制可能中断后台提示；重新打开可恢复计时状态。卸载或清除应用数据会删除本地记录。
