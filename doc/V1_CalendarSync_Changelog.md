# V1.1 版本更新日志：双端日历同步架构 (DataLayer)

相比于仅拥有独立运行能力的基础版本，本次更新实现了跨设备的系统级日历无缝接入，彻底打通了任务数据的外部输入通道。

## 核心架构重构
1. **双端分离架构 (Mobile & Wear)**
   - **新增 Mobile 模块**：新建了一个名为 `WatchTask Bridge` 的伴侣应用（部署于手机端）。其唯一职责是作为获取手机 `READ_CALENDAR` 权限的静默跳板。
   - **DataLayer 通信层**：弃用了在 Wear OS 独立请求非标准日历 API 的尝试，全面改用 Google 官方的高优蓝牙/TCP 通道（Wearable DataLayer API）。

## 手机端 (Mobile) 变更
1. **WearSyncService 服务层**
   - 实现了一个后台驻留服务 `WearSyncService`，监听来自手表的 `/sync_calendar` 指令。
   - 在收到指令时，安全地读取系统原生 `CalendarContract`，查询未来 7 天内非全天（剔除节假日/生日）的活动日程。
   - 将日程数据序列化为 JSON 格式，通过 `PutDataMapRequest` 实时推送到手表端。

## 手表端 (Wear) 变更
1. **CalendarSyncManager 管理器**
   - 新增 `CalendarSyncManager` 负责向下游发送唤醒指令（`MessageClient`），并监听上游的数据返回（`DataClient`）。
   - **防重机制**：依赖数据表新增的 `externalId` 字段，实现日历事件的去重过滤。
2. **魔法标签解析系统 (Tagging System)**
   - 实现了对系统日历标题的智能清洗与优先级映射。通过识别标题前缀（如 `!紧急`、`!常规`），动态替换硬编码的优先级，完美融入雷达的四级紧迫度算法。
3. **UI 与底层逻辑融合**
   - 修复了双端的时间显示差异，统一输出 `MM/dd HH:mm` 的格式化字符。
   - 在 `AddTaskScreen` 中将伪造的日历数据替换为对真实物理链路的调用。
4. **数据库升级**
   - Room 数据库升级至 `version 8`，新增 `externalId` 字段，支持全量破坏性迁移。
