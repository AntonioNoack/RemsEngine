package me.anno.tests.io.files

import me.anno.io.config.ConfigBasics
import me.anno.io.files.FileReference
import me.anno.io.files.Reference.getReference
import me.anno.utils.OS
import me.anno.utils.assertions.assertEquals
import me.anno.utils.files.LocalFile.toGlobalFile
import org.junit.jupiter.api.Test

class LocalFileTests {

    private val registry = HashMap<String, () -> FileReference>()
    private fun register(name: String, file: FileReference) = register(name) { file }
    private fun register(name: String, file: () -> FileReference) {
        registry.put("$$name$", file)
    }

    init {
        register("CONFIG") { ConfigBasics.configFolder }
        register("CACHE") { ConfigBasics.cacheFolder }
        register("DOWNLOADS", OS.downloads)
        register("DOCUMENTS", OS.documents)
        register("DESKTOP", OS.desktop)
        register("PICTURES", OS.pictures)
        register("VIDEOS", OS.videos)
        register("MUSIC", OS.music)
        register("HOME", OS.home)
    }

    @Test
    fun testLegacyFormat() {
        val subName = "A/b.txt"
        val workspace = OS.documents.getChild("Workspace")

        for ((key, value) in registry) {
            val file = "$key/$subName".toGlobalFile(workspace)
            assertEquals(value().getChild(subName), file)
        }
    }

    @Test
    fun testURLs() {
        val url = "https://phychi.com"
        val workspace = OS.documents.getChild("Workspace")

        val file = getReference(url)
        assertEquals(url, file.absolutePath)
        val localPath = file.toLocalPath()
        assertEquals(url, localPath)
        val globalFile = url.toGlobalFile(workspace)
        assertEquals(file, globalFile)
    }

    @Test
    fun testLinuxFiles() {
        val path = "/tmp/documents"
        val file = path.toGlobalFile()
        assertEquals(path, file.absolutePath)
    }

    @Test
    fun testWindowsFiles() {
        val path = "C:/tmp/documents"
        val file = path.toGlobalFile()
        assertEquals(path, file.absolutePath)
    }

    @Test
    fun testLoadingFile() {
        val path = "C:/tmp/documents"
        val file = path.toGlobalFile()
        assertEquals(path, file.absolutePath)
    }
}