package io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupExport
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@JvmInline
value class BackupDocumentId(val value: String) {
    init {
        require(value.isNotBlank()) { "Backup document identity must not be blank." }
    }
}

interface BackupDocumentStore {
    suspend fun read(documentId: BackupDocumentId): BackupDocumentReadResult

    suspend fun write(documentId: BackupDocumentId, content: String): BackupDocumentWriteResult
}

sealed interface BackupDocumentReadResult {
    data class Success(val content: String) : BackupDocumentReadResult

    data class Failure(val error: BackupDocumentIoError) : BackupDocumentReadResult
}

sealed interface BackupDocumentWriteResult {
    data object Success : BackupDocumentWriteResult

    data class Failure(val error: BackupDocumentIoError) : BackupDocumentWriteResult
}

enum class BackupDocumentIoError {
    CANNOT_OPEN,
    READ_FAILED,
    WRITE_FAILED
}

class AndroidBackupDocumentStore(
    private val contentResolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BackupDocumentStore {
    override suspend fun read(documentId: BackupDocumentId): BackupDocumentReadResult = withContext(ioDispatcher) {
        try {
            val input = contentResolver.openInputStream(Uri.parse(documentId.value))
                ?: return@withContext BackupDocumentReadResult.Failure(BackupDocumentIoError.CANNOT_OPEN)
            val content = input.bufferedReader(Charsets.UTF_8).use { reader -> reader.readText() }
            BackupDocumentReadResult.Success(content)
        } catch (_: SecurityException) {
            BackupDocumentReadResult.Failure(BackupDocumentIoError.CANNOT_OPEN)
        } catch (_: IOException) {
            BackupDocumentReadResult.Failure(BackupDocumentIoError.READ_FAILED)
        }
    }

    override suspend fun write(
        documentId: BackupDocumentId,
        content: String
    ): BackupDocumentWriteResult = withContext(ioDispatcher) {
        try {
            val output = contentResolver.openOutputStream(Uri.parse(documentId.value), WRITE_MODE)
                ?: return@withContext BackupDocumentWriteResult.Failure(BackupDocumentIoError.CANNOT_OPEN)
            output.bufferedWriter(Charsets.UTF_8).use { writer -> writer.write(content) }
            BackupDocumentWriteResult.Success
        } catch (_: SecurityException) {
            BackupDocumentWriteResult.Failure(BackupDocumentIoError.CANNOT_OPEN)
        } catch (_: IOException) {
            BackupDocumentWriteResult.Failure(BackupDocumentIoError.WRITE_FAILED)
        }
    }

    private companion object {
        const val WRITE_MODE = "wt"
    }
}

object BackupDocumentPicker {
    fun createDocumentContract(): ActivityResultContracts.CreateDocument =
        ActivityResultContracts.CreateDocument(OrganizerBackupExport.MIME_TYPE)

    fun openDocumentContract(): ActivityResultContracts.OpenDocument = ActivityResultContracts.OpenDocument()

    val importMimeTypes: Array<String>
        get() = arrayOf(OrganizerBackupExport.MIME_TYPE, "text/json")
}
