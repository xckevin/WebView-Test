# WebView 测试 App 工作总结

日期：2026-06-05

## 项目目标

这个项目的目标是做一个 Android 设备上的 H5/WebView 测试工具。它不是普通浏览器，而是给混合应用、活动页、落地页、内嵌 H5 调试用的单会话测试工作台。

核心诉求包括：

- 通过手动输入、粘贴或扫码加载 H5 页面。
- 保存访问历史和可复现测试用例。
- 在 App 内预览页面并查看基础调试信息。
- 提供类似 Chrome DevTools 的常用能力，并可在 release 包中通过 Settings 手动打开 WebView debugging，连接电脑 Chrome 调试。
- 支持 Android/WebView 独有能力切换，用于测试不同环境和权限场景。

## 已完成内容

### 1. 项目基础与依赖配置

已完成 Android 单模块 Kotlin + Compose 工程配置。

已接入：

- Jetpack Compose + Material3
- Navigation Compose
- Lifecycle ViewModel/Runtime Compose
- Room
- DataStore Preferences
- CameraX
- ML Kit Barcode Scanning
- kotlinx.serialization JSON
- JUnit、AndroidX Test、Compose UI Test

Manifest 已配置：

- `INTERNET`
- `CAMERA`
- `RECORD_AUDIO`
- `ACCESS_FINE_LOCATION`
- cleartext HTTP 测试支持：`network_security_config.xml`

### 2. 核心模型与 URL 处理

已完成核心模型：

- `WebTestConfig`
- `WebTestCase`
- `HistoryItem`
- `AppSettings`

已完成 URL 标准化：

- 支持 `http` / `https`
- 输入 `example.com` 会规范化为 `https://example.com`
- 拒绝空输入和不支持的 scheme
- local HTML 通过专门入口绕过远程 URL 标准化

已完成配置 JSON 序列化测试。

### 3. 本地持久化

已完成 Room + DataStore 本地存储：

- 测试用例持久化
- 历史记录持久化
- 全局 Settings 持久化
- `WebTestConfig` 与 Room 实体转换

已完成测试用例导入/导出：

- JSON export
- JSON import
- 版本校验
- payload 校验
- trim 后的名称冲突处理

### 4. App 导航与整体 UI

已完成 Compose 导航框架：

- Workbench 主工作台
- Scanner 扫码页
- Settings 设置页

Workbench 已完成响应式布局：

- 小屏底部面板
- 大屏/横屏侧边面板
- 单 WebView 实例稳定保留，切换面板和全屏时不销毁 WebView

### 5. Workbench 状态模型

已完成 `WorkbenchViewModel` 和状态机：

- URL 输入
- 加载状态
- progress
- current URL/title
- requested URL 和 active navigation 分离
- 防止 stale WebView event 覆盖当前页面
- 支持重复 URL reload
- 支持 WebView 内部点击跳转
- 支持刷新时保持 source type
- 支持本地文件 source type

这一块经过多轮修复，重点解决了 WebView 事件乱序、同 URL reload、内部导航、历史写入重复等问题。

### 6. WebView 宿主与配置切换

已完成原生 WebView 嵌入：

- `AndroidView` 承载 WebView
- `TestWebViewClient`
- `TestWebChromeClient`
- `WebViewNavigationTracker`
- `WebViewSettingsApplier`
- `WebViewController`

已支持的 WebView 配置：

- JavaScript 开关
- DOM Storage 开关
- Cookie 开关
- Third-party cookie 开关
- cache mode
- mixed content
- default/custom/desktop User-Agent
- desktop mode
- start fullscreen
- WebView back-first 行为

### 7. Workbench 功能面板

已完成：

- URL Bar：输入、加载、粘贴、扫码、刷新、设置、全屏、本地 HTML 入口
- Config Panel：WebView 配置切换
- Cases Panel：保存/打开/删除测试用例，导入/导出
- History Panel：查看/打开/清空历史
- Debug Panel：Console、Errors、Page、Cookies、Storage、Source、Elements、JS Exec、Requests、Downloads

