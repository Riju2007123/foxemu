package com.fox.emu.driver

import java.io.File

object TurnipLoader {
    init { System.loadLibrary("driver_loader") }

    external fun loadDriver(driverPath: String, icdJsonPath: String): Boolean
    external fun setEnv(key: String, value: String)

    /**
     * driverSoName must match whatever .so the resolved Turnip zip actually
     * contains (verify after extraction — Banners-Turnip ships
     * libvulkan_freedreno.so in most archives; some forks differ).
     */
    fun init(appFilesDir: File, driverSoName: String = "libvulkan_freedreno.so"): Boolean {
        val driverFile = File(appFilesDir, "drivers/$driverSoName")
        val icdFile = File(appFilesDir, "drivers/turnip.icd.json")
        if (!driverFile.exists()) {
            throw IllegalStateException(
                "Driver missing at ${driverFile.path} — run Settings > Start Setup first"
            )
        }
        // Recommended by Banners-Turnip release notes for the 710/720 test build
        setEnv("TU_DEBUG", "sysmem")
        setEnv("WRAPPER_BLIT", "1")
        return loadDriver(driverFile.absolutePath, icdFile.absolutePath)
    }
}
