# 日程助手（Android 原生版）

本目录是「日程助手」网页版的 **Android 原生应用**实现，数据 **完全本地存储**（Room / SQLite），
通过 **GitHub Actions** 自动构建 debug APK，无需本地配置签名或服务器。

与网页版 (`index.php` / `api.php`) 共享同一套数据模型，并支持 **JSON 互导**（与网页版 `schedule-data.json` 完全兼容）。

---

## 功能对照（网页版 → 安卓原生）

| 网页版功能 | 安卓实现 | 说明 |
| --- | --- | --- |
| 今日倒计时（天/时/分/秒实时刷新，最多 3 个） | `TodayScreen` 顶部卡片 | 每秒刷新，到点显示「已结束」 |
| 今日时间线（课程 + 工作日程，含调课/调休/自定义） | `TodayScreen` 列表 | 移植 `getDayTimeline` |
| 周课表（单/双周循环）+ 周次步进器（‹ 第N周 › / 回到本周） | `TimetableScreen` 7×节次网格 | 奇偶周按所选周次决定 |
| 课表单元格点击编辑 / 新增课程 | `CourseFormSheet` | 点击已有课 = 编辑；点击空格 = 在该星期/节次新增 |
| 调课 / 调休 / 单日自定义（overrides） | `OverrideFormSheet` + `SettingsScreen` | 三种模式：停课 / 按某天课表 / 单日自定义课表 |
| 工作日程（按日期）+ 提醒 | `EventsScreen` + `EventFormSheet` | 支持全天、类型、提前提醒分钟、颜色、备注 |
| 系统通知 + 声音提醒（关闭应用仍提醒） | `ReminderScheduler` + `ReminderReceiver` | 用 `AlarmManager` 精确闹钟（非 WorkManager），开机后 `BootReceiver` 自动恢复 |
| 浅 / 深主题 | `SettingsScreen` 主题切换 + `ScheduleTheme` | 实时跟随设置 |
| JSON 导入 / 导出备份 | `SettingsScreen`（系统文件选择器） | 与网页版 `schedule-data.json` 结构完全一致 |
| 节次管理 | `SectionFormSheet` | 名称 + 起止时间 |
| PWA / 桌面快捷方式 | Android 原生安装（图标为自适应图标） | — |

> 说明：网页版的「图片背景毛玻璃」在原生版中以「浅/深主题」承载，未实现背景图毛玻璃层（设置项保留但不强制生效），其余功能与 UI 风格保持一致。

---

## 目录结构

```
android/
├── build.gradle.kts            # 顶层：插件版本（AGP 8.5.2 / Kotlin 1.9.24 / KSP 1.9.24-1.0.20）
├── settings.gradle.kts         # 仓库（google / mavenCentral）+ 包含 :app
├── gradle.properties
├── gradlew / gradlew.bat       # Gradle 包装脚本（自动下载 Gradle 8.9）
├── gradle/wrapper/             # gradle-wrapper.jar + gradle-wrapper.properties
├── .github/workflows/build.yml # GitHub Actions 构建 debug APK
└── app/
    ├── build.gradle.kts        # 依赖与编译配置（minSdk 26 / targetSdk 34）
    └── src/main/
        ├── AndroidManifest.xml # 权限与组件声明
        ├── res/                # 图标（矢量）、字符串、主题
        └── java/com/scheduleassistant/app/
            ├── ScheduleApplication.kt
            ├── data/           # Room：model / Dao / AppDatabase / ScheduleRepository
            ├── notify/         # 本地提醒：Scheduler / Receiver / BootReceiver / NotificationHelper
            ├── util/           # DateUtils / ScheduleUtils / Ids
            └── ui/
                ├── MainActivity.kt / MainScreen.kt / MainViewModel.kt
                ├── theme/Theme.kt
                ├── components/ # 公共组件 + 各类表单底部弹窗
                └── screens/    # Today / Timetable / Events / Settings 四个主界面
```

---

## 本地运行 / 构建

### 方式一：Android Studio
1. 用 Android Studio 打开本 `android/` 目录。
2. 连接设备或启动模拟器（API ≥ 26）。
3. 点击 ▶ Run，或直接 `Build → Build Bundle(s) / APK(s) → Build APK(s)`。

### 方式二：命令行（无需 Android Studio）
需要本地已安装 **JDK 17**，并配置好 Android SDK（`ANDROID_HOME` 指向 SDK 根目录，且已安装 `platforms;android-34` 与 `build-tools;34.0.0`）：

