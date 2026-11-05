package com.fox.emu.setup

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.tukaani.xz.XZInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

sealed class SetupStep(val label: String) {
    object ResolveTurnip : SetupStep("Resolving latest Turnip 710/720 build")
    object DriverDownload : SetupStep("Downloading Turnip driver")
    object ResolveProton : SetupStep("Resolving latest Proton ARM64EC build")
    object ProtonDownload : SetupStep("Downloading Proton ARM64EC (FEX unixlib included)")
    object ProtonExtract : SetupStep("Extracting Proton")
    object FexDownload : SetupStep("Downloading FEX interpreter (for native Linux games)")
    object RootfsDownload : SetupStep("Downloading x86_64 rootfs (for native Linux games)")
    object RootfsExtract : SetupStep("Extracting rootfs")
    object Done : SetupStep("Setup complete")
}

class SetupManager(private val context: Context) {

    private val client = OkHttpClient()
    private val baseDir = context.filesDir

    // Repos confirmed from release pages: Banners-Turnip ships a BannerHub /
    // AdrenoTools-compatible 710/720/722 experimental ZIP every build;
    // proton-wine ships arm64ec Proton with the FEXCore unixlib loader built
    // in, which is all Windows games need. Pure-Linux (non-Wine) x86_64
    // ELF binaries still need standalone FEX + an x86_64 rootfs, handled
    // separately below.
    private val turnipRepo = "The412Banner/Banners-Turnip"
    private val protonRepo = "The412Banner/proton-wine"
    private val fexRepo = "FEX-Emu/FEX"

    // Stable, official, not a release asset that rotates — safe to hardcode.
    private val x86_64RootfsUrl =
        "https://cdimage.ubuntu.com/ubuntu-base/releases/22.04/release/ubuntu-base-22.04-base-amd64.tar.gz"

    suspend fun runSetup(onProgress: (SetupStep, Float) -> Unit) = withContext(Dispatchers.IO) {
        val driverDir = File(baseDir, "drivers").apply { mkdirs() }
        val protonDir = File(baseDir, "proton").apply { mkdirs() }
        val rootfsDir = File(baseDir, "rootfs").apply { mkdirs() }
        val binDir = File(baseDir, "bin").apply { mkdirs() }

        // 1. Turnip driver — A710/720/722 build, resolved live
        onProgress(SetupStep.ResolveTurnip, 0f)
        val turnipAsset = GithubReleaseResolver.resolveLatestAsset(
            repo = turnipRepo,
            mustContain = listOf("710-720-Test"),
            mustNotContain = listOf("a8xx", "-sw", "wayland", "-linux")
        )
        onProgress(SetupStep.DriverDownload, 0f)
        val turnipZip = File(driverDir, turnipAsset.name)
        downloadFile(turnipAsset.browser_download_url, turnipZip) { p -> onProgress(SetupStep.DriverDownload, p) }
        extractZip(turnipZip, driverDir)
        turnipZip.delete()
        // normalize whatever .so name the zip contains so TurnipLoader finds it predictably
        driverDir.walkTopDown().firstOrNull { it.name.endsWith(".so") && it.name.contains("vulkan") }
            ?.let { it.copyTo(File(driverDir, "libvulkan_freedreno.so"), overwrite = true) }

        // 2. Proton arm64ec — bundles the FEXCore unixlib, covers all Windows games
        onProgress(SetupStep.ResolveProton, 0f)
        val protonAsset = GithubReleaseResolver.resolveLatestAsset(
            repo = protonRepo,
            mustContain = listOf("arm64ec", "sdk28"),
            mustNotContain = listOf("x86_64", "sdk35")
        )
        onProgress(SetupStep.ProtonDownload, 0f)
        val protonFile = File(protonDir, protonAsset.name)
        downloadFile(protonAsset.browser_download_url, protonFile) { p -> onProgress(SetupStep.ProtonDownload, p) }
        onProgress(SetupStep.ProtonExtract, 0f)
        // .wcp is a zip container (Winlator Compatibility Package format)
        extractZip(protonFile, protonDir)

        // 3. Standalone FEX interpreter + x86_64 rootfs, for native Linux (non-Wine) games
        onProgress(SetupStep.FexDownload, 0f)
        val fexAsset = GithubReleaseResolver.resolveLatestAsset(
            repo = fexRepo,
            mustContain = listOf("aarch64"),
            mustNotContain = listOf("sources", ".deb")
        )
        val fexArchive = File(binDir, fexAsset.name)
        downloadFile(fexAsset.browser_download_url, fexArchive) { p -> onProgress(SetupStep.FexDownload, p) }
        if (fexArchive.name.endsWith(".tar.xz")) extractTarXz(fexArchive, binDir)
        else if (fexArchive.name.endsWith(".tar.gz") || fexArchive.name.endsWith(".tgz")) extractTarGz(fexArchive, binDir)
        fexArchive.delete()

        onProgress(SetupStep.RootfsDownload, 0f)
        val rootfsArchive = File(baseDir, "ubuntu_rootfs.tar.gz")
        downloadFile(x86_64RootfsUrl, rootfsArchive) { p -> onProgress(SetupStep.RootfsDownload, p) }
        onProgress(SetupStep.RootfsExtract, 0f)
        extractTarGz(rootfsArchive, rootfsDir)
        rootfsArchive.delete()

        onProgress(SetupStep.Done, 1f)
    }

    private fun downloadFile(url: String, dest: File, onProgress: (Float) -> Unit) {
        val req = Request.Builder().url(url).header("User-Agent", "FoxEmu-Setup").build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("Download failed: $url (${resp.code})")
            val body = resp.body ?: throw RuntimeException("Empty body: $url")
            val total = body.contentLength()
            var read = 0L
            body.byteStream().use { input ->
                FileOutputStream(dest).use { out ->
                    val buf = ByteArray(1 shl 16)
                    while (true) {
                        val n = input.read(buf)
                        if (n == -1) break
                        out.write(buf, 0, n)
                        read += n
                        if (total > 0) onProgress(read.toFloat() / total)
                    }
                }
            }
        }
    }

    private fun extractZip(zip: File, outDir: File) {
        ZipInputStream(zip.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(outDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { zis.copyTo(it) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun extractTarGz(archive: File, outDir: File) {
        GzipCompressorInputStream(archive.inputStream()).use { gz ->
            extractTar(gz, outDir)
        }
    }

    private fun extractTarXz(archive: File, outDir: File) {
        XZInputStream(archive.inputStream()).use { xz ->
            extractTar(xz, outDir)
        }
    }

    private fun extractTar(input: java.io.InputStream, outDir: File) {
        TarArchiveInputStream(input).use { tar ->
            var entry = tar.nextTarEntry
            while (entry != null) {
                val outFile = File(outDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { tar.copyTo(it) }
                    if (entry.mode and 0b001001001 != 0) outFile.setExecutable(true)
                }
                entry = tar.nextTarEntry
            }
        }
    }
}
