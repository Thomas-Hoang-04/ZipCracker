package com.thomas.zipcracker

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.datastore.core.DataStoreFactory
import com.thomas.zipcracker.metadata.AppState
import com.thomas.zipcracker.ui.CloseDialog
import com.thomas.zipcracker.ui.ConfirmDialog
import com.thomas.zipcracker.utility.PreferencesSerializer
import com.thomas.zipcracker.threading.Watcher
import com.thomas.zipcracker.ui.Theme
import com.thomas.zipcracker.ui.ZipCrackerTheme
import com.thomas.zipcracker.ui.isDarkThemeActive
import com.thomas.zipcracker.utility.masterPath
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import zipcracker.composeapp.generated.resources.Res
import zipcracker.composeapp.generated.resources.close
import zipcracker.composeapp.generated.resources.minimize
import zipcracker.composeapp.generated.resources.warning_message
import zipcracker.composeapp.generated.resources.warning_title
import zipcracker.composeapp.generated.resources.zipcracker
import java.io.File
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@OptIn(InternalComposeUiApi::class)
fun main() {
    val datastore = DataStoreFactory.create(
        serializer = PreferencesSerializer(),
        produceFile = { File("$masterPath/resources/preferences.json") }
    )
    application {
        val scope = rememberCoroutineScope()
        val pool = remember { mutableListOf<Thread>() }
        var showExitDialog by remember { mutableStateOf(false) }
        var showConfirmDialog by remember { mutableStateOf(false) }
        var uiMode by remember { mutableStateOf(Theme.SYSTEM) }
        val initialDark = LocalSystemTheme.current == SystemTheme.Dark
        var isDark by remember { mutableStateOf(initialDark) }
        val isVisible = remember { mutableStateOf(true) }
        val state = remember { mutableStateOf(AppState.NOT_INITIATED) }
        var elapsedTime by remember { mutableLongStateOf(0L) }
        var prompt by remember { mutableStateOf("Pause") }
        val openPrompt by derivedStateOf { if (isVisible.value) "Minimize" else "Show" }
        var pwdConsumed by remember { mutableLongStateOf(0) }
        val windowState = rememberWindowState(
            width = 800.dp,
            height = 840.dp,
            position = WindowPosition.Aligned(Alignment.Center),
            placement = WindowPlacement.Floating
        )

        Locale.setDefault(Locale.of("vi", "VN"))

        val handleExit: suspend () -> Unit = {
            if (!Watcher.stop) {
                Watcher.stop = true
                delay(1000)
                if (scope.isActive) scope.cancel()
            }
            exitApplication()
        }

        LaunchedEffect(state.value) {
            delay(1000)
            while (state.value == AppState.RUNNING) {
                pwdConsumed = Watcher.pwdConsumed
                elapsedTime = Watcher.timer
                delay(1000)
            }
        }

        Tray(
            icon = painterResource(Res.drawable.zipcracker),
            tooltip = if (state.value == AppState.RUNNING) "Cracking in progress"
            else if (isVisible.value) "ZipCracker" else "ZipCracker (Minimized)",
            onAction = { isVisible.value = true },
            menu = {
                if (state.value == AppState.RUNNING) {
                    Item("$pwdConsumed passwords consumed") {}
                    Item("Elapsed time: ${elapsedTime.seconds}") {}
                    Separator()
                    Item(prompt) {
                        Watcher.pause = !Watcher.pause
                        prompt = if (Watcher.pause) "Resume" else "Pause"
                    }
                    Item("Stop") {
                        scope.launch {
                            state.value = AppState.CANCELLED
                            delay(25)
                            Watcher.stop = true
                            Watcher.pause = false
                            while (pool.any { it.isAlive }) {
                                delay(500)
                            }
                            pool.clear()
                        }
                    }
                    Separator()
                }
                Item(openPrompt) { isVisible.value = !isVisible.value }
                Item("Exit") {
                    scope.launch {
                        if (state.value == AppState.RUNNING) {
                            isVisible.value = true
                            delay(500)
                            showConfirmDialog = true
                        }
                        else handleExit()
                    }
                }
            }
        )

        LaunchedEffect(Unit) {
            val theme =  datastore.data.first().uiMode
            uiMode = theme ?: uiMode
            isDark = (uiMode == Theme.DARK) || (uiMode == Theme.SYSTEM && isDarkThemeActive())
            datastore.updateData { it.copy(uiMode = uiMode) }
            while (isActive) {
                if (uiMode == Theme.SYSTEM && isDark != isDarkThemeActive()) {
                    isDark = isDarkThemeActive()
                }
                delay(1000)
            }
        }

        ZipCrackerTheme(darkTheme = isDark) {
            Window(
                state = windowState,
                onCloseRequest = {
                    scope.launch {
                        if (state.value == AppState.RUNNING) {
                            showConfirmDialog = true
                            delay(500)
                        } else {
                            showExitDialog = true
                            delay(1000)
                            handleExit()
                        }
                    }
                },
                visible = isVisible.value,
                title = "ZipCracker",
                resizable = false,
                icon = painterResource(Res.drawable.zipcracker)
            ) {
                if (showExitDialog) { CloseDialog() }

                if (showConfirmDialog) {
                    ConfirmDialog(
                        icon = Icons.Default.Warning,
                        title = stringResource(Res.string.warning_title, "Decrypting"),
                        message = stringResource(Res.string.warning_message),
                        positiveText = stringResource(Res.string.minimize),
                        negativeText = stringResource(Res.string.close),
                        onAccept = {
                            isVisible.value = false
                            showConfirmDialog = false
                        },
                        onExit = {
                            scope.launch {
                                showConfirmDialog = false
                                showExitDialog = true
                                delay(1000)
                                handleExit()
                            }
                        }
                    )
                }

                MenuBar {
                    Menu("Themes") {
                        CheckboxItem(
                            "Dark",
                            uiMode == Theme.DARK,
                        ) {
                            isDark = true
                            uiMode = Theme.DARK
                            scope.launch {
                                datastore.updateData { it.copy(uiMode = Theme.DARK) }
                            }
                        }
                        CheckboxItem(
                            "Light",
                            uiMode == Theme.LIGHT,
                        ) {
                            isDark = false
                            uiMode = Theme.LIGHT
                            scope.launch {
                                datastore.updateData { it.copy(uiMode = Theme.LIGHT) }
                            }
                        }
                        CheckboxItem(
                            "System",
                            uiMode == Theme.SYSTEM,
                        ) {
                            uiMode = Theme.SYSTEM
                            isDark = isDarkThemeActive()
                            scope.launch {
                                datastore.updateData { it.copy(uiMode = Theme.SYSTEM) }
                            }
                        }
                    }
                }

                App(this.window, state, pool, datastore)
            }
        }
    }
}
