package cn.petpet.app.data.pethealth

/** 内置品种库（猫常见品种 + 田园猫；狗按体型分类 + 中华田园犬）。 */
object PetBreedLibrary {

    val breeds: List<BreedOption> = listOf(
        // ---- 猫 ----
        BreedOption("cat_shorthair", "中华田园猫", PetKind.CAT, null, "3-5 kg", 3.0, 5.0),
        BreedOption("cat_british", "英国短毛猫", PetKind.CAT, null, "4-8 kg", 4.0, 8.0),
        BreedOption("cat_american", "美国短毛猫", PetKind.CAT, null, "3.5-6 kg", 3.5, 6.0),
        BreedOption("cat_ragdoll", "布偶猫", PetKind.CAT, null, "4-9 kg", 4.0, 9.0),
        BreedOption("cat_siamese", "暹罗猫", PetKind.CAT, null, "3-5 kg", 3.0, 5.0),
        BreedOption("cat_fold", "苏格兰折耳猫", PetKind.CAT, null, "2.5-5 kg", 2.5, 5.0),
        BreedOption("cat_maine", "缅因猫", PetKind.CAT, null, "5-11 kg", 5.0, 11.0),
        BreedOption("cat_persian", "波斯猫", PetKind.CAT, null, "3-6 kg", 3.0, 6.0),
        BreedOption("cat_mixed", "串串猫", PetKind.CAT, null, "3-6 kg", 3.0, 6.0),
        // ---- 狗（按体型）----
        BreedOption("dog_chihuahua", "吉娃娃", PetKind.DOG, "小型", "1.5-3 kg", 1.5, 3.0),
        BreedOption("dog_poodle_toy", "玩具贵宾", PetKind.DOG, "小型", "2-4 kg", 2.0, 4.0),
        BreedOption("dog_pomeranian", "博美", PetKind.DOG, "小型", "1.8-3.5 kg", 1.8, 3.5),
        BreedOption("dog_bichon", "比熊", PetKind.DOG, "小型", "3-5 kg", 3.0, 5.0),
        BreedOption("dog_pug", "巴哥", PetKind.DOG, "小型", "6-8 kg", 6.0, 8.0),
        BreedOption("dog_corgi", "柯基", PetKind.DOG, "中型", "10-14 kg", 10.0, 14.0),
        BreedOption("dog_shiba", "柴犬", PetKind.DOG, "中型", "8-11 kg", 8.0, 11.0),
        BreedOption("dog_french_bulldog", "法斗", PetKind.DOG, "中型", "8-13 kg", 8.0, 13.0),
        BreedOption("dog_border_collie", "边牧", PetKind.DOG, "中型", "14-20 kg", 14.0, 20.0),
        BreedOption("dog_husky", "哈士奇", PetKind.DOG, "大型", "16-27 kg", 16.0, 27.0),
        BreedOption("dog_golden", "金毛", PetKind.DOG, "大型", "25-34 kg", 25.0, 34.0),
        BreedOption("dog_labrador", "拉布拉多", PetKind.DOG, "大型", "25-36 kg", 25.0, 36.0),
        BreedOption("dog_german_shepherd", "德牧", PetKind.DOG, "大型", "22-40 kg", 22.0, 40.0),
        BreedOption("dog_berner", "伯恩山", PetKind.DOG, "巨型", "36-50 kg", 36.0, 50.0),
        BreedOption("dog_great_dane", "大丹犬", PetKind.DOG, "巨型", "50-70 kg", 50.0, 70.0),
        BreedOption("dog_tianyuan", "中华田园犬", PetKind.DOG, "中型", "10-25 kg", 10.0, 25.0),
        BreedOption("dog_mixed", "串串犬", PetKind.DOG, "中型", "5-25 kg", 5.0, 25.0),
    )

    fun byId(id: String): BreedOption? = breeds.firstOrNull { it.id == id }

    /** 按类型筛选品种（猫或狗）。 */
    fun byKind(kind: PetKind): List<BreedOption> = breeds.filter { it.kind == kind }

    /** 标准体重区间（kg 二元组），未知品种返回 null。 */
    fun standardWeightRange(breedId: String): Pair<Double, Double>? =
        byId(breedId)?.let { it.minWeightKg to it.maxWeightKg }

    /** 狗体型档位。 */
    fun sizeClassOf(breedId: String): String? = byId(breedId)?.sizeClass

    /** 按体型分类列出狗品种。 */
    fun dogsBySize(): Map<String, List<BreedOption>> =
        byKind(PetKind.DOG).groupBy { it.sizeClass ?: "中型" }
}