```bash
cd android
./gradlew assembleDebug        # Windows 用 gradlew.bat
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

---

## 通过 GitHub 构建 APK

仓库根目录已包含 `.github/workflows/build.yml`：

- 触发：推送到 `main` / `master` 分支，或手动在 Actions 页点击 **Run workflow**。
- 流程：检出代码 → 安装 JDK 17 → 安装 Android SDK（platform 34 + build-tools）→ `./gradlew assembleDebug` → 上传 APK 为 Artifact。
- 获取：在对应 workflow run 页面底部 **Artifacts** 区下载 `schedule-assistant-apk`（内含 `app-debug.apk`）。
- APK 为 debug 签名（系统自动生成），安装到手机即可使用，无需额外签名。

> 提示：调试包可直接安装；若要发布到应用商店，需自行配置 release 签名（`signingConfigs`）。

---

## 发布签名（Release APK / 上架）

debug 包可直接安装使用；若要生成**正式发布包**（用于上架或分发他人），需配置签名密钥。本工程已内置 `signingConfigs.release`，通过环境变量读取密钥，**密钥不入库**。

1. 本地生成密钥库（只需一次）：

   ```bash
   keytool -genkeypair -v -keystore release-key.jks \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -alias scheduleassistant
   ```

2. 将密钥库编码为 base64：

   - macOS / Linux：`base64 -w0 release-key.jks > release-key.jks.base64`
   - Windows：`certutil -encode release-key.jks release-key.jks.base64`

3. 在仓库 **Settings → Secrets and variables → Actions** 中添加 4 个仓库密钥：

   | Secret 名称 | 值 |
   | --- | --- |
   | `KEYSTORE_BASE64` | `release-key.jks.base64` 的文本内容 |
   | `KEYSTORE_PASSWORD` | 密钥库密码 |
   | `KEY_ALIAS` | `scheduleassistant` |
   | `KEY_PASSWORD` | 密钥（别名）密码 |

4. 推送到 `main` / `master` 或手动 Run workflow。检测到 `KEYSTORE_BASE64` 后，Actions 会：
   - 解码 base64 还原 `release-key.jks`
   - 执行 `./gradlew assembleRelease`
   - 额外上传 **release** 产物 `schedule-assistant-apk-release`（含 `app-release.apk`）

> 未配置密钥时，workflow 仍照常构建并上传 **debug** 包；只有 release 步骤会被跳过。本地也可直接 `./gradlew assembleRelease` 配合上述环境变量自行构建。

---

## 数据导入 / 导出

- 在「设置 → 数据备份」中：
  - **导出**：调用系统文件保存框，保存为 `schedule-data.json`。
  - **导入**：选择任意 `schedule-data.json`（含网页版导出文件），覆盖全部本地数据。
- 字段与网页版 `schedule-data.json` 完全一致（`meta / settings / courses / overrides / events / countdowns`，节次附在 `settings.sections`）。

---

## 权限说明

| 权限 | 用途 |
| --- | --- |
| `POST_NOTIFICATIONS` | Android 13+ 上课/日程提醒通知（运行时申请） |
| `SCHEDULE_EXACT_ALARM` | 精确闹钟，保证到点提醒（安装即授予） |
| `RECEIVE_BOOT_COMPLETED` | 开机后自动恢复已注册的提醒 |

---

## 应用图标

![日程助手应用图标](app_icon_preview.png)

直接使用网页端 PWA 图标（对话框里的一本打开的书 + 笑脸），与网页版 `icons/icon-512.png` 保持一致。系统会按设备形状自动遮罩为圆角 / 圆形 / 方章等。

- 自适应图标前景：`drawable/ic_web_icon.png`（来自网页端图标）
- 自适应图标背景：`#2563eb`（主题蓝）
- 同时补全了 `mipmap-mdpi` 到 `mipmap-xxxhdpi` 的方形与圆形 legacy 图标，作为低版本 / 部分启动器的兜底

---

## 技术栈

- **Kotlin + Jetpack Compose (Material3)** 原生 UI
- **Room (SQLite)** 本地存储，KSP 注解处理
- **AlarmManager** 本地提醒（精确闹钟 + 开机恢复）
- AGP 8.5.2 / Kotlin 1.9.24 / Compose 编译器 1.5.14 / Gradle 8.9
- minSdk 26，targetSdk 34，包名 `com.scheduleassistant.app`，主题色 `#2563eb`
