package com.nextcraft.forge.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ChrootOptimizerTest {

    private fun sampleHardwareProfile(
        ramTier: DeviceHardwareOptimizer.RamTier = DeviceHardwareOptimizer.RamTier.RAM_8GB,
        totalRamMb: Long = 7_500L,
        swapTotalMb: Long = 4_096L,
        socVendor: DeviceHardwareOptimizer.SocVendor = DeviceHardwareOptimizer.SocVendor.SNAPDRAGON,
        socTier: DeviceHardwareOptimizer.SocTier = DeviceHardwareOptimizer.SocTier.BALANCED_MID,
    ) = DeviceHardwareOptimizer.HardwareProfile(
        socVendor = socVendor,
        socModel = "SM7475",
        socTier = socTier,
        topology = DeviceHardwareOptimizer.CoreTopology(8, 4, 4),
        ramTier = ramTier,
        totalRamMb = totalRamMb,
        availRamMb = totalRamMb / 2,
        swap = DeviceHardwareOptimizer.SwapProfile(
            swapTotalMb = swapTotalMb,
            swapFreeMb = swapTotalMb,
            zramAlgorithm = "lz4",
        ),
    )

    @Test
    fun classifiesAllAndroidKernelFamiliesAndPageSizes() {
        val kLegacy = ChrootOptimizer.inspectKernel("4.14.186-perf+", pageSizeBytes = 4_096)
        assertEquals(ChrootOptimizer.KernelFamily.LEGACY_3_18_TO_4_14, kLegacy.family)
        assertTrue(kLegacy.needsKernelReleaseSpoof)
        assertEquals("5.15.0-nextcraft-arm64", kLegacy.spoofedRelease)
        assertFalse(kLegacy.is16KbPageKernel)

        val kCommon = ChrootOptimizer.inspectKernel("4.19.157-qgki", pageSizeBytes = 4_096)
        assertEquals(ChrootOptimizer.KernelFamily.ANDROID_COMMON_4_19_TO_5_4, kCommon.family)
        assertTrue(kCommon.needsKernelReleaseSpoof)
        assertEquals("5.15.0-nextcraft-arm64", kCommon.spoofedRelease)

        val kGki1 = ChrootOptimizer.inspectKernel("5.15.148-android13-8", pageSizeBytes = 4_096)
        assertEquals(ChrootOptimizer.KernelFamily.GKI_1_0_5_10_TO_5_15, kGki1.family)
        assertTrue(kGki1.isGkiKernel)
        assertFalse(kGki1.needsKernelReleaseSpoof)
        assertEquals("5.15.148-android13-8", kGki1.spoofedRelease)

        val kGki2_16k = ChrootOptimizer.inspectKernel("6.6.30-android15-8-gki", pageSizeBytes = 16_384)
        assertEquals(ChrootOptimizer.KernelFamily.GKI_2_0_6_1_PLUS, kGki2_16k.family)
        assertTrue(kGki2_16k.isGkiKernel)
        assertTrue(kGki2_16k.is16KbPageKernel)
        assertEquals("6.6.30-android15-8-gki", kGki2_16k.spoofedRelease)
    }

    @Test
    fun sizesSharedMemoryAndThreadsAcrossRamAndSocTiers() {
        val eightGbSwap = ChrootOptimizer.buildConfig(
            hwProfile = sampleHardwareProfile(ramTier = DeviceHardwareOptimizer.RamTier.RAM_8GB, totalRamMb = 7_400L, swapTotalMb = 4_096L),
            kernelRelease = "5.15.120-android13",
            pageSizeBytes = 4_096,
        )
        assertEquals(512, eightGbSwap.shmSizeMb)
        assertEquals(768, eightGbSwap.tmpSizeMb)
        assertEquals(6, eightGbSwap.uvThreadpoolSize)

        val sixGbNoSwap = ChrootOptimizer.buildConfig(
            hwProfile = sampleHardwareProfile(
                ramTier = DeviceHardwareOptimizer.RamTier.RAM_6GB,
                totalRamMb = 5_500L,
                swapTotalMb = 0L,
                socVendor = DeviceHardwareOptimizer.SocVendor.UNISOC,
                socTier = DeviceHardwareOptimizer.SocTier.ENTRY_EFFICIENCY,
            ),
            kernelRelease = "4.14.190-sprd",
            pageSizeBytes = 4_096,
        )
        assertEquals(256, sixGbNoSwap.shmSizeMb)
        assertEquals(512, sixGbNoSwap.tmpSizeMb)
        assertEquals(4, sixGbNoSwap.uvThreadpoolSize)

        val sixteenGb = ChrootOptimizer.buildConfig(
            sampleHardwareProfile(
                ramTier = DeviceHardwareOptimizer.RamTier.RAM_16GB_PLUS,
                totalRamMb = 15_200L,
                swapTotalMb = 8_192L,
                socTier = DeviceHardwareOptimizer.SocTier.FLAGSHIP_PERFORMANCE,
            ),
            kernelRelease = "6.1.50-android14",
            pageSizeBytes = 16_384,
        )
        assertEquals(1024, sixteenGb.shmSizeMb)
        assertEquals(2048, sixteenGb.tmpSizeMb)
        assertEquals(8, sixteenGb.uvThreadpoolSize)
        assertEquals("16384", sixteenGb.environmentVars["NEXTCRAFT_PAGE_SIZE"])
    }

    @Test
    fun environmentCarriesNoProotVariables() {
        val cfg = ChrootOptimizer.buildConfig(sampleHardwareProfile(), kernelRelease = "5.10.198-android12", pageSizeBytes = 4_096)
        assertTrue(cfg.environmentVars.containsKey("NEXTCRAFT_KERNEL_RELEASE"))
        assertFalse(cfg.environmentVars.keys.any { it.startsWith("PROOT_") })
    }

    @Test
    fun removesOnlyTheWrapperFilesAnOlderBuildInstalled() {
        val rootfs = Files.createTempDirectory("nextcraft-chroot-test").toFile()
        try {
            val fast = File(rootfs, "opt/nextcraft/bin/proot-fast").apply { parentFile.mkdirs(); writeText("# wrapper\n") }
            val shim = File(rootfs, "usr/local/bin/proot").apply { parentFile.mkdirs(); writeText("# marker: NEXTCRAFT_PROOT_FAST_WRAPPER\n") }
            ChrootOptimizer.removeLegacyProotWrappers(rootfs)
            assertFalse(fast.exists())
            assertFalse(shim.exists())

            val realBinary = File(rootfs, "usr/local/bin/proot").apply { writeText("ELF-real-proot") }
            ChrootOptimizer.removeLegacyProotWrappers(rootfs)
            assertTrue("a proot the user installed must stay", realBinary.isFile)
        } finally {
            rootfs.deleteRecursively()
        }
    }
}
