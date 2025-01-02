package com.thomas.zipcracker

import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import com.thomas.zipcracker.metadata.AppState
import com.thomas.zipcracker.crypto.CrackingOptions
import com.thomas.zipcracker.metadata.LastPwdMetadata
import com.thomas.zipcracker.metadata.Log
import com.thomas.zipcracker.ui.FileInput
import com.thomas.zipcracker.ui.ProgressTracker
import com.thomas.zipcracker.ui.RowWithIncrementer
import com.thomas.zipcracker.ui.TitleWithErrorWarning
import com.thomas.zipcracker.metadata.OpMode
import com.thomas.zipcracker.utility.UserPreferences
import com.thomas.zipcracker.threading.Watcher
import com.thomas.zipcracker.metadata.ZIPStatus
import com.thomas.zipcracker.threading.crack
import com.thomas.zipcracker.ui.ConfirmDialog
import com.thomas.zipcracker.ui.ResultDetails
import com.thomas.zipcracker.ui.ResultTitle
import com.thomas.zipcracker.utility.checkZIPDecryption
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.FileKitPlatformSettings
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

import zipcracker.composeapp.generated.resources.*
import java.awt.Window
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

@Composable
fun App(
    parentWindow: Window?,
    state: MutableState<AppState>,
    pool: MutableList<Thread>,
    datastore: DataStore<UserPreferences>
) {
    var file by remember { mutableStateOf<File?>(null) }
    val fileDisplay by derivedStateOf {
        file?.path ?: ""
    }

    val dictionaryFile = remember { mutableListOf<File>() }
    var dictionaryDisplay by remember { mutableStateOf("") }

    val pwdOptions = remember { mutableIntStateOf(0b0101) }
    val pwdOptionsLabels = stringArrayResource(Res.array.pwd_combinations)
    var encryption by remember { mutableStateOf(ZIPStatus.NO_ENCRYPTION) }

    val opMode = remember { mutableStateOf(OpMode.BRUTE) }
    val isBenchmark by derivedStateOf { opMode.value == OpMode.BENCHMARK }

    val threadCount = remember { mutableIntStateOf(1) }
    val threadCountDisplay = remember { mutableStateOf(threadCount.toString()) }
    val maxThread = remember { Runtime.getRuntime().availableProcessors() }

    val pwdLength = remember { mutableIntStateOf(4) }
    val pwdLengthDisplay = remember { mutableStateOf(pwdLength.toString()) }

    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val scrollState = rememberScrollState()

    val refFileError = stringResource(Res.string.file_error)
    val refDictError = stringResource(Res.string.dict_error)
    val refPwdOptionsError = stringResource(Res.string.pwd_error)
    val formatError = stringResource(Res.string.format_error)
    val encryptionError = stringResource(Res.string.encryption_error)

    val fileError = remember { mutableStateOf<String?>(null) }
    var pwdOptionsError by remember { mutableStateOf<String?>(null) }
    var dictError by remember { mutableStateOf<String?>(null) }

    val showResult by derivedStateOf {
        state.value == AppState.COMPLETED || state.value == AppState.CANCELLED
    }

    var recovery by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val validated: () -> Boolean = {
        fileError.value = when {
            file == null -> refFileError
            else -> {
                when (val status = checkZIPDecryption(file!!.path)) {
                    ZIPStatus.UNKNOWN_FORMAT -> formatError
                    ZIPStatus.NO_ENCRYPTION -> encryptionError
                    else -> {
                        encryption = status
                        null
                    }
                }
            }
        }
        pwdOptionsError = if (pwdOptions.value == 0) { refPwdOptionsError }
            else { null }
        dictError = if (opMode.value == OpMode.DICTIONARY && dictionaryFile.isEmpty()) {
            refDictError
        } else { null }
        fileError.value == null && pwdOptionsError == null && dictError == null
    }

    val result = remember { ConcurrentLinkedQueue<String>() }
    val activate: suspend (CrackingOptions, Boolean) -> Unit = { opt, rec ->
        Watcher.tracker = true
        Watcher.pause = false
        Watcher.stop = false
        delay(100)
        state.value = AppState.RUNNING
        result.clear()
        var lastPwdMetadata: LastPwdMetadata? = null
        if (rec) {
            lastPwdMetadata = datastore.data.first().lastPwdInfo
        }
        crack(
            threadPool = pool,
            options = opt,
            result = result,
            error = fileError,
            state = state,
            datastore = datastore,
            lastPwdMetadata = lastPwdMetadata
        )
    }


    val zipLauncher = rememberFilePickerLauncher(
        type = PickerType.File(listOf("zip")),
        title = stringResource(Res.string.select_title),
        platformSettings = FileKitPlatformSettings(
            parentWindow = parentWindow
        )
    ) { f ->
        file = f?.file
    }

    val dictLauncher = rememberFilePickerLauncher(
        mode = PickerMode.Multiple(null),
        type = PickerType.File(listOf("txt")),
        title = stringResource(Res.string.select_dict_file),
        platformSettings = FileKitPlatformSettings(
            parentWindow = parentWindow
        )
    ) { fs ->
        fs?.forEach { dictionaryFile.add(it.file) }
        dictionaryDisplay = dictionaryFile.joinToString("; ") { it.path }
    }

    LaunchedEffect(state.value) {
        if (state.value == AppState.CANCELLED || state.value == AppState.COMPLETED) {
            dictionaryDisplay = ""
        }
    }

    if (recovery) {
        ConfirmDialog(
            icon = Icons.Filled.Info,
            title = stringResource(Res.string.recovery_title),
            message = stringResource(Res.string.recovery_msg),
            positiveText = stringResource(Res.string.recovery_yes),
            negativeText = stringResource(Res.string.recovery_no),
            onAccept = {
                scope.launch {
                    val options = datastore.data.first().lastOptions ?: return@launch
                    opMode.value = options.opMode
                    threadCount.value = options.maxAllowedThread
                    threadCountDisplay.value = threadCount.value.toString()
                    file = File(options.file)
                    if (options.opMode == OpMode.DICTIONARY) {
                        dictionaryFile.clear()
                        options.dictFiles.forEach { dictionaryFile.add(File(it)) }
                        dictionaryDisplay = dictionaryFile.joinToString("; ") { it.path }
                    } else {
                        pwdOptions.value = options.pwdOptions
                        pwdLength.value = options.maxPwdLength
                        pwdLengthDisplay.value = pwdLength.value.toString()
                    }
                    delay(1500)
                    recovery = false
                    withContext(Dispatchers.Default) {
                        activate(options, true)
                    }
                }
            },
            onExit = {
                scope.launch {
                    datastore.updateData { UserPreferences(uiMode = it.uiMode) }
                    recovery = false
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        val options = datastore.data.first().lastOptions
        if (options != null && options.opMode != OpMode.BENCHMARK) {
            delay(1000)
            recovery = true
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .verticalScroll(scrollState)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) { focusManager.clearFocus(true) }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Image(
                painterResource(Res.drawable.zipcracker),
                contentDescription = "ZipCracker logo",
                modifier = Modifier.fillMaxWidth(0.06f)
            )
            Image(
                painterResource(Res.drawable.logo),
                contentDescription = "ZipCracker name",
                modifier = Modifier.fillMaxWidth(0.45f)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Button(
                enabled = state.value == AppState.NOT_INITIATED,
                colors = ButtonDefaults.buttonColors(
                    contentColor = Color.White,
                    disabledContentColor = MaterialTheme.colorScheme.contentColorFor(
                        MaterialTheme.colorScheme.background),
                ),
                onClick = {
                    scope.launch {
                        if (validated()) {
                            val options = CrackingOptions(
                                file = file?.absolutePath ?: return@launch,
                                encryption = encryption,
                                dictFiles = dictionaryFile.map { it.absolutePath },
                                maxAllowedThread = threadCount.value,
                                opMode = opMode.value,
                                maxPwdLength = if (opMode.value == OpMode.DICTIONARY) -1
                                else pwdLength.value,
                                pwdOptions = if (opMode.value == OpMode.DICTIONARY) -1
                                else pwdOptions.value
                            )
                            dictionaryFile.clear()
                            datastore.updateData {
                                it.copy(
                                    lastOptions = options,
                                )
                            }
                            withContext(Dispatchers.Default) {
                                activate(options, false)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .height(52.dp)

            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Crack ZIP",
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(Res.string.crack_button),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            if (state.value == AppState.RUNNING) {
                ProgressTracker(
                    state = state,
                    opMode = opMode.value,
                    onStop = {
                        scope.launch {
                            state.value = AppState.CANCELLED
                            delay(25)
                            Watcher.stop = true
                            delay(50)
                        }
                    }
                )
            }
            else if (state.value == AppState.COMPLETED
                || state.value == AppState.CANCELLED) {
                Spacer(modifier = Modifier.width(12.dp))
                ResultTitle(state)
            }

        }
        Spacer(modifier = Modifier.height(28.dp))
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(0.95f).align(Alignment.CenterHorizontally),
            color = MaterialTheme.colorScheme.primary,
            thickness = 1.5.dp
        )
        Spacer(modifier = Modifier.height(28.dp))
        if (showResult) {
            ResultDetails(
                parentWindow = parentWindow,
                pwdSet = result.toHashSet(),
                state = state,
                metadata = Log(
                    file = (file ?: return@Column).absolutePath,
                    encryption = encryption,
                    mode = opMode.value,
                    thread = threadCount.value
                ),
            )
        } else {
            TitleWithErrorWarning(
                title = stringResource(Res.string.select_title),
                fontSize = 20.sp,
                error = fileError.value,
            )
            FileInput(
                filename = fileDisplay,
                launcher = zipLauncher,
                state = state,
            ) {
                Text(stringResource(Res.string.select_prompt))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    stringResource(Res.string.select_method),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme
                        .contentColorFor(MaterialTheme.colorScheme.background),
                    modifier = Modifier
                        .fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OpMode.entries.forEach { entry ->
                        RadioButton(
                            enabled = state.value != AppState.RUNNING,
                            selected = opMode.value == entry,
                            onClick = {
                                opMode.value = entry
                                if (entry != OpMode.DICTIONARY) {
                                    pwdOptions.value = 0b0101
                                    pwdLength.value = 4
                                    pwdLengthDisplay.value = pwdLength.value.toString()
                                } else {
                                    dictionaryFile.clear()
                                    dictionaryDisplay = ""
                                }
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = Color.Gray
                            )
                        )
                        Text(
                            text = when (entry) {
                                OpMode.BRUTE -> stringResource(Res.string.select_brute)
                                OpMode.DICTIONARY -> stringResource(Res.string.select_dict)
                                OpMode.BENCHMARK -> stringResource(Res.string.select_benchmark)
                            },
                            fontSize = 16.sp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    enabled = state.value != AppState.RUNNING
                                ) {
                                    opMode.value = entry
                                },
                            color = if (opMode.value == entry) MaterialTheme.colorScheme.primary
                            else Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            RowWithIncrementer(
                title = stringResource(Res.string.max_thread_title, maxThread),
                num = threadCount,
                displayNum = threadCountDisplay,
                threshold = maxThread,
                buttonEnabled = state.value != AppState.RUNNING,
                textReadOnly = state.value == AppState.RUNNING,
                onValueChange = {
                    threadCount.value = if (threadCount.value < 1) 1
                    else if (threadCount.value > maxThread) maxThread
                    else threadCount.value
                },
                onFocusChanged = {
                    if (threadCount.value < 1) { threadCount.value = 1 }
                    else if (threadCount.value > maxThread) { threadCount.value = maxThread }
                    threadCountDisplay.value = threadCount.value.toString()
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (opMode.value == OpMode.DICTIONARY) {
                TitleWithErrorWarning(
                    title = stringResource(Res.string.select_dict_file),
                    error = dictError
                )
                FileInput(
                    filename = dictionaryDisplay,
                    launcher = dictLauncher,
                    state = state
                ) {
                    Text(stringResource(Res.string.select_dict_prompt))
                }
            } else {
                TitleWithErrorWarning(
                    title = stringResource(Res.string.pwd_title),
                    error = pwdOptionsError,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    LazyVerticalGrid(
                        verticalArrangement = Arrangement.Center,
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .heightIn(max = 200.dp),
                    ) {
                        items(2) { index ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp)
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                repeat(2) { idx ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().then(
                                            if (index * 2 + idx == 1) Modifier.padding(top = 8.5.dp)
                                            else Modifier
                                        )
                                    ) {
                                        Checkbox(
                                            enabled = (state.value != AppState.RUNNING)
                                                    && !(isBenchmark && (index * 2 + idx == 0 || index * 2 + idx == 2)),
                                            checked = (pwdOptions.value and (1 shl (index * 2 + idx))) != 0,
                                            onCheckedChange = {
                                                pwdOptions.value = if (it) {
                                                    pwdOptions.value or (1 shl (index * 2 + idx))
                                                } else {
                                                    pwdOptions.value and ((1 shl (index * 2 + idx)).inv())
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = MaterialTheme.colorScheme.primary,
                                                uncheckedColor = MaterialTheme.colorScheme.primary,
                                                checkmarkColor = Color.White,
                                                disabledUncheckedColor = Color.Gray,
                                                disabledCheckedColor = Color.Gray,
                                            ),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = pwdOptionsLabels[index * 2 + idx],
                                            color = MaterialTheme.colorScheme
                                                .contentColorFor(MaterialTheme.colorScheme.background),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                RowWithIncrementer(
                    title = stringResource(Res.string.pwd_length),
                    num = pwdLength,
                    displayNum = pwdLengthDisplay,
                    threshold = 8,
                    buttonEnabled = (state.value != AppState.RUNNING) && !isBenchmark,
                    textReadOnly = (state.value == AppState.RUNNING || isBenchmark),
                    onValueChange = {
                        pwdLength.value = if (pwdLength.value < 1) 1
                        else if (pwdLength.value > 8) 8
                        else pwdLength.value
                    },
                    onFocusChanged = {
                        if (pwdLength.value < 1) { pwdLength.value = 1 }
                        else if (pwdLength.value > 8) { pwdLength.value = 8 }
                        pwdLengthDisplay.value = pwdLength.value.toString()
                    }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}


