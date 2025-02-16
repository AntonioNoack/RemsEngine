package me.anno.tests.engine.light

import me.anno.Build
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.LightSpawner
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.pipeline.LightData
import me.anno.gpu.pipeline.Pipeline
import me.anno.maths.Maths.TAU
import me.anno.mesh.Shapes.flatCube
import me.anno.ui.editor.color.spaces.HSLuv
import me.anno.utils.OS
import me.anno.utils.structures.lists.Lists.createArrayList
import org.joml.AABBd
import org.joml.Matrix4x3f
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin

// same as .set4x3delta(drawMatrix, RenderState.cameraPosition).invert()
fun Matrix4x3f.setTranslateScaleInverse(px: Double, py: Double, pz: Double, scale: Double): Matrix4x3f {
    val isc = 1f / scale
    val iws = 1f / scale.toFloat()
    val dx = isc * RenderState.cameraPosition.x
    val dy = isc * RenderState.cameraPosition.y
    val dz = isc * RenderState.cameraPosition.z
    return set(
        iws, 0f, 0f,
        0f, iws, 0f,
        0f, 0f, iws,
        (dx - px * isc).toFloat(),
        (dy - py * isc).toFloat(),
        (dz - pz * isc).toFloat()
    )
}

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

    Entity("VOX/Truck", scene)
        .add(MeshComponent(OS.downloads.getChild("MagicaVoxel/vox/truck.vox")))

    val lights = Entity("Lights", scene)

    val sun = Entity("Sun", lights)
        .add(DirectionalLight().apply { shadowMapCascades = 1; color.set(3f) })
        .setPosition(0.0, -10.0, 0.0)
        .setScale(50f)
    sun.transform.localRotation = Quaternionf().rotateY(0.8f).rotateX(-0.8f)

    val ringOfLights = Entity("Ring Of Lights", lights)
    ringOfLights.add(object : LightSpawner() {

        // bottleneck seems to be the data transfer from CPU to GPU

        val superRings = 300
        val elementSize = 10.0
        val lightLevel = 20f
        val numColors = 3
        val colorRepetitions = 3
        val scale = Vector3d(elementSize)
        val colors = createArrayList(numColors) {
            val angle = it / numColors.toFloat()
            val color = HSLuv.toRGB(Vector3f(angle, 1f, 0.7f)).mul(lightLevel)
            PointLight().apply { this.color.set(color) }
        }
        val positionY = elementSize * 0.5

        var firstTurn = true
        override fun fill(pipeline: Pipeline, instancedLights: LightData, transform: Transform) {
            // to do acceleration structure for frustum tests?
            var k = 0
            val dst = instancedLights[colors[0]]
            val nr = numColors * colorRepetitions
            val sc = scale.x
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
                        drawMatrix.scale(scale.x.toFloat(), scale.y.toFloat(), scale.z.toFloat())
                    }
                    val px = drawMatrix.m30
                    val py = drawMatrix.m31
                    val pz = drawMatrix.m32
                    aabb.set(px, py, pz).addMargin(sc)
                    if (frustum.isVisible(aabb)) {
                        // same as
                        // .set4x3delta(drawMatrix, RenderState.cameraPosition, RenderState.worldScale).invert()
                        invCamSpaceMatrix.setTranslateScaleInverse(px, py, pz, scale.x)
                        dst.add(light, drawMatrix, invCamSpaceMatrix)
                    }
                }
            }
            firstTurn = false
        }
    })

    // add a floor for testing
    val cube = flatCube.front
    val floor = Entity("Floor", scene)
        .setPosition(0.0, -50.0, 0.0)
        .setScale(2000f, 50f, 2000f)
    Entity("Metallic", floor)
        .setPosition(0.5, 0.0, 0.0)
        .setScale(0.5f, 1f, 1f)
        .add(MeshComponent(cube, Material.metallic(-1, 0.2f)))
    Entity("Rough", floor)
        .setPosition(-0.5, 0.0, 0.0)
        .setScale(0.5f, 1f, 1f)
        .add(MeshComponent(cube))

    testSceneWithUI("Lights2", scene)
}