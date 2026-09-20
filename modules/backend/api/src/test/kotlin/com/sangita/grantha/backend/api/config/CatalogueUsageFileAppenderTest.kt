package com.sangita.grantha.backend.api.config

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.slf4j.LoggerFactory

class CatalogueUsageFileAppenderTest {
    @Test
    fun `catalogue-usage logger writes raw JSON lines to a dedicated file`() {
        val directory = Files.createTempDirectory("catalogue-usage")
        val env = ApiEnvironment(
            environment = Environment.TEST,
            catalogueUsageDirectory = directory.toString(),
        )
        val context = LoggerFactory.getILoggerFactory() as ch.qos.logback.classic.LoggerContext
        try {
            LogbackConfig.configure(env)
            val logger = LoggerFactory.getLogger("catalogue-usage")
            logger.info("""{"eventId":"test-event-1","action":"search"}""")
            context.getLogger("catalogue-usage").detachAndStopAllAppenders()

            val file = directory.resolve("catalogue-usage.jsonl")
            assertTrue(Files.exists(file), "expected JSONL file at $file")
            val text = Files.readString(file).trim()
            assertTrue(text.contains("\"eventId\":\"test-event-1\""), text)
            assertEquals(1, text.lines().size)
            assertTrue(!text.contains("ISO8601") && !text.contains("catalogue-usage -"), text)
        } finally {
            context.reset()
        }
    }
}
