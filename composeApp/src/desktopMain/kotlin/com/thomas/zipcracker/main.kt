package com.thomas.zipcracker


import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.thomas.zipcracker.threading.testThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import zipcracker.composeapp.generated.resources.Res
import zipcracker.composeapp.generated.resources.zipcracker

fun main(): Unit = application {
    val scope = rememberCoroutineScope()
    val windowState = rememberWindowState(
        width = 1280.dp,
        height = 800.dp,
        position = WindowPosition.Aligned(Alignment.Center),
        placement = WindowPlacement.Floating
    )
    Window(
        state = windowState,
        onCloseRequest = ::exitApplication,
        title = "ZipCracker",
        icon = painterResource(Res.drawable.zipcracker)
    ) {
        MenuBar {
            Menu("File") {
                Item("Open") {
                    scope.launch() {
                        withContext(Dispatchers.IO) {
                            testThread()
                        }
                    }
                }
                Item("Save") { println("Save") }
                Item("Exit") { exitApplication() }
            }
            Menu("Edit") {
                Item("Copy") {  }
                Item("Paste") { }
            }
        }
        App()
    }
}