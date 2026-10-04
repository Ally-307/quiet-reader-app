package com.quietreader.data

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

enum class BookFormat(val extension: String) {
    EPUB("epub"),
    TXT("txt"),
    PDF("pdf"),
}

data class ImportedBook(
    val id: String,
    val title: String,
    val author: String,
    val chapter: String,
    val format: BookFormat,
    val filePath: String,
    val fileName: String,
    val progress: Float = 0f,
)

data class ParsedBookText(
    val chapter: String,
    val paragraphs: List<String>,
)

data class BookImportResult(
    val book: ImportedBook,
    val readable: Boolean,
)

data class ImportedSource(
    val id: String,
    val name: String,
    val url: String,
    val ruleCount: Int,
    val enabled: Boolean,
    val rawPath: String,
)

data class SourceImportResult(
    val added: List<ImportedSource>,
    val skipped: Int,
)

data class TypographySettings(
    val fontSizeSp: Float = 18f,
    val lineHeightMultiplier: Float = 1.75f,
    val fontFamily: String = "宋体",
)

data class SyncSettings(
    val url: String = "",
    val username: String = "",
)

enum class BackupConflictType {
    BOOK,
    SOURCE,
    SETTINGS,
}

data class BackupConflict(
    val type: BackupConflictType,
    val backupLabel: String,
    val localLabel: String,
)

data class BackupPreview(
    val stagedPath: String,
    val fileName: String,
    val version: Int,
    val bookCount: Int,
    val sourceCount: Int,
    val conflicts: List<BackupConflict>,
)

enum class BackupConflictPolicy {
    KEEP_LOCAL,
    USE_BACKUP,
}

data class BackupRestoreResult(
    val addedBooks: Int,
    val replacedBooks: Int,
    val keptBooks: Int,
    val addedSources: Int,
    val replacedSources: Int,
    val keptSources: Int,
    val settingsRestored: Boolean,
)

/**
 * Small local-first persistence layer for the first native milestone.
 * Metadata stays in SharedPreferences; imported files and raw source JSON stay in app-private files.
 */
class ReaderRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val booksDirectory = File(appContext.filesDir, "books").apply { mkdirs() }
    private val sourcesDirectory = File(appContext.filesDir, "sources").apply { mkdirs() }
    private val backupDirectory = File(appContext.filesDir, "backup-staging").apply { mkdirs() }

    private data class BackupBookRecord(
        val id: String,
        val title: String,
        val author: String,
        val chapter: String,
        val format: BookFormat,
        val fileName: String,
        val progress: Float,
        val entry: String,
        val size: Long,
        val sha256: String,
    )

    private data class BackupSourceRecord(
        val id: String,
        val name: String,
        val url: String,
        val ruleCount: Int,
        val enabled: Boolean,
        val entry: String,
        val size: Long,
        val sha256: String,
    )

    private data class BackupBundle(
        val version: Int,
        val books: List<BackupBookRecord>,
        val sources: List<BackupSourceRecord>,
        val typography: TypographySettings,
        val sync: SyncSettings,
    )

    private data class FileMutation(
        val destination: File,
        val previousCopy: File?,
    )

    private data class PreferenceSnapshot(
        val books: String?,
        val sources: String?,
        val typography: TypographySettings,
        val sync: SyncSettings,
    )

    fun loadBooks(): List<ImportedBook> {
        val stored = preferences.getString(KEY_BOOKS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(stored)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val path = item.optString("filePath")
                    val format = runCatching { BookFormat.valueOf(item.optString("format")) }.getOrNull() ?: continue
                    if (path.isBlank() || !File(path).exists()) continue
                    add(
                        ImportedBook(
                            id = item.optString("id"),
                            title = item.optString("title", "未命名书籍"),
                            author = item.optString("author", "本地导入"),
                            chapter = item.optString("chapter", "正文"),
                            format = format,
                            filePath = path,
                            fileName = item.optString("fileName", File(path).name),
                            progress = item.optDouble("progress", 0.0).toFloat().coerceIn(0f, 1f),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun loadSources(): List<ImportedSource> {
        val stored = preferences.getString(KEY_SOURCES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(stored)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id")
                    val name = item.optString("name")
                    val url = item.optString("url")
                    if (id.isBlank() || name.isBlank() || url.isBlank()) continue
                    add(
                        ImportedSource(
                            id = id,
                            name = name,
                            url = url,
                            ruleCount = item.optInt("ruleCount", 0),
                            enabled = item.optBoolean("enabled", false),
                            rawPath = item.optString("rawPath"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun loadTypography(): TypographySettings {
        return TypographySettings(
            fontSizeSp = preferences.getFloat(KEY_FONT_SIZE, 18f).coerceIn(14f, 26f),
            lineHeightMultiplier = preferences.getFloat(KEY_LINE_HEIGHT, 1.75f).coerceIn(1.35f, 2.2f),
            fontFamily = preferences.getString(KEY_FONT_FAMILY, "宋体") ?: "宋体",
        )
    }

    fun saveTypography(settings: TypographySettings) {
        preferences.edit()
            .putFloat(KEY_FONT_SIZE, settings.fontSizeSp.coerceIn(14f, 26f))
            .putFloat(KEY_LINE_HEIGHT, settings.lineHeightMultiplier.coerceIn(1.35f, 2.2f))
            .putString(KEY_FONT_FAMILY, settings.fontFamily)
            .apply()
    }

    fun loadSyncSettings(): SyncSettings {
        return SyncSettings(
            url = preferences.getString(KEY_SYNC_URL, "") ?: "",
            username = preferences.getString(KEY_SYNC_USERNAME, "") ?: "",
        )
    }

    fun saveSyncSettings(settings: SyncSettings) {
        preferences.edit()
            .putString(KEY_SYNC_URL, settings.url.trim())
            .putString(KEY_SYNC_USERNAME, settings.username.trim())
            .apply()
    }

    fun setSourceEnabled(sourceId: String, enabled: Boolean) {
        val updated = loadSources().map { source ->
            if (source.id == sourceId) source.copy(enabled = enabled) else source
        }
        saveSources(updated)
    }

    suspend fun importBook(uri: Uri, format: BookFormat): BookImportResult = withContext(Dispatchers.IO) {
        val originalName = displayName(uri)
        val id = "book_${UUID.randomUUID()}"
        val destination = File(booksDirectory, "$id.${format.extension}")
        copyUri(uri, destination)

        val preview = when (format) {
            BookFormat.TXT -> parseText(decodeText(readBytes(destination, PREVIEW_BYTES)))
            BookFormat.EPUB -> readEpubText(destination, PREVIEW_BYTES)
            BookFormat.PDF -> ParsedBookText(chapter = format.chapterLabel, paragraphs = emptyList())
        }
        val title = if (format == BookFormat.EPUB) {
            readEpubTitle(destination) ?: cleanTitle(originalName, format)
        } else {
            cleanTitle(originalName, format)
        }
        val author = if (format == BookFormat.TXT) extractAuthor(readTextPreview(destination)) else "本地导入"
        val book = ImportedBook(
            id = id,
            title = title,
            author = author,
            chapter = preview.chapter,
            format = format,
            filePath = destination.absolutePath,
            fileName = originalName,
        )
        saveBooks(listOf(book) + loadBooks())
        BookImportResult(book = book, readable = preview.paragraphs.isNotEmpty())
    }

    suspend fun importSource(uri: Uri): SourceImportResult = withContext(Dispatchers.IO) {
        importSourceText(readUriText(uri))
    }

    suspend fun importSourceText(raw: String): SourceImportResult = withContext(Dispatchers.IO) {
        val objects = parseSourceObjects(raw)
        if (objects.isEmpty()) throw IllegalArgumentException("没有找到可导入的书源对象")

        val existing = loadSources().toMutableList()
        val added = mutableListOf<ImportedSource>()
        var skipped = 0
        objects.forEach { source ->
            val name = firstNonBlank(source, NAME_KEYS)
            val url = firstNonBlank(source, URL_KEYS)
            if (name.isBlank() || url.isBlank()) {
                skipped += 1
                return@forEach
            }
            val id = "source_${UUID.randomUUID()}"
            val rawFile = File(sourcesDirectory, "$id.json")
            rawFile.writeText(source.toString(), Charsets.UTF_8)
            val record = ImportedSource(
                id = id,
                name = name,
                url = url,
                ruleCount = countRules(source),
                enabled = false,
                rawPath = rawFile.absolutePath,
            )
            added += record
        }
        saveSources((added + existing).distinctBy { it.url })
        SourceImportResult(added = added, skipped = skipped)
    }

    suspend fun readBookText(book: ImportedBook): ParsedBookText? = withContext(Dispatchers.IO) {
        val file = File(book.filePath)
        if (!file.exists()) return@withContext null
        when (book.format) {
            BookFormat.TXT -> parseText(decodeText(readBytes(file, MAX_READER_BYTES)))
            BookFormat.EPUB -> readEpubText(file, MAX_READER_BYTES)
            BookFormat.PDF -> null
        }
    }

    suspend fun exportBackup(output: OutputStream): Int = withContext(Dispatchers.IO) {
        val books = loadBooks().filter { File(it.filePath).isFile }
        val sources = loadSources().filter { File(it.rawPath).isFile }
        validateExportFiles(books, sources)
        val bookEntries = books.mapIndexed { index, book ->
            book to "books/${index.toString().padStart(5, '0')}.${book.format.extension}"
        }
        val sourceEntries = sources.mapIndexed { index, source ->
            source to "sources/${index.toString().padStart(5, '0')}.json"
        }
        ZipOutputStream(output.buffered()).use { archive ->
            val manifest = JSONObject()
                .put("format", BACKUP_FORMAT)
                .put("version", BACKUP_VERSION)
                .put("createdAt", System.currentTimeMillis())
                .put("books", JSONArray().apply {
                    bookEntries.forEach { (book, entry) -> put(book.toBackupJson(entry)) }
                })
                .put("sources", JSONArray().apply {
                    sourceEntries.forEach { (source, entry) -> put(source.toBackupJson(entry)) }
                })
                .put("typography", JSONObject()
                    .put("fontSizeSp", loadTypography().fontSizeSp)
                    .put("lineHeightMultiplier", loadTypography().lineHeightMultiplier)
                    .put("fontFamily", loadTypography().fontFamily))
                .put("sync", JSONObject()
                    .put("url", loadSyncSettings().url)
                    .put("username", loadSyncSettings().username))
            writeZipEntry(archive, "manifest.json", manifest.toString(2).toByteArray(Charsets.UTF_8))

            bookEntries.forEach { (book, entry) ->
                val file = File(book.filePath)
                writeZipFile(archive, entry, file)
            }
            sourceEntries.forEach { (source, entry) ->
                val file = File(source.rawPath)
                writeZipFile(archive, entry, file)
            }
        }
        books.size + sources.size
    }

    suspend fun prepareBackup(uri: Uri): BackupPreview = withContext(Dispatchers.IO) {
        cleanupStagedBackups()
        val staged = File(backupDirectory, "pending-${UUID.randomUUID()}.zip")
        try {
            copyUriLimited(uri, staged, MAX_BACKUP_BYTES)
            val bundle = readBackup(staged)
            BackupPreview(
                stagedPath = staged.absolutePath,
                fileName = displayName(uri),
                version = bundle.version,
                bookCount = bundle.books.size,
                sourceCount = bundle.sources.size,
                conflicts = detectConflicts(bundle),
            )
        } catch (error: Exception) {
            staged.delete()
            throw error
        }
    }

    fun discardBackup(preview: BackupPreview) {
        val staged = File(preview.stagedPath)
        if (isInside(staged, backupDirectory) && staged.isFile) staged.delete()
    }

    suspend fun restoreBackup(
        preview: BackupPreview,
        policy: BackupConflictPolicy,
    ): BackupRestoreResult = withContext(Dispatchers.IO) {
        val staged = File(preview.stagedPath)
        if (!isInside(staged, backupDirectory) || !staged.isFile) {
            throw IllegalArgumentException("备份暂存文件已失效，请重新选择")
        }

        val restoreDirectory = File(backupDirectory, "restore-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val bundle = readBackup(staged)
            val extractedBooks = bundle.books.mapIndexed { index, book ->
                val file = File(restoreDirectory, "book-${index.toString().padStart(5, '0')}.${book.format.extension}")
                extractZipEntry(staged, book.entry, file, book.size)
                verifyExtractedFile(file, book.size, book.sha256)
                book to file
            }
            val extractedSources = bundle.sources.mapIndexed { index, source ->
                val file = File(restoreDirectory, "source-${index.toString().padStart(5, '0')}.json")
                extractZipEntry(staged, source.entry, file, source.size)
                verifyExtractedFile(file, source.size, source.sha256)
                source to file
            }
            mergeBackup(bundle, extractedBooks, extractedSources, policy)
        } finally {
            restoreDirectory.deleteRecursively()
            staged.delete()
        }
    }

    private fun ImportedBook.toBackupJson(entry: String): JSONObject = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("author", author)
        .put("chapter", chapter)
        .put("format", format.name)
        .put("fileName", fileName)
        .put("progress", progress)
        .put("entry", entry)
        .put("size", File(filePath).length())
        .put("sha256", sha256(File(filePath)))

    private fun ImportedSource.toBackupJson(entry: String): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("url", url)
        .put("ruleCount", ruleCount)
        .put("enabled", enabled)
        .put("entry", entry)
        .put("size", File(rawPath).length())
        .put("sha256", sha256(File(rawPath)))

    private fun readBackup(file: File): BackupBundle {
        if (!file.isFile || file.length() > MAX_BACKUP_BYTES) {
            throw IllegalArgumentException("备份文件过大或不可读取")
        }
        ZipFile(file).use { archive ->
            val entries = archive.entries().toList()
            if (entries.size > MAX_BACKUP_ENTRIES) throw IllegalArgumentException("备份包含过多文件")
            validateArchiveSizes(entries)
            val names = mutableSetOf<String>()
            entries.forEach { entry ->
                if (!names.add(entry.name) || !isSafeBackupEntry(entry)) {
                    throw IllegalArgumentException("备份包含不安全的文件路径")
                }
            }
            val manifestEntry = archive.getEntry(MANIFEST_NAME)
                ?: throw IllegalArgumentException("备份缺少 manifest.json")
            val manifestText = archive.getInputStream(manifestEntry).use { input ->
                String(readAllBytesLimited(input, MAX_MANIFEST_BYTES), Charsets.UTF_8)
            }
            val manifest = runCatching { JSONObject(manifestText.removePrefix("\uFEFF")) }
                .getOrElse { throw IllegalArgumentException("manifest.json 格式不可用") }
            if (manifest.optString("format") != BACKUP_FORMAT) {
                throw IllegalArgumentException("不是静读备份文件")
            }
            val version = manifest.optInt("version", 0)
            if (version !in 1..BACKUP_VERSION) {
                throw IllegalArgumentException("暂不支持备份版本 $version")
            }

            val books = if (version >= 2 && manifest.optJSONArray("books") != null) {
                parseBookRecords(manifest.optJSONArray("books")!!, archive)
            } else {
                parseLegacyBookRecords(manifest.optInt("books", 0), entries, archive)
            }
            val sources = if (version >= 2 && manifest.optJSONArray("sources") != null) {
                parseSourceRecords(manifest.optJSONArray("sources")!!, archive)
            } else {
                parseLegacySourceRecords(manifest.optInt("sources", 0), entries, archive)
            }
            val referenced = (books.map { it.entry } + sources.map { it.entry }).toSet()
            if (MANIFEST_NAME in referenced || referenced.any { archive.getEntry(it) == null }) {
                throw IllegalArgumentException("备份清单与文件内容不一致")
            }
            val dataEntries = entries.filter { !it.isDirectory && it.name != MANIFEST_NAME }.map { it.name }.toSet()
            if (dataEntries != referenced) throw IllegalArgumentException("备份包含未登记的文件")

            return BackupBundle(
                version = version,
                books = books,
                sources = sources,
                typography = parseTypography(manifest.optJSONObject("typography")),
                sync = parseSyncSettings(manifest.optJSONObject("sync")),
            )
        }
    }

    private fun parseBookRecords(array: JSONArray, archive: ZipFile): List<BackupBookRecord> {
        if (array.length() > MAX_BACKUP_ITEMS) throw IllegalArgumentException("备份书籍数量过多")
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: throw IllegalArgumentException("书籍记录不可用")
                val id = requireRecordId(item.optString("id"), "书籍")
                val format = parseBookFormat(item.optString("format"))
                val entry = requireDataEntry(item.optString("entry"), "books", format.extension)
                val size = requireExpectedSize(item, archive, entry, MAX_BACKUP_ITEM_BYTES)
                val sha256 = requireExpectedHash(item, archive, entry, MAX_BACKUP_ITEM_BYTES)
                add(
                    BackupBookRecord(
                        id = id,
                        title = item.optString("title", "未命名书籍").trim().ifBlank { "未命名书籍" },
                        author = item.optString("author", "本地导入").trim().ifBlank { "本地导入" },
                        chapter = item.optString("chapter", "正文").trim().ifBlank { "正文" },
                        format = format,
                        fileName = item.optString("fileName", entry.substringAfterLast('/')).trim().ifBlank { entry.substringAfterLast('/') },
                        progress = item.optDouble("progress", 0.0).toFloat().coerceIn(0f, 1f),
                        entry = entry,
                        size = size,
                        sha256 = sha256,
                    ),
                )
            }
        }.also { records ->
            if (records.map { it.id }.toSet().size != records.size || records.map { it.entry }.toSet().size != records.size) {
                throw IllegalArgumentException("备份中存在重复书籍记录")
            }
        }
    }

    private fun parseSourceRecords(array: JSONArray, archive: ZipFile): List<BackupSourceRecord> {
        if (array.length() > MAX_BACKUP_ITEMS) throw IllegalArgumentException("备份书源数量过多")
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: throw IllegalArgumentException("书源记录不可用")
                val id = requireRecordId(item.optString("id"), "书源")
                val entry = requireDataEntry(item.optString("entry"), "sources", "json")
                val size = requireExpectedSize(item, archive, entry, MAX_SOURCE_BYTES.toLong())
                val sha256 = requireExpectedHash(item, archive, entry, MAX_SOURCE_BYTES.toLong())
                val url = item.optString("url").trim()
                if (url.isBlank()) throw IllegalArgumentException("书源缺少地址")
                validateSourceEntry(archive, entry, url)
                add(
                    BackupSourceRecord(
                        id = id,
                        name = item.optString("name", "未命名书源").trim().ifBlank { "未命名书源" },
                        url = url,
                        ruleCount = item.optInt("ruleCount", 0).coerceAtLeast(0),
                        enabled = item.optBoolean("enabled", false),
                        entry = entry,
                        size = size,
                        sha256 = sha256,
                    ),
                )
            }
        }.also { records ->
            if (records.map { it.id }.toSet().size != records.size ||
                records.map { normalizeUrl(it.url) }.toSet().size != records.size ||
                records.map { it.entry }.toSet().size != records.size
            ) {
                throw IllegalArgumentException("备份中存在重复书源记录")
            }
        }
    }

    private fun parseLegacyBookRecords(expectedCount: Int, entries: List<ZipEntry>, archive: ZipFile): List<BackupBookRecord> {
        val bookEntries = entries.filter { it.name.startsWith("books/") && !it.isDirectory }.sortedBy { it.name }
        if (bookEntries.size != expectedCount) throw IllegalArgumentException("旧版备份缺少书籍文件")
        return bookEntries.map { entry ->
            val fileName = entry.name.substringAfterLast('/')
            val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
            val format = parseBookFormat(extension)
            val id = requireRecordId(fileName.removeSuffix(".$extension"), "书籍")
            val hash = hashArchiveEntry(archive, entry.name, MAX_BACKUP_ITEM_BYTES)
            BackupBookRecord(id, id, "本地导入", format.chapterLabel, format, fileName, 0f, entry.name, entry.size, hash)
        }
    }

    private fun parseLegacySourceRecords(expectedCount: Int, entries: List<ZipEntry>, archive: ZipFile): List<BackupSourceRecord> {
        val sourceEntries = entries.filter { it.name.startsWith("sources/") && !it.isDirectory }.sortedBy { it.name }
        if (sourceEntries.size != expectedCount) throw IllegalArgumentException("旧版备份缺少书源文件")
        return sourceEntries.map { entry ->
            val fileName = entry.name.substringAfterLast('/')
            val id = requireRecordId(fileName.removeSuffix(".json"), "书源")
            val hash = hashArchiveEntry(archive, entry.name, MAX_SOURCE_BYTES.toLong())
            val source = archive.getInputStream(entry).use { input ->
                val raw = String(readAllBytesLimited(input, MAX_SOURCE_BYTES.toLong()), Charsets.UTF_8)
                parseSourceObjects(raw).firstOrNull()
            } ?: throw IllegalArgumentException("旧版备份中的书源不可用")
            val name = firstNonBlank(source, NAME_KEYS)
            val url = firstNonBlank(source, URL_KEYS)
            if (name.isBlank() || url.isBlank()) throw IllegalArgumentException("旧版备份中的书源缺少名称或地址")
            BackupSourceRecord(id, name, url, countRules(source), false, entry.name, entry.size, hash)
        }
    }

    private fun parseTypography(value: JSONObject?): TypographySettings = TypographySettings(
        fontSizeSp = (value?.optDouble("fontSizeSp", 18.0)?.toFloat() ?: 18f).coerceIn(14f, 26f),
        lineHeightMultiplier = (value?.optDouble("lineHeightMultiplier", 1.75)?.toFloat() ?: 1.75f).coerceIn(1.35f, 2.2f),
        fontFamily = value?.optString("fontFamily", "宋体")?.trim()?.ifBlank { "宋体" } ?: "宋体",
    )

    private fun parseSyncSettings(value: JSONObject?): SyncSettings = SyncSettings(
        url = value?.optString("url", "")?.trim() ?: "",
        username = value?.optString("username", "")?.trim() ?: "",
    )

    private fun detectConflicts(bundle: BackupBundle): List<BackupConflict> {
        val localBooks = loadBooks()
        val localSources = loadSources()
        val conflicts = buildList {
            bundle.books.forEach { backup ->
                localBooks.firstOrNull { it.id == backup.id }?.let { local ->
                    add(BackupConflict(BackupConflictType.BOOK, backup.title, local.title))
                }
            }
            bundle.sources.forEach { backup ->
                localSources.firstOrNull {
                    it.id == backup.id || normalizeUrl(it.url) == normalizeUrl(backup.url)
                }?.let { local ->
                    add(BackupConflict(BackupConflictType.SOURCE, backup.name, local.name))
                }
            }
            if (loadTypography() != bundle.typography || loadSyncSettings() != bundle.sync) {
                add(BackupConflict(BackupConflictType.SETTINGS, "备份中的阅读设置", "当前设备设置"))
            }
        }
        return conflicts
    }

    private fun mergeBackup(
        bundle: BackupBundle,
        books: List<Pair<BackupBookRecord, File>>,
        sources: List<Pair<BackupSourceRecord, File>>,
        policy: BackupConflictPolicy,
    ): BackupRestoreResult {
        val snapshot = PreferenceSnapshot(
            books = preferences.getString(KEY_BOOKS, null),
            sources = preferences.getString(KEY_SOURCES, null),
            typography = loadTypography(),
            sync = loadSyncSettings(),
        )
        val localBooks = loadBooks().toMutableList()
        val localSources = loadSources().toMutableList()
        val fileMutations = mutableListOf<FileMutation>()
        val filesToDeleteAfterCommit = linkedSetOf<File>()
        var addedBooks = 0
        var replacedBooks = 0
        var keptBooks = 0
        var addedSources = 0
        var replacedSources = 0
        var keptSources = 0
        val rollbackDirectory = File(backupDirectory, "rollback-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            books.forEach { (backup, stagedFile) ->
                val conflict = localBooks.firstOrNull { it.id == backup.id }
                if (conflict != null && policy == BackupConflictPolicy.KEEP_LOCAL) {
                    keptBooks += 1
                    return@forEach
                }
                val destination = File(booksDirectory, "${backup.id}.${backup.format.extension}")
                installStagedFile(stagedFile, destination, rollbackDirectory, fileMutations)
                if (conflict != null) {
                    localBooks.removeAll { it.id == backup.id }
                    managedFile(conflict.filePath, booksDirectory, destination)?.let(filesToDeleteAfterCommit::add)
                    replacedBooks += 1
                } else {
                    addedBooks += 1
                }
                localBooks.add(
                    ImportedBook(
                        id = backup.id,
                        title = backup.title,
                        author = backup.author,
                        chapter = backup.chapter,
                        format = backup.format,
                        filePath = destination.absolutePath,
                        fileName = backup.fileName,
                        progress = backup.progress,
                    ),
                )
            }

            sources.forEach { (backup, stagedFile) ->
                val conflicts = localSources.filter {
                    it.id == backup.id || normalizeUrl(it.url) == normalizeUrl(backup.url)
                }
                if (conflicts.isNotEmpty() && policy == BackupConflictPolicy.KEEP_LOCAL) {
                    keptSources += 1
                    return@forEach
                }
                val destination = File(sourcesDirectory, "${backup.id}.json")
                installStagedFile(stagedFile, destination, rollbackDirectory, fileMutations)
                if (conflicts.isNotEmpty()) {
                    localSources.removeAll {
                        it.id == backup.id || normalizeUrl(it.url) == normalizeUrl(backup.url)
                    }
                    conflicts.mapNotNull { conflict ->
                        managedFile(conflict.rawPath, sourcesDirectory, destination)
                    }.forEach(filesToDeleteAfterCommit::add)
                    replacedSources += 1
                } else {
                    addedSources += 1
                }
                localSources.add(
                    ImportedSource(
                        id = backup.id,
                        name = backup.name,
                        url = backup.url,
                        ruleCount = backup.ruleCount,
                        enabled = backup.enabled,
                        rawPath = destination.absolutePath,
                    ),
                )
            }

            val settingsRestored = policy == BackupConflictPolicy.USE_BACKUP
            persistRestoredState(
                books = localBooks,
                sources = localSources,
                typography = if (settingsRestored) bundle.typography else snapshot.typography,
                sync = if (settingsRestored) bundle.sync else snapshot.sync,
            )
            val retainedPaths = (localBooks.map { File(it.filePath) } + localSources.map { File(it.rawPath) })
                .mapNotNull { runCatching { it.canonicalPath }.getOrNull() }
                .toSet()
            filesToDeleteAfterCommit.filterNot { file ->
                runCatching { file.canonicalPath in retainedPaths }.getOrDefault(true)
            }.forEach { file ->
                if (file.isFile) file.delete()
            }
            return BackupRestoreResult(
                addedBooks = addedBooks,
                replacedBooks = replacedBooks,
                keptBooks = keptBooks,
                addedSources = addedSources,
                replacedSources = replacedSources,
                keptSources = keptSources,
                settingsRestored = settingsRestored,
            )
        } catch (error: Exception) {
            runCatching { rollbackFileMutations(fileMutations) }.exceptionOrNull()?.let(error::addSuppressed)
            runCatching { restorePreferences(snapshot) }.exceptionOrNull()?.let(error::addSuppressed)
            throw error
        } finally {
            rollbackDirectory.deleteRecursively()
        }
    }

    private fun extractZipEntry(zipFile: File, entryName: String, destination: File, maxBytes: Long) {
        ZipFile(zipFile).use { archive ->
            val entry = archive.getEntry(entryName) ?: throw IllegalArgumentException("备份文件缺少 $entryName")
            if (entry.isDirectory) throw IllegalArgumentException("备份文件项不可用")
            if (entry.size > maxBytes) throw IllegalArgumentException("备份中的文件过大")
            archive.getInputStream(entry).use { input ->
                FileOutputStream(destination).use { output -> copyLimited(input, output, maxBytes) }
            }
        }
    }

    private fun copyUriLimited(uri: Uri, destination: File, maxBytes: Long) {
        val input = appContext.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("无法读取所选备份")
        input.use { stream ->
            FileOutputStream(destination).use { output -> copyLimited(stream, output, maxBytes) }
        }
    }

    private fun copyLimited(input: InputStream, output: OutputStream, maxBytes: Long) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            total += read
            if (total > maxBytes) throw IllegalArgumentException("备份文件过大")
            output.write(buffer, 0, read)
        }
    }

    private fun readAllBytesLimited(input: InputStream, maxBytes: Long): ByteArray {
        val output = ByteArrayOutputStream()
        copyLimited(input, output, maxBytes)
        return output.toByteArray()
    }

    private fun validateArchiveSizes(entries: List<ZipEntry>) {
        var total = 0L
        entries.filterNot { it.isDirectory }.forEach { entry ->
            val size = entry.size
            if (size < 0L) throw IllegalArgumentException("备份文件大小不可用")
            val itemLimit = when {
                entry.name == MANIFEST_NAME -> MAX_MANIFEST_BYTES
                entry.name.startsWith("books/") -> MAX_BACKUP_ITEM_BYTES
                entry.name.startsWith("sources/") -> MAX_SOURCE_BYTES.toLong()
                else -> MAX_BACKUP_ITEM_BYTES
            }
            if (size > itemLimit) throw IllegalArgumentException("备份中的文件过大")
            if (total > MAX_BACKUP_UNCOMPRESSED_BYTES - size) {
                throw IllegalArgumentException("备份解压后的数据量过大")
            }
            total += size
        }
    }

    private fun validateExportFiles(books: List<ImportedBook>, sources: List<ImportedSource>) {
        var total = 0L
        val files = books.map { File(it.filePath) to MAX_BACKUP_ITEM_BYTES } +
            sources.map { File(it.rawPath) to MAX_SOURCE_BYTES.toLong() }
        files.forEach { (file, limit) ->
            val size = file.length()
            if (size < 0L || size > limit) throw IllegalArgumentException("待备份文件过大：${file.name}")
            if (total > MAX_BACKUP_UNCOMPRESSED_BYTES - size) {
                throw IllegalArgumentException("待备份数据总量过大")
            }
            total += size
        }
    }

    private fun requireExpectedSize(
        item: JSONObject,
        archive: ZipFile,
        entryName: String,
        maxBytes: Long,
    ): Long {
        val entry = requireArchiveFile(archive, entryName)
        val expected = item.optLong("size", -1L)
        if (expected < 0L || expected > maxBytes || entry.size != expected) {
            throw IllegalArgumentException("备份文件大小与清单不一致")
        }
        return expected
    }

    private fun requireExpectedHash(
        item: JSONObject,
        archive: ZipFile,
        entryName: String,
        maxBytes: Long,
    ): String {
        val expected = item.optString("sha256").trim().lowercase(Locale.ROOT)
        if (!expected.matches(Regex("[0-9a-f]{64}"))) {
            throw IllegalArgumentException("备份缺少有效的内容校验值")
        }
        val actual = hashArchiveEntry(archive, entryName, maxBytes)
        if (actual != expected) throw IllegalArgumentException("备份内容校验失败")
        return expected
    }

    private fun validateSourceEntry(archive: ZipFile, entryName: String, expectedUrl: String) {
        val entry = requireArchiveFile(archive, entryName)
        val source = archive.getInputStream(entry).use { input ->
            val raw = String(readAllBytesLimited(input, MAX_SOURCE_BYTES.toLong()), Charsets.UTF_8)
            parseSourceObjects(raw).firstOrNull()
        } ?: throw IllegalArgumentException("备份中的书源规则不可用")
        val url = firstNonBlank(source, URL_KEYS)
        if (url.isBlank() || normalizeUrl(url) != normalizeUrl(expectedUrl)) {
            throw IllegalArgumentException("书源规则与备份清单不一致")
        }
    }

    private fun hashArchiveEntry(archive: ZipFile, entryName: String, maxBytes: Long): String {
        val entry = requireArchiveFile(archive, entryName)
        return archive.getInputStream(entry).use { input -> digest(input, maxBytes).second }
    }

    private fun sha256(file: File): String = file.inputStream().use { input ->
        digest(input, MAX_BACKUP_ITEM_BYTES).second
    }

    private fun digest(input: InputStream, maxBytes: Long): Pair<Long, String> {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            total += read
            if (total > maxBytes) throw IllegalArgumentException("备份中的文件过大")
            digest.update(buffer, 0, read)
        }
        return total to digest.digest().joinToString("") { byte ->
            String.format(Locale.ROOT, "%02x", byte.toInt() and 0xff)
        }
    }

    private fun verifyExtractedFile(file: File, expectedSize: Long, expectedHash: String) {
        if (file.length() != expectedSize || sha256(file) != expectedHash) {
            throw IllegalArgumentException("备份文件提取后校验失败")
        }
    }

    private fun requireArchiveFile(archive: ZipFile, entryName: String): ZipEntry {
        val entry = archive.getEntry(entryName)
        if (entry == null || entry.isDirectory) throw IllegalArgumentException("备份文件项不可用")
        return entry
    }

    private fun requireDataEntry(value: String, folder: String, extension: String): String {
        val entry = value.trim()
        val fileName = entry.substringAfterLast('/')
        if (!entry.startsWith("$folder/") || entry.count { it == '/' } != 1 ||
            fileName.isBlank() || fileName.contains('\\') || fileName.contains("..") ||
            !fileName.endsWith(".$extension", ignoreCase = true) ||
            !fileName.all { it.isLetterOrDigit() || it == '.' || it == '_' || it == '-' }
        ) {
            throw IllegalArgumentException("备份文件项不可用")
        }
        return entry
    }

    private fun requireRecordId(value: String, label: String): String {
        val id = value.trim()
        if (id.isBlank() || id.length > 120 || !id.all { it.isLetterOrDigit() || it == '_' || it == '-' }) {
            throw IllegalArgumentException("${label}标识不可用")
        }
        return id
    }

    private fun parseBookFormat(value: String): BookFormat = runCatching {
        BookFormat.valueOf(value.trim().uppercase(Locale.ROOT))
    }.getOrElse { throw IllegalArgumentException("书籍格式不可用") }

    private fun isSafeBackupEntry(entry: ZipEntry): Boolean {
        if (entry.name == MANIFEST_NAME) return !entry.isDirectory
        if (entry.isDirectory || entry.name.startsWith('/') || entry.name.contains('\\')) return false
        val parts = entry.name.split('/')
        return parts.size == 2 && parts[0] in setOf("books", "sources") &&
            parts[1].isNotBlank() && !parts[1].contains("..") &&
            parts[1].all { it.isLetterOrDigit() || it == '.' || it == '_' || it == '-' }
    }

    private fun normalizeUrl(url: String): String = url.trim().removeSuffix("/")

    private fun managedFile(path: String, directory: File, replacement: File): File? {
        val file = File(path)
        return file.takeIf {
            it.absolutePath != replacement.absolutePath && isInside(it, directory) && it.isFile
        }
    }

    private fun installStagedFile(
        staged: File,
        destination: File,
        rollbackDirectory: File,
        mutations: MutableList<FileMutation>,
    ) {
        val previousCopy = if (destination.isFile) {
            File(rollbackDirectory, "${UUID.randomUUID()}.bak").also { backup ->
                destination.copyTo(backup, overwrite = false)
            }
        } else {
            null
        }
        val temporary = File(destination.parentFile, ".${destination.name}.${UUID.randomUUID()}.restore")
        try {
            staged.copyTo(temporary, overwrite = false)
            try {
                Files.move(
                    temporary.toPath(),
                    destination.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            mutations.add(FileMutation(destination, previousCopy))
        } finally {
            temporary.delete()
        }
    }

    private fun rollbackFileMutations(mutations: List<FileMutation>) {
        mutations.asReversed().forEach { mutation ->
            if (mutation.previousCopy?.isFile == true) {
                mutation.previousCopy.copyTo(mutation.destination, overwrite = true)
            } else {
                mutation.destination.delete()
            }
        }
    }

    private fun isInside(file: File, directory: File): Boolean {
        val filePath = runCatching { file.canonicalFile.absolutePath }.getOrNull() ?: return false
        val directoryPath = runCatching { directory.canonicalFile.absolutePath }.getOrNull() ?: return false
        return filePath == directoryPath || filePath.startsWith(directoryPath + File.separator)
    }

    private fun cleanupStagedBackups() {
        backupDirectory.listFiles()?.forEach { file ->
            if (file.name.startsWith("pending-") || file.name.startsWith("restore-") || file.name.startsWith("rollback-")) {
                file.deleteRecursively()
            }
        }
    }

    private fun saveBooks(books: List<ImportedBook>) {
        preferences.edit().putString(KEY_BOOKS, serializeBooks(books)).apply()
    }

    private fun saveSources(sources: List<ImportedSource>) {
        preferences.edit().putString(KEY_SOURCES, serializeSources(sources)).apply()
    }

    private fun serializeBooks(books: List<ImportedBook>): String {
        val array = JSONArray()
        books.forEach { book ->
            array.put(
                JSONObject()
                    .put("id", book.id)
                    .put("title", book.title)
                    .put("author", book.author)
                    .put("chapter", book.chapter)
                    .put("format", book.format.name)
                    .put("filePath", book.filePath)
                    .put("fileName", book.fileName)
                    .put("progress", book.progress),
            )
        }
        return array.toString()
    }

    private fun serializeSources(sources: List<ImportedSource>): String {
        val array = JSONArray()
        sources.forEach { source ->
            array.put(
                JSONObject()
                    .put("id", source.id)
                    .put("name", source.name)
                    .put("url", source.url)
                    .put("ruleCount", source.ruleCount)
                    .put("enabled", source.enabled)
                    .put("rawPath", source.rawPath),
            )
        }
        return array.toString()
    }

    @SuppressLint("ApplySharedPref")
    private fun persistRestoredState(
        books: List<ImportedBook>,
        sources: List<ImportedSource>,
        typography: TypographySettings,
        sync: SyncSettings,
    ) {
        if (!preferences.edit()
                .putString(KEY_BOOKS, serializeBooks(books))
                .putString(KEY_SOURCES, serializeSources(sources))
                .putFloat(KEY_FONT_SIZE, typography.fontSizeSp)
                .putFloat(KEY_LINE_HEIGHT, typography.lineHeightMultiplier)
                .putString(KEY_FONT_FAMILY, typography.fontFamily)
                .putString(KEY_SYNC_URL, sync.url)
                .putString(KEY_SYNC_USERNAME, sync.username)
                .commit()
        ) {
            throw IllegalStateException("无法保存恢复后的数据")
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun restorePreferences(snapshot: PreferenceSnapshot) {
        val editor = preferences.edit()
        if (snapshot.books == null) editor.remove(KEY_BOOKS) else editor.putString(KEY_BOOKS, snapshot.books)
        if (snapshot.sources == null) editor.remove(KEY_SOURCES) else editor.putString(KEY_SOURCES, snapshot.sources)
        editor
            .putFloat(KEY_FONT_SIZE, snapshot.typography.fontSizeSp)
            .putFloat(KEY_LINE_HEIGHT, snapshot.typography.lineHeightMultiplier)
            .putString(KEY_FONT_FAMILY, snapshot.typography.fontFamily)
            .putString(KEY_SYNC_URL, snapshot.sync.url)
            .putString(KEY_SYNC_USERNAME, snapshot.sync.username)
            .commit()
    }

    private fun copyUri(uri: Uri, destination: File) {
        val input = appContext.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("无法读取所选文件")
        input.use { stream ->
            destination.outputStream().use { output -> stream.copyTo(output) }
        }
    }

    private fun readUriText(uri: Uri): String {
        val input = appContext.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("无法读取书源文件")
        input.use { stream ->
            return decodeText(readBytes(stream, MAX_SOURCE_BYTES))
        }
    }

    private fun displayName(uri: Uri): String {
        var cursor: Cursor? = null
        return try {
            cursor = appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor?.moveToFirst() == true) {
                cursor?.getString(0)?.takeIf { it.isNotBlank() } ?: fallbackName(uri)
            } else {
                fallbackName(uri)
            }
        } finally {
            cursor?.close()
        }
    }

    private fun fallbackName(uri: Uri): String = uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null } ?: "未命名文件"

    private fun cleanTitle(fileName: String, format: BookFormat): String {
        val suffix = ".${format.extension}"
        return fileName.removeSuffix(suffix).removeSuffix(suffix.uppercase(Locale.ROOT)).trim().ifBlank { "未命名书籍" }
    }

    private fun extractAuthor(text: String): String {
        val match = Regex("(?m)^\\s*(作者|Author)\\s*[:：]\\s*(.+?)\\s*$", RegexOption.IGNORE_CASE).find(text)
        return match?.groupValues?.getOrNull(2)?.trim()?.ifBlank { null } ?: "本地导入"
    }

    private fun parseSourceObjects(raw: String): List<JSONObject> {
        val root = JSONTokener(raw.trim().removePrefix("\uFEFF")).nextValue()
        return when (root) {
            is JSONObject -> {
                val nested = root.optJSONArray("sources") ?: root.optJSONArray("data")
                if (nested == null) listOf(root) else buildList {
                    for (index in 0 until nested.length()) nested.optJSONObject(index)?.let(::add)
                }
            }
            is JSONArray -> buildList {
                for (index in 0 until root.length()) root.optJSONObject(index)?.let(::add)
            }
            else -> emptyList()
        }
    }

    private fun firstNonBlank(source: JSONObject, keys: List<String>): String {
        return keys.firstNotNullOfOrNull { key ->
            source.optString(key).trim().takeIf { it.isNotBlank() }
        } ?: ""
    }

    private fun countRules(source: JSONObject): Int {
        return RULE_KEYS.count { key -> source.optString(key).trim().isNotBlank() }
    }

    private fun readEpubText(file: File, maxBytes: Int): ParsedBookText {
        val html = StringBuilder()
        var remaining = maxBytes
        ZipFile(file).use { archive ->
            val entries = mutableListOf<String>()
            val enumeration = archive.entries()
            while (enumeration.hasMoreElements()) {
                val name = enumeration.nextElement().name
                if (!name.endsWith(".xhtml", true) && !name.endsWith(".html", true) && !name.endsWith(".htm", true)) continue
                if (name.contains("nav", true) || name.contains("toc", true)) continue
                entries += name
            }
            entries.sorted().take(MAX_EPUB_ENTRIES).forEach { name ->
                if (remaining <= 0) return@forEach
                val entry = archive.getEntry(name) ?: return@forEach
                archive.getInputStream(entry).use { input ->
                    val bytes = readBytes(input, remaining)
                    remaining -= bytes.size
                    html.append(String(bytes, Charsets.UTF_8)).append("\n\n")
                }
            }
        }
        val plainText = Html.fromHtml(
            html.toString()
                .replace(Regex("(?is)<(script|style|head)\\b.*?</\\1>"), "")
                .replace(Regex("(?i)<br\\s*/?>"), "\n")
                .replace(Regex("(?i)</(p|div|h[1-6]|li)>"), "\n\n"),
            Html.FROM_HTML_MODE_LEGACY,
        ).toString()
        return parseText(plainText)
    }

    private fun readEpubTitle(file: File): String? {
        return runCatching {
            ZipFile(file).use { archive ->
                val opfName = findOpfPath(archive) ?: return@use null
                val opf = archive.getEntry(opfName) ?: return@use null
                val xml = archive.getInputStream(opf).use { input -> String(readBytes(input, PREVIEW_BYTES), Charsets.UTF_8) }
                Regex("(?is)<dc:title\\b[^>]*>(.*?)</dc:title>").find(xml)?.groupValues?.getOrNull(1)
                    ?.replace(Regex("<[^>]+>"), "")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
            }
        }.getOrNull()
    }

    private fun findOpfPath(archive: ZipFile): String? {
        val container = archive.getEntry("META-INF/container.xml") ?: return null
        val xml = archive.getInputStream(container).use { input -> String(readBytes(input, 64 * 1024), Charsets.UTF_8) }
        return Regex("full-path\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
            .find(xml)?.groupValues?.getOrNull(1)
    }

    private fun readTextPreview(file: File): String = decodeText(readBytes(file, PREVIEW_BYTES))

    private fun decodeText(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }
        val utf8 = String(bytes, Charsets.UTF_8)
        val replacementCount = utf8.count { it == '\uFFFD' }
        return if (replacementCount > 2 && replacementCount * 40 > utf8.length) {
            String(bytes, Charset.forName("GB18030"))
        } else {
            utf8
        }
    }

    private fun parseText(text: String): ParsedBookText {
        val normalized = text.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n').trim()
        if (normalized.isBlank()) return ParsedBookText("正文", emptyList())

        val headingPattern = Regex("^\\s*(第[0-9０-９一二三四五六七八九十百千零〇]+[章节回卷集].{0,80}|Chapter\\s+\\d+.*|序章|楔子|尾声|番外.*)$", RegexOption.IGNORE_CASE)
        var chapter = "正文"
        val paragraphs = mutableListOf<String>()
        val current = StringBuilder()

        fun flush() {
            val value = current.toString().trim()
            if (value.isNotBlank()) paragraphs += value
            current.clear()
        }

        normalized.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isBlank()) {
                flush()
            } else if (chapter == "正文" && headingPattern.matches(line)) {
                flush()
                chapter = line
            } else {
                if (current.isNotEmpty()) current.append('\n')
                current.append(line)
            }
        }
        flush()

        if (paragraphs.isEmpty()) return ParsedBookText(chapter, splitLongParagraph(normalized))
        return ParsedBookText(chapter, paragraphs.flatMap(::splitLongParagraph).take(MAX_PARAGRAPHS))
    }

    private fun splitLongParagraph(value: String): List<String> {
        if (value.length <= MAX_PARAGRAPH_LENGTH) return listOf(value)
        return value.chunked(MAX_PARAGRAPH_LENGTH)
    }

    private fun readBytes(file: File, maxBytes: Int): ByteArray = file.inputStream().use { readBytes(it, maxBytes) }

    private fun readBytes(input: java.io.InputStream, maxBytes: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var remaining = maxBytes
        while (remaining > 0) {
            val read = input.read(buffer, 0, minOf(buffer.size, remaining))
            if (read <= 0) break
            output.write(buffer, 0, read)
            remaining -= read
        }
        return output.toByteArray()
    }

    private fun writeZipEntry(archive: ZipOutputStream, name: String, bytes: ByteArray) {
        archive.putNextEntry(ZipEntry(name))
        archive.write(bytes)
        archive.closeEntry()
    }

    private fun writeZipFile(archive: ZipOutputStream, name: String, file: File) {
        archive.putNextEntry(ZipEntry(name))
        file.inputStream().use { input -> input.copyTo(archive) }
        archive.closeEntry()
    }

    private companion object {
        const val PREFERENCES_NAME = "quiet_reader_local"
        const val BACKUP_FORMAT = "quiet-reader-backup"
        const val BACKUP_VERSION = 2
        const val MANIFEST_NAME = "manifest.json"
        const val KEY_BOOKS = "books"
        const val KEY_SOURCES = "sources"
        const val KEY_FONT_SIZE = "font_size"
        const val KEY_LINE_HEIGHT = "line_height"
        const val KEY_FONT_FAMILY = "font_family"
        const val KEY_SYNC_URL = "sync_url"
        const val KEY_SYNC_USERNAME = "sync_username"
        const val PREVIEW_BYTES = 2 * 1024 * 1024
        const val MAX_SOURCE_BYTES = 4 * 1024 * 1024
        const val MAX_READER_BYTES = 32 * 1024 * 1024
        const val MAX_BACKUP_BYTES = 512L * 1024L * 1024L
        const val MAX_BACKUP_ITEM_BYTES = 256L * 1024L * 1024L
        const val MAX_BACKUP_UNCOMPRESSED_BYTES = 480L * 1024L * 1024L
        const val MAX_MANIFEST_BYTES = 2L * 1024L * 1024L
        const val MAX_BACKUP_ENTRIES = 513
        const val MAX_BACKUP_ITEMS = 256
        const val MAX_PARAGRAPHS = 240
        const val MAX_PARAGRAPH_LENGTH = 800
        const val MAX_EPUB_ENTRIES = 80
        val NAME_KEYS = listOf("bookSourceName", "sourceName", "name", "title")
        val URL_KEYS = listOf("bookSourceUrl", "sourceUrl", "url")
        val RULE_KEYS = listOf("ruleSearch", "ruleExplore", "ruleBookInfo", "ruleToc", "ruleContent", "ruleReview")
    }
}

private val BookFormat.chapterLabel: String
    get() = when (this) {
        BookFormat.EPUB -> "EPUB · 待读取目录"
        BookFormat.TXT -> "正文"
        BookFormat.PDF -> "PDF · 固定版式"
    }
