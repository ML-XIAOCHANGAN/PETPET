package cn.petpet.app.model

import androidx.annotation.DrawableRes
import cn.petpet.app.R

/**
 * 宠物品种：名称、介绍、头像资源。
 * 首次领养时系统随机抽取一只。
 */
data class PetBreed(
    val id: String,
    val name: String,
    val type: PetType,
    @DrawableRes val avatarRes: Int,
    val intro: String,
)

enum class PetType { CAT, DOG }

object PetBreeds {
    val ALL: List<PetBreed> = listOf(
        // ── 小猫 ──
        PetBreed(
            id = "black_cat", name = "小黑猫", type = PetType.CAT,
            avatarRes = R.drawable.pet_black_cat,
            intro = "神秘又粘人的小精灵，夜色里的一团温柔。它总爱安静地趴在你身边，用呼噜声告诉你：你被爱着。",
        ),
        PetBreed(
            id = "gray_cat", name = "灰色小猫", type = PetType.CAT,
            avatarRes = R.drawable.pet_gray_cat,
            intro = "沉稳的绅士猫，性格温和内敛。它喜欢观察世界，却把最柔软的肚皮只留给你。",
        ),
        PetBreed(
            id = "orange_cat", name = "小橘猫", type = PetType.CAT,
            avatarRes = R.drawable.pet_orange_cat,
            intro = "天生的快乐制造机，吃嘛嘛香的干饭王。它用圆滚滚的肚子证明：被好好爱着的猫，都会发光。",
        ),
        PetBreed(
            id = "calico", name = "三花小猫", type = PetType.CAT,
            avatarRes = R.drawable.pet_calico,
            intro = "活泼俏皮的小公主，身上三种颜色像打翻的调色盘。它精力充沛，是最会撒娇的小淘气。",
        ),
        PetBreed(
            id = "ragdoll", name = "布偶小猫", type = PetType.CAT,
            avatarRes = R.drawable.pet_ragdoll,
            intro = "蓝眼睛的布偶娃娃，软乎乎的抱抱怪。它性格极好，被抱起来时会像布偶一样瘫软在你怀里。",
        ),
        PetBreed(
            id = "white_british", name = "纯白英短", type = PetType.CAT,
            avatarRes = R.drawable.pet_white_british,
            intro = "奶白色的小团子，圆圆脸圆圆眼。它安静优雅，是家里最会'陪伴'的小绅士。",
        ),
        PetBreed(
            id = "golden_shaded", name = "金渐层长毛", type = PetType.CAT,
            avatarRes = R.drawable.pet_golden_shaded,
            intro = "披着金色光芒的贵族猫，长毛蓬松如云朵。它性格亲人不认生，天生就是镜头里的明星。",
        ),
        PetBreed(
            id = "maine_coon", name = "缅因小猫", type = PetType.CAT,
            avatarRes = R.drawable.pet_maine_coon,
            intro = "猫中巨无霸，温柔的大个子。它外表霸气内心柔软，是能陪你散步的大猫猫。",
        ),
        PetBreed(
            id = "scottish_fold", name = "苏格兰折耳", type = PetType.CAT,
            avatarRes = R.drawable.pet_scottish_fold,
            intro = "耳朵趴趴的小汤圆，天生自带委屈脸。它安静乖巧，用软软的喵叫融化你的心。",
        ),
        PetBreed(
            id = "siamese", name = "暹罗小猫", type = PetType.CAT,
            avatarRes = R.drawable.pet_siamese,
            intro = "猫中的'小狗'，聪明黏人爱说话。它会用特有的嗓音跟你聊天，是你最忠诚的小跟班。",
        ),
        // ── 小狗 ──
        PetBreed(
            id = "shiba", name = "小柴犬", type = PetType.DOG,
            avatarRes = R.drawable.pet_shiba,
            intro = "元气满满的微笑天使，尾巴永远摇得像小风扇。它独立又忠诚，是治愈系的表情包担当。",
        ),
        PetBreed(
            id = "golden_retriever", name = "小金毛", type = PetType.DOG,
            avatarRes = R.drawable.pet_golden_retriever,
            intro = "阳光开朗的大暖男，对人类有着天生的温柔。它永远咧着嘴笑，是行走的快乐源泉。",
        ),
        PetBreed(
            id = "samoyed", name = "萨摩耶", type = PetType.DOG,
            avatarRes = R.drawable.pet_samoyed,
            intro = "雪白的微笑天使，'微笑天使'本尊。它毛发如雪、笑容温暖，是冬天里的小太阳。",
        ),
        PetBreed(
            id = "bichon", name = "比熊小狗", type = PetType.DOG,
            avatarRes = R.drawable.pet_bichon,
            intro = "棉花糖一样的小毛球，性格活泼爱撒娇。它黏人又可爱，是抱在怀里就化开的小甜心。",
        ),
        PetBreed(
            id = "pomeranian", name = "博美小狗", type = PetType.DOG,
            avatarRes = R.drawable.pet_pomeranian,
            intro = "毛茸茸的小狐狸，嗓门和气势都不小。它聪明机警，是家里的小型守护神。",
        ),
        PetBreed(
            id = "border_collie", name = "边牧小狗", type = PetType.DOG,
            avatarRes = R.drawable.pet_border_collie,
            intro = "狗界智商天花板，机灵得会跟你斗智斗勇。它精力旺盛，是最棒的玩伴和伙伴。",
        ),
        PetBreed(
            id = "german_shepherd", name = "德牧小狗", type = PetType.DOG,
            avatarRes = R.drawable.pet_german_shepherd,
            intro = "威风凛凛的忠诚卫士，内心却是大孩子。它对家人极度护短，是可靠的安全感来源。",
        ),
        PetBreed(
            id = "berner", name = "伯恩山小狗", type = PetType.DOG,
            avatarRes = R.drawable.pet_berner,
            intro = "三色温柔巨犬，憨厚老实的大块头。它性格温驯耐心，是家里的暖心大玩偶。",
        ),
    )

    fun byId(id: String): PetBreed? = ALL.firstOrNull { it.id == id }

    /** 随机抽一只宠物。 */
    fun random(): PetBreed = ALL.random()
}
