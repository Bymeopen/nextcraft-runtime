package com.nextcraft.forge.runtime

import android.content.Context

/**
 * How guest commands reach the Ubuntu rootfs. This app only has the root engine: `su -mm` + kernel mounts +
 * `chroot-exec.sh` (ADR-002). Devices without root use the separate non-root app edition, not a fallback here.
 */
enum class RuntimeEngine { CHROOT }

/**
 * Root detection, cached per process: the first `su` call shows the Magisk/KernelSU/APatch dialog, so it must not
 * be repeated on every launch. Code that has no [Context] (RootfsRepair, native spawn) reads [current].
 */
object RuntimeEngines {
    @Volatile
    private var detected: RuntimeEngine? = null

    @Volatile
    private var detectionDone = false

    /** Monotonic ms of the last probe that found no root; a null result is only trusted for [RETRY_AFTER_MS]. */
    @Volatile
    private var failedAtMs = 0L

    private const val RETRY_AFTER_MS = 20_000L

    /** [RuntimeEngine.CHROOT] when root was granted; `null` means nothing can run. */
    fun detect(context: Context, rootTimeoutSeconds: Long = 8L, refresh: Boolean = false): RuntimeEngine? {
        if (detectionDone && !refresh) {
            // A granted result is cached for the process; a denied/timed-out one is re-probed after a cool-down
            // (the root dialog may simply not have been answered within the timeout).
            if (detected != null || (System.nanoTime() / 1_000_000L) - failedAtMs < RETRY_AFTER_MS) return detected
        }
        val engine = RuntimeEngine.CHROOT.takeIf { isRootAccessAvailable(rootTimeoutSeconds) }
        detected = engine
        detectionDone = true
        if (engine == null) failedAtMs = (System.nanoTime() / 1_000_000L)
        return engine
    }

    /** Forgets the cached result, e.g. after the user grants root. */
    fun invalidate() {
        detectionDone = false
        detected = null
    }

    /** Last detected engine; CHROOT until a detection ran. Not proof of root: use [detect] to check that root was granted. */
    fun current(): RuntimeEngine = detected ?: RuntimeEngine.CHROOT

    /**
     * Process arguments that run [shellCommand] (the `. env; rm -f env; exec …` line built by
     * [RuntimeInstaller.prepareChrootShellCommand]).
     */
    fun launchArgv(shellCommand: String): List<String> = listOf("su", "-mm", "-c", shellCommand)

    /** Test hook: pretend a detection produced [engine]. */
    internal fun overrideForTests(engine: RuntimeEngine?) {
        detected = engine
        detectionDone = engine != null
    }
}
