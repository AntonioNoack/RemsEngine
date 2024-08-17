package me.anno.tests.engine

import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.AssetImport.deepCopyImport
import me.anno.io.files.FileReference
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads
import kotlin.test.assertEquals

object DeepImport {

    data class Folder(val folders: Map<String, Folder>, val files: List<String>) {
        constructor(names: List<String>) : this(emptyMap(), names)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        testRunDeepImport()
    }

    fun testRunDeepImport() {
        OfficialExtensions.initForTests()
        val dst = desktop.getChild("deepImport")
        dst.delete()
        // todo who and why is copying b.png and g.png?
        deepCopyImport(dst, listOf(downloads.getChild("3d/DamagedHelmet.glb")), null)
        assertEquals(
            Folder(
                mapOf(
                    "materials" to Folder(listOf("Material_MR.json", "Scene.json")),
                    "meshes" to Folder(listOf("mesh_helmet_LP_13930damagedHelmet.json")),
                    "textures" to Folder((0..4).map { "$it.jpg" })
                ), listOf("DamagedHelmet.json")
            ),
            readFolderStructure(dst)
        )
    }

    fun readFolderStructure(file: FileReference): Folder {
        val children = file.listChildren()
        return Folder(
            children.filter { it.isDirectory }
                .associate { it.name to readFolderStructure(it) },
            children.filter { !it.isDirectory }
                .map { it.name }.sorted()
        )
    }
}