package io.github.alexanderwilhelmsenberg.oneuiorganizer.diagnostics

import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.time.Instant

interface AppEventLog {
    fun record(message: String, throwable: Throwable? = null)

    fun read(): String

    fun clear()
}

object NoOpAppEventLog : AppEventLog {
    override fun record(message: String, throwable: Throwable?) = Unit

    override fun read(): String = ""

    override fun clear() = Unit
}

class FileAppEventLog(
    private val file: File,
    private val maximumBytes: Long = DEFAULT_MAXIMUM_BYTES,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis
) : AppEventLog {
    private val lock = Any()

    override fun record(message: String, throwable: Throwable?) {
        try {
            synchronized(lock) {
                file.parentFile?.mkdirs()
                if (file.exists() && file.length() >= maximumBytes) {
                    file.writeText("")
                }
                PrintWriter(FileWriter(file, true)).use { writer ->
                    writer.print(Instant.ofEpochMilli(nowEpochMillis()))
                    writer.print(" [")
                    writer.print(Thread.currentThread().name)
                    writer.print("] ")
                    writer.println(message)
                    throwable?.printStackTrace(writer)
                }
            }
        } catch (_: Throwable) {
            // Diagnostics must never become another failure source, including during an OOM crash path.
        }
    }

    override fun read(): String = try {
        synchronized(lock) {
            if (file.exists()) file.readText() else ""
        }
    } catch (throwable: Throwable) {
        "Unable to read event log: ${throwable::class.java.simpleName}: ${throwable.message.orEmpty()}"
    }

    override fun clear() {
        try {
            synchronized(lock) {
                if (file.exists()) {
                    file.writeText("")
                }
            }
        } catch (_: Throwable) {
            // Diagnostics must never become another failure source.
        }
    }

    private companion object {
        const val DEFAULT_MAXIMUM_BYTES = 256L * 1024L
    }
}
