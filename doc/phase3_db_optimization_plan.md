# 第三阶段：数据库底层结构重构与优化计划

为了彻底解决单机版 Demo 遗留的各类底层数据缺陷（详见我们在前一阶段分析的缺乏 UUID、时间戳类型错误、假删除等问题），并为未来的多端（手机-手表）数据同步打下坚实的基础，我们将实施以下分阶段优化方案：

## 阶段一：实体升级与数据库迁移准备 (Entity & DB)
**目标**：重构底层数据结构，引入分布式唯一标识与审计字段。
1. **重构 `RadarTask` 数据类**：
   - 将 `id` 字段由自增 `Int` 替换为 `String`，作为全局唯一的 UUID。
   - 新增布尔类型 `isCompleted` 字段，默认值为 `false`。
   - 新增时间戳字段：`dueDate: Long?`（真正的截止时间）、`createdAt: Long`（创建时间）和 `updatedAt: Long`（更新时间）。
2. **定义类型转换器**：
   - 编写 `Converters` 类，通过 `@TypeConverter` 显式规定 `TaskPriority` 和 `TaskSource` 枚举的字符串存储转换，防止未来重命名导致的崩溃。
3. **数据库升级机制**：
   - 修改 `AppDatabase` 将 `version` 从 1 提升至 2。
   - 由于当前仅存在测试数据，为提高开发效率，我们将采用 `fallbackToDestructiveMigration()` 清除旧表并重建结构，然后继续通过 Callback 填入升级版的新格式 Mock 数据。

## 阶段二：DAO 层逻辑演进与复杂查询 (DAO & Repo)
**目标**：适配新表结构，强化排序与状态过滤。
1. **重写查询逻辑**：
   - 更改 `getAllTasks()` 的 SQL 语句，例如：`ORDER BY isCompleted ASC, priority DESC, sortOrder ASC, createdAt DESC`。这将建立极其科学的列表顺序（未完成优先 -> 紧急优先 -> 置顶优先 -> 新创建优先）。
2. **业务方法适配**：
   - 修改 `TaskDao` 和 `TaskRepository`，将涉及 ID 的参数全部从 `Int` 替换为 `String`。
   - 实现“软删除”：新增 `markTaskAsCompleted(id: String, updateTime: Long)` 替代原先直接 DELETE 的物理删除。

## 阶段三：ViewModel 与 UI 层对接 (ViewModel & UI)
**目标**：修复业务逻辑，完成闭环。
1. **ID 生成逻辑修改**：
   - 在 `TaskViewModel.addTask` 方法中，自动调用 `UUID.randomUUID().toString()` 并记录当前的 `System.currentTimeMillis()`。
2. **状态流更新逻辑**：
   - 更新所有涉及修改的操作，在改变对象状态时，同步更新 `updatedAt` 字段。
3. **UI 编译适配**：
   - 修改 Compose 组件层（如 `TaskDetailScreen` 和 Navigation 等路由跳转逻辑）所传递的 `taskId` 类型，将其统统由 `Int` 适配为 `String`。

## 阶段四：跨端互联基础构建 (Data Layer)
**目标**：升级手表接收端的解析逻辑。
1. **Service 参数对齐**：
   - 修改 `TaskDataListenerService`，从 `DataMap` 解析额外的元数据字段（如 uuid、createdAt 等）。
   - 增加基本的冲突解决策略（例如基于 updatedAt 的 Last-Write-Wins 规则，防止旧数据覆盖新数据）。
