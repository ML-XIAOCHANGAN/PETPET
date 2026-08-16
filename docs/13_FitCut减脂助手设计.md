# 13 FitCut 减脂助手（内嵌轻量版）设计文档

> 2026-08 · 需求来源：用户提供完整 App 规格，经确认落地为 **内嵌 PETPET 成长页的轻量模块**。
> 技术路线：沿用项目现有技术栈（Kotlin + Compose + DataStore + JSON 编解码 + Canvas 自绘），
> **不引入** Room / Hilt / Retrofit / Navigation / MPAndroidChart，保持极简零依赖。

## 一、功能定位

- 名称：FitCut 减脂助手
- 目标：根据用户身体数据生成个性化减脂计划，追踪每日饮食与运动，可视化进度
- 入口：成长页（GrowthScreen）新增「FitCut 减脂助手」卡片 → 点击进入全屏模块
- 价值：科学减脂（BMR/TDEE 计算 + 热量缺口）+ 低门槛记录（内置食物库/训练库）

## 二、架构决策

| 维度 | 决策 |
|---|---|
| 架构 | MVVM（AndroidViewModel + StateFlow），与 PetPetViewModel 同款模式 |
| 存储 | 独立 DataStore 文件 `fitcut_preferences`，JSON 编解码（复用 PersistenceCodecs 模式） |
| 网络 | 预留接口（FitCutFoodSearchClient 空实现），本期全本地 |
| 图表 | Canvas 自绘（折线图 / 进度环 / 条形对比），不引第三方库 |
| 页面容器 | FitCutScreen 全屏覆盖（PetPetApp 层 showFitCut 状态控制），内部 5 tab 状态切换 |
| 深色模式 | 跟随全局 darkTheme（PetPetTheme 已有） |
| 主题 | 健康绿主色（#22c55e 系）+ 白色 + 橙色提醒（#f97316），模块内局部覆盖 |

### 目录结构（新增文件）

```
data/fitcut/
  FitCutModels.kt           # UserProfile / FoodItem / MealRecord / WorkoutRecord / WeightRecord / Achievement
  FitCutCalculator.kt       # BMR / TDEE / 热量缺口 / 营养素目标 / BMI / 周计划
  FitCutFoodLibrary.kt      # 内置食物库（40+ 常见食物，含热量与三大营养素）
  FitCutExerciseLibrary.kt  # 内置训练库（有氧 4 + 力量 5 肌群 × 动作）
  FitCutRepository.kt       # DataStore 持久化 + JSON 编解码 + 记录 CRUD
ui/screens/fitcut/
  FitCutScreen.kt           # 全屏容器 + 底部 5 tab（概览/饮食/运动/统计/我的）
  FitCutOnboarding.kt       # 首次引导：档案输入 → BMR/TDEE 展示 → 计划生成
  FitCutDashboard.kt        # 今日概览
  FitCutDietScreen.kt       # 饮食记录
  FitCutExerciseScreen.kt   # 运动追踪
  FitCutStatsScreen.kt      # 数据统计
  FitCutProfileScreen.kt    # 我的（档案/目标/关于）
ui/components/
  FitCutCharts.kt           # 自绘图表：ProgressRing / LineChart / BarCompare / MacroPie
```

## 三、数据模型

```kotlin
enum class Gender { MALE, FEMALE }
enum class ActivityLevel(val factor: Double, val label: String) {
    SEDENTARY(1.2, "久坐"), LIGHT(1.375, "轻度"), MODERATE(1.55, "中度"), HEAVY(1.725, "重度")
}
enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }   // 早/午/晚/加餐

data class UserProfile(
    gender: Gender, age: Int, heightCm: Int, weightKg: Double,
    targetWeightKg: Double, activity: ActivityLevel, createdAt: String,
)

data class FoodItem(
    id: String, name: String, caloriesPer100g: Double,   // 每 100g kcal
    proteinPer100g: Double, carbPer100g: Double, fatPer100g: Double, isCustom: Boolean = false,
)

data class MealRecord(
    id: Long, date: String,               // yyyy-MM-dd
    mealType: MealType, foodId: String, foodName: String,
    grams: Double,                        // 实际克数
    calories: Double, protein: Double, carb: Double, fat: Double,  // 按克数折算后的快照
    createdAt: String,
)

data class WorkoutRecord(
    id: Long, date: String, exerciseName: String, category: String,  // 有氧/力量
    minutes: Int, calories: Double, createdAt: String,
)

data class WeightRecord(
    id: Long, date: String, weightKg: Double, createdAt: String,
)

data class Achievement(
    id: String, title: String, icon: String, unlocked: Boolean, progress: Int, target: Int,
)
```

### 计算逻辑（FitCutCalculator）

```
BMR（Mifflin-St Jeor）：
  男 = 10×体重kg + 6.25×身高cm − 5×年龄 + 5
  女 = 10×体重kg + 6.25×身高cm − 5×年龄 − 161
TDEE = BMR × 活动系数（1.2/1.375/1.55/1.725）
热量缺口 = clamp(目标体重差×7700 ÷ 计划天数, 300, 500)   // 7700 kcal ≈ 1kg 脂肪
  目标体重差 ≤ 0 → 缺口 0（维持/增肌模式提示）
推荐摄入 = TDEE − 缺口
营养素目标（占推荐摄入热量）：
  蛋白质 30% → 4 kcal/g；碳水 40% → 4 kcal/g；脂肪 30% → 9 kcal/g
周目标 = 0.5~1 kg/周（按总差值/周数，clamp 到健康区间）
计划周数 = clamp(round(差值×7700 / (3500)), 4, 12)      // 每周约 3500 kcal 缺口 ≈ 0.45kg
BMI = 体重kg / (身高m)²
```

## 四、页面流程

```
成长页 → [FitCut 减脂助手卡片] → FitCutScreen
  ├─ 未建档 → FitCutOnboarding（3 步：档案输入 → 计算展示 → 计划确认）
  └─ 已建档 → 底部 5 tab：
      概览 Dashboard：今日热量进度环（摄入/目标）、运动打卡卡、体重趋势迷你折线、连续打卡天数、成就入口
      饮食 Diet：早/午/晚/加餐分组列表 + 当日汇总（剩余热量 + 蛋白质/碳水/脂肪三环）
                 + 添加食物（搜索内置库 / 自定义录入，选克数）
      运动 Exercise：训练库（有氧 4 项 + 力量 5 肌群）→ 打卡弹窗（选时长）→ 历史列表
      统计 Stats：体重折线（近 30 天）、摄入 vs 消耗条形对比、营养素占比环、BMI 变化
      我的 Profile：档案展示与编辑（重走引导）、目标体重/缺口调整、关于、数据重置
```

## 五、内置库

- 食物库：40+ 常见食物（米饭、鸡胸肉、鸡蛋、牛奶、香蕉、燕麦、西兰花…），
  每项含每 100g 热量/蛋白/碳水/脂肪，搜索按名称模糊匹配
- 训练库：有氧（跑步/跳绳/游泳/骑行，含 kcal/min 系数）；力量 5 肌群（胸/背/腿/肩/腹），
  每动作含名称、目标肌群、要领文字、组数次数建议

## 六、成就系统（计算型，无需单独存解锁状态）

- 累计减重 2kg / 5kg（体重记录首尾差）
- 连续打卡 7 / 30 天（运动记录日期连续）
- 首次记录饮食 / 首次运动打卡
- 达标日 ×7（当日摄入 ≤ 推荐摄入）

## 七、提醒（轻量实现）

不引入 AlarmManager/WorkManager。在概览页做「今日待办」提示卡（饮水 8 杯、记录三餐、运动打卡），
打卡后自动勾选——软提醒，符合「简单可靠」原则。系统级通知留待后续版本。

## 八、实现顺序（本次交付）

1. 数据层：FitCutModels + Calculator + FoodLibrary + ExerciseLibrary + Repository
2. 引导页 Onboarding（档案 → 计算 → 计划）
3. FitCutScreen 容器 + 5 tab
4. 概览 + 图表组件（ProgressRing / LineChart / BarCompare）
5. 饮食记录
6. 运动追踪
7. 统计页 + 成就
8. 成长页入口 + 构建 + 模拟器验收

## 九、验收标准

- 首次进入走引导，建档后直达概览
- 饮食添加后当日热量/营养素实时更新
- 运动打卡后历史与连续天数更新
- 统计页图表随数据变化
- 深色模式可用；构建 + 单测全绿；模拟器截图确认
