package com.thomas.zipcracker.component

import androidx.compose.runtime.Composable
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.thomas.zipcracker.processor.OpMode
import com.thomas.zipcracker.processor.Watcher
import com.thomas.zipcracker.ui.ExitColor
import com.thomas.zipcracker.ui.IndicatorColor
import com.thomas.zipcracker.ui.LightPrimaryColor
import com.thomas.zipcracker.utility.formatNumber
import io.github.vinceglb.filekit.compose.PickerResultLauncher
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import zipcracker.composeapp.generated.resources.Res
import zipcracker.composeapp.generated.resources.close
import zipcracker.composeapp.generated.resources.closing
import zipcracker.composeapp.generated.resources.dec
import zipcracker.composeapp.generated.resources.inc
import zipcracker.composeapp.generated.resources.minimize
import zipcracker.composeapp.generated.resources.select_button
import zipcracker.composeapp.generated.resources.warning_message
import zipcracker.composeapp.generated.resources.warning_title
import kotlin.time.Duration.Companion.seconds

@Composable
fun CloseDialog() {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        )
    ) {
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(10.dp)
                ),
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(top = 16.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(Res.string.closing),
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.background),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(top = 2.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = IndicatorColor,
                    trackColor = Color.LightGray,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun ConfirmDialog(
    onMinimize: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        )
    ) {
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(10.dp)
                ),
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(top = 16.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Exit warning",
                    tint = LightPrimaryColor,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(Res.string.warning_title, "Decrypting"),
                    color = MaterialTheme.colorScheme.contentColorFor(
                        MaterialTheme.colorScheme.background
                    ),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                stringResource(Res.string.warning_message),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.contentColorFor(
                    MaterialTheme.colorScheme.background
                ),
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(top = 2.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onMinimize,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LightPrimaryColor,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(Res.string.minimize), fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onExit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ExitColor,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(Res.string.close), fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun ErrorText(error: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.WarningAmber,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(22.dp)
        )
        Text(
            error,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .padding(start = 8.dp)
        )
    }
}

@Composable
fun TitleWithErrorWarning(
    title: String,
    error: String?,
    fontSize: TextUnit = 18.sp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme
                .contentColorFor(MaterialTheme.colorScheme.background),
        )
        error?.let { ErrorText(it) }
    }
}

@Composable
fun RowWithIncrementer(
    title: String,
    num: MutableIntState,
    displayNum: MutableState<String>,
    threshold: Int,
    isRunning: MutableState<Boolean>,
    onValueChange: () -> Unit,
    onFocusChanged: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme
                .contentColorFor(MaterialTheme.colorScheme.background)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .padding(horizontal = 16.dp)
        ) {
            IconButton(
                enabled = !isRunning.value,
                onClick = {
                    if (num.value > 1) {
                        num.value--
                        displayNum.value = num.value.toString()
                    }
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = stringResource(Res.string.dec),
                    tint = MaterialTheme.colorScheme
                        .contentColorFor(MaterialTheme.colorScheme.background)
                )
            }
            OutlinedTextField(
                readOnly = isRunning.value,
                value = TextFieldValue(
                    text = displayNum.value,
                    selection = TextRange(displayNum.value.length)
                ),
                onValueChange = {
                    displayNum.value = it.text
                    if (displayNum.value.toIntOrNull() != null) {
                        num.value = displayNum.value.toInt()
                        onValueChange()
                    }
                },
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.background),
                    unfocusedTextColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.background),
                ),
                singleLine = true,
                modifier = Modifier
                    .width(60.dp)
                    .heightIn(max = 52.dp)
                    .onFocusChanged {
                        onFocusChanged()
                    }
            )
            IconButton(
                enabled = !isRunning.value,
                onClick = {
                    if (num.value < threshold) {
                        num.value++
                        displayNum.value = num.value.toString()
                    }
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.inc),
                    tint = MaterialTheme.colorScheme
                        .contentColorFor(MaterialTheme.colorScheme.background)
                )
            }
        }
    }
}

@Composable
fun FileInput(
    filename: String,
    launcher: PickerResultLauncher,
    isRunning: MutableState<Boolean>,
    label: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 20.dp)
    ) {
        OutlinedTextField(
            value = filename,
            enabled = false,
            onValueChange = {},
            label = label,
            textStyle = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                disabledBorderColor = MaterialTheme.colorScheme.onSecondaryContainer,
                disabledTextColor = MaterialTheme.colorScheme.primary,
                disabledLabelColor = MaterialTheme.colorScheme.onSecondary,
            ),
            modifier = Modifier
                .weight(0.8f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !isRunning.value
                ) {
                    launcher.launch()
                },
            singleLine = true,
        )
        Spacer(modifier = Modifier.width(28.dp))
        Button(
            enabled = !isRunning.value,
            onClick = { launcher.launch() },
            modifier = Modifier
                .weight(0.2f)
        ) {
            Text(
                stringResource(Res.string.select_button),
                fontSize = 15.sp,
                color = Color.White,
            )
        }
    }
}

@Composable
@Preview
fun ProgressPreview() {
    ProgressTracker(
        isRunning = mutableStateOf(true),
        opMode = OpMode.DICTIONARY,
        time = mutableLongStateOf(0)
    )
}

@Composable
fun ProgressTracker(
    isRunning: MutableState<Boolean>,
    opMode: OpMode,
    onStop: () -> Unit = {},
    time: MutableLongState,
) {
    var paused by remember { mutableStateOf(Watcher.pause) }
    var progress by remember { mutableFloatStateOf(0f) }
    var progressDict by remember { mutableStateOf("0/0") }
    var speed by remember { mutableIntStateOf(0) }
    val displayProgress by derivedStateOf {
        if (progress > 1f) "100"
        else "%.2f".format(progress * 100)
    }
    val displaySpeed by derivedStateOf {
        formatNumber(speed.toDouble())
    }

    LaunchedEffect(isRunning.value) {
        delay(250)
        while (isRunning.value) {
            paused = Watcher.pause
            if (!paused) {
                if (opMode == OpMode.DICTIONARY) {
                    progressDict = formatNumber(Watcher.pwdConsumed.toDouble()) +
                            "/" + formatNumber(Watcher.pwdEntered.toDouble())
                } else {
                    progress = if (Watcher.maxPassword > 0) {
                        Watcher.pwdConsumed / Watcher.maxPassword.toFloat()
                    } else { 0f }
                }
                speed = Watcher.speed
                time.value++
            }
            delay(1000)
        }
    }

    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Elapsed time: ${time.value.seconds}",
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.background),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (paused) {
                    IconButton(
                        onClick = { Watcher.pause = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Continue",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    IconButton(
                        onClick = { Watcher.pause = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(
                    onClick = onStop,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Stop",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (opMode == OpMode.DICTIONARY) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = IndicatorColor,
                trackColor = Color.LightGray,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp
            )
        } else {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = IndicatorColor,
                trackColor = Color.LightGray,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Speed: $displaySpeed pwd/s",
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.background),
            )
            if (opMode == OpMode.DICTIONARY) {
                Text(
                    "Processed: $progressDict",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.background),
                )
            } else {
                Text(
                    "Processed: $displayProgress%",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.background),
                )
            }
        }
    }
}
