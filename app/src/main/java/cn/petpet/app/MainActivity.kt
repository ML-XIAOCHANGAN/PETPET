package cn.petpet.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.petpet.app.ui.PetPetApp
import cn.petpet.app.ui.PetPetViewModel
import cn.petpet.app.overlay.FloatingPetController

class MainActivity : ComponentActivity() {
    private var chatFocusRequest by mutableLongStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleLaunchIntent(intent)
        enableEdgeToEdge()
        setContent {
            val viewModel: PetPetViewModel = viewModel()
            PetPetApp(
                viewModel = viewModel,
                chatFocusRequest = chatFocusRequest,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        if (
            FloatingPetController.isAutoStartEnabled(this) &&
            FloatingPetController.hasRequiredPermissions(this)
        ) {
            FloatingPetController.start(
                context = this,
                enableAutoStart = false,
            )
        }
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_CHAT, false) == true) {
            chatFocusRequest += 1L
        }
    }

    companion object {
        const val EXTRA_OPEN_CHAT = "cn.petpet.app.extra.OPEN_CHAT"
    }
}
