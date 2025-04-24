package me.anno.tests.terrain.v2

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.AutoTileableMaterial
import me.anno.ecs.components.mesh.terrain.v2.TriTerrainRenderer
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.noise.PerlinNoise
import me.anno.utils.OS.pictures
import me.anno.utils.OS.res
import me.anno.utils.callbacks.F2F
import org.joml.Vector4f

// todo gpu-accelerated editing
// done LODs
// partially-done terrain loading/unloading
// done (de)serialization

fun main() {

    OfficialExtensions.initForTests()

    val material = AutoTileableMaterial()
    material.diffuseMap = pictures.getChild("textures/grass.jpg")

    val scene = Entity("Scene")
    val terrainRenderer = TriTerrainRenderer()
    terrainRenderer.material = material

    val noise = PerlinNoise(1234L, 15, 0.47f, -2100f, 2100f, Vector4f(1f / 6000f))
    val height = F2F(noise::getSmooth)
    val terrainLoader = TerrainLoaderV2(height)

    // todo bug: that circle is sometimes invisible, why???
    val editor = TerrainEditModeV2()
    editor.cursor = Entity("Cursor", scene)
        .add(MeshComponent(res.getChild("meshes/TeleportCircle.glb")))

    scene.add(terrainRenderer)
    scene.add(terrainLoader)
    scene.add(editor)

    terrainLoader.init(-4, -4, 4, 4)
    testSceneWithUI("TerrainV2", scene) {
        EditorState.select(editor)
    }
}