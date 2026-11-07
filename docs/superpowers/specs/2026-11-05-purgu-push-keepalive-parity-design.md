# Purgu 推送与保活 1:1 对齐设计

## 目标

将 Purgu 当前实现的本地推送、FRC 默认配置与刷新、Firebase 和中台埋点、FCM topic 静默唤醒、保活调度、常驻通知栏、桌面快捷方式及通知权限流程接入 cleansuperai。保留本项目已有功能和未提交改动。场景触发条件、频控、文案、通知样式、点击路由、事件字段与 Purgu 对照验证。

## 现状与关键差异

| 链路 | Purgu | cleansuperai |
| --- | --- | --- |
| 本地推送 | `LocalPushConfig` 内置 39 个 scene，3 个 bucket、7 个模板；`LocalEventPushBootstrap` 启动监听与调度 | 仅 `CleanSuperAiApp` 退后台 5 秒通知；无场景引擎 |
| FRC | `FrcBootstrap` 统一 defaults；`local_push_config_v1`、`kp_config_v1`，双分段及快照恢复 | 未引入 Firebase Remote Config |
| 保活 | 前台服务、开机、闹钟、快速/心跳 Job、Home 键、FCM、前后台与通知删除自愈 | 无 `keepalive` 组件和相关 Manifest 声明 |
| FCM | 订阅 `purgu_wake`，高优先级或 `kpa_wake`/`wake` 数据消息触发保活；失败指数退避重试 | 仅展示普通 FCM 通知，未订阅 topic |
| 通知 UI | 7 套场景模板、折叠/展开 RemoteViews、常驻工具栏 | 只有系统标准通知 |
| 埋点 | Firebase `ka_*`、`lp_funnel`；中台 10008 保活/本地推送、10018 FCM、10019 进程心跳、10009 用户路径 | 这几条链路均未接入 |
| 权限 | 启动页 `MainActivity` 创建时立即申请通知权限；进入 `HomeActivity` 后约 500ms 若仍未授权再申请一次，结果返回后继续存储权限；拒绝次数决定首页提示是系统授权还是设置页 | `SplashActivity` 不申请；HomeFragment 在媒体权限后才申请通知权限，拒绝状态为单个布尔值 |
| 快捷方式 | `shortcuts.xml` 4 个静态入口，点击进入对应路由 | 只有桌面小组件，无静态快捷方式 |

## 迁移方案

1. **基础依赖与配置**：引入 Remote Config 依赖；移植 `DualSegmentJson`、`UserSegmentHelper`、`FrcBootstrap`、推送/保活配置、有效快照、冷启动和刷新时序。保留 `local_push_config_v1`、`kp_config_v1` 的 schema 与默认值；核实 Firebase 项目实际线上值，不把 Purgu 的 app id 当成本项目配置。
2. **本地推送引擎**：移植 39 场景的触发、7 套模板和通知资源，以及全局/桶/场景三层频控、未点击停止、重复提交、通知去重/分组/上限、展示与点击账本。将 Purgu Activity router 映射到本项目 `MainActivity` Fragment 路由；扫描完成、清理完成和中断等场景接入现有清理流程。
3. **后台可靠性**：接入截图、包变化、充电/电量、网络、屏幕、下载、MediaStore、退后台和未打开监听；移植定点 Alarm、恢复 Job 与重排逻辑；Manifest 注册组件及必要权限。移除旧的 5 秒退后台通知，避免双发。
4. **保活与 FCM**：接入协调器、前台服务通知、启动/重启与各种 Job/Alarm/Receiver；在 FCM 服务中识别高优先级和 wake data，并按开关补链。topic 应使用本项目服务端约定的主题；若服务端沿用 Purgu 的 `purgu_wake`，保持该字面值并做投递验证。
5. **埋点**：Firebase `ka_outcome`、`ka_service`、`ka_alive_gap`、`lp_funnel` 与对应参数；中台 10008、10018、10019，以及用户路径 10009。接入启动/首页权限展示与结果、功能点击、进程心跳和 FCM 到达埋点。当前项目网络接口与 Purgu 有差异，应封装适配器核对请求参数和响应，不能只复制 event type。
6. **权限与入口**：在本项目 `SplashActivity` 创建时执行 Purgu 启动页的第一次请求；进入 `MainActivity` 首页后约 500ms，未获准时执行第二次请求，结果返回后继续存储权限；两次请求分别埋点。持久化拒绝次数，达到 2 次后首页提示跳转应用通知设置；获准后启动常驻通知。新增四个静态快捷方式并复用当前页面导航；常驻通知点击路径逐个验收。小组件保持本项目现有触发点。

## 实施顺序与验收

按以上 1–6 顺序分阶段提交。每阶段需通过 `assembleDebug`；使用同一设备、相同权限和 FRC JSON 对照 Purgu：39 场景逐个验证触发/跳过/文案/折叠与展开/点击目的地；重启、强杀、开机、通知删除及 FCM 高优先级 data 消息验证恢复；Firebase DebugView 和中台测试接收端核对事件字段与去重。Android 版本至少覆盖 12、13、14/15，记录 OEM 对后台调度的差异。

## 还需核实的功能差异

现有用户功能对齐工作尚未提交，不能把文件存在视为功能已验收。完成推送迁移后，用 Purgu 全部页面/入口为清单复验：文件与媒体候选集及删除结果、应用卸载与流量范围、隐私空间导入/恢复、设置与小组件、所有权限返回路径；逐项记录通过、差异和设备证据。

静态审查还发现以下独立差异，需要纳入验收清单：

- Purgu 的 `ProcessHeartbeatTracker`/Job/Store 与中台 10019，及启动页 Home 键路径、功能点击等 10009 漏斗；当前项目没有对应实现。
- Purgu 的 organic/paid 分段锁定、FRC 有效快照恢复、热启动去重与变更后调度重排；这些不能只靠复制默认 JSON 达成。
- Purgu 通知文案资源覆盖多个语言目录，而当前项目的通知相关资源尚未迁入；1:1 UI 需同时核对语言、折叠/展开布局和通知渠道。

此轮只完成静态代码差异审查，未完成运行时 1:1 验证，也不能宣称已穷尽所有产品功能差异。

## 边界与风险

Purgu 的 app id、Firebase 项目、topic 发布端、中台鉴权和广告配置不能机械复制。Android 后台限制可能使某些路径受系统策略影响；验收标准是与 Purgu 在同设备、同系统状态下的实际行为一致。当前工作树有大量用户改动，迁移期间必须保留并避免覆盖。
