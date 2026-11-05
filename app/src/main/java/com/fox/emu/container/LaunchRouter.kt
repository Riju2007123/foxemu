package com.fox.emu.container

enum class Platform { WINDOWS, LINUX, ANDROID }

data class GameEntry(
    val title: String,
    val platform: Platform,
    val executablePath: String,
    val containerId: String
)

class LaunchRouter(private val containerManager: ContainerManager) {

    fun launch(game: GameEntry) {
        when (game.platform) {
            Platform.WINDOWS -> launchWindows(game)
            Platform.LINUX -> launchLinux(game)
            Platform.ANDROID -> containerManager.installAndLaunchApk(game.executablePath)
        }
    }

    private fun launchWindows(game: GameEntry) {
        // proton-wine's arm64ec wine64 has the FEXCore unixlib loader built
        // in — it self-handles x86_64 Windows DLLs without us invoking FEX
        // as a separate wrapper, so wine64 is the entry point directly.
        val wine64 = "${containerManager.protonPath}/bin/wine64"
        val wineprefix = containerManager.ensureWinePrefix(game.containerId)

        containerManager.runInNamespace(
            game.containerId,
            listOf(wine64, game.executablePath),
            extraEnv = mapOf(
                "WINEPREFIX" to wineprefix,
                "WINEARCH" to "win64",
                "VK_ICD_FILENAMES" to containerManager.turnipIcdPath,
                "VK_DRIVER_FILES" to containerManager.turnipIcdPath,
                "DXVK_FILTER_DEVICE_NAME" to "Turnip",
                "TU_DEBUG" to "sysmem",
                "WRAPPER_BLIT" to "1"
            )
        )
    }

    private fun launchLinux(game: GameEntry) {
        // Native x86_64 Linux ELF -> standalone FEX, no Wine layer at all
        containerManager.runInNamespace(
            game.containerId,
            listOf(containerManager.fexBinPath, "--", game.executablePath),
            extraEnv = mapOf(
                "FEX_ROOTFS" to containerManager.rootfsPath,
                "VK_ICD_FILENAMES" to containerManager.turnipIcdPath,
                "VK_DRIVER_FILES" to containerManager.turnipIcdPath,
                "TU_DEBUG" to "sysmem",
                "WRAPPER_BLIT" to "1"
            )
        )
    }
}
