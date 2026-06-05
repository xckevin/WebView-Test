# WebViewTest

Android H5/WebView 测试工具。用于在真实设备或模拟器上快速加载、预览、复现和调试各类 H5 页面，适合混合应用开发、活动页验证、WebView 兼容性排查和 Android WebView 能力测试。

## 功能概览

- URL 加载：支持手动输入、粘贴、刷新，自动规范化 `http` / `https` URL。
- 扫码加载：内置 CameraX + ML Kit 二维码扫描，扫码结果可直接回填/加载。
- 本地 HTML：通过系统文件选择器加载 `content://` / `file://` HTML，并记录为本地文件历史。
- 历史记录：自动保存访问历史，区分远程 URL 和本地文件。
- 测试用例：保存当前 URL 与 WebView 配置，支持重新打开、删除、导入、导出。
- 配置切换：JavaScript、DOM Storage、Cookie、三方 Cookie、cache、mixed content、User-Agent、desktop mode、权限策略、全屏等。
- App 内调试：Console、Errors、Page、Cookies、Storage、Source、Elements、JS Exec、Requests、Downloads。
- WebView 高级行为：文件选择、下载、视频全屏、长按上下文菜单、Web camera/microphone/geolocation 权限。
- Release 调试开关：Settings 中可手动开启 WebView debugging，用电脑 Chrome `chrome://inspect` 调试 release 包中的 WebView。

## 技术栈

- Kotlin
- Jetpack Compose + Material3
- Android WebView + `AndroidView`
- Navigation Compose
- Room
- DataStore Preferences
- CameraX
- ML Kit Barcode Scanning
- kotlinx.serialization JSON
- JUnit / AndroidX Test / Compose UI Test

## 项目结构

```text
app/src/main/java/com/xckevin/android/app/webview/test/
├── data/          # Room、DataStore、Repository、导入导出
├── debug/         # Debug state、reducer、页面脚本
├── model/         # WebTestConfig、WebTestCase、HistoryItem
├── scanner/       # CameraX + ML Kit 扫码
├── ui/            # Workbench、Settings、Scanner、通用 UI
├── util/          # URL 标准化等工具
└── web/           # WebView host、client、settings、权限、下载、文件选择、全屏
```

更多设计和总结见：

- [设计文档](docs/superpowers/specs/2026-06-05-webview-test-app-design.md)
- [工作总结](docs/2026-06-05-webview-test-app-summary.md)

## 环境要求

- Android Studio / Android SDK
- JDK 17
- Android Gradle Plugin 9.x
- Android SDK Platform 36
- Android Build Tools 36.1.0
- 运行设备或模拟器：minSdk 24，targetSdk 36

本地需要配置 `local.properties`，至少包含：

```properties
sdk.dir=/path/to/Android/sdk
```

## 构建与运行

编译 debug：

```bash
./gradlew :app:compileDebugKotlin
```

安装 debug 包到已连接设备：

```bash
./gradlew :app:installDebug
```

运行 App 后，默认进入 Workbench。可以输入 URL、扫码或打开本地 HTML 文件进行测试。

## 测试

运行 debug 单元测试：

```bash
./gradlew :app:testDebugUnitTest
```

编译 Android instrumentation tests：

```bash
./gradlew :app:compileDebugAndroidTestKotlin
```

在已连接设备或模拟器上运行 connected tests：

```bash
./gradlew :app:connectedDebugAndroidTest
```

最近一次完整验证结果：

- `./gradlew :app:testDebugUnitTest :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin` 通过。
- `./gradlew :app:connectedDebugAndroidTest` 在 `Pixel_9_API_35(AVD) - 15` 上通过，6/6 tests passed。

## 使用说明

### 加载远程页面

1. 在顶部 URL 输入框输入地址，例如 `example.com`。
2. 点击 `Load`。
3. App 会自动规范化为 `https://example.com` 并加载。

### 扫码加载

1. 点击 `Scan`。
2. 授权相机权限。
3. 扫描二维码。
4. 如果结果是 URL，会回到 Workbench 并加载。

### 加载本地 HTML

1. 点击 `Open local HTML`。
2. 从系统文件选择器选择 HTML 文件。
3. App 会尝试持久化读取权限，并以本地文件类型记录历史。

### 保存测试用例

1. 加载页面并调整 Config。
2. 打开 `Cases` 面板。
3. 保存当前页面为测试用例。
4. 后续可从 Cases 面板重新打开。

### 使用 App 内调试

打开 `Debug` 面板，可查看：

- Console log
- 页面错误
- 页面状态
- Cookies
- localStorage / sessionStorage
- HTML source 快照
- 简化 Elements 摘要
- JS 执行结果
- 简单请求记录
- 下载请求记录

### 使用 Chrome DevTools 调试

1. 打开 Settings。
2. 开启 `Enable WebView debugging`。
3. 用 USB 连接设备到电脑。
4. 在桌面 Chrome 打开 `chrome://inspect`。
5. 找到当前 App 的 WebView 并点击 inspect。

该开关默认关闭，包括 release 包。

## 当前限制

当前版本不是完整 Chrome DevTools 替代品。尚未实现：

- JS breakpoint / step debugging
- Performance timeline
- Memory profiling
- 完整 Network headers/body 捕获
- 完整 Elements inspector 与 live CSS 编辑
- 多标签 / 多 WebView session
- 云同步或团队共享
- DownloadManager 完成/失败终态回写

详细未完成项和路线图见 [工作总结](docs/2026-06-05-webview-test-app-summary.md)。

## 开发注意事项

- WebView 层只负责应用配置和发出事件，不直接读写数据库。
- ViewModel 是 UI、持久化和 WebView event 的边界。
- 本地 HTML 使用系统文件选择器，避免申请宽泛存储权限。
- Web 权限按 `WebTestConfig` 策略处理：`ALLOW`、`DENY`、`ASK_EVERY_TIME`。
- Release WebView debugging 必须由 Settings 开关手动开启，不能默认开启。

## 当前工作区状态

功能提交完成后，仓库中可能仍有本地 IDE/构建产物或计划文档未提交，例如 `.idea/`、`.kotlin/`、`docs/superpowers/plans/`。这些不属于 App 功能代码，需要按团队约定单独决定是否提交、忽略或清理。
