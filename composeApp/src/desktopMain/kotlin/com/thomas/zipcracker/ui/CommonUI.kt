package com.thomas.zipcracker.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.core.DataStoreFactory
import com.thomas.zipcracker.App
import com.thomas.zipcracker.metadata.AppState
import com.thomas.zipcracker.metadata.Log
import com.thomas.zipcracker.metadata.OpMode
import com.thomas.zipcracker.metadata.ZIPStatus
import com.thomas.zipcracker.threading.Watcher
import com.thomas.zipcracker.utility.PreferencesSerializer
import com.thomas.zipcracker.utility.formatNumber
import com.thomas.zipcracker.utility.getSaveDirectory
import com.thomas.zipcracker.utility.masterPath
import com.thomas.zipcracker.utility.writeLogFile
import io.github.vinceglb.filekit.compose.PickerResultLauncher
import io.github.vinceglb.filekit.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.core.FileKitPlatformSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import zipcracker.composeapp.generated.resources.Res
import zipcracker.composeapp.generated.resources.avg_speed
import zipcracker.composeapp.generated.resources.closing
import zipcracker.composeapp.generated.resources.`continue`
import zipcracker.composeapp.generated.resources.dec
import zipcracker.composeapp.generated.resources.file_fail
import zipcracker.composeapp.generated.resources.file_success
import zipcracker.composeapp.generated.resources.inc
import zipcracker.composeapp.generated.resources.max_speed
import zipcracker.composeapp.generated.resources.pwd_consumed
import zipcracker.composeapp.generated.resources.pwd_entered
import zipcracker.composeapp.generated.resources.pwd_msg
import zipcracker.composeapp.generated.resources.pwd_none
import zipcracker.composeapp.generated.resources.save_file
import zipcracker.composeapp.generated.resources.select_benchmark
import zipcracker.composeapp.generated.resources.select_brute
import zipcracker.composeapp.generated.resources.select_button
import zipcracker.composeapp.generated.resources.select_dict
import zipcracker.composeapp.generated.resources.speed_low
import zipcracker.composeapp.generated.resources.stat_dict_progress
import zipcracker.composeapp.generated.resources.stat_encryption
import zipcracker.composeapp.generated.resources.stat_file
import zipcracker.composeapp.generated.resources.stat_method
import zipcracker.composeapp.generated.resources.stat_progress
import zipcracker.composeapp.generated.resources.stat_speed
import zipcracker.composeapp.generated.resources.stat_thread
import zipcracker.composeapp.generated.resources.stat_time
import zipcracker.composeapp.generated.resources.state_fail
import zipcracker.composeapp.generated.resources.state_success
import zipcracker.composeapp.generated.resources.statistics
import zipcracker.composeapp.generated.resources.time
import java.awt.Window
import java.io.File
import kotlin.time.Duration.Companion.seconds

@Composable
fun CloseDialog() {
    Dialog(
        onDismissRequest = {},
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
    icon: ImageVector,
    title: String,
    message: String,
    positiveText: String,
    negativeText: String,
    onAccept: () -> Unit,
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
                    imageVector = icon,
                    contentDescription = "Warning",
                    tint = LightPrimaryColor,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    color = MaterialTheme.colorScheme.contentColorFor(
                        MaterialTheme.colorScheme.background
                    ),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                message,
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
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LightPrimaryColor,
                        contentColor = Color.White
                    )
                ) {
                    Text(positiveText, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onExit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ExitColor,
                        contentColor = Color.White
                    )
                ) {
                    Text(negativeText, fontSize = 14.sp)
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
    onValueChange: () -> Unit,
    onFocusChanged: () -> Unit,
    buttonEnabled: Boolean,
    textReadOnly: Boolean,
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
                enabled = buttonEnabled,
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
                readOnly = textReadOnly,
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
                enabled = buttonEnabled,
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
    state: MutableState<AppState>,
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
                    enabled = state.value != AppState.RUNNING
                ) {
                    launcher.launch()
                },
            singleLine = true
        )
        Spacer(modifier = Modifier.width(28.dp))
        Button(
            enabled = state.value != AppState.RUNNING,
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.White,
                disabledContentColor = MaterialTheme.colorScheme.contentColorFor(
                    MaterialTheme.colorScheme.background),
            ),
            onClick = { launcher.launch() },
            modifier = Modifier
                .weight(0.2f)
        ) {
            Text(
                stringResource(Res.string.select_button),
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
fun ProgressTracker(
    state: MutableState<AppState>,
    opMode: OpMode,
    onStop: () -> Unit = {},
) {
    var paused by remember { mutableStateOf(Watcher.pause) }
    var progress by remember { mutableFloatStateOf(0f) }
    var progressDict by remember { mutableStateOf("0/0") }
    var time by remember { mutableLongStateOf(0L) }
    var speed by remember { mutableLongStateOf(0L) }
    val displayProgress by derivedStateOf {
        if (progress > 1f) "100"
        else "%.2f".format(progress * 100)
    }
    val displaySpeed by derivedStateOf {
        formatNumber(speed.toDouble())
    }

    LaunchedEffect(state.value) {
        delay(250)
        while (state.value == AppState.RUNNING) {
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
                time = Watcher.timer
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
                stringResource(Res.string.stat_time, time.seconds.toString()),
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
                stringResource(Res.string.stat_speed, displaySpeed),
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.background),
            )
            if (opMode == OpMode.DICTIONARY) {
                Text(
                    stringResource(Res.string.stat_dict_progress, progressDict),
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.background),
                )
            } else {
                Text(
                    stringResource(Res.string.stat_progress, displayProgress),
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.background),
                )
            }
        }
    }
}

