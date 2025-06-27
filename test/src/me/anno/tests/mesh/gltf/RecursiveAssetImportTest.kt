package me.anno.tests.mesh.gltf

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.AssetImport
import me.anno.io.files.FileFileRef.Companion.createTempFolder
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test

class RecursiveAssetImportTest {

    class RecursiveAsset : PrefabSaveable() {
        var reference: FileReference = InvalidRef
    }

    @Test
    fun testRecursiveAssetImport() {
        registerCustomClass(Entity()) // to avoid the useless warning
        registerCustomClass(RecursiveAsset())

        val srcFolder = createTempFolder("RecursiveAsset")
        val dstFolder = createTempFolder("RecursiveAsset2")

        val fileA = srcFolder.getChild("FileA.json")
        val fileB = srcFolder.getChild("FileB.json")
        val prefabA = Prefab("RecursiveAsset")
        prefabA["reference"] = fileB
        val prefabB = Prefab("RecursiveAsset")
        prefabB["reference"] = fileA

        fileA.writeText(prefabA.toString())
        fileB.writeText(prefabB.toString())

        AssetImport.deepCopyImport(dstFolder, listOf(fileA), null)

        assertEquals(2, dstFolder.listChildren().size)

        srcFolder.delete()
        dstFolder.delete()

        Engine.requestShutdown()
    }
}