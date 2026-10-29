package com.nextcraft.forge.runtime

import java.io.File
import java.util.Locale

/**
 * Kernel-aware tuning for the Root Chroot runtime (`su -mm` + `chroot-exec.sh`): kernel generation (`3.18`..`6.12+`,
 * GKI 2.0, 16 KB page size) classification, `/dev/shm` and `/tmp` sizing from the RAM tier, libuv threadpool size, and
 * the `NEXTCRAFT_*` environment the guest scripts read.
 *
 * `KernelFamily`:
 *  - `LEGACY_3_18_TO_4_14`: legacy vendor kernels; the reported release is spoofed to `5.15.0-nextcraft-arm64` for
 *    Ubuntu 24.04 (`glibc 2.39`) and `io_uring` is disabled.
 *  - `ANDROID_COMMON_4_19_TO_5_4`: early Treble / QGKI kernels; same spoof, `rseq`/`io_uring` disabled.
 *  - `GKI_1_0_5_10_TO_5_15`: Android 12/13 GKI kernels; real release kept.
 *  - `GKI_2_0_6_1_PLUS`: Android 14+ GKI 2.0 kernels, including 16 KB page-size kernels (alignment hints via
 *    `JAVAC_FLAGS` / `NODE_OPTIONS` / `MALLOC_ARENA_MAX`).
 */
object ChrootOptimizer {

    enum class KernelFamily(val displayName: String, val shortBadge: String) {
        LEGACY_3_18_TO_4_14("Legacy Android Kernel (3.18–4.14)", "Kernel 4.x Legacy"),
        ANDROID_COMMON_4_19_TO_5_4("Android Common / QGKI (4.19–5.4)", "Kernel 4.19/5.4"),
        GKI_1_0_5_10_TO_5_15("Android GKI (5.10–5.15)", "GKI 5.10/5.15"),
        GKI_2_0_6_1_PLUS("Android Modern GKI 2.0 (6.1–6.12+)", "GKI 2.0 6.x+"),
    }

    data class KernelProfile(
        val rawRelease: String,
        val major: Int,
        val minor: Int,
        val patch: Int,
        val family: KernelFamily,
        val isGkiKernel: Boolean,
        val pageSizeBytes: Int,
        val needsKernelReleaseSpoof: Boolean,
        val spoofedRelease: String,
    ) {
        val is16KbPageKernel: Boolean
            get() = pageSizeBytes >= 16_384
    }

    data class ChrootOptimizationConfig(
        val kernelProfile: KernelProfile,
        val spoofedKernelRelease: String,
        val shmSizeMb: Int,
        val tmpSizeMb: Int,
        val uvThreadpoolSize: Int,
        val environmentVars: Map<String, String>,
    )

    /**
     * Parses a Linux kernel `uname -r` string (e.g. `5.15.148-android13-8-g1234` or `4.19.157-perf+`)
     * into `(major, minor, patch)`.
     */
    fun parseKernelVersion(unameRelease: String): Triple<Int, Int, Int> {
        val match = Regex("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?").find(unameRelease.trim())
            ?: return Triple(5, 10, 0)
        val major = match.groupValues[1].toIntOrNull() ?: 5
        val minor = match.groupValues[2].toIntOrNull() ?: 10
        val patch = match.groupValues.getOrNull(3)?.takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
        return Triple(major, minor, patch)
    }

    /**
     * Classifies any Android kernel (`3.18` through `6.12+`) into its [KernelFamily].
     */
    fun classifyKernelFamily(major: Int, minor: Int): KernelFamily = when {
        major >= 6 -> KernelFamily.GKI_2_0_6_1_PLUS
        major == 5 && minor >= 10 -> KernelFamily.GKI_1_0_5_10_TO_5_15
        (major == 5 && minor < 10) || (major == 4 && minor >= 19) -> KernelFamily.ANDROID_COMMON_4_19_TO_5_4
        else -> KernelFamily.LEGACY_3_18_TO_4_14
    }

    /**
     * Determines whether the kernel needs a spoofed release string for Ubuntu 24.04 (glibc 2.39)
     * and modern Node/Rust toolchains.
     */
    fun selectOptimalKernelRelease(rawUnameRelease: String): String {
        val (major, minor, _) = parseKernelVersion(rawUnameRelease)
        // Ubuntu 24.04 glibc 2.39 and modern V8/Rust/Bun probes work best when seeing >= 5.15 LTS or >= 6.1 LTS.
        return if (major < 5 || (major == 5 && minor < 10)) {
            "5.15.0-nextcraft-arm64"
        } else {
            rawUnameRelease.trim().ifEmpty { "6.1.0-nextcraft-arm64" }
        }
    }

