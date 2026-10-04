package com.quietreader.ui

import android.graphics.Bitmap
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.DrawableRes
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quietreader.data.BookFormat
import com.quietreader.data.BackupConflictPolicy
import com.quietreader.data.BackupConflictType
import com.quietreader.data.BackupPreview
import com.quietreader.data.ImportedBook
import com.quietreader.data.ImportedSource
import com.quietreader.data.ParsedBookText
import com.quietreader.data.PdfDocumentSession
import com.quietreader.data.ReaderRepository
import com.quietreader.data.SyncSettings
import com.quietreader.data.TypographySettings
import com.quietreader.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt
import java.util.Locale

private enum class MainRoute {
    SHELF,
    DISCOVER,
    STATS,
    PROFILE,
    READER,
}

private enum class ReaderSheet {
    NONE,
    IMPORT_BOOK,
    IMPORT_SOURCE,
    PASTE_SOURCE,
    TYPE,
    TTS,
    PDF_VIEW,
    SYNC,
    BACKUP,
    RESTORE_PREVIEW,
}

private enum class PdfPageFit {
    WIDTH,
    PAGE,
}

private enum class PickerRequest(val format: BookFormat? = null, val mimeTypes: Array<String>) {
    EPUB(BookFormat.EPUB, arrayOf("application/epub+zip", "application/octet-stream")),
    TXT(BookFormat.TXT, arrayOf("text/plain", "text/*", "application/octet-stream")),
    PDF(BookFormat.PDF, arrayOf("application/pdf", "application/octet-stream")),
    SOURCE_JSON(null, arrayOf("application/json", "text/json", "text/plain", "application/octet-stream")),
}

private data class ShelfBook(
    val id: String,
    val title: String,
    val author: String,
    val chapter: String,
    val progress: Float,
    @DrawableRes val cover: Int,
    val localBook: ImportedBook,
)

private fun ImportedBook.asShelfBook(): ShelfBook = ShelfBook(
    id = id,
    title = title,
    author = author,
    chapter = chapter,
    progress = progress,
    cover = R.drawable.cover_stars,
    localBook = this,
)

