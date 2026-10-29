package com.nextcraft.forge.runtime

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Claude Code and Antigravity CLI are proprietary: they are downloaded from the vendor, never re-hosted by this project. */
class OfficialAgentSourcesTest {
    @Test
    fun claudeFallbackVersion_isAValidReleaseVersion() {
        assertTrue(RuntimeInstaller.CLAUDE_VERSION_PATTERN.matches(RuntimeInstaller.CLAUDE_FALLBACK_VERSION))
    }

    @Test
    fun claudeVersionPattern_rejectsAnythingThatCouldEscapeTheReleaseUrl() {
        val pattern = RuntimeInstaller.CLAUDE_VERSION_PATTERN
        assertTrue(pattern.matches("2.1.308"))
        assertFalse(pattern.matches("2.1.308/../x"))
        assertFalse(pattern.matches("2.1.308; rm -rf /"))
        assertFalse(pattern.matches(""))
    }

    @Test
    fun agyFallbackIsAnOfficialVendorUrlWithAFullSha512() {
        assertTrue(RuntimeInstaller.AGY_FALLBACK_URL.startsWith("https://storage.googleapis.com/antigravity-public/"))
        assertEquals(128, RuntimeInstaller.AGY_FALLBACK_SHA512.length)
        assertTrue(RuntimeInstaller.AGY_FALLBACK_SHA512.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun sourceNoLongerReferencesProjectHostedProprietaryBundles() {
        val sources = File("src/main/java").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue("source tree not found from ${File(".").absoluteFile}", sources.isNotEmpty())
        val forbidden = listOf("nextcraft-claude-arm64", "nextcraft-agy-arm64", "CLAUDE_BUNDLE", "AGY_BUNDLE")
        val offenders = sources.flatMap { file ->
            val text = file.readText()
            forbidden.filter { it in text }.map { "${file.name}: $it" }
        }
        assertTrue("project-hosted proprietary bundles are referenced again: $offenders", offenders.isEmpty())
    }
}
