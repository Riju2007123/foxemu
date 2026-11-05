package com.fox.emu.container

import android.content.Context
import java.io.File

/**
 * Owns the on-disk layout: rootfs (FEX x86_64 Ubuntu rootfs), proton dir
 * (arm64ec Wine/Proton + bundled FEXCore unixlib), drivers dir (Turnip +
 * ICD json), and one home directory per game container (wineprefix,
 * saves, config — kept isolated per-game so one install doesn't corrupt
 * another's Windows registry).
 */
class ContainerManager(private val context: Context) {

    private val filesDir get() = context.filesDir

    val rootfsPath: String get() = File(filesDir, "rootfs").absolutePath
    val protonPath: String get() = File(filesDir, "proton").absolutePath
    val fexBinPath: String get() = File(filesDir, "bin/FEXInterpreter").absolutePath
    val turnipIcdPath: String get() = File(filesDir, "drivers/turnip.icd.json").absolutePath

    fun containerHome(containerId: String): String {
        val dir = File(File(filesDir, "containers"), containerId)
        dir.mkdirs()
        return dir.absolutePath
    }

    fun ensureWinePrefix(containerId: String): String {
        val prefix = File(containerHome(containerId), "wineprefix")
        prefix.mkdirs()
        return prefix.absolutePath
    }

    /**
     * Runs a command inside the FEX-translated namespace. On Android this
     * means exec'ing through the app's own process (FEX itself does the
     * x86_64 -> arm64 translation; no separate mount namespace is strictly
     * required since FEX's rootfs overlay handles library resolution).
     * ProcessBuilder here targets this app's private files dir, which is
     * writable and executable (unlike /sdcard) — required for Android 10+.
     */
    fun runInNamespace(containerId: String, cmd: List<String>, extraEnv: Map<String, String> = emptyMap()) {
        val pb = ProcessBuilder(cmd)
        pb.redirectErrorStream(true)
        pb.directory(File(containerHome(containerId)))
        val env = pb.environment()
        env["HOME"] = containerHome(containerId)
        env["FEX_ROOTFS"] = rootfsPath
        env["LD_LIBRARY_PATH"] = "$protonPath/lib:$protonPath/lib64"
        extraEnv.forEach { (k, v) -> env[k] = v }
        pb.start()
    }

    fun installAndLaunchApk(apkPath: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
        intent.setDataAndType(
            androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", File(apkPath)
            ),
            "application/vnd.android.package-archive"
        )
        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
