package me.anno.image.exr

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files

object NativeLoader {
    fun load(resourcePath: String, libName: String) {
        try {
            val input = NativeLoader::class.java.getResourceAsStream(resourcePath)
                ?: throw FileNotFoundException("Library not found in resources: $resourcePath")
            input.use { input ->
                val tmp = File.createTempFile(libName, ".so")
                tmp.deleteOnExit()
                tmp.outputStream().use { os ->
                    input.copyTo(os)
                }
                System.load(tmp.absolutePath)
            }
        } catch (e: IOException) {
            throw RuntimeException("Failed to load native library '$libName'", e)
        }
    }
}