package me.anno.tests.ecs

import me.anno.ecs.Entity
import me.anno.ecs.components.light.AmbientLight
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.LightSpawner
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.shaders.Skybox
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.pipeline.Pipeline
import me.anno.maths.Maths.TAU
import me.anno.mesh.Shapes.flatCube
import me.anno.ui.editor.color.spaces.HSLuv
import me.anno.utils.OS
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin

/**
 * recreate colorful test scene with 100k lights from ages ago :3,
 *
 * just with modern methods :3 (faster, more direct, easier API)
 * */
fun main() {

    ECSRegistry.init()

    val scene = Entity()
    scene.add(Skybox())

    val truck = Entity("VOX/Truck", scene)
    truck.add(MeshComponent(OS.downloads.getChild("MagicaVoxel/vox/truck.vox")))

    val lights = Entity("Lights", scene)
    lights.add(AmbientLight().apply { color.set(0.25f) })

    val sun = Entity("Sun", lights)
    sun.add(DirectionalLight().apply { shadowMapCascades = 1; color.set(3f) })
    sun.position = sun.position.set(0.0, -10.0, 0.0)
    sun.scale = sun.scale.set(50.0)
    sun.rotation = sun.rotation.rotateY(0.8).rotateX(-0.8)

    val ringOfLights = Entity("Ring Of Lights", lights)
    ringOfLights.add(object : LightSpawner() {

        // bottleneck seems to be the data transfer from CPU to GPU

        val superRings = 300
        val elementSize = 10.0
        val lightLevel = 20f
        val numColors = 3
        val colorRepetitions = 3
        val scale = Vector3d(elementSize)
        val colors = Array(numColors) {
            val angle = it / numColors.toFloat()
            val color = HSLuv.toRGB(Vector3f(angle, 1f, 0.7f)).mul(lightLevel)
            PointLight().apply { this.color.set(color) }
        }
        val positionY = elementSize * 0.5

        var firstTurn = true
        override fun fill(pipeline: Pipeline, entity: Entity, clickId: Int): Int {
            var k = 0
            val dst = pipeline.lightStage.instanced[colors[0]]
            val nr = numColors * colorRepetitions
            val sc = scale.x
            val isc = 1.0 / scale.x
            val iws = 1.0 / (sc * RenderState.worldScale)
            val dx = isc * RenderState.cameraPosition.x
            val dy = isc * RenderState.cameraPosition.y
            val dz = isc * RenderState.cameraPosition.z
            for (j in 0 until superRings) {
                val radius = 50.0 * (1.0 + j * 0.1)
                val ringLightCount = (radius * 0.5).toInt()
                for (i in 0 until ringLightCount) {
                    val light = colors[(nr * i / ringLightCount) % numColors]
                    val (drawMatrix, invCamSpaceMatrix) = getTransform(k++)
                    if (firstTurn) {
                        val angle = TAU * i.toDouble() / ringLightCount
                        drawMatrix.setTranslation(radius * cos(angle), positionY, radius * sin(angle))
                        drawMatrix.scale(scale)
                    }
                    // same as
                    // .set4x3delta(drawMatrix, RenderState.cameraPosition, RenderState.worldScale).invert()
                    val iwz = iws.toFloat()
                    invCamSpaceMatrix.set(
                        iwz, 0f, 0f,
                        0f, iwz, 0f,
                        0f, 0f, iwz,
                        (dx - drawMatrix.m30 * isc).toFloat(),
                        (dy - drawMatrix.m31 * isc).toFloat(),
                        (dz - drawMatrix.m32 * isc).toFloat()
                    )
                    dst.add(light, drawMatrix, invCamSpaceMatrix)
                }
            }
            firstTurn = false
            return clickId
        }
    })

    // add a floor for testing
    val cubePath = flatCube.front.ref
    val floor = Entity("Floor", scene)
    floor.position = Vector3d(0.0, -50.0, 0.0)
    floor.scale = Vector3d(2000.0, 50.0, 2000.0)
    val floorMesh1E = Entity("Metallic", floor)
    floorMesh1E.position = Vector3d(0.5, 0.0, 0.0)
    floorMesh1E.scale = Vector3d(0.5, 1.0, 1.0)
    val floorMesh2E = Entity("Rough", floor)
    floorMesh2E.position = Vector3d(-0.5, 0.0, 0.0)
    floorMesh2E.scale = Vector3d(0.5, 1.0, 1.0)
    floorMesh2E.add(MeshComponent(cubePath))
    floorMesh1E.add(MeshComponent(cubePath).apply {
        materials = listOf(Material().apply {
            metallicMinMax.set(1f)
            roughnessMinMax.set(0.2f)
        }.ref)
    })

    testSceneWithUI("Lights2", scene)
}