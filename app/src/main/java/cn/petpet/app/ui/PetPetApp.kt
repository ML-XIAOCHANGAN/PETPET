package cn.petpet.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.petpet.app.model.HomeSection
import cn.petpet.app.model.PetBreeds
import cn.petpet.app.ui.components.VoxelNavigationBar
import cn.petpet.app.ui.components.AdoptionDialog
import cn.petpet.app.ui.screens.CommunityScreen
import cn.petpet.app.ui.screens.GrowthScreen
import cn.petpet.app.ui.screens.MyPetScreen
import cn.petpet.app.ui.theme.PetPetTheme

@Composable
fun PetPetApp(
    viewModel: PetPetViewModel,
    chatFocusRequest: Long = 0L,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    PetPetTheme(darkTheme = state.darkTheme) {
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(state.notice) {
            state.notice?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearNotice()
            }
        }

        LaunchedEffect(chatFocusRequest) {
            if (chatFocusRequest > 0L) viewModel.selectSection(HomeSection.MY_PET)
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!state.showAdoption) {
                    VoxelNavigationBar(
                        selected = state.section,
                        onSelect = viewModel::selectSection,
                        petType = state.pet?.let { PetBreeds.byId(it.breedId)?.type },
                    )
                }
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                AnimatedContent(
                    targetState = state.section,
                    transitionSpec = {
                        val enteringFromRight = targetState.ordinal > initialState.ordinal
                        val enter = slideInHorizontally(
                            animationSpec = tween(360, easing = FastOutSlowInEasing),
                            initialOffsetX = { width -> if (enteringFromRight) width / 4 else -width / 4 },
                        ) + fadeIn(tween(280))
                        val exit = slideOutHorizontally(
                            animationSpec = tween(360, easing = FastOutSlowInEasing),
                            targetOffsetX = { width -> if (enteringFromRight) -width / 4 else width / 4 },
                        ) + fadeOut(tween(220))
                        enter togetherWith exit
                    },
                    label = "底部页面切换",
                ) { section ->
                    if (state.showAdoption) {
                        // 领养流程中：宠物尚未确认，显示欢迎占位
                        androidx.compose.material3.Text(
                            text = "🐾",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.displayLarge,
                        )
                    } else {
                        when (section) {
                            HomeSection.MY_PET -> MyPetScreen(
                                state = state,
                                onInteract = viewModel::interact,
                                onSendMessage = viewModel::sendMessage,
                                onDarkThemeChange = viewModel::setDarkTheme,
                                onChatConfigChange = viewModel::setChatConfig,
                                chatFocusRequest = chatFocusRequest,
                            )
                            HomeSection.GROWTH -> GrowthScreen(
                                pet = requireNotNull(state.pet),
                                diaryEntries = state.diaryEntries,
                                onAddDiary = viewModel::addDiaryEntry,
                            )
                            HomeSection.COMMUNITY -> CommunityScreen(
                                pet = requireNotNull(state.pet),
                                posts = state.posts,
                                online = state.communityOnline,
                                onRefreshNetwork = viewModel::refreshCommunityFromNetwork,
                                onCreatePost = viewModel::createPost,
                                onToggleLike = viewModel::toggleLike,
                                onAddComment = viewModel::addComment,
                                onReport = viewModel::reportPost,
                                onToggleCollect = viewModel::toggleCollect,
                                onToggleDislike = viewModel::toggleDislike,
                            )
                        }
                    }
                }

                // 首次领养流程：随机抽取 → 品种介绍 → 命名
                state.candidateBreed?.let { breed ->
                    if (state.showAdoption) {
                        AdoptionDialog(
                            breed = breed,
                            onRoll = viewModel::rollCandidatePet,
                            onAdopt = viewModel::adoptPet,
                        )
                    }
                }
            }
        }
    }
}
