package com.nextcraft.forge.runtime

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The app has one engine, the root chroot; the probe that grants it needs a device. */
class RuntimeEngineTest {
    @After
    fun reset() = RuntimeEngines.overrideForTests(null)

    @Test
    fun launchArgv_runsThroughSuWithPrivateMounts() {
        val command = ". '/x/.env'; rm -f '/x/.env'; exec '/x/chroot-exec.sh'"
        assertEquals(listOf("su", "-mm", "-c", command), RuntimeEngines.launchArgv(command))
    }

    @Test
    fun current_defaultsToChrootUntilDetected() {
        assertEquals(RuntimeEngine.CHROOT, RuntimeEngines.current())
        RuntimeEngines.overrideForTests(RuntimeEngine.CHROOT)
        assertEquals(RuntimeEngine.CHROOT, RuntimeEngines.current())
    }

    @Test
    fun onlyTheChrootEngineExists() {
        assertEquals(listOf(RuntimeEngine.CHROOT), RuntimeEngine.entries)
        RuntimeEngines.overrideForTests(null)
        assertNull(RuntimeEngine.entries.firstOrNull { it.name.equals("PROOT", ignoreCase = true) })
    }
}
