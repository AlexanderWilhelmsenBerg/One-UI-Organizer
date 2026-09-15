package io.github.alexanderwilhelmsenberg.oneuiorganizer.diagnostics

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class FileAppEventLogTest {
    @Test
    fun recordsMessagesAndThrowableDetailsAcrossInstances() {
        val directory = Files.createTempDirectory("one-ui-organizer-event-log").toFile()
        val file = directory.resolve("event.log")
        val log = FileAppEventLog(file = file, nowEpochMillis = { 1_000L })

        log.record("backup opened")
        log.record("metadata failed", IllegalStateException("boom"))

        val restored = FileAppEventLog(file).read()
        assertContains(restored, "backup opened")
        assertContains(restored, "metadata failed")
        assertContains(restored, "IllegalStateException: boom")
    }

    @Test
    fun clearRemovesExistingEvents() {
        val directory = Files.createTempDirectory("one-ui-organizer-event-log-clear").toFile()
        val file = directory.resolve("event.log")
        val log = FileAppEventLog(file)
        log.record("before clear")

        log.clear()

        assertEquals("", log.read())
    }
}
