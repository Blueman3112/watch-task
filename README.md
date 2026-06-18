# Watch Task — 手表端螺旋任务管理

基于 Wear OS + Jetpack Compose 的智能手表任务管理应用，采用螺旋形可视化布局展示任务。

## ✨ 特性

- 🌀 **螺旋可视化** — 任务以螺旋形排布，直觉式呈现优先级与顺序
- 🟢🔴 **双色状态** — 绿色正常 / 红色警戒，一眼识别紧急程度
- 📋 **三类任务来源** — 手动输入、日历自动生成、微信导入
- ⌚ **表冠交互** — 旋转表冠滚动浏览任务
- 🎯 **置顶 & 完成** — 长按进入置顶模式，快速管理任务优先级

## 🛠 技术栈

| 技术 | 说明 |
|------|------|
| Kotlin | 开发语言 |
| Jetpack Compose for Wear OS | UI 框架 |
| Wear OS Material3 | 设计规范 |
| Canvas API | 螺旋动画绘制 |
| ViewModel | 状态管理 |

## 📁 项目结构

```
app/src/main/java/com/example/test0512/
├── model/
│   └── RadarTask.kt          # 数据模型（任务、优先级、来源）
├── presentation/
│   ├── MainActivity.kt        # 主界面 & 导航
│   ├── TaskViewModel.kt       # 状态管理
│   └── components/
│       └── TaskComponents.kt  # 螺旋画布、任务详情、添加任务等组件
└── data/
    ├── AppDatabase.kt         # Room 数据库
    ├── TaskDao.kt             # 数据访问
    └── TaskEntity.kt          # 数据库实体
```

## 🚀 构建运行

1. 使用 Android Studio 打开项目
2. 连接 Wear OS 设备或启动 Wear OS 模拟器
3. 运行 `app` 模块

## 📄 文档

- [指导老师反馈意见整理](doc/指导老师反馈意见整理.md)
- [阶段性任务规划](doc/阶段性任务规划.md)
