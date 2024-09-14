package me.anno.tests.engine

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.prefab.PrefabCache.getPrefabSampleInstance
import me.anno.engine.ECSRegistry
import me.anno.io.files.Reference.getReference
import org.junit.jupiter.api.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DefaultAssetsTest {

    init {
        ECSRegistry.init()
    }

    @Test
    fun testMeshesExist() {
        for (name in listOf("Cube", "CylinderY", "UVSphere", "IcoSphere", "PlaneY")) {
            assertIs<Mesh>(getPrefabSampleInstance(getReference("meshes/$name.json")))
        }
    }

    @Test
    fun testMaterialsExist() {
        for (name in listOf("Default", "Mirror", "Golden", "Glass", "Black", "Emissive", "UVDebug")) {
            assertIs<Material>(getPrefabSampleInstance(getReference("materials/$name.json")))
        }
    }

    @Test
    fun testTexturesExist() {
        for (name in listOf("UVChecker", "Icon")) {
            assertTrue(getReference("textures/$name.png").exists)
        }
    }
}