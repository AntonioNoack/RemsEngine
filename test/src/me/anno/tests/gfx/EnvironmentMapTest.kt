package me.anno.tests.gfx

import me.anno.ecs.Entity
import me.anno.ecs.components.light.EnvironmentMap
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.ecs.components.mesh.utils.MeshJoiner
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.texture.Clamping
import me.anno.image.raw.ByteImage
import me.anno.image.raw.ByteImageFormat
import me.anno.maths.Maths.sq
import me.anno.utils.OS
import me.anno.utils.structures.Collections.crossMap
import org.joml.Matrix4x3f
import org.joml.Vector2i

fun main() {
    OfficialExtensions.initForTests()

    // test environment map
    val scene = Entity("Scene")
        .add(metalRoughness())
        .add(Skybox())

    Entity("Helmet", scene)
        .setPosition(2.8, 0.0, 0.0)
        .add(MeshComponent(OS.downloads.getChild("3d/DamagedHelmet.glb")))
        .add(EnvironmentMap())

    Entity("Middle", scene)
        .add(MeshComponent(IcosahedronModel.createIcosphere(3), Material.metallic(-1, 0f)))
        .add(EnvironmentMap())

    testSceneWithUI("EnvironmentMap", scene)
}

fun metalRoughness(): MeshComponent {
    val sphereMesh = IcosahedronModel.createIcosphere(2)
    val i = 8
    val s = 2.8f
    val di = 0.5f * s
    val list = (-i until i).toList()
    val indices = list.crossMap(list, ArrayList(sq(list.size))) { x, y -> Vector2i(x, y) }
    val mesh = object : MeshJoiner<Vector2i>(false, false, false) {
        override fun getMesh(element: Vector2i): Mesh = sphereMesh
        override fun getTransform(element: Vector2i, dst: Matrix4x3f) {
            val (x, z) = element
            dst.setTranslation(x * s + di, 0f, z * s + di)
        }
    }.join(indices)
    val bounds = mesh.getBounds()
    val pos = mesh.positions!!
    val uvs = FloatArray(pos.size / 3 * 2)
    val x0 = bounds.minX
    val dx = 1f / bounds.deltaX
    for (j in 0 until pos.size / 3) {
        val i3 = j * 3
        val i2 = j * 2
        uvs[i2] = (pos[i3] - x0) * dx
        uvs[i2 + 1] = (pos[i3 + 2] - x0) * dx
    }
    mesh.uvs = uvs
    val mat = Material()
    mat.roughnessMinMax.set(0f, 1f)
    mat.metallicMinMax.set(0f, 1f)
    val texValues = ByteArray(16) { (it * 255 / 15).toByte() }
    mat.clamping = Clamping.CLAMP
    mat.metallicMap = ByteImage(16, 1, ByteImageFormat.R, texValues).ref
    mat.roughnessMap = ByteImage(1, 16, ByteImageFormat.R, texValues).ref
    mesh.materials = listOf(mat.ref)
    return MeshComponent(mesh)
}