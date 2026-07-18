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
            Regex("""\bTimber\.[A-Za-z_]\w*\s*\("""),
            Regex("""\bprintStackTrace\s*\("""),
            Regex("""\b(?:print|println)\s*\("""),
            Regex("""\bSystem\.(?:out|err)\b""")
        )

        val findings = findInProductionSources(directLogging)

        assertTrue(
            "Direct production logging is prohibited because notification title/body may be in scope:\n" +
                findings.joinToString("\n"),
            findings.isEmpty()
        )
    }

    @Test
    fun `production sources do not stringify notification carriers`() {
        val findings = productionKotlinFiles().flatMap { path ->
            val lines = Files.readAllLines(path)
            val source = lines.joinToString("\n")
            val identifiers = sensitiveCarrierIdentifiers(source)
            val patterns = identifiers.flatMap(::stringificationPatterns) + inlineCarrierPatterns()

            lines.mapIndexedNotNull { index, line ->
                if (patterns.any { pattern -> pattern.containsMatchIn(line) }) {
                    finding(path, index, line)
                } else {
                    null
                }
            }
        }

        assertTrue(
            "Notification carriers must not be stringified into logs or exception messages:\n" +
                findings.joinToString("\n"),
            findings.isEmpty()
        )
    }

    private fun sensitiveCarrierIdentifiers(source: String): Set<String> {
        val identifiers = mutableSetOf(
            "sbn",
            "statusBarNotification",
            "extras",
            "bundle",
            "notificationRecord",
            "notificationEntity"
        )

        // Capture parameters and properties even when their names differ from repository conventions.
        Regex(
            """\b([A-Za-z_]\w*)\s*:\s*(?:[A-Za-z_]\w*\.)?""" +
                """(?:StatusBarNotification|Bundle|NotificationRecord|NotificationEntity)\b\??"""
        ).findAll(source).forEach { match ->
            identifiers += match.groupValues[1]
        }

        // Capture locally inferred carrier values created directly by constructors.
        Regex(
            """\b(?:val|var)\s+([A-Za-z_]\w*)\s*=\s*(?:[A-Za-z_]\w*\.)?""" +
                """(?:StatusBarNotification|Bundle|NotificationRecord|NotificationEntity)\s*\("""
        ).findAll(source).forEach { match ->
            identifiers += match.groupValues[1]
        }

        // Notification extras are normally inferred as Bundle without an explicit local type.
        Regex("""\b(?:val|var)\s+([A-Za-z_]\w*)\s*=\s*[^\n;]*\.extras\b""")
            .findAll(source)
            .forEach { match -> identifiers += match.groupValues[1] }

        return identifiers
    }

    private fun stringificationPatterns(identifier: String): List<Regex> {
        val name = Regex.escape(identifier)
        return listOf(
            Regex("""\b$name\.toString\s*\("""),
            Regex("""\${'$'}\{\s*$name\s*}"""),
            Regex("""\${'$'}$name\b"""),
            Regex("""\b(?:String\.valueOf|Objects\.toString)\s*\(\s*$name\b"""),
            Regex("""\.append\s*\(\s*$name\b"""),
            Regex("""(?:\+\s*$name\b|\b$name\s*\+)""")
        )
    }

    private fun inlineCarrierPatterns(): List<Regex> = listOf(
        Regex("""\bBundle\.toString\s*\("""),
        Regex("""\.extras\.toString\s*\("""),
        Regex("""\${'$'}\{\s*[^}]*\.extras\s*}"""),
        Regex(
            """\b(?:StatusBarNotification|Bundle|NotificationRecord|NotificationEntity)""" +
                """\s*\([^)]*\)\s*\.toString\s*\("""
        )
    )

    private fun findInProductionSources(patterns: List<Regex>): List<String> =
        productionKotlinFiles().flatMap { path ->
            Files.readAllLines(path).mapIndexedNotNull { index, line ->
                if (patterns.any { pattern -> pattern.containsMatchIn(line) }) {
                    finding(path, index, line)
                } else {
                    null
                }
            }
        }

    private fun productionKotlinFiles(): List<Path> {
        val root = productionSourceRoot()
        return Files.walk(root).use { paths ->
            paths.iterator().asSequence()
                .filter { path -> Files.isRegularFile(path) && path.fileName.toString().endsWith(".kt") }
                .toList()
        }
    }

    private fun finding(path: Path, index: Int, line: String): String {
        val root = productionSourceRoot()
        return "${root.relativize(path)}:${index + 1}: ${line.trim()}"
    }

    private fun productionSourceRoot(): Path {
        val candidates = listOf(
            Paths.get("src", "main", "java"),
            Paths.get("app", "src", "main", "java")
        )
        return candidates.firstOrNull { candidate -> Files.isDirectory(candidate) }
            ?: error("Production source root was not found from ${Paths.get("").toAbsolutePath()}")
    }
}
