package cn.petpet.app.ui.fitcut

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.petpet.app.data.fitcut.Exercise
import cn.petpet.app.data.fitcut.FitCutExerciseLibrary
import cn.petpet.app.data.fitcut.WorkoutRecord
import cn.petpet.app.ui.components.VoxelButton
import cn.petpet.app.ui.components.VoxelButtonStyle

/** 运动追踪页：训练库 + 打卡 + 历史。 */
@Composable
fun FitCutExerciseScreen(viewModel: FitCutViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = viewModel.today()
    val todayWorkouts = state.data.workoutRecords.filter { it.date == today }
    val history = state.data.workoutRecords.sortedByDescending { it.createdAt }

    var logging by remember { mutableStateOf<Exercise?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("今日运动", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text(
                            if (todayWorkouts.isEmpty()) "还没有打卡记录" else "已打卡 ${todayWorkouts.size} 次",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "🔥 连续 ${state.achievements.firstOrNull { it.id == "streak_7" }?.progress ?: 0} 天",
                        fontWeight = FontWeight.Black,
                        color = FitCutOrange,
                        fontSize = 15.sp,
                    )
                }
            }
        }

        FitCutExerciseLibrary.categories.forEach { category ->
            item {
                Text(
                    if (category == "有氧") "🏃 有氧训练" else "💪 力量训练",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
            items(FitCutExerciseLibrary.byCategory(category), key = { it.name }) { exercise ->
                ExerciseCard(exercise) { logging = exercise }
            }
        }

        item {
            Text("📋 打卡历史", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        if (history.isEmpty()) {
            item {
                Text(
                    "暂无记录，选一个动作开始吧",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
        } else {
            items(history.take(30), key = { it.id }) { record ->
                WorkoutRow(record)
            }
        }
    }

    logging?.let { exercise ->
        LogWorkoutDialog(
            exercise = exercise,
            onDismiss = { logging = null },
            onLog = { minutes ->
                viewModel.logWorkout(exercise.name, exercise.category, minutes)
                logging = null
            },
        )
    }
}

@Composable
private fun ExerciseCard(exercise: Exercise, onLog: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(exercise.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    exercise.setsReps,
                    fontSize = 12.sp,
                    color = FitCutGreen,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                "目标肌群：${exercise.targetMuscle}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                exercise.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            VoxelButton(
                text = "打卡",
                onClick = onLog,
                compact = true,
            )
        }
    }
}

@Composable
private fun LogWorkoutDialog(
    exercise: Exercise,
    onDismiss: () -> Unit,
    onLog: (Int) -> Unit,
) {
    var minutes by rememberSaveable { mutableStateOf("30") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("打卡：${exercise.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "${exercise.category} · ${exercise.targetMuscle} · 建议 ${exercise.setsReps}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { v -> minutes = v.filter(Char::isDigit) },
                    label = { Text("运动时长（分钟）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                val m = minutes.toIntOrNull() ?: 0
                Text(
                    if (m > 0) "预计消耗约 ${(m * 0.12 * 65).toInt()} kcal" else "请输入有效时长",
                    fontSize = 13.sp,
                    color = FitCutGreen,
                    fontWeight = FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VoxelButton(text = "取消", onClick = onDismiss, compact = true, style = VoxelButtonStyle.NEUTRAL)
                    VoxelButton(
                        text = "完成打卡 ✅",
                        onClick = { if (m > 0) onLog(m) },
                        compact = true,
                        enabled = m > 0,
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutRow(record: WorkoutRecord) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(record.exerciseName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(
                    "${record.date} · ${record.minutes} 分钟",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${record.calories.toInt()} kcal",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = FitCutGreen,
            )
        }
    }
}