### 8. App 内调试能力

已完成基础调试面板：

- Console log 捕获：`WebChromeClient.onConsoleMessage`
- 页面加载错误、HTTP error、SSL error 记录
- 页面状态：URL、title、progress、navigation id
- Cookies 读取和清除
- localStorage / sessionStorage 读取和清除
- HTML source 快照读取
- 简化 Elements 摘要读取
- JavaScript 执行
- 可见的简单请求记录
- 下载请求记录

说明：当前 Debug Panel 是设备侧常用调试能力，不等同于完整 Chrome DevTools。

### 9. 扫码能力

已完成内部扫码：

- CameraX Preview
- ML Kit Barcode Analyzer
- Camera permission 请求
- URL 识别
- 非 URL 结果展示
- 非 URL 可复制或按 URL 使用
- 扫码结果回传 Workbench 自动加载

### 10. Settings 与 release WebView debugging

已完成 Settings 页面：

- `Enable WebView debugging` 手动开关
- 默认关闭，包括 release 包
- 开关打开后调用 `WebView.setWebContentsDebuggingEnabled(true)`
- 页面展示 `chrome://inspect` 连接说明

已完成 reset actions：

- Reset history
- Reset WebView defaults
- Clear cookies
- Clear WebView cache

已完成 Settings 内测试用例导入/导出入口。

### 11. 高级 WebView 动作

已完成：

- 文件选择：`WebChromeClient.onShowFileChooser` + `ActivityResultContracts.OpenDocument`
- 文件选择 policy：`ALLOW` / `DENY`
- 文件选择并发请求取消和 release 清理
- 本地 HTML 加载：系统文件选择器、`content://` / `file://` 加载、尽量持久化读权限
- 本地文件历史：`SourceType.LOCAL_FILE`
- 本地文件 case reopen：按 URL 推断 source type，避免误记为 remote
- DownloadListener：记录 `WebPageEvent.DownloadRequested`
- HTTP/HTTPS 下载：通过系统 `DownloadManager`
- data URL 下载：记录但不保存
- 视频全屏：`onShowCustomView` / `onHideCustomView`
- Back/Exit fullscreen 退出视频全屏或普通全屏
- Web 权限处理：camera、microphone、geolocation
- 权限策略：`ALLOW` / `DENY` / `ASK_EVERY_TIME`
- Android runtime permission 联动
- per-resource grant：camera/microphone 可以按资源分别授权
- partial runtime grant：只 grant 实际通过 Android 权限的资源
- permission cancel/hide：处理 `onPermissionRequestCanceled` 与 geolocation hide
- stale prompt 防护：旧 prompt 操作不会误 grant/deny 已取消 request
- media/geolocation prompt 交叉覆盖防护
- 长按上下文菜单：Copy URL、Open in current session、Download URL、View resource URL
- email/phone/geo 类型不强行拦截，交回 WebView 默认行为

### 12. 测试与验证

已完成测试覆盖：

- URL 标准化
- WebTestConfig 序列化
- Case import/export
- Scanner parser
- Scanner ViewModel
- Settings ViewModel
- Workbench ViewModel
- Debug reducer
- PageScripts
- WebView settings applier
- WebView navigation tracker
- File chooser handler
- Web permission handler
- Compose Workbench 基础 UI

已完成最终验证：

```bash
./gradlew :app:testDebugUnitTest :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin
```

结果：通过。

已启动模拟器并执行端到端 connected Android 测试：

```bash
./gradlew :app:connectedDebugAndroidTest
```

设备：`Pixel_9_API_35(AVD) - 15`

结果：6 个 connected tests 全部通过。

测试完成后已关闭模拟器。

## 已提交记录

主要提交包括：

