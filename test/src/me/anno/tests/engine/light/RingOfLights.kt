package me.anno.tests.engine.light

import me.anno.Build
import me.anno.ecs.Entity
import me.anno.ecs.components.light.AmbientLight
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.LightSpawner
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.pipeline.Pipeline
import me.anno.maths.Maths.TAU
import me.anno.mesh.Shapes.flatCube
import me.anno.ui.editor.color.spaces.HSLuv
import me.anno.utils.OS
import org.joml.AABBd
import org.joml.Quaterniond
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
    Build.isDebug = false // disable glGetError() calls

    val scene = Entity()
    // scene.add(SkyboxBase().apply { skyColor.set(0f) })

    val truck = Entity("VOX/Truck", scene)
    truck.add(MeshComponent(OS.downloads.getChild("MagicaVoxel/vox/truck.vox")))

    val lights = Entity("Lights", scene)
    lights.add(AmbientLight().apply { color.set(0f) })

    val sun = Entity("Sun", lights)
    sun.add(DirectionalLight().apply { shadowMapCascades = 1; color.set(3f) })
    sun.setPosition(0.0, -10.0, 0.0)
    sun.setScale(50.0)
    sun.transform.localRotation = Quaterniond().rotateY(0.8).rotateX(-0.8)

    val ringOfLights = Entity("Ring Of Lights", lights)
    ringOfLights.add(object : LightSpawner() {

        // bottleneck seems to be the data transfer from CPU to GPU

        val superRings = 300
        val elementSize = 10.0
        val lightLevel = 2000f
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
            // to do acceleration structure for frustum tests?
            var k = 0
            val dst = pipeline.lightStage.instanced[colors[0]]
            val nr = numColors * colorRepetitions
            val sc = scale.x
            val isc = 1.0 / scale.x
            val iws = (1.0 / (sc * RenderState.worldScale)).toFloat()
            val dx = isc * RenderState.cameraPosition.x
            val dy = isc * RenderState.cameraPosition.y
            val dz = isc * RenderState.cameraPosition.z
            val aabb = AABBd()
            val frustum = pipeline.frustum
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
                    val px = drawMatrix.m30
                    val py = drawMatrix.m31
                    val pz = drawMatrix.m32
                    aabb.setMin(px - sc, py - sc, pz - sc)
                    aabb.setMax(px + sc, py + sc, pz + sc)
                    if (frustum.isVisible(aabb)) {
                        // same as
                        // .set4x3delta(drawMatrix, RenderState.cameraPosition, RenderState.worldScale).invert()
                        invCamSpaceMatrix.set(
                            iws, 0f, 0f,
                            0f, iws, 0f,
                            0f, 0f, iws,
                            (dx - px * isc).toFloat(),
                            (dy - py * isc).toFloat(),
                            (dz - pz * isc).toFloat()
                        )
                        dst.add(light, drawMatrix, invCamSpaceMatrix)
                    }
                }
            }
            firstTurn = false
            return clickId
        }
    })

    // add a floor for testing
    val cubePath = flatCube.front.ref
    val floor = Entity("Floor", scene)
    floor.setPosition(0.0, -50.0, 0.0)
    floor.setScale(2000.0, 50.0, 2000.0)
    val floorMesh1E = Entity("Metallic", floor)
    floorMesh1E.setPosition(0.5, 0.0, 0.0)
    floorMesh1E.setScale(0.5, 1.0, 1.0)
    val floorMesh2E = Entity("Rough", floor)
    floorMesh2E.setPosition(-0.5, 0.0, 0.0)
    floorMesh2E.setScale(0.5, 1.0, 1.0)
    floorMesh2E.add(MeshComponent(cubePath))
    floorMesh1E.add(MeshComponent(cubePath).apply {
        materials = listOf(Material().apply {
            metallicMinMax.set(1f)
            roughnessMinMax.set(0.2f)
        }.ref)
    })

    testSceneWithUI("Lights2", scene)
}