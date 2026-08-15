package cn.petpet.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cn.petpet.app.model.PetBreed
import cn.petpet.app.model.PetType

/**
 * 首次领养弹窗：展示随机抽到的宠物（头像 + 品种介绍），
 * 用户可重新抽取或直接命名确认。
 */
@Composable
fun AdoptionDialog(
    breed: PetBreed,
    onRoll: () -> Unit,
    onAdopt: (String) -> Unit,
) {
    var petName by rememberSaveable { mutableStateOf("") }

    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "🎉 恭喜领养到一只${breed.name}！",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )

                // 宠物头像
                Image(
                    painter = painterResource(breed.avatarRes),
                    contentDescription = breed.name,
                    modifier = Modifier
                        .size(180.dp)
                        .padding(8.dp),
                    contentScale = ContentScale.Fit,
                )

                // 品种徽章
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (breed.type == PetType.CAT) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                    ) {
                        Text(
                            text = if (breed.type == PetType.CAT) "🐱 猫咪血统" else "🐶 狗狗血统",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // 品种介绍
                Text(
                    text = breed.intro,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // 命名输入
                OutlinedTextField(
                    value = petName,
                    onValueChange = { if (it.length <= 12) petName = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("给你的小宠物起个名字") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                )

                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    VoxelButton(
                        text = "换一只 ↻",
                        onClick = onRoll,
                        style = VoxelButtonStyle.NEUTRAL,
                        modifier = Modifier.weight(1f),
                    )
                    VoxelButton(
                        text = "就它了！",
                        onClick = { onAdopt(petName) },
                        enabled = petName.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}