- `00f8d61 docs: add webview test app design`
- `7d85a2a chore: configure webview test app dependencies`
- `2722879 feat: add webview test models`
- `a15bc71 feat: add local persistence`
- `c452352 feat: add app navigation shell`
- `98e032f feat: add workbench state model`
- `f2da9b3 feat: add webview event host`
- `dde431b feat: build workbench UI`
- `a45c234 feat: add structured debug panel`
- `986cafb feat: add internal QR scanner`
- `05c1026 feat: add settings and import export`
- `213057c feat: add advanced webview actions`

中间还有多次针对 WebView navigation、reload、事件归属、import/export 校验的修复提交。

## 当前未完成内容

以下内容在 v1 设计中明确不做或暂未完整实现：

### 1. 完整 Chrome DevTools parity

当前 App 内 Debug Panel 只覆盖设备侧常用调试能力。

未完成：

- JS breakpoint
- Step over / step into / pause
- Sources 面板
- Performance timeline
- Memory profiling
- Network waterfall
- 完整 request/response headers/body
- WebSocket frame inspection
- CSS live editing

完整调试仍依赖 Settings 打开 WebView debugging 后，用电脑 Chrome 的 `chrome://inspect`。

### 2. 完整 Elements Inspector

当前 Elements 只是通过 JavaScript 读取常见元素摘要。

未完成：

- Chrome 风格 DOM tree
- 页面上点选元素
- 高亮元素边框
- 查看/编辑 computed style
- 实时修改 attributes / styles

### 3. Network 深度抓包

当前只记录 WebView 可见的导航/请求 URL 与 download metadata。

未完成：

- 请求 headers
- 响应 headers
- response body
- POST body
- timing breakdown
- redirect chain 详情

如果未来要实现，需要考虑代理、Cronet、ServiceWorker、WebViewClient 能力限制或外部抓包方案。

### 4. DownloadManager 终态追踪

当前已做到：

- WebView 下载请求回调记录
- HTTP/HTTPS 交给系统 DownloadManager
- enqueue 同步失败写入 debug message

未完成：

- 保存 DownloadManager id
- 监听 `ACTION_DOWNLOAD_COMPLETE`
- 查询 `COLUMN_STATUS` / `COLUMN_REASON`
- 在 Debug Panel 展示下载成功/失败终态

### 5. 更完整的本地文件测试

当前已支持通过系统文件选择器加载 local HTML。

未完成：

- local HTML 相对资源引用的完整兼容测试
- 多文件目录授权
- assets/fixtures 一键加载
- local file load failure 的专门 UI 状态

### 6. 更完整的权限测试矩阵

当前已支持 geolocation/camera/microphone 的策略和 runtime permission 联动。

未完成：

- 权限测试专用 fixture 页面
- 权限请求结果的更细粒度 UI 展示
- 权限 revoke 后自动提示重新测试
- Android 不同 API level 的权限行为矩阵

### 7. 多会话/多标签

当前是单会话 Workbench。

未完成：

- 多标签
- 多 WebView session 并行
- 每个 session 独立 debug state
- session 之间复制配置

### 8. 云同步和团队协作

当前所有数据都在本地。

未完成：

- 测试用例云同步
- 团队共享 case
- 导入导出之外的远程配置分发

### 9. Release 包人工调试验收

已实现 release debugging 开关逻辑。

仍建议补充一次人工验收：

- 构建 release 包
- 安装到真机
- 默认确认 `chrome://inspect` 不可见
- 在 Settings 打开开关
- 确认电脑 Chrome 可 inspect 当前 WebView
- 关闭开关后确认不可 inspect

## 后续建议路线图

### P0：让 v1 更适合真实测试使用

- 补权限测试 fixture 页面，覆盖 camera/mic/geolocation。
- 补 local HTML fixture 页面，覆盖 storage、source、elements、download。
- 在 Debug Panel 展示当前 source type、权限策略、desktop mode 等当前环境摘要。
- DownloadManager 增加下载完成/失败状态回写。
- 增加 release 包 connected/manual 验收脚本说明。

