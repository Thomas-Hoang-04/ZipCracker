package com.thomas.zipcracker.component

import androidx.compose.runtime.Composable
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text

@Composable
@Preview
fun CloseDialog() {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("App closing") },
        text = { CircularProgressIndicator() },
        confirmButton = { },
        dismissButton = { }
    )
}