@Composable
fun ResultTitle(
    state: MutableState<AppState>,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 10.dp)
    ) {
            Icon(
                imageVector = if (state.value == AppState.COMPLETED) Icons.Default.CheckCircle
                    else Icons.Default.Cancel,
                contentDescription = "Result state",
                tint = if (state.value == AppState.COMPLETED) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(26.dp)
            )
            Text(
                stringResource(
                    if (state.value == AppState.COMPLETED) Res.string.state_success
                    else Res.string.state_fail
                ),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = if (state.value == AppState.COMPLETED) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
            )
        }
}

data class KeyValueText(
    val title: String,
    val value: String,
)

@Composable
fun KeyValueText(content: KeyValueText) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            content.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.background),
        )
        Text(
            content.value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.background),
        )
    }
}

@Composable
fun ResultDetails(
    parentWindow: Window?,
    pwdSet: HashSet<String>,
    state: MutableState<AppState>,
    metadata: Log,
) {
    val scope = rememberCoroutineScope()
    val size = pwdSet.size
    val pwd = remember { pwdSet.joinToString() }

    val fileTitle = stringResource(Res.string.stat_file)

    val encryptionMode = when (metadata.encryption) {
        ZIPStatus.AES_ENCRYPTION -> "AES"
        ZIPStatus.STANDARD_ENCRYPTION -> "ZIP 2.0 (ZipCrypto)"
        else -> "Unknown encryption"
    }
    val encryptionTitle = stringResource(Res.string.stat_encryption, encryptionMode)

    val modeTitle = stringResource(Res.string.stat_method)
    val brute = stringResource(Res.string.select_brute)
    val dict = stringResource(Res.string.select_dict)
    val benchmark = stringResource(Res.string.select_benchmark)
    val method = when (metadata.mode) {
        OpMode.BRUTE -> brute
        OpMode.DICTIONARY -> dict
        OpMode.BENCHMARK -> benchmark
    }

    val threadTitle = stringResource(Res.string.stat_thread)

    val timeStat = stringResource(Res.string.time)
    val avgSpeed = stringResource(Res.string.avg_speed)
    val maxSpeed = stringResource(Res.string.max_speed)
    val speedLow = stringResource(Res.string.speed_low)
    val consumed = stringResource(Res.string.pwd_consumed)
    val entered = stringResource(Res.string.pwd_entered)

    var fileSaveSuccess by remember { mutableStateOf<Boolean?>(null) }

    val speed = remember { if (Watcher.timer > 10) Watcher.pwdEntered / Watcher.timer
        else Watcher.speedRecord.average().toLong() }

    val statistics = remember {
        listOf(
            KeyValueText(modeTitle, method),
            KeyValueText(threadTitle, metadata.thread.toString()),
            KeyValueText(timeStat, Watcher.timer.seconds.toString()),
            KeyValueText(avgSpeed, "$speed pwd/s"),
            KeyValueText(
                maxSpeed,
                "${formatNumber(
                    Watcher.speedRecord.maxOrNull()?.toDouble() ?: 0.0
                )} pwd/s"
            ),
            KeyValueText(
                speedLow,
                "${formatNumber(
                    Watcher.calculateFivePercentLow().toDouble()
                )} pwd/s"
            ),
            KeyValueText(entered, formatNumber(Watcher.pwdEntered.toDouble())),
            KeyValueText(consumed, formatNumber(Watcher.pwdConsumed.toDouble())),
        )
    }

    val statSaver = rememberFileSaverLauncher(
        platformSettings = FileKitPlatformSettings(
            parentWindow = parentWindow
        )
    ) { f ->
        fileSaveSuccess = f != null
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
    ) {
        Text(
            stringResource(Res.string.statistics),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.contentColorFor(
                MaterialTheme.colorScheme.background),
        )
        Text(
            "$fileTitle${metadata.file}",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.background),
        )
        Text(
            encryptionTitle,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.background),
        )
        Text(
            if (size == 0) stringResource(Res.string.pwd_none)
            else pluralStringResource(Res.plurals.pwd_msg, size, pwd),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.background),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
        ) {
            items(statistics) { item ->
                KeyValueText(item)
            }
        }
        Spacer(modifier = Modifier.height(0.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Color.White,
                        disabledContentColor = MaterialTheme.colorScheme.contentColorFor(
                            MaterialTheme.colorScheme.background),
                    ),
                    onClick = {
                        scope.launch {
                            val file = writeLogFile(statistics, pwdSet, metadata)
                            statSaver.launch(
                                baseName = "statistics",
                                extension = "txt",
                                initialDirectory = getSaveDirectory(),
                                bytes = file
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.FileDownload,
                        contentDescription = "Save statistics",
                        modifier = Modifier.size(20.dp).align(Alignment.CenterVertically),
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        stringResource(Res.string.save_file),
                        fontSize = 17.sp,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
                if (fileSaveSuccess == true) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "File saved",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        stringResource(Res.string.file_success),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                else if (fileSaveSuccess == false) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Filled.Cancel,
                        contentDescription = "File save failed",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        stringResource(Res.string.file_fail),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
            Button(
                onClick = { state.value = AppState.NOT_INITIATED },
                colors = ButtonDefaults.buttonColors(
                    containerColor = LightButtonColor,
                    contentColor = Color.White,
                    disabledContentColor = MaterialTheme.colorScheme.contentColorFor(
                        MaterialTheme.colorScheme.background),
                ),
            ) {
                Text(
                    stringResource(Res.string.`continue`),
                    fontSize = 17.sp,
                )
            }
        }
    }
}

@Preview
@Composable
fun AppPreview() {
    App(
        null,
        state = mutableStateOf(AppState.COMPLETED),
        mutableListOf(),
        DataStoreFactory.create(
            serializer = PreferencesSerializer(),
            produceFile = { File("$masterPath/preferences.json") }
        )
    )
}