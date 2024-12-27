package com.thomas.zipcracker


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.awaitApplication
import androidx.compose.ui.window.rememberWindowState
import com.thomas.zipcracker.component.CloseDialog
import com.thomas.zipcracker.threading.pause
import com.thomas.zipcracker.threading.stop
import com.thomas.zipcracker.threading.testThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import zipcracker.composeapp.generated.resources.Res
import zipcracker.composeapp.generated.resources.zipcracker
import kotlin.coroutines.CoroutineContext

fun main(): Unit = application {
    val scope = rememberCoroutineScope()
    val pool = remember { mutableListOf<Thread>() }
    var showDialog by remember { mutableStateOf(false) }
    val windowState = rememberWindowState(
        width = 1280.dp,
        height = 800.dp,
        position = WindowPosition.Aligned(Alignment.Center),
        placement = WindowPlacement.Floating
    )

    Window(
        state = windowState,
        onCloseRequest = {
            scope.launch {
                showDialog = true
                delay(500)
                if (!stop) {
                    stop = true
                    while (pool.any { it.isAlive }) {
                        pool.removeIf { !it.isAlive }
                        delay(500)
                    }
                    pool.clear()
                    if (scope.isActive) scope.cancel()
                }
                exitApplication()
            }
        },
        title = "ZipCracker",
        icon = painterResource(Res.drawable.zipcracker)
    ) {
        if (showDialog) {
            CloseDialog()
        }

        MenuBar {
            Menu("File") {
                Item("Open") {
                    stop = false
                    pause = false
                    scope.launch {
                        withContext(Dispatchers.Default) {
                            testThread(pool)
                        }
                    }
                }
                Item("Pause") { pause = !pause }
                Item("Exit") {
                    scope.launch {
                        stop = true
                        pause = false
                        while (pool.any { it.isAlive }) {
                            pool.removeIf { !it.isAlive }
                            delay(500)
                        }
                        pool.clear()
                        println("Exiting")
                    }
                }
            }
            Menu("Edit") {
                Item("Copy") {  }
                Item("Paste") { }
            }
        }
        App()
    }
}
