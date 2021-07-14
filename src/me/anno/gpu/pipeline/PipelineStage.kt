package me.anno.gpu.pipeline

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.RendererComponent
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX.shaderColor
import me.anno.gpu.RenderState
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.Shader
import me.anno.io.Saveable
import org.joml.Matrix4fc
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.lwjgl.opengl.GL21.glUniformMatrix4x3fv
import org.lwjgl.system.MemoryUtil

/**
 * sorting: +1 = front to back, 0 = no sorting, -1 = back to front
 * todo teilgraphen könnten auch AABBs haben, und dann könnten wir frühzeitig die DrawCalls eliminieren
 * (das wäre notwendig für rießige Szenen)
 * */
class PipelineStage(
    val name: String,
    val sorting: Sorting,
    val blendMode: BlendMode,
    val useDepth: Boolean,
    val inversedDepth: Boolean,
    val cullMode: Int, // 0 = both, gl_front, gl_back
    val defaultShader: BaseShader
) : Saveable() {

    val depthMode = if (useDepth) {
        if (inversedDepth) {
            DepthMode.GREATER
        } else {
            DepthMode.LESS
        }
    } else DepthMode.ALWAYS

    companion object {
        val matrixBuffer = MemoryUtil.memAllocFloat(16)
        val lastMaterial = HashMap<Shader, Material>(64)
    }

    enum class Sorting {
        NO_SORTING,
        FRONT_TO_BACK,
        BACK_TO_FRONT
    }

    class DrawRequest(
        var mesh: Mesh,
        var renderer: RendererComponent,
        var entity: Entity,
        var materialIndex: Int
    )

    fun bindDraw(cameraMatrix: Matrix4fc, cameraPosition: Vector3d) {
        RenderState.blendMode.use(blendMode) {
            RenderState.depthMode.use(depthMode) {
                RenderState.cullMode.use(cullMode) {
                    draw(cameraMatrix, cameraPosition)
                }
            }
        }
    }

    fun draw(cameraMatrix: Matrix4fc, cameraPosition: Vector3d) {

        // todo bind the blending mode

        // the dotViewDir may be easier to calculate, and technically more correct, but it has one major flaw:
        // it changes when the cameraDirection is changing. This ofc is not ok, since it would resort the entire list,
        // and that's expensive

        // todo sorting function, that also uses the materials, so we need to switch seldom?

        when (sorting) {
            Sorting.NO_SORTING -> {
            }
            Sorting.FRONT_TO_BACK -> {
                drawRequests.sortBy {
                    it.entity.transform.distanceSquaredGlobally(cameraPosition)
                }
            }
            Sorting.BACK_TO_FRONT -> {
                drawRequests.sortBy {
                    it.entity.transform.distanceSquaredGlobally(cameraPosition)
                }
            }
        }

        // todo apply view-space culling
        // todo also tree-space culling may be useful, but we'd have to keep the tree structure somehow, while still being able to sort stuff
        for (index in 0 until writeIndex) {

            // gl_Position = cameraMatrix * v4(localTransform * v4(pos,1),1)

            val request = drawRequests[index]
            val mesh = request.mesh
            val entity = request.entity
            val transform = entity.transform
            val materialIndex = request.materialIndex
            val material = mesh.materials[materialIndex]
            val baseShader = material.shader ?: defaultShader
            val renderer = request.renderer

            // mesh.draw(shader, material)
            val shader = baseShader.value
            shader.use()

            val previousMaterial = lastMaterial.put(shader, material)

            if (previousMaterial == null) {
                // todo add all things, the shader needs to know, e.g. light direction, strength, ...
                // (for the cheap shaders, which are not deferred)
                shader.m4x4("cameraMatrix", cameraMatrix)
                // shader.m4x3("worldTransform", ) // todo world transform could be used for procedural shaders...
                // todo local transform could be used for procedural shaders as well
            }

            val wt = transform.globalTransform
            shader.m4x3delta("localTransform", wt, cameraPosition)

            if (previousMaterial != material) {
                // todo bind textures for the material
                // todo bind all default properties, e.g. colors, roughness, metallic, clear coat/sheen, ...
                for ((uniformName, valueType) in material.shaderOverrides) {
                    valueType.bind(shader, uniformName)
                }
            }

            shaderColor(shader, "tint", -1)

            // todo only if the entity has changed (?), also if the mesh has changed?
            renderer.defineVertexTransform(shader, entity, mesh)

            mesh.draw(shader, materialIndex)

        }

        lastMaterial.clear()

    }

    fun Shader.v3delta(location: String, a: Matrix4x3d, b: Vector3d) {
        val uniformIndex = this[location]
        if (uniformIndex >= 0) {
            v3(uniformIndex, (a.m30() - b.x).toFloat(), (a.m31() - b.y).toFloat(), (a.m32() - b.z).toFloat())
        }
    }

    fun Shader.v3delta(location: String, a: Vector3d, b: Vector3d) {
        val uniformIndex = this[location]
        if (uniformIndex >= 0) {
            v3(uniformIndex, (a.x - b.x).toFloat(), (a.y - b.y).toFloat(), (a.z - b.z).toFloat())
        }
    }

    /**
     * uploads the transform, minus some offset, to the GPU uniform <location>
     * the delta ensures, that we don't have to calculate high-precision numbers on the GPU
     * */
    fun Shader.m4x3delta(location: String, m: Matrix4x3d, b: Vector3d) {
        val uniformIndex = this[location]
        if (uniformIndex >= 0) {

            // false = column major, however the labelling of these things is awkward
            // A_ji, as far, as I can see
            matrixBuffer.limit(12)
            matrixBuffer.position(0)
            matrixBuffer.put(m.m00().toFloat())
            matrixBuffer.put(m.m01().toFloat())
            matrixBuffer.put(m.m02().toFloat())

            matrixBuffer.put(m.m10().toFloat())
            matrixBuffer.put(m.m11().toFloat())
            matrixBuffer.put(m.m12().toFloat())

            matrixBuffer.put(m.m20().toFloat())
            matrixBuffer.put(m.m21().toFloat())
            matrixBuffer.put(m.m22().toFloat())

            matrixBuffer.put((m.m30() - b.x).toFloat())
            matrixBuffer.put((m.m31() - b.y).toFloat())
            matrixBuffer.put((m.m32() - b.z).toFloat())

            matrixBuffer.position(0)

            glUniformMatrix4x3fv(uniformIndex, false, matrixBuffer)

        }
    }

    var writeIndex = 0
    val drawRequests = ArrayList<DrawRequest>()

    fun reset() {
        // there is too much space
        if (writeIndex < drawRequests.size / 2) {
            drawRequests.clear()
        }
        writeIndex = 0
    }

    fun add(renderer: RendererComponent, mesh: Mesh, entity: Entity, materialIndex: Int) {
        if (writeIndex < drawRequests.size) {
            val request = DrawRequest(mesh, renderer, entity, materialIndex)
            drawRequests.add(request)
            writeIndex++
        } else {
            val request = drawRequests[writeIndex]
            request.mesh = mesh
            request.renderer = renderer
            request.entity = entity
            request.materialIndex = materialIndex
        }
    }

    override val className: String = "PipelineStage"
    override val approxSize: Int = 5
    override fun isDefaultValue(): Boolean = false

}