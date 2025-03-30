package me.anno.tests.mesh.gltf.writer

import me.anno.ecs.Entity
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.light.LightComponent
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths.TAU
import me.anno.mesh.gltf.GLTFWriter
import me.anno.utils.OS.desktop
import me.anno.utils.async.Callback
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    val scene = Entity()
    val numCameras = 6
    val mesh = LightComponent.shapeForTesting
    val bounds = mesh.getBounds()
    val dx = bounds.centerX
    val dy = bounds.centerY
    val dz = bounds.centerZ
    for (i in 0 until numCameras) {
        val angle = i * TAU / numCameras
        val child = Entity(scene)
            .setPosition(sin(angle) + dx, 1.0 + dy, cos(angle) + dz)
            .setRotation(-0.5f, angle.toFloat(), 0f)
        child.add(Camera().apply {
            name = "Camera[$i]"
            far = 5f
        })
    }
    // todo ortho camera in the engine is generally broken... :/
    val ortho = Entity(scene).setPosition(dx.toDouble(), dy.toDouble(), dz + 1.0)
    ortho.add(Camera().apply {
        name = "Ortho"
        isPerspective = false
        far = 5f
    })
    scene.add(MeshComponent(mesh))
    val dst = desktop.getChild("GLTF-Cameras.glb")
    dst.getParent().tryMkdirs()
    GLTFWriter().write(scene, dst, Callback.onSuccess {
        testSceneWithUI("GLTFWriter-Cameras", scene)
    })
}