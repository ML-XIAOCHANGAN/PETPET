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
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.petpet.app.model.HomeSection
import cn.petpet.app.ui.components.VoxelNavigationBar
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
                VoxelNavigationBar(
                    selected = state.section,
                    onSelect = viewModel::selectSection,
                )
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
                    when (section) {
                        HomeSection.MY_PET -> MyPetScreen(
                            state = state,
                            onInteract = viewModel::interact,
                            onSendMessage = viewModel::sendMessage,
                            onDarkThemeChange = viewModel::setDarkTheme,
                            chatFocusRequest = chatFocusRequest,
                        )
                        HomeSection.GROWTH -> GrowthScreen(
                            pet = requireNotNull(state.pet),
                            diaryEntries = state.diaryEntries,
                            onAddDiary = viewModel::addDiaryEntry,
                        )
                        HomeSection.COMMUNITY -> CommunityScreen(
                            posts = state.posts,
                            onCreatePost = viewModel::createPost,
                            onToggleLike = viewModel::toggleLike,
                            onAddComment = viewModel::addComment,
                            onReport = viewModel::reportPost,
                        )
                    }
                }
            }
        }
    }
}