private const val MAX_TTS_CHARACTERS = 6000
private const val TTS_UTTERANCE_ID = "quiet-reader-current"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderApp() {
    var routeName by rememberSaveable { mutableStateOf(MainRoute.SHELF.name) }
    var sheetName by rememberSaveable { mutableStateOf(ReaderSheet.NONE.name) }
    var selectedBookId by rememberSaveable { mutableStateOf("") }
    var controlsVisible by rememberSaveable { mutableStateOf(false) }
    var ttsPlaying by rememberSaveable { mutableStateOf(false) }
    var ttsSpeed by rememberSaveable { mutableFloatStateOf(1f) }
    var pdfPageFitName by rememberSaveable { mutableStateOf(PdfPageFit.WIDTH.name) }
    var importedBooks by remember { mutableStateOf(emptyList<ImportedBook>()) }
    var importedSources by remember { mutableStateOf(emptyList<ImportedSource>()) }
    var typographySettings by remember { mutableStateOf(TypographySettings()) }
    var syncSettings by remember { mutableStateOf(SyncSettings()) }
    var readerText by remember { mutableStateOf<ParsedBookText?>(null) }
    var pickerRequest by remember { mutableStateOf<PickerRequest?>(null) }
    var backupPreview by remember { mutableStateOf<BackupPreview?>(null) }
    var busyMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var ttsReady by remember { mutableStateOf(false) }

    val route = MainRoute.valueOf(routeName)
    val sheet = ReaderSheet.valueOf(sheetName)
    val pdfPageFit = PdfPageFit.valueOf(pdfPageFitName)
    val context = LocalContext.current
    val repository = remember(context) { ReaderRepository(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val textToSpeech = remember(context) {
        TextToSpeech(context) { status ->
            scope.launch { ttsReady = status == TextToSpeech.SUCCESS }
        }.apply {
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    scope.launch { ttsPlaying = false }
                }

                override fun onError(utteranceId: String?) {
                    scope.launch { ttsPlaying = false }
                }
            })
        }
    }
    val shelfBooks = remember(importedBooks) { importedBooks.map(ImportedBook::asShelfBook) }
    val selectedBook = shelfBooks.firstOrNull { it.id == selectedBookId }

    LaunchedEffect(repository) {
        importedBooks = repository.loadBooks()
        importedSources = repository.loadSources()
        typographySettings = repository.loadTypography()
        syncSettings = repository.loadSyncSettings()
    }

    fun navigate(target: MainRoute) {
        routeName = target.name
        if (target != MainRoute.READER) controlsVisible = false
    }

    fun openSheet(target: ReaderSheet) {
        sheetName = target.name
    }

    fun notify(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun startImport(request: PickerRequest) {
        pickerRequest = request
        sheetName = ReaderSheet.NONE.name
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        val request = pickerRequest
        pickerRequest = null
        if (uri == null || request == null) {
            if (uri == null) notify("已取消选择文件")
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            busyMessage = "正在导入"
            try {
                if (request.format != null) {
                    val result = repository.importBook(uri, request.format)
                    importedBooks = repository.loadBooks()
                    notify("已导入：${result.book.title}")
                } else {
                    val result = repository.importSource(uri)
                    importedSources = repository.loadSources()
                    val skippedLabel = if (result.skipped > 0) "，跳过 ${result.skipped} 项" else ""
                    notify("已导入 ${result.added.size} 个书源$skippedLabel")
                }
            } catch (error: Exception) {
                val message = error.message?.takeIf { it.isNotBlank() } ?: "文件格式或内容不可用"
                notify("导入失败：$message")
            } finally {
                busyMessage = null
            }
        }
    }

    val backupPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri == null) {
            notify("已取消备份")
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            busyMessage = "正在导出"
            try {
                val output = context.contentResolver.openOutputStream(uri)
                    ?: throw IllegalArgumentException("无法创建备份文件")
                val itemCount = output.use { repository.exportBackup(it) }
                notify("备份已导出，共 $itemCount 项")
            } catch (error: Exception) {
                notify("备份失败：${error.message ?: "无法写入文件"}")
            } finally {
                busyMessage = null
            }
        }
    }

    val restorePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) {
            notify("已取消恢复")
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            busyMessage = "正在检查备份"
            try {
                backupPreview = repository.prepareBackup(uri)
                sheetName = ReaderSheet.RESTORE_PREVIEW.name
            } catch (error: Exception) {
                notify("备份不可用：${error.message ?: "文件格式或内容不完整"}")
            } finally {
                busyMessage = null
            }
        }
    }

    fun startBackup() {
        backupPicker.launch("静读备份.zip")
    }

    fun startRestore() {
        sheetName = ReaderSheet.NONE.name
        restorePicker.launch(arrayOf("application/zip", "application/octet-stream"))
    }

    fun closeBackupPreview() {
        backupPreview?.let(repository::discardBackup)
        backupPreview = null
        sheetName = ReaderSheet.NONE.name
    }

    fun restoreBackup(policy: BackupConflictPolicy) {
        val preview = backupPreview ?: return
        scope.launch {
            busyMessage = "正在恢复"
            try {
                val result = repository.restoreBackup(preview, policy)
                backupPreview = null
                sheetName = ReaderSheet.NONE.name
                importedBooks = repository.loadBooks()
                importedSources = repository.loadSources()
                typographySettings = repository.loadTypography()
                syncSettings = repository.loadSyncSettings()
                val replaced = result.replacedBooks + result.replacedSources
                val kept = result.keptBooks + result.keptSources
                val settings = if (result.settingsRestored) "，设置已恢复" else "，冲突设置已保留"
                notify("恢复完成：新增 ${result.addedBooks + result.addedSources} 项，覆盖 $replaced 项，保留 $kept 项$settings")
            } catch (error: Exception) {
                notify("恢复失败：${error.message ?: "无法写入本地数据"}")
            } finally {
                busyMessage = null
            }
        }
    }

    LaunchedEffect(pickerRequest) {
        pickerRequest?.let { filePicker.launch(it.mimeTypes) }
    }

    LaunchedEffect(sheet, backupPreview) {
        if (sheet == ReaderSheet.RESTORE_PREVIEW && backupPreview == null) {
            sheetName = ReaderSheet.NONE.name
        }
    }

    LaunchedEffect(selectedBook?.id) {
        readerText = null
        val localBook = selectedBook?.localBook ?: return@LaunchedEffect
        if (localBook.format == BookFormat.TXT || localBook.format == BookFormat.EPUB) {
            readerText = repository.readBookText(localBook)
        }
    }

    LaunchedEffect(route, selectedBook?.id) {
        if (route == MainRoute.READER && selectedBook == null) {
            routeName = MainRoute.SHELF.name
            controlsVisible = false
        }
    }

    DisposableEffect(textToSpeech) {
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    LaunchedEffect(ttsPlaying, ttsReady, selectedBook?.id, readerText) {
        if (ttsPlaying && ttsReady) {
            if (selectedBook == null) {
                ttsPlaying = false
                return@LaunchedEffect
            }
            textToSpeech.language = Locale.SIMPLIFIED_CHINESE
            val text = readerText?.paragraphs.orEmpty()
                .joinToString("\n")
                .take(MAX_TTS_CHARACTERS)
            if (text.isNotBlank()) {
                textToSpeech.setSpeechRate(ttsSpeed)
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, TTS_UTTERANCE_ID)
            } else {
                ttsPlaying = false
            }
        } else if (!ttsPlaying) {
            textToSpeech.stop()
        }
    }

    LaunchedEffect(ttsSpeed, ttsReady) {
        if (ttsReady) textToSpeech.setSpeechRate(ttsSpeed)
    }

    LaunchedEffect(route) {
        if (route != MainRoute.READER && ttsPlaying) ttsPlaying = false
    }

    BackHandler(enabled = route == MainRoute.READER) {
        navigate(MainRoute.SHELF)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (route != MainRoute.READER) {
                ReaderBottomBar(
                    current = route,
                    onNavigate = ::navigate,
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AnimatedContent(
                targetState = route,
                label = "route-transition",
                transitionSpec = {
                    (fadeIn(tween(280)) + slideInHorizontally(tween(280)) { it / 32 }) togetherWith
                        (fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { -it / 32 })
                },
            ) { targetRoute ->
                when (targetRoute) {
                    MainRoute.SHELF -> ShelfScreen(
                        books = shelfBooks,
                        onOpenReader = {
                            selectedBookId = it.id
                            navigate(MainRoute.READER)
                        },
                        onImportBooks = { openSheet(ReaderSheet.IMPORT_BOOK) },
                        onDiscover = { navigate(MainRoute.DISCOVER) },
                    )

                    MainRoute.DISCOVER -> DiscoverScreen(
                        books = shelfBooks,
                        sources = importedSources,
                        onImportSource = { openSheet(ReaderSheet.IMPORT_SOURCE) },
                        onOpenReader = {
                            selectedBookId = it.id
                            navigate(MainRoute.READER)
                        },
                        onToggleSource = { source ->
                            repository.setSourceEnabled(source.id, !source.enabled)
                            importedSources = repository.loadSources()
                            notify(if (source.enabled) "已停用：${source.name}" else "已启用：${source.name}")
                        },
                    )

                    MainRoute.STATS -> StatsScreen()
                    MainRoute.PROFILE -> ProfileScreen(
                        typography = typographySettings,
                        syncSettings = syncSettings,
                        onSync = { openSheet(ReaderSheet.SYNC) },
                        onTypography = { openSheet(ReaderSheet.TYPE) },
                        onTts = { openSheet(ReaderSheet.TTS) },
                        onNotify = ::notify,
                        onBackup = { openSheet(ReaderSheet.BACKUP) },
                    )

                    MainRoute.READER -> selectedBook?.let { book ->
                        ReaderScreen(
                            book = book,
                            loadedText = readerText,
                            typography = typographySettings,
                            pdfPageFit = pdfPageFit,
                            controlsVisible = controlsVisible,
                            ttsPlaying = ttsPlaying,
                            onToggleControls = { controlsVisible = !controlsVisible },
                            onBack = { navigate(MainRoute.SHELF) },
                            onOpenType = { openSheet(ReaderSheet.TYPE) },
                            onOpenTts = { openSheet(ReaderSheet.TTS) },
                            onOpenPdfView = { openSheet(ReaderSheet.PDF_VIEW) },
                        )
                    } ?: ReaderUnavailableScreen(onBack = { navigate(MainRoute.SHELF) })
                }
            }

            if (busyMessage != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                    shape = RoundedCornerShape(4.dp),
                    tonalElevation = 0.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(9.dp))
                        Text(busyMessage ?: "正在处理", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }

    if (sheet != ReaderSheet.NONE) {
        ModalBottomSheet(
            onDismissRequest = {
                when {
                    sheet == ReaderSheet.RESTORE_PREVIEW && busyMessage == null -> closeBackupPreview()
                    sheet != ReaderSheet.RESTORE_PREVIEW -> sheetName = ReaderSheet.NONE.name
                }
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dragHandle = { SheetHandle() },
        ) {
            when (sheet) {
                ReaderSheet.IMPORT_BOOK -> ImportSheet(
                    title = "导入本地书籍",
                    options = listOf(
                        SheetOption(Icons.Default.MenuBook, "选择 EPUB", "保留目录、封面和排版结构"),
                        SheetOption(Icons.Default.List, "选择 TXT", "自动识别编码与章节"),
                        SheetOption(Icons.Default.FileOpen, "选择 PDF", "固定版式阅读"),
                    ),
                    onSelected = { option ->
                        when (option.title) {
                            "选择 EPUB" -> startImport(PickerRequest.EPUB)
                            "选择 TXT" -> startImport(PickerRequest.TXT)
                            "选择 PDF" -> startImport(PickerRequest.PDF)
                        }
                    },
                )

                ReaderSheet.IMPORT_SOURCE -> ImportSheet(
                    title = "导入书源",
                    options = listOf(
                        SheetOption(Icons.Default.ImportExport, "导入 JSON", "兼容阅读 3.0 / Legado 书源"),
                        SheetOption(Icons.Default.Code, "粘贴规则", "先预览，再选择启用项"),
                        SheetOption(Icons.Default.BugReport, "扫描二维码", "从另一台设备接收配置"),
                    ),
                    onSelected = { option ->
                        when (option.title) {
                            "导入 JSON" -> startImport(PickerRequest.SOURCE_JSON)
                            "粘贴规则" -> openSheet(ReaderSheet.PASTE_SOURCE)
                            "扫描二维码" -> {
                                sheetName = ReaderSheet.NONE.name
                                notify("二维码导入将在书源预览阶段接入")
                            }
                        }
                    },
                )

                ReaderSheet.PASTE_SOURCE -> PasteSourceSheet(
                    onDismiss = { sheetName = ReaderSheet.NONE.name },
                    onImport = { raw ->
                        sheetName = ReaderSheet.NONE.name
                        scope.launch {
                            busyMessage = "正在导入"
                            try {
                                val result = repository.importSourceText(raw)
                                importedSources = repository.loadSources()
                                val skippedLabel = if (result.skipped > 0) "，跳过 ${result.skipped} 项" else ""
                                notify("已导入 ${result.added.size} 个书源$skippedLabel")
                            } catch (error: Exception) {
                                notify("导入失败：${error.message ?: "规则格式不可用"}")
                            } finally {
                                busyMessage = null
                            }
                        }
                    },
                )

                ReaderSheet.TYPE -> TypographySheet(
                    initial = typographySettings,
                    onSave = { settings ->
                        repository.saveTypography(settings)
                        typographySettings = settings
                        sheetName = ReaderSheet.NONE.name
                        notify("排版设置已保存")
                    },
                    onDismiss = { sheetName = ReaderSheet.NONE.name },
                )
                ReaderSheet.TTS -> TtsSheet(
                    book = selectedBook,
                    playing = ttsPlaying,
                    ready = ttsReady,
                    speed = ttsSpeed,
                    onSpeedChange = { ttsSpeed = it },
                    onToggle = { ttsPlaying = !ttsPlaying },
                    onDismiss = { sheetName = ReaderSheet.NONE.name },
                )

                ReaderSheet.PDF_VIEW -> PdfViewSheet(
                    initial = pdfPageFit,
                    onApply = { fit ->
                        pdfPageFitName = fit.name
                        sheetName = ReaderSheet.NONE.name
                    },
                    onDismiss = { sheetName = ReaderSheet.NONE.name },
                )

                ReaderSheet.SYNC -> SyncSheet(
                    initial = syncSettings,
                    onDismiss = { sheetName = ReaderSheet.NONE.name },
                    onSaved = { settings ->
                        repository.saveSyncSettings(settings)
                        syncSettings = settings
                        sheetName = ReaderSheet.NONE.name
                        notify("同步设置已保存")
                    },
                )

                ReaderSheet.BACKUP -> BackupSheet(
                    onExport = {
                        sheetName = ReaderSheet.NONE.name
                        startBackup()
                    },
                    onRestore = ::startRestore,
                    onDismiss = { sheetName = ReaderSheet.NONE.name },
                )

                ReaderSheet.RESTORE_PREVIEW -> backupPreview?.let { preview ->
                    RestorePreviewSheet(
                        preview = preview,
                        enabled = busyMessage == null,
                        onRestore = ::restoreBackup,
                        onDismiss = { if (busyMessage == null) closeBackupPreview() },
                    )
                }

                ReaderSheet.NONE -> Unit
            }
        }
    }
}

@Composable
private fun ReaderBottomBar(
    current: MainRoute,
    onNavigate: (MainRoute) -> Unit,
) {
    val items = listOf(
        Triple(MainRoute.SHELF, Icons.Default.LibraryBooks, "书架"),
        Triple(MainRoute.DISCOVER, Icons.Default.Explore, "发现"),
        Triple(MainRoute.STATS, Icons.Default.BarChart, "统计"),
        Triple(MainRoute.PROFILE, Icons.Default.Person, "我的"),
    )
    NavigationBar(
        modifier = Modifier.height(76.dp),
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets.navigationBars,
    ) {
        items.forEach { (route, icon, label) ->
            NavigationBarItem(
                selected = current == route,
                onClick = { onNavigate(route) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onBackground,
                    selectedTextColor = MaterialTheme.colorScheme.onBackground,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
private fun ShelfScreen(
    books: List<ShelfBook>,
    onOpenReader: (ShelfBook) -> Unit,
    onImportBooks: () -> Unit,
    onDiscover: () -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("09 / 2026", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("书架", style = MaterialTheme.typography.displaySmall)
            }
            QuietIconButton(Icons.Default.Search, "搜索书架", onClick = onDiscover)
            QuietIconButton(Icons.Default.Add, "导入书籍", onClick = onImportBooks)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.72f))

        if (books.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 52.dp, bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    modifier = Modifier.size(68.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f),
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp),
                    )
                }
                Text("书架还是空的", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 18.dp))
                Text(
                    "导入 EPUB、TXT 或 PDF。书籍文件只保存在本机。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 7.dp),
                )
                Text("0 本书", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 9.dp))
                OutlinedButton(
                    onClick = onImportBooks,
                    modifier = Modifier.padding(top = 18.dp).fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(4.dp),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("导入第一本书")
                }
            }
        } else {
            val firstBook = books.first()
            Text(
                text = if (firstBook.progress > 0f) "继续阅读" else "开始阅读",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 22.dp, bottom = 4.dp),
            )
            ContinueReadingRow(book = firstBook, onClick = { onOpenReader(firstBook) })
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.72f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 23.dp, bottom = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("全部书籍", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("${books.size} 本", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                QuietIconButton(Icons.Default.GridView, "网格视图", onClick = {})
            }

            books.chunked(3).forEach { rowBooks ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    rowBooks.forEach { book ->
                        BookCard(
                            book = book,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenReader(book) },
                        )
                    }
                    repeat(3 - rowBooks.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            OutlinedButton(
                onClick = onImportBooks,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(4.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(19.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("导入本地书籍")
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun ContinueReadingRow(book: ShelfBook, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookCover(book = book, modifier = Modifier.width(72.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (book.progress > 0f) "继续阅读 · ${book.progress.percentLabel()}" else "开始阅读",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.height(7.dp))
            Text(book.chapter, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(9.dp))
            LinearProgressIndicator(
                progress = { book.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.error,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.44f),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Icon(Icons.Default.ChevronRight, contentDescription = "打开", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BookCard(book: ShelfBook, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .wrapContentHeight(),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            BookCover(book = book, modifier = Modifier.fillMaxWidth())
            if (book.progress > 0f) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                    color = Color(0xCC252522),
                    shape = RoundedCornerShape(2.dp),
                ) {
                    Text(
                        text = book.progress.percentLabel(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(7.dp))
        Text(book.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(book.author, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun BookCover(book: ShelfBook, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(book.cover),
        contentDescription = "${book.title}封面",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(2.dp)),
    )
}

@Composable
private fun DiscoverScreen(
    books: List<ShelfBook>,
    sources: List<ImportedSource>,
    onImportSource: () -> Unit,
    onOpenReader: (ShelfBook) -> Unit,
    onToggleSource: (ImportedSource) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        ScreenTitle(title = "发现", eyebrow = "从你导入的书源开始")
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)) },
        ) {
            listOf("找书", "我的书源").forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        if (selectedTab == 0) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(4.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("搜索书名、作者或关键词") },
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text("本地书籍", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            val visibleBooks = books.filter { book ->
                query.isBlank() || book.title.contains(query, ignoreCase = true) || book.author.contains(query, ignoreCase = true)
            }
            if (visibleBooks.isEmpty()) {
                Text(
                    if (books.isEmpty() && query.isBlank()) "还没有导入书籍" else "没有匹配的书",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 18.dp),
                )
            }
            visibleBooks.take(5).forEach { book ->
                SearchResultRow(book = book, onClick = { onOpenReader(book) })
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text("搜索提示", style = MaterialTheme.typography.titleMedium)
            Text(
                "书源只来自你的导入配置。搜索前请先在“我的书源”中启用一个可用规则集。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("已启用 ${sources.count { it.enabled }} 个规则集", style = MaterialTheme.typography.titleMedium)
                    Text("本地保存 · 可随时导出", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(
                    onClick = onImportSource,
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("导入")
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            if (sources.isEmpty()) {
                Text("还没有导入书源", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 18.dp))
            } else {
                sources.forEach { source ->
                    SourceRow(
                        title = source.name,
                        subtitle = "${source.url} · ${source.ruleCount} 条规则",
                        enabled = source.enabled,
                        onClick = { onToggleSource(source) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("导入流程", style = MaterialTheme.typography.titleMedium)
            Text(
                "JSON → 安全预览 → 选择启用项 → 运行测试。任何失败请求都会留在调试日志中。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SearchResultRow(book: ShelfBook, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookCover(book = book, modifier = Modifier.width(52.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(book.title, style = MaterialTheme.typography.titleMedium)
            Text("${book.author} · ${book.chapter}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = "打开", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SourceRow(title: String, subtitle: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = if (enabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.size(42.dp),
        ) {
            Icon(
                Icons.Default.Code,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(10.dp),
            )
        }
        Spacer(modifier = Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = enabled,
            onCheckedChange = { onClick() },
            modifier = Modifier.padding(start = 7.dp),
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.58f))
}

@Composable
private fun StatsScreen() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        ScreenTitle(title = "阅读统计", eyebrow = "只属于你的阅读年鉴")
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("本周", "本月", "今年").forEachIndexed { index, label ->
                FilterChip(selected = index == 0, onClick = {}, label = { Text(label) })
            }
        }
        Spacer(modifier = Modifier.height(26.dp))
        Text("0 分钟", style = MaterialTheme.typography.displaySmall)
        Text("本周阅读时长", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(23.dp))
        val bars = listOf("一", "二", "三", "四", "五", "六", "日").map { it to 0f }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            bars.forEach { (day, value) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height((110 * value).dp)
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f), RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(day, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 20.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
        StatLine("连续阅读", "0 天", Icons.Default.Refresh)
        StatLine("完成书籍", "0 本", Icons.Default.Check)
        StatLine("估算字数", "0 字", Icons.Default.MenuBook)
        Spacer(modifier = Modifier.height(18.dp))
        Text("阅读时长排行", style = MaterialTheme.typography.titleMedium)
        Text(
            "暂无阅读记录。开始阅读后，这里只展示真实统计数据。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun StatLine(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ProfileScreen(
    typography: TypographySettings,
    syncSettings: SyncSettings,
    onSync: () -> Unit,
    onTypography: () -> Unit,
    onTts: () -> Unit,
    onNotify: (String) -> Unit,
    onBackup: () -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        ScreenTitle(title = "我的", eyebrow = "设置只属于你的阅读空间")
        ProfileSectionLabel("阅读")
        SettingsRow(Icons.Default.TextFields, "默认排版", "字体、字号、行距与页边距", "${typography.fontFamily} ${typography.fontSizeSp.roundToInt()}", onTypography)
        SettingsRow(Icons.Default.Headphones, "听书设置", "系统 TTS 与媒体控制", "1.0×", onTts)
        SettingsRow(Icons.Default.DarkMode, "主题", "日间、夜间、护眼与自定义", "日间", { onNotify("主题切换将在下一步接入") })
        ProfileSectionLabel("数据")
        SettingsRow(Icons.Default.Cloud, "多设备同步", "WebDAV · 本地优先", if (syncSettings.url.isBlank()) "未连接" else "已配置", onSync)
        SettingsRow(Icons.Default.Security, "备份与恢复", "导出、恢复与冲突预览", "", onBackup)
        ProfileSectionLabel("应用")
        SettingsRow(Icons.Default.Language, "阅读规则", "导入格式与请求安全", "Legado JSON", { onNotify("规则说明已保留在项目文档") })
        SettingsRow(Icons.Default.Settings, "关于静读", "v0.1.0 · Android 原生预览", "", { onNotify("这是 Android 原生第一阶段") })
        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun ProfileSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 19.dp, bottom = 2.dp),
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(21.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (value.isNotBlank()) {
            Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.width(7.dp))
        }
        Icon(Icons.Default.ChevronRight, contentDescription = "打开", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.56f))
}

@Composable
private fun ReaderUnavailableScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("没有可打开的书籍", style = MaterialTheme.typography.titleLarge)
        Text(
            "请返回书架并导入一本本地书籍。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
        OutlinedButton(onClick = onBack, shape = RoundedCornerShape(4.dp)) {
            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(7.dp))
            Text("返回书架")
        }
    }
}

@Composable
private fun ReaderScreen(
    book: ShelfBook,
    loadedText: ParsedBookText?,
    typography: TypographySettings,
    pdfPageFit: PdfPageFit,
    controlsVisible: Boolean,
    ttsPlaying: Boolean,
    onToggleControls: () -> Unit,
    onBack: () -> Unit,
    onOpenType: () -> Unit,
    onOpenTts: () -> Unit,
    onOpenPdfView: () -> Unit,
) {
    val pdfBook = book.localBook.takeIf { it.format == BookFormat.PDF }
    val pdfListState = rememberLazyListState()
    var pdfPageCount by remember(book.id) { mutableIntStateOf(0) }
    val currentPdfPage by remember(pdfListState) {
        derivedStateOf {
            if (pdfPageCount <= 0) 0 else (pdfListState.firstVisibleItemIndex + 1).coerceIn(1, pdfPageCount)
        }
    }
    val readerScope = rememberCoroutineScope()
    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(
        fontFamily = when (typography.fontFamily) {
            "黑体" -> FontFamily.SansSerif
            else -> FontFamily.Serif
        },
        fontSize = typography.fontSizeSp.sp,
        lineHeight = (typography.fontSizeSp * typography.lineHeightMultiplier).sp,
    )
    val paragraphs = loadedText?.paragraphs.orEmpty()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onToggleControls),
    ) {
        if (pdfBook != null) {
            PdfReaderContent(
                book = pdfBook,
                pageFit = pdfPageFit,
                listState = pdfListState,
                onPageCountChanged = { pdfPageCount = it },
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .statusBarsPadding(),
            ) {
                Spacer(modifier = Modifier.height(62.dp))
                Text(loadedText?.chapter ?: book.chapter, style = MaterialTheme.typography.titleLarge)
                Text("${book.title} · ${book.author}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp, bottom = 28.dp))
                if ((book.localBook.format == BookFormat.TXT || book.localBook.format == BookFormat.EPUB) && loadedText == null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 24.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("正在读取正文", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                paragraphs.forEach { paragraph ->
                    Text(
                        text = paragraph,
                        style = bodyStyle,
                        modifier = Modifier.padding(bottom = 20.dp),
                    )
                }
                Spacer(modifier = Modifier.height(110.dp))
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(160)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.94f))
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    QuietIconButton(Icons.Default.ArrowBack, "返回书架", onClick = onBack)
                    Text(book.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    QuietIconButton(Icons.Default.BookmarkBorder, "添加书签", onClick = {})
                    QuietIconButton(Icons.Default.MoreHoriz, "更多操作", onClick = {})
                }
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                        if (pdfBook != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    if (pdfPageCount > 0) "第 $currentPdfPage 页 / 共 $pdfPageCount 页" else "正在载入 PDF",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "固定版式 · 暂不支持听书",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        LinearProgressIndicator(
                            progress = {
                                if (pdfBook != null && pdfPageCount > 0) {
                                    currentPdfPage.toFloat() / pdfPageCount.toFloat()
                                } else {
                                    book.progress
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = MaterialTheme.colorScheme.error,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            if (pdfBook != null) {
                                ReaderTool(
                                    Icons.Default.ArrowBack,
                                    "上一页",
                                    onClick = {
                                        readerScope.launch {
                                            pdfListState.animateScrollToItem((currentPdfPage - 2).coerceAtLeast(0))
                                        }
                                    },
                                    enabled = currentPdfPage > 1,
                                )
                                ReaderTool(Icons.Default.AspectRatio, "版式", onOpenPdfView)
                                ReaderTool(
                                    Icons.Default.ArrowForward,
                                    "下一页",
                                    onClick = {
                                        readerScope.launch {
                                            pdfListState.animateScrollToItem(currentPdfPage.coerceAtMost(pdfPageCount - 1))
                                        }
                                    },
                                    enabled = pdfPageCount > 0 && currentPdfPage < pdfPageCount,
                                )
                            } else {
                                ReaderTool(Icons.Default.TextFields, "排版", onOpenType)
                                ReaderTool(if (ttsPlaying) Icons.Default.Pause else Icons.Default.Headphones, if (ttsPlaying) "暂停" else "听书", onOpenTts)
                                ReaderTool(Icons.Default.List, "目录", onClick = {})
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfReaderContent(
    book: ImportedBook,
    pageFit: PdfPageFit,
    listState: LazyListState,
    onPageCountChanged: (Int) -> Unit,
) {
    var session by remember(book.filePath) { mutableStateOf<PdfDocumentSession?>(null) }
    var loading by remember(book.filePath) { mutableStateOf(true) }
    var errorMessage by remember(book.filePath) { mutableStateOf<String?>(null) }

    LaunchedEffect(book.filePath) {
        var openedSession: PdfDocumentSession? = null
        session = null
        loading = true
        errorMessage = null
        onPageCountChanged(0)
        try {
            withContext(Dispatchers.IO) {
                openedSession = PdfDocumentSession.open(File(book.filePath))
            }
            session = checkNotNull(openedSession)
            openedSession = null
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            errorMessage = error.message?.takeIf { it.isNotBlank() } ?: "文件已损坏或受密码保护"
        } finally {
            openedSession?.close()
            loading = false
        }
    }

    DisposableEffect(session) {
        val activeSession = session
        onDispose { activeSession?.close() }
    }

    val activeSession = session
    LaunchedEffect(activeSession?.pageCount) {
        onPageCountChanged(activeSession?.pageCount ?: 0)
    }

    when {
        loading -> ReaderLoadingState("正在打开 PDF")
        errorMessage != null -> ReaderErrorState(
            title = "无法打开 PDF",
            message = errorMessage ?: "文件不可用",
        )
        activeSession != null -> BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val widthFit = (maxWidth - 20.dp).coerceAtLeast(1.dp)
            val pageFitWidth = minOf(
                (maxWidth - 52.dp).coerceAtLeast(1.dp),
                (maxHeight - 56.dp).coerceAtLeast(1.dp) * activeSession.defaultPageAspectRatio,
            )
            val pageWidth = if (pageFit == PdfPageFit.WIDTH) widthFit else pageFitWidth
            val requestedWidthPx = with(density) { pageWidth.roundToPx() }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(
                    count = activeSession.pageCount,
                    key = { pageIndex -> "${book.id}:$pageIndex" },
                ) { pageIndex ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PdfPage(
                            session = activeSession,
                            pageIndex = pageIndex,
                            pageWidth = pageWidth,
                            requestedWidthPx = requestedWidthPx,
                        )
                        Text(
                            text = "${pageIndex + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
        else -> ReaderErrorState(title = "无法打开 PDF", message = "文件不可用")
    }
}

@Composable
private fun PdfPage(
    session: PdfDocumentSession,
    pageIndex: Int,
    pageWidth: androidx.compose.ui.unit.Dp,
    requestedWidthPx: Int,
) {
    var bitmap by remember(session, pageIndex, requestedWidthPx) { mutableStateOf<Bitmap?>(null) }
    var errorMessage by remember(session, pageIndex, requestedWidthPx) { mutableStateOf<String?>(null) }
    var retryKey by remember(session, pageIndex, requestedWidthPx) { mutableIntStateOf(0) }

    LaunchedEffect(session, pageIndex, requestedWidthPx, retryKey) {
        bitmap = null
        errorMessage = null
        try {
            bitmap = session.renderPage(pageIndex, requestedWidthPx)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            errorMessage = error.message?.takeIf { it.isNotBlank() } ?: "页面渲染失败"
        }
    }

    val pageAspectRatio = bitmap
        ?.takeIf { it.width > 0 && it.height > 0 }
        ?.let { it.width / it.height.toFloat() }
        ?: session.defaultPageAspectRatio

    Surface(
        modifier = Modifier.width(pageWidth).aspectRatio(pageAspectRatio),
        color = Color.White,
        shape = RoundedCornerShape(2.dp),
        shadowElevation = 1.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val renderedPage = bitmap
            when {
                renderedPage != null -> {
                    val imageBitmap = remember(renderedPage) { renderedPage.asImageBitmap() }
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "PDF 第 ${pageIndex + 1} 页",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
                errorMessage != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "第 ${pageIndex + 1} 页加载失败",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF555555),
                    )
                    OutlinedButton(
                        onClick = { retryKey += 1 },
                        modifier = Modifier.padding(top = 10.dp),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(7.dp))
                        Text("重试")
                    }
                }
                else -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun ReaderLoadingState(message: String) {
    Row(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ReaderErrorState(title: String, message: String) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun ReaderTool(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val contentColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp), tint = contentColor)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = contentColor)
    }
}

@Composable
private fun ScreenTitle(title: String, eyebrow: String? = null) {
    Column(modifier = Modifier.padding(top = 12.dp, bottom = 20.dp)) {
        eyebrow?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text(title, style = MaterialTheme.typography.displaySmall)
    }
}

@Composable
private fun QuietIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(21.dp))
    }
}

private data class SheetOption(val icon: ImageVector, val title: String, val subtitle: String)

@Composable
private fun SheetHandle() {
    Box(
        modifier = Modifier
            .padding(top = 9.dp, bottom = 2.dp)
            .width(34.dp)
            .height(4.dp)
            .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)),
    )
}

@Composable
private fun ImportSheet(
    title: String,
    options: List<SheetOption>,
    onSelected: (SheetOption) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPaddingCompat(),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 14.dp, bottom = 13.dp))
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelected(option) }
                    .padding(vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(option.icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(15.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(option.title, style = MaterialTheme.typography.titleMedium)
                    Text(option.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = "选择", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
        }
        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun BackupSheet(
    onExport: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPaddingCompat(),
    ) {
        SheetTitleRow(title = "备份与恢复", onDismiss = onDismiss)
        Text(
            "书籍文件、书源规则、阅读进度和本地设置会写入一个 ZIP 文件。恢复前会先检查内容并显示冲突。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(15.dp))
        OutlinedButton(
            onClick = onExport,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(4.dp),
        ) {
            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("导出当前数据")
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
            onClick = onRestore,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(4.dp),
        ) {
            Icon(Icons.Default.ImportExport, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("选择备份并恢复")
        }
        Text(
            "恢复只会写入静读的应用私有目录，不会修改原备份文件。",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 14.dp, bottom = 20.dp),
        )
    }
}

@Composable
private fun RestorePreviewSheet(
    preview: BackupPreview,
    enabled: Boolean,
    onRestore: (BackupConflictPolicy) -> Unit,
    onDismiss: () -> Unit,
) {
    var policyName by rememberSaveable(preview.stagedPath) {
        mutableStateOf(BackupConflictPolicy.KEEP_LOCAL.name)
    }
    val policy = BackupConflictPolicy.valueOf(policyName)
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .navigationBarsPaddingCompat(),
    ) {
        SheetTitleRow(title = "恢复预览", onDismiss = onDismiss)
        Text(
            preview.fileName,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "备份格式 v${preview.version} · 已通过文件完整性检查",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BackupMetric(value = preview.bookCount.toString(), label = "本地书籍", modifier = Modifier.weight(1f))
            BackupMetric(value = preview.sourceCount.toString(), label = "书源规则", modifier = Modifier.weight(1f))
            BackupMetric(value = preview.conflicts.size.toString(), label = "待处理冲突", modifier = Modifier.weight(1f))
        }
        if (preview.version == 1) {
            Text(
                "这是旧版备份。书籍进度和启用状态按旧格式恢复，阅读设置仍可选择是否覆盖。",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 15.dp),
            )
        }
        Text(
            if (preview.conflicts.isEmpty()) "未发现同标识数据冲突" else "发现 ${preview.conflicts.size} 项冲突",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 22.dp, bottom = 4.dp),
        )
        if (preview.conflicts.isEmpty()) {
            Text(
                "新增内容会并入当前书架，原有内容不会被修改。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 4.dp)) {
                    preview.conflicts.forEach { conflict ->
                        BackupConflictRow(conflict)
                    }
                }
            }
        }
        Text(
            "冲突处理方式",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = policy == BackupConflictPolicy.KEEP_LOCAL,
                onClick = { policyName = BackupConflictPolicy.KEEP_LOCAL.name },
                label = { Text("保留本地") },
                leadingIcon = if (policy == BackupConflictPolicy.KEEP_LOCAL) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
            )
            FilterChip(
                selected = policy == BackupConflictPolicy.USE_BACKUP,
                onClick = { policyName = BackupConflictPolicy.USE_BACKUP.name },
                label = { Text("使用备份") },
                leadingIcon = if (policy == BackupConflictPolicy.USE_BACKUP) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
            )
        }
        Text(
            if (policy == BackupConflictPolicy.KEEP_LOCAL) {
                "冲突项保持当前设备内容，新增书籍和书源仍会导入；阅读设置保持本地。"
            } else {
                "冲突项使用备份内容覆盖，并恢复备份中的排版与同步设置。"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 7.dp),
        )
        Spacer(modifier = Modifier.height(15.dp))
        OutlinedButton(
            onClick = { onRestore(policy) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(4.dp),
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (policy == BackupConflictPolicy.KEEP_LOCAL) "合并并保留本地冲突" else "覆盖冲突并恢复")
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun BackupMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        shape = RoundedCornerShape(4.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BackupConflictRow(conflict: com.quietreader.data.BackupConflict) {
    val icon = when (conflict.type) {
        BackupConflictType.BOOK -> Icons.Default.MenuBook
        BackupConflictType.SOURCE -> Icons.Default.Code
        BackupConflictType.SETTINGS -> Icons.Default.Settings
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(conflict.backupLabel, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("当前：${conflict.localLabel}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PasteSourceSheet(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
) {
    var rawJson by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPaddingCompat(),
    ) {
        SheetTitleRow(title = "粘贴规则", onDismiss = onDismiss)
        Text("仅解析 JSON 结构，不会在导入时执行网络请求。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = rawJson,
            onValueChange = { rawJson = it },
            modifier = Modifier.fillMaxWidth().height(190.dp).padding(top = 12.dp),
            label = { Text("书源 JSON") },
            placeholder = { Text("粘贴阅读 3.0 / Legado 书源") },
        )
        OutlinedButton(
            onClick = { onImport(rawJson) },
            enabled = rawJson.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp).padding(top = 13.dp),
            shape = RoundedCornerShape(4.dp),
        ) {
            Icon(Icons.Default.ImportExport, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("校验并保存")
        }
        Spacer(modifier = Modifier.height(22.dp))
    }
}

@Composable
private fun PdfViewSheet(
    initial: PdfPageFit,
    onApply: (PdfPageFit) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedName by rememberSaveable(initial.name) { mutableStateOf(initial.name) }
    val selected = PdfPageFit.valueOf(selectedName)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPaddingCompat(),
    ) {
        SheetTitleRow(title = "PDF 版式", onDismiss = onDismiss)
        Text("页面显示", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                PdfPageFit.WIDTH to "适合宽度",
                PdfPageFit.PAGE to "显示整页",
            ).forEach { (fit, label) ->
                FilterChip(
                    selected = selected == fit,
                    onClick = { selectedName = fit.name },
                    label = { Text(label) },
                    modifier = Modifier.weight(1f),
                    leadingIcon = if (selected == fit) ({
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    }) else null,
                )
            }
        }
        Text(
            text = when (selected) {
                PdfPageFit.WIDTH -> "页面铺满可用宽度，适合连续向下阅读。"
                PdfPageFit.PAGE -> "整页保持在屏幕内，适合查看图文版式。"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 14.dp),
        )
        OutlinedButton(
            onClick = { onApply(selected) },
            modifier = Modifier.fillMaxWidth().height(50.dp).padding(top = 14.dp),
            shape = RoundedCornerShape(4.dp),
        ) {
            Icon(Icons.Default.AspectRatio, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("应用版式")
        }
        Spacer(modifier = Modifier.height(22.dp))
    }
}

@Composable
private fun TypographySheet(
    initial: TypographySettings,
    onSave: (TypographySettings) -> Unit,
    onDismiss: () -> Unit,
) {
    var fontSize by rememberSaveable(initial.fontSizeSp) { mutableFloatStateOf(initial.fontSizeSp) }
    var lineHeight by rememberSaveable(initial.lineHeightMultiplier) { mutableFloatStateOf(initial.lineHeightMultiplier) }
    var fontFamily by rememberSaveable(initial.fontFamily) { mutableStateOf(initial.fontFamily) }

    fun save() {
        onSave(
            TypographySettings(
                fontSizeSp = fontSize,
                lineHeightMultiplier = lineHeight,
                fontFamily = fontFamily,
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPaddingCompat(),
    ) {
        SheetTitleRow(title = "排版", onDismiss = onDismiss)
        Text("字体", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.padding(top = 10.dp, bottom = 17.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("宋体", "黑体", "自定义").forEachIndexed { index, label ->
                FilterChip(
                    selected = fontFamily == label,
                    onClick = { fontFamily = label },
                    label = { Text(label) },
                    leadingIcon = if (fontFamily == label) ({ Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }) else null,
                )
            }
        }
        SettingSliderRow(label = "字号", valueLabel = "${fontSize.roundToInt()}sp", value = fontSize, range = 14f..26f, onValueChange = { fontSize = it })
        SettingSliderRow(label = "行距", valueLabel = String.format(Locale.ROOT, "%.2f", lineHeight), value = lineHeight, range = 1.35f..2.2f, onValueChange = { lineHeight = it })
        Row(modifier = Modifier.fillMaxWidth().padding(top = 13.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text("左右翻页") }, leadingIcon = { Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(17.dp)) })
            AssistChip(onClick = {}, label = { Text("纸张主题") }, leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(17.dp)) })
        }
        OutlinedButton(
            onClick = ::save,
            modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = 12.dp),
            shape = RoundedCornerShape(4.dp),
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(7.dp))
            Text("应用排版")
        }
        Spacer(modifier = Modifier.height(22.dp))
    }
}

@Composable
private fun SettingSliderRow(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(48.dp))
        Slider(value = value, onValueChange = onValueChange, valueRange = range, modifier = Modifier.weight(1f))
        Text(valueLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.widthIn(min = 42.dp))
    }
}

@Composable
private fun TtsSheet(
    book: ShelfBook?,
    playing: Boolean,
    ready: Boolean,
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
) {
    val textAvailable = book != null && book.localBook.format != BookFormat.PDF
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPaddingCompat(),
    ) {
        SheetTitleRow(title = "听书", onDismiss = onDismiss)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 10.dp)) {
            Surface(color = MaterialTheme.colorScheme.onBackground, shape = RoundedCornerShape(4.dp), modifier = Modifier.size(50.dp)) {
                Icon(Icons.Default.Headphones, contentDescription = null, tint = MaterialTheme.colorScheme.background, modifier = Modifier.padding(13.dp))
            }
            Spacer(modifier = Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(book?.title ?: "尚未选择书籍", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(book?.chapter ?: "导入并打开书籍后可使用听书", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onToggle, enabled = ready && textAvailable, modifier = Modifier.size(52.dp)) {
                Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (playing) "暂停" else "播放", modifier = Modifier.size(27.dp))
            }
        }
        if (book == null) {
            Text("当前没有书籍，播放按钮保持关闭。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 7.dp))
        } else if (!textAvailable) {
            Text("PDF 是固定版式文件，当前版本不提取文字，暂不支持听书。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 7.dp))
        } else if (!ready) {
            Text("系统语音服务尚未就绪", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 7.dp))
        }
        SettingSliderRow(label = "语速", valueLabel = String.format(Locale.ROOT, "%.1f×", speed), value = speed, range = 0.6f..1.8f, onValueChange = onSpeedChange)
        Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text("系统语音") }, leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(17.dp)) })
            AssistChip(onClick = {}, label = { Text("定时停止") }, leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(17.dp)) })
        }
        Spacer(modifier = Modifier.height(22.dp))
    }
}

@Composable
private fun SyncSheet(
    initial: SyncSettings,
    onDismiss: () -> Unit,
    onSaved: (SyncSettings) -> Unit,
) {
    var url by rememberSaveable(initial.url) { mutableStateOf(initial.url) }
    var username by rememberSaveable(initial.username) { mutableStateOf(initial.username) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPaddingCompat(),
    ) {
        SheetTitleRow(title = "多设备同步", onDismiss = onDismiss)
        Text("本地优先", style = MaterialTheme.typography.titleMedium)
        Text("不连接同步服务也能完整阅读。密码将由 Android Keystore 在本机保护。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp, bottom = 16.dp))
        OutlinedTextField(value = url, onValueChange = { url = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("WebDAV 地址") }, leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = null) })
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(value = username, onValueChange = { username = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("用户名") }, leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) })
        Spacer(modifier = Modifier.height(17.dp))
        OutlinedButton(onClick = { onSaved(SyncSettings(url = url, username = username)) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(4.dp)) {
            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("保存并测试连接")
        }
        Spacer(modifier = Modifier.height(22.dp))
    }
}

@Composable
private fun SheetTitleRow(title: String, onDismiss: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 11.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        QuietIconButton(Icons.Default.Close, "关闭", onDismiss)
    }
}

private fun Float.percentLabel(): String = "${(this * 100).roundToInt()}%"

@Composable
private fun Modifier.navigationBarsPaddingCompat(): Modifier = this.padding(bottom = 8.dp)
