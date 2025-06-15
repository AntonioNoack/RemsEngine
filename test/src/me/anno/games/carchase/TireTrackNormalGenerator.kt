package me.anno.games.carchase

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material.Companion.defaultMaterial
import me.anno.ecs.components.mesh.material.MaterialCache
import me.anno.engine.EngineBase.Companion.workspace
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.engine.ui.render.Renderers.attributeRenderers
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.DepthMode
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.image.ImageScale
import me.anno.io.files.Reference.getReference
import me.anno.jvm.HiddenOpenGLContext
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.length
import me.anno.maths.geometry.MeshSplitter
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.OS.desktop
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import org.joml.AABBf
import org.joml.Matrix4f
import org.joml.Matrix4x3f
import kotlin.math.abs
import kotlin.math.atan2

val tracksDecalFile = desktop.getChild("Tracks.png")

/**
 * input: wheel mesh
 * output: normal texture to be used as a decal
 * */
fun main() {
    OfficialExtensions.initForTests()
    val debug = false
    if (!debug) HiddenOpenGLContext.createOpenGL()
    // given a wheel, find its max and minimum radius
    //  then for each part of its radius, raycast/render its normal
    //  -> render the mesh, but project it onto a flat surface first
    //  based on the angle each vertex is on
    //  to make sure, we can split the vertices, split the mesh in two parts vertically
    val project = getReference("C:/Users/Antonio/Documents/RemsEngine/Construction")
    workspace = project
    val source = project.getChild("Vehicles/meshes/SM_Veh_Roller_01_Wheel_rl-002.json")
    val mesh = MeshCache[source] as Mesh
    val (p0, _, p1, _) = MeshSplitter.split(mesh) { it.z }
    val parts = listOf(p0, p1)
    for (part in parts) unroll(part)
    // to do wait for textures? -> not used in our case
    val bounds = AABBf()
    for (part in parts) bounds.union(part.getBounds())

    val width = 512
    val height = 512

    val symmetry = 6 * 3

    val fb = Framebuffer("normals", width, height, TargetType.UInt8x4, DepthBufferType.INTERNAL)

    fun render() {

        val extra = -PIf / 2f

        val transform = Matrix4f()
            .scale(1f, symmetry.toFloat(), 0.1f)
            .rotateX(-PIf / 2f + extra)

        val localTransform = Matrix4x3f()
        localTransform.ortho(
            bounds.minX, bounds.maxX,
            bounds.minY, bounds.maxY,
            bounds.minZ, bounds.maxZ
        ).rotateLocalX(-extra)

        val renderer = attributeRenderers[DeferredLayerType.NORMAL]
        useFrame(fb, renderer) {
            GFXState.depthMode.use(DepthMode.FORWARD_CLOSER) {
                fb.clearDepth()

                val shader = pbrModelShader.value
                shader.use()

                // define camera matrix and such
                //  - transform, localTransform
                shader.m4x4("transform", transform)
                shader.m4x3("localTransform", localTransform)

                for (part in parts) {
                    shader.v1i("hasVertexColors", part.hasVertexColors)
                    for (materialId in 0 until part.numMaterials) {
                        val material = MaterialCache[part.materials.getOrNull(materialId)] ?: defaultMaterial
                        material.bind(shader)
                        part.draw(null, shader, materialId)
                    }
                }
            }
        }
    }

    if (!debug) {
        render()
        fb.getTexture0()
            .write(tracksDecalFile)
    } else if (false) {
        val scene = Entity()
            .add(MeshComponent(p0))
            .add(MeshComponent(p1))
        testSceneWithUI("WheelSplit", scene)
    } else {
        testDrawing("Tracks") {
            render()
            val (w, h) = ImageScale.scaleMax(
                fb.width, fb.height,
                it.width, it.height
            )
            drawTexture(it.x, it.y, w, h, fb.getTexture0())
        }
    }

    Engine.requestShutdown()
}

fun unroll(mesh: Mesh) {
    // bounds should be on either side of z -> clamp angle to that side
    val posZ = mesh.getBounds().centerZ > 0f
    unroll(mesh.positions, 3, posZ)
    unroll(mesh.normals, 3, posZ)
    unroll(mesh.tangents, 4, posZ)
    mesh.invalidateGeometry()
}

fun unroll(values: FloatArray?, stride: Int, posZ: Boolean) {
    values ?: return
    val center = if (posZ) PIf / 2 else -PIf / 2
    forLoopSafely(values.size, stride) { idx ->
        val iy = values[idx + 1]
        val iz = values[idx + 2]
        var angle = atan2(iz, iy)
        if (abs(angle - center) > PIf) {
            if (angle > center) angle -= TAUf
            else angle += TAUf
        }
        values[idx + 1] = length(iy, iz) // depth becomes y
        values[idx + 2] = angle // angle (run length) becomes z
        // x (width) stays
    }
}