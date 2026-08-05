# 日程助手（Schedule Assistant）

面向教师的**课表与日程管理工具**：Android 原生应用（Kotlin + Jetpack Compose），本地存储、完全离线，GitHub Actions 自动构建并发布 APK。

> 本仓库包含 Android 原生应用（`android/`）与 CI 构建工作流；网页版源码不在此仓库内。

## 版本与下载

| 版本 | 说明 |
|---|---|
| **v1.0.0**（最新） | 首个发行版，见右侧 **Releases** → 下载 `schedule-assistant-v1.0.0.apk` |

- APK 由 CI（GitHub Actions）云端构建并自动发布到 **Releases**，每次推送 `v*` 标签即生成新发行版。
- debug 签名，可直接安装测试；首次安装需允许"安装未知来源应用"。
- 下载地址：[Releases](https://github.com/xxqnspace/schedule-assistant/releases)

## 功能特性

- **周课表**：单/双周循环展示，支持任意星期开学（开学日至周日为第 1 周）；编辑模式下点击格子直接添加/编辑课程，当前星期高亮
- **今日安排**：当日课程 + 工作日程时间线，倒计时（"距离 XX 还有 N 天"），类型配色区分（上课/会议/备课/值班等），已完成项置底标记
- **工作日程**：按日期管理，系统通知 + 声音提醒，提前量可全局默认或单条自定义
- **调课 / 调休**：按日期覆盖课表（取消/复制周几/自定义课程）
- **学期信息**：学期名称、起始日、姓名；自动计算当前第几周，开学前显示"放假中"
- **外观**：浅色/深色主题、纯色/背景图（URL）、卡片毛玻璃质感
- **数据**：JSON 导入导出备份（与网页版格式兼容）；首次启动自动初始化默认节次
- **隐私**：数据全部保存在本地（Room），无网络权限、无账号、无广告

## 技术栈

- Kotlin + Jetpack Compose（Material 3）
- Room（本地数据库）、DataStore 风格单例配置
- AlarmManager 精确闹钟 + 系统通知（Android 13+ 动态权限引导）
- Coil（图片加载）、GitHub Actions（构建/测试/发布）

## 构建与测试

```bash
cd android
gradle lintDebug            # 静态检查
gradle testDebugUnitTest    # 单元测试
gradle assembleDebug        # 构建 debug APK
# 产物：android/app/build/outputs/apk/debug/app-debug.apk
```

CI 流水线（`.github/workflows/build.yml`）：
- 每次推送到 `main`：lint + 单元测试 + 构建 debug APK（产物以 artifact 形式留存）
- 每次推送 `v*` 标签：额外自动创建 GitHub **Release** 并上传 APK（云端完成，无需本地操作）

## 项目结构

```
android/                     # Android 原生应用（Gradle 工程）
  app/src/main/java/com/scheduleassistant/app/
    data/                    # Room 实体 / DAO / 仓库 / 导入导出
    notify/                  # 提醒调度 / 通知 / 开机广播
    ui/                      # Compose 界面（主页/课表/日程/设置 + 表单）
    util/                    # 日期工具 / ID 生成等
  app/src/test/              # 单元测试
.github/workflows/build.yml  # CI 构建与发布
```

## 相关文档

- [安卓代码审计报告](安卓代码审计报告.md)
- [UI 优化审计报告](安卓代码审计报告-UI优化.md)
- [GitHub Actions 说明](GITHUB_ACTIONS.md)
