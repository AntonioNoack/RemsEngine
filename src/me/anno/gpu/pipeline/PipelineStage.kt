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
import org.joml.*
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
    val blendMode: BlendMode?,
    val depthMode: DepthMode,
    val writeDepth: Boolean,
    var cullMode: Int, // 0 = both, gl_front, gl_back
    val defaultShader: BaseShader
) : Saveable() {

    companion object {

        val defaultMaterial = Material()

        val matrixBuffer = MemoryUtil.memAllocFloat(16)
        val lastMaterial = HashMap<Shader, Material>(64)

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

        fun Matrix4f.mul4x3delta(m: Matrix4x3d, b: Vector3d) {

            // false = column major, however the labelling of these things is awkward
            // A_ji, as far, as I can see

            mul(

                m.m00().toFloat(),
                m.m01().toFloat(),
                m.m02().toFloat(),
                0f,

                m.m10().toFloat(),
                m.m11().toFloat(),
                m.m12().toFloat(),
                0f,

                m.m20().toFloat(),
                m.m21().toFloat(),
                m.m22().toFloat(),
                0f,

                (m.m30() - b.x).toFloat(),
                (m.m31() - b.y).toFloat(),
                (m.m32() - b.z).toFloat(),
                1f

            )

        }

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

    private val tmp3x3 = Matrix3f()

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
        var lastEntity: Entity? = null
        var lastMesh: Mesh? = null
        var lastShader: Shader? = null

        for (index in 0 until writeIndex) {

            // gl_Position = cameraMatrix * v4(localTransform * v4(pos,1),1)

            val request = drawRequests[index]
            val mesh = request.mesh
            val entity = request.entity
            val transform = entity.transform
            val materialIndex = request.materialIndex
            val material = mesh.materials.getOrNull(materialIndex) ?: defaultMaterial
            val baseShader = material.shader ?: defaultShader
            val renderer = request.renderer

            // mesh.draw(shader, material)
            val shader = baseShader.value
            shader.use()

            val previousMaterial = lastMaterial.put(shader, material)

            if (previousMaterial == null) {
                // todo add all things, the shader needs to know, e.g. light direction, strength, ...
                // (for the cheap shaders, which are not deferred)
                shader.m4x4("transform", cameraMatrix)
                // shader.m4x4("cameraMatrix", cameraMatrix)
                // shader.m4x3("worldTransform", ) // todo world transform could be used for procedural shaders...
                // todo local transform could be used for procedural shaders as well
            }

            val wt = transform.globalTransform
            shader.m4x3delta("localTransform", wt, cameraPosition)

            val invLocalUniform = shader["invLocalTransform"]
            if (invLocalUniform >= 0) {
                val invLocal = tmp3x3.set(
                    wt.m00().toFloat(), wt.m01().toFloat(), wt.m02().toFloat(),
                    wt.m10().toFloat(), wt.m11().toFloat(), wt.m12().toFloat(),
                    wt.m20().toFloat(), wt.m21().toFloat(), wt.m22().toFloat(),
                ).invert()
                shader.m3x3(invLocalUniform, invLocal)
            }

            if (previousMaterial != material) {
                // todo bind textures for the material
                // todo bind all default properties, e.g. colors, roughness, metallic, clear coat/sheen, ...
                for ((uniformName, valueType) in material.shaderOverrides) {
                    valueType.bind(shader, uniformName)
                }
            }

            shaderColor(shader, "tint", -1)

            // only if the entity or mesh changed
            // not if the material has changed
            // this updates the skeleton and such
            if (entity !== lastEntity || lastMesh !== mesh || lastShader !== shader) {
                renderer.defineVertexTransform(shader, entity, mesh)
                lastEntity = entity
                lastMesh = mesh
                lastShader = shader
            }

            mesh.draw(shader, materialIndex)

        }

        lastMaterial.clear()

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
        if (writeIndex >= drawRequests.size) {
            val request = DrawRequest(mesh, renderer, entity, materialIndex)
            drawRequests.add(request)
        } else {
            val request = drawRequests[writeIndex]
            request.mesh = mesh
            request.renderer = renderer
            request.entity = entity
            request.materialIndex = materialIndex
        }
        writeIndex++
    }

    override val className: String = "PipelineStage"
    override val approxSize: Int = 5
    override fun isDefaultValue(): Boolean = false

}