package cn.petpet.app.data.fitcut

/**
 * 内置常见食物库（每 100g 热量 kcal / 蛋白质 g / 碳水 g / 脂肪 g）。
 * 数据为常见估算值，仅作记录参考。
 */
object FitCutFoodLibrary {

    val foods: List<FoodItem> = listOf(
        // 主食
        FoodItem("rice", "米饭", 116.0, 2.6, 25.9, 0.3),
        FoodItem("noodle", "面条（煮）", 110.0, 3.6, 22.0, 0.5),
        FoodItem("oatmeal", "燕麦片", 367.0, 13.2, 61.7, 6.7),
        FoodItem("sweet_potato", "红薯", 86.0, 1.6, 20.1, 0.1),
        FoodItem("corn", "玉米", 112.0, 4.0, 22.8, 1.2),
        FoodItem("whole_bread", "全麦面包", 246.0, 9.0, 43.0, 4.2),
        // 肉蛋奶
        FoodItem("chicken_breast", "鸡胸肉", 133.0, 24.6, 0.0, 3.4),
        FoodItem("beef_lean", "瘦牛肉", 143.0, 20.2, 0.0, 6.2),
        FoodItem("pork_loin", "猪里脊", 143.0, 20.2, 0.0, 6.2),
        FoodItem("salmon", "三文鱼", 208.0, 20.4, 0.0, 13.4),
        FoodItem("shrimp", "虾仁", 93.0, 18.6, 0.0, 0.8),
        FoodItem("egg", "鸡蛋", 144.0, 13.3, 2.8, 8.8),
        FoodItem("egg_white", "蛋白", 52.0, 11.0, 0.7, 0.1),
        FoodItem("milk", "纯牛奶", 65.0, 3.3, 4.9, 3.6),
        FoodItem("yogurt", "无糖酸奶", 62.0, 3.2, 4.9, 3.4),
        FoodItem("tofu", "北豆腐", 84.0, 8.1, 4.2, 3.7),
        // 蔬菜
        FoodItem("broccoli", "西兰花", 36.0, 4.1, 4.3, 0.6),
        FoodItem("spinach", "菠菜", 28.0, 2.6, 4.5, 0.3),
        FoodItem("cucumber", "黄瓜", 16.0, 0.8, 2.9, 0.2),
        FoodItem("tomato", "西红柿", 20.0, 0.9, 4.0, 0.2),
        FoodItem("lettuce", "生菜", 15.0, 1.3, 2.0, 0.3),
        FoodItem("carrot", "胡萝卜", 32.0, 1.0, 8.0, 0.2),
        FoodItem("mushroom", "蘑菇", 24.0, 2.7, 4.1, 0.1),
        // 水果
        FoodItem("banana", "香蕉", 93.0, 1.4, 22.0, 0.2),
        FoodItem("apple", "苹果", 53.0, 0.4, 13.7, 0.2),
        FoodItem("orange", "橙子", 48.0, 0.8, 11.1, 0.2),
        FoodItem("blueberry", "蓝莓", 57.0, 0.7, 14.5, 0.3),
        FoodItem("strawberry", "草莓", 32.0, 1.0, 7.1, 0.2),
        FoodItem("kiwi", "猕猴桃", 61.0, 0.8, 14.5, 0.6),
        // 坚果豆类
        FoodItem("almond", "杏仁", 579.0, 21.2, 21.6, 49.9),
        FoodItem("walnut", "核桃", 654.0, 14.9, 13.7, 58.8),
        FoodItem("peanut", "花生", 567.0, 24.8, 16.1, 44.3),
        FoodItem("soy_milk", "无糖豆浆", 31.0, 3.0, 1.2, 1.6),
        // 其他
        FoodItem("olive_oil", "橄榄油", 884.0, 0.0, 0.0, 100.0),
        FoodItem("avocado", "牛油果", 160.0, 2.0, 8.5, 14.7),
        FoodItem("protein_shake", "蛋白粉（1勺）", 120.0, 24.0, 3.0, 1.5),
        FoodItem("black_coffee", "黑咖啡", 2.0, 0.1, 0.0, 0.0),
        FoodItem("tea", "无糖茶", 1.0, 0.0, 0.2, 0.0),
    )

    /** 按名称模糊搜索（忽略大小写，命中名称包含关键字）。 */
    fun search(keyword: String): List<FoodItem> {
        val kw = keyword.trim()
        if (kw.isEmpty()) return foods
        return foods.filter { it.name.contains(kw, ignoreCase = true) }
    }

    fun byId(id: String): FoodItem? = foods.firstOrNull { it.id == id }
}