    /**
     * Builds a complete [KernelProfile] for the given `uname -r` string and hardware page size.
     */
    fun inspectKernel(
        rawUnameRelease: String = System.getProperty("os.version").orEmpty(),
        pageSizeBytes: Int = detectPageSizeBytes(),
    ): KernelProfile {
        val (major, minor, patch) = parseKernelVersion(rawUnameRelease)
        val family = classifyKernelFamily(major, minor)
        val lower = rawUnameRelease.lowercase(Locale.US)
        val isGki = lower.contains("android12") ||
            lower.contains("android13") ||
            lower.contains("android14") ||
            lower.contains("android15") ||
            lower.contains("android16") ||
            lower.contains("gki") ||
            major >= 6 ||
            (major == 5 && minor >= 10)
        val needsSpoof = major < 5 || (major == 5 && minor < 10)
        val spoofed = selectOptimalKernelRelease(rawUnameRelease)
        return KernelProfile(
            rawRelease = rawUnameRelease.trim().ifEmpty { "5.15.0-android" },
            major = major,
            minor = minor,
            patch = patch,
            family = family,
            isGkiKernel = isGki,
            pageSizeBytes = pageSizeBytes.coerceAtLeast(4_096),
            needsKernelReleaseSpoof = needsSpoof,
            spoofedRelease = spoofed,
        )
    }

    private fun detectPageSizeBytes(): Int = runCatching {
        val pageSize = android.system.Os.sysconf(android.system.OsConstants._SC_PAGESIZE)
        if (pageSize > 0L) pageSize.toInt() else 4_096
    }.getOrDefault(4_096)

    /**
     * Computes the chroot tuning parameters for the given hardware profile and kernel version.
     */
    fun buildConfig(
        hwProfile: DeviceHardwareOptimizer.HardwareProfile,
        kernelRelease: String = System.getProperty("os.version").orEmpty(),
        pageSizeBytes: Int = detectPageSizeBytes(),
    ): ChrootOptimizationConfig {
        val kernelProfile = inspectKernel(
            rawUnameRelease = kernelRelease,
            pageSizeBytes = pageSizeBytes,
        )

        // Dynamic /dev/shm and /tmp sizing based on RAM tier + built-in SwapRAM
        val hasSwap = hwProfile.swap.hasActiveSwap
        val shmSizeMb = when (hwProfile.ramTier) {
            DeviceHardwareOptimizer.RamTier.RAM_16GB_PLUS -> 1024
            DeviceHardwareOptimizer.RamTier.RAM_12GB -> 768
            DeviceHardwareOptimizer.RamTier.RAM_10GB -> 512
            DeviceHardwareOptimizer.RamTier.RAM_8GB -> if (hasSwap) 512 else 384
            DeviceHardwareOptimizer.RamTier.RAM_6GB -> if (hasSwap) 384 else 256
            DeviceHardwareOptimizer.RamTier.RAM_4GB -> 192
        }
        val tmpSizeMb = when (hwProfile.ramTier) {
            DeviceHardwareOptimizer.RamTier.RAM_16GB_PLUS -> 2048
            DeviceHardwareOptimizer.RamTier.RAM_12GB -> 1536
            DeviceHardwareOptimizer.RamTier.RAM_10GB -> 1024
            DeviceHardwareOptimizer.RamTier.RAM_8GB -> 768
            DeviceHardwareOptimizer.RamTier.RAM_6GB -> 512
            DeviceHardwareOptimizer.RamTier.RAM_4GB -> 384
        }

        // libuv threadpool size for Node.js (Claude Code, DSH, npm/pnpm/vite) inside the chroot
        val uvThreads = when (hwProfile.socTier) {
            DeviceHardwareOptimizer.SocTier.ENTRY_EFFICIENCY -> 4
            DeviceHardwareOptimizer.SocTier.BALANCED_MID -> 6
            DeviceHardwareOptimizer.SocTier.FLAGSHIP_PERFORMANCE -> 8
        }

        val spoofedKernel = kernelProfile.spoofedRelease
        val envVars = linkedMapOf(
            "UV_THREADPOOL_SIZE" to uvThreads.toString(),
            "NEXTCRAFT_SHM_MB" to shmSizeMb.toString(),
            "NEXTCRAFT_TMP_MB" to tmpSizeMb.toString(),
            "NEXTCRAFT_KERNEL_RELEASE" to spoofedKernel,
            "NEXTCRAFT_KERNEL_FAMILY" to kernelProfile.family.shortBadge,
            "NEXTCRAFT_PAGE_SIZE" to kernelProfile.pageSizeBytes.toString(),
        )

        return ChrootOptimizationConfig(
            kernelProfile = kernelProfile,
            spoofedKernelRelease = spoofedKernel,
            shmSizeMb = shmSizeMb,
            tmpSizeMb = tmpSizeMb,
            uvThreadpoolSize = uvThreads,
            environmentVars = envVars,
        )
    }

    /**
     * Removes the PRoot helper scripts an older build installed into the rootfs (`/opt/nextcraft/bin/proot-fast` and the
     * `/usr/local/bin/proot` shim). A real `proot` binary the user installed is left alone: only files carrying the
     * wrapper marker are deleted.
     */
    fun removeLegacyProotWrappers(rootfs: File) {
        if (!rootfs.isDirectory) return
        File(rootfs, "opt/nextcraft/bin/proot-fast").delete()
        val shim = File(rootfs, "usr/local/bin/proot")
        if (shim.isFile && runCatching { shim.readText().contains("NEXTCRAFT_PROOT_FAST_WRAPPER") }.getOrDefault(false)) {
            shim.delete()
        }
    }
}
