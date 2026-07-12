package com.notificationbox.app.safety

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSafetySourceTest {
    @Test
    fun `production sources do not mutate or suppress source notifications`() {
        val forbiddenCalls = listOf(
            Regex("""\bcancelNotification\s*\("""),
            Regex("""\bcancelNotifications\s*\("""),
            Regex("""\bcancelAllNotifications\s*\("""),
            Regex("""\bsnoozeNotification\s*\("""),
            Regex("""\brequestListenerHints\s*\("""),
            Regex("""\brequestInterruptionFilter\s*\(""")
        )

        val findings = findInProductionSources(forbiddenCalls)

        assertTrue(
            "Notification listener mutation or suppression APIs must not be used:\n" +
                findings.joinToString("\n"),
            findings.isEmpty()
        )
    }

    @Test
    fun `production sources do not write notification data to direct logs`() {
        val directLogging = listOf(
            Regex("""\bandroid\.util\.Log\b"""),
            Regex("""\bLog\.(?:v|d|i|w|e|wtf)\s*\("""),
            Regex("""\bTimber\."""),
            Regex("""\bprintStackTrace\s*\("""),
            Regex("""\b(?:print|println)\s*\(""")
        )

        val findings = findInProductionSources(directLogging)

        assertTrue(
            "Direct production logging is prohibited because notification title/body may be in scope:\n" +
                findings.joinToString("\n"),
            findings.isEmpty()
        )
    }

    private fun findInProductionSources(patterns: List<Regex>): List<String> {
        val root = productionSourceRoot()
        return Files.walk(root).use { paths ->
            paths.iterator().asSequence()
                .filter { path -> Files.isRegularFile(path) && path.fileName.toString().endsWith(".kt") }
                .flatMap { path ->
                    Files.readAllLines(path).asSequence().mapIndexedNotNull { index, line ->
                        if (patterns.any { pattern -> pattern.containsMatchIn(line) }) {
                            "${root.relativize(path)}:${index + 1}: ${line.trim()}"
                        } else {
                            null
                        }
                    }
                }
                .toList()
        }
    }

    private fun productionSourceRoot(): Path {
        val candidates = listOf(
            Paths.get("src", "main", "java"),
            Paths.get("app", "src", "main", "java")
        )
        return candidates.firstOrNull(Files::isDirectory)
            ?: error("Production source root was not found from ${Paths.get("").toAbsolutePath()}")
    }
}