### P1：增强调试能力

- Requests 增加 redirect/main-frame/resource 分类。
- Source/Elements 结果做格式化展示，而不是只显示 JSON/string。
- Elements 支持搜索、按 tag/id/class 过滤。
- Storage 支持单 key 删除。
- Cookies 支持按当前 URL 展示结构化 key/value。

### P2：增强测试用例体系

- Case 增加分组/tag。
- Case 增加 source type 字段，而不是仅靠 URL 推断。
- Case 支持复制、重命名、批量删除。
- Case 支持最近运行结果、最后一次错误摘要。
- 导入冲突处理从 ViewModel 状态完善到完整 UI 决策流。

### P3：更强的 WebView 环境模拟

- 增加屏幕尺寸/方向测试辅助。
- 增加 dark/light mode 快速切换。
- 增加网络环境提示或与系统设置联动。
- 增加自定义请求 header 的可行性调研。
- 增加 JS bridge 注入测试能力。

### P4：团队和自动化

- 增加 CI 中的 unit test 与 androidTest 编译。
- 增加 nightly emulator connected test。
- 输出测试报告。
- 支持测试用例文件作为仓库 fixture。
- 未来可考虑同步服务或共享配置中心。

## 当前工作区注意事项

最后一次功能提交后，仓库仍有以下未提交项：

- `.idea/misc.xml`
- `.idea/compiler.xml`
- `.idea/deploymentTargetSelector.xml`
- `.idea/markdown.xml`
- `.kotlin/`
- `docs/superpowers/plans/`

这些不是本次功能提交内容，应在后续单独判断是否保留、忽略或清理。

## 结论

v1 的核心能力已经完成：它可以作为一个单会话 Android WebView/H5 测试工作台使用，支持 URL/扫码/本地 HTML 加载、历史、测试用例、配置切换、设备侧基础调试、release WebView debugging 开关，以及 Android WebView 高级行为测试。

当前剩余工作主要集中在“更像完整 DevTools”“更深 Network/Elements 能力”“更完整 fixture 和自动化验收”“团队协作与多会话”这些增强方向。

## 2026-06-08 P0/P1 完成记录

已继续完成路线图中的 P0 + P1 能力：

- 新增内置权限 fixture：`file:///android_asset/fixtures/permission_fixture.html`，覆盖 camera、microphone、camera+microphone、geolocation 请求。
- 新增内置本地调试 fixture：`file:///android_asset/fixtures/local_debug_fixture.html`，覆盖 console、storage、cookie、elements、source、data URL download。
- Workbench URL overflow 菜单新增两个 fixture 入口，可直接加载上述内置页面。
- Debug Panel 的 Page tab 新增当前环境摘要：source type、JavaScript、DOM storage、desktop mode、UA mode、cache/mixed content、cookie 与权限策略。
- DownloadManager 下载请求现在记录 download id、文件名、初始状态，并在系统下载完成广播后查询终态，回写 success/failed/unknown、reason、local URI。
- Requests 记录新增分类：`main-frame`、`resource`、`redirect`。
- Source/Elements/Storage/Cookies 结果展示改为结构化格式化输出，减少原始 JSON/string 直出。
- Elements 摘要支持 CSS selector 和 tag/id/class/text 搜索过滤。
- Storage 支持按单 key 删除 localStorage 或 sessionStorage。
- Cookies 读取按当前 URL 返回，并在面板中按 key/value 结构化展示。
- 新增 release WebView debugging 验收说明：`docs/release-webview-debugging-acceptance.md`。

新增/更新测试覆盖：

- `DebugReducerTest`
- `PageScriptsTest`
- `DebugResultFormatterTest`
- `WorkbenchViewModelTest`
- `StringResourceParityTest`
