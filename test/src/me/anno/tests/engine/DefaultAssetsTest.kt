package me.anno.tests.engine

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.MaterialBase
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.OfficialExtensions
import me.anno.io.files.Reference.getReference
import me.anno.utils.assertions.assertIs
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DefaultAssetsTest {

    @BeforeEach
    fun init() {
        OfficialExtensions.initForTests()
    }

    @Test
    fun testMeshesExist() {
        for (name in listOf("Cube", "CylinderY", "UVSphere", "IcoSphere", "PlaneY")) {
            assertIs(Mesh::class, PrefabCache[getReference("meshes/$name.json")].waitFor()?.sample!!)
        }
    }

    @Test
    fun testMaterialsExist() {
        for (name in listOf("Default", "Mirror", "Golden", "Glass", "Black", "Emissive", "UVDebug")) {
            assertIs(MaterialBase::class, PrefabCache[getReference("materials/$name.json")].waitFor()?.sample!!)
        }
    }

    @Test
    fun testTexturesExist() {
        for (name in listOf("UVChecker", "Icon")) {
            assertTrue(getReference("textures/$name.png").exists)
        }
    }
}