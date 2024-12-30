package com.thomas.zipcracker

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.thomas.zipcracker.component.CloseDialog
import com.thomas.zipcracker.component.ConfirmDialog
import com.thomas.zipcracker.processor.Watcher
import com.thomas.zipcracker.ui.ZipCrackerTheme
import com.thomas.zipcracker.ui.isDarkThemeActive
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import zipcracker.composeapp.generated.resources.Res
import zipcracker.composeapp.generated.resources.zipcracker
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

fun main(): Unit = application {
    val scope = rememberCoroutineScope()
    val pool = remember { mutableListOf<Thread>() }
    var showExitDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isDark by remember { mutableStateOf(isDarkThemeActive()) }
    val isVisible = remember { mutableStateOf(true) }
    val isRunning = remember { mutableStateOf(false) }
    var elapsedTime by remember { mutableStateOf(0L) }
    var prompt by remember { mutableStateOf("Pause") }
    val openPrompt by derivedStateOf { if (isVisible.value) "Minimize" else "Show" }
    var pwdConsumed by remember { mutableStateOf(0) }
    val windowState = rememberWindowState(
        width = 800.dp,
        height = 800.dp,
        position = WindowPosition.Aligned(Alignment.Center),
        placement = WindowPlacement.Floating
    )

    Locale.setDefault(Locale.of("vi"))

    val handleExit: suspend () -> Unit = {
        if (!Watcher.stop) {
            Watcher.stop = true
            while (pool.any { it.isAlive }) {
                pool.forEach(Thread::interrupt)
                delay(250)
            }
            pool.clear()
            if (scope.isActive) scope.cancel()
        }
        exitApplication()
    }

    LaunchedEffect(isRunning.value) {
        delay(250)
        while (isRunning.value) {
            pwdConsumed = Watcher.pwdConsumed
            elapsedTime++
            delay(1000)
        }
    }

    Tray(
        icon = painterResource(Res.drawable.zipcracker),
        tooltip = if (isRunning.value) "Cracking in progress"
            else if (isVisible.value) "ZipCracker" else "ZipCracker (Minimized)",
        onAction = { isVisible.value = true },
        menu = {
            if (isRunning.value) {
                Item("$pwdConsumed passwords consumed") {}
                Item("Elapsed time: ${elapsedTime.seconds}") {}
                Separator()
                Item(prompt) {
                    Watcher.pause = !Watcher.pause
                    prompt = if (Watcher.pause) "Resume" else "Pause"
                }
                Item("Stop") {
                    scope.launch {
                        Watcher.stop = true
                        Watcher.pause = false
                        while (pool.any { it.isAlive }) {
                            delay(500)
                        }
                        pool.clear()
                        isRunning.value = false
                    }
                }
                Separator()
            }
            Item(openPrompt) { isVisible.value = !isVisible.value }
            Item("Exit") {
                scope.launch {
                    if (isRunning.value) {
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
        while (isActive) {
            if (isDarkThemeActive() != isDark) {
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
                    if (isRunning.value) {
                        showConfirmDialog = true
                        delay(500)
                    } else {
                        showExitDialog = true
                        delay(1500)
                        handleExit()
                    }
                }
            },
            visible = isVisible.value,
            title = "ZipCracker",
            resizable = false,
            icon = painterResource(Res.drawable.zipcracker)
        ) {
            if (showExitDialog) {
                CloseDialog()
            }

            if (showConfirmDialog) {
                ConfirmDialog(
                    onMinimize = {
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

            App(isRunning, pool)
        }
    }
}
