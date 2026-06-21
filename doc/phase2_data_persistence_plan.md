# 第二阶段：数据持久化与互联互通开发计划 (Phase 2)

本阶段核心目标是彻底脱离 Mock 数据，为手表待办应用引入 **Room 数据库** 并在底层打通 **Wear OS Data Layer API**，实现手机下发数据并实时响应到 UI。

根据评估与技术决策，本阶段任务划分为以下四大核心步骤：

## 阶段一：项目依赖升级与配置 (环境搭建)
1. **修改 `libs.versions.toml`**：
   - 添加 Room 相关依赖（版本 2.6.1）。
2. **修改 `app/build.gradle.kts`**：
   - 引入 `com.google.devtools.ksp` 插件（实际执行中因 AGP 9.0 兼容性问题，已废弃 KAPT 并改用 KSP，并将 Kotlin 版本规范为 2.0.0）。
   - 引入 `room-runtime`、`room-ktx` 以及 `ksp` 编译器依赖。
   - 同步 Gradle 确保环境正常无报错。

## 阶段二：Room 数据库核心构建 (本地数据层)
1. **升级 Entity**：修改 `RadarTask.kt`，将其打上 `@Entity(tableName = "tasks")` 注解，并将 `id` 变更为 `@PrimaryKey(autoGenerate = true) val id: Int = 0`。
2. **构建 DAO**：创建 `TaskDao.kt`，提供基于协程 Flow 的 `getAllTasks()` 响应式查询方法，以及 `insert`、`update`、`delete` 挂起函数。
3. **构建 Database**：创建 `AppDatabase.kt` 单例。并实现 `RoomDatabase.Callback`，在数据库首次创建的 `onCreate` 时期，自动触发协程写入我们原有的 12 条 Mock 数据以保持初始界面的视觉饱满。
4. **构建 Repository**：创建 `TaskRepository.kt`，封装 DAO 提供的方法，作为 UI 和 Service 访问数据的唯一出口。

## 阶段三：TaskViewModel 响应式流重构 (状态迁移)
1. **ViewModel 改造**：为 `TaskViewModel` 增加 `TaskRepository` 构造参数。
2. **状态流替换**：废弃原有的 `mutableStateListOf`。使用 `repository.getAllTasks().stateIn(...)` 将数据库中的内容转化为 Compose 可观察的 `StateFlow` 或 `State`。
3. **操作方法修改**：修改原先的 `addTask`、`updateTaskPriority`、`pinToTop`、`completeTask` 等方法，全部转调 Repository 中的挂起函数。
4. **依赖注入 (DI) 配置**：不引入复杂 DI，创建一个自定义的 `ViewModelProvider.Factory`，在 `MainActivity` 获取 application 级别的 `AppDatabase` 并实例化 ViewModel。
5. **UI 编译验证**：修改相关 Compose 界面中对 `tasks` 的引用逻辑，确保能成功编译并运行应用，测试预置数据是否成功加载、长按修改/置顶等功能是否正常持久化。

## 阶段四：Wear OS Data Layer 互联层 (后台通讯)
1. **创建后台服务**：新建 `TaskDataListenerService.kt` 并继承 `WearableListenerService`。
2. **监听通道**：重写 `onDataChanged` 方法，监听指定路径（例如 `/new_task`）。
3. **数据解析与存库**：将手机端发来的 `DataMap` 解析为新的 `RadarTask`，并调用 `TaskRepository.insertTask()` 插入数据库。依靠我们在阶段三搭建的 Flow，UI 会自动响应该更新。
4. **服务注册**：在 `AndroidManifest.xml` 中配置 `<service>` 标签并注册 `com.google.android.gms.wearable.DATA_CHANGED` 的 `intent-filter`。

---
> **当前状态**：**100% 完工**。所有四个阶段的代码修改均已落实。
> **附注**：实际执行中解决了 Kotlin 版本与 KAPT 的兼容问题，重构方案平稳落地。相关功能的验证方法可参考 `walkthrough.md`。
