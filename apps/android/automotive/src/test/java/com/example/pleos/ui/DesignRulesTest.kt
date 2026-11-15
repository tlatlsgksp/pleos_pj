package com.example.pleos.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Keeps screens on the layout rules (docs/visual/design-foundation.html): text roles from
 * PleosText, spacing from Space, numbers through Format. Sizes live in ui/design only.
 */
class DesignRulesTest {

    private val featureDir = File("src/main/java/com/example/pleos/ui/feature")

    private val rules = mapOf(
        "a Material type style instead of a PleosText role" to Regex("""MaterialTheme\.typography"""),
        "a dp literal instead of a Space step or a design component" to Regex("""\b\d+(\.\d+)?\.dp\b"""),
        "a unit formatted by hand instead of Format" to Regex(""""%[^"]*(km|kWh| L)""""),
    )

    @Test
    fun `screens use the design rules`() {
        val files = featureDir.walkTopDown().filter { it.extension == "kt" }.toList()
        assertTrue("no screen sources found under ${featureDir.absolutePath}", files.isNotEmpty())
        val violations = files.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                rules.entries.firstOrNull { it.value.containsMatchIn(line) }?.let { "${file.name}:${index + 1} ${it.key}: ${line.trim()}" }
            }
        }
        assertTrue(violations.joinToString("\n", prefix = "\n"), violations.isEmpty())
    }
}
