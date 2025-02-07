package me.anno.gpu.pipeline.occlusion

import me.anno.engine.ui.render.RenderState
import me.anno.gpu.DepthMode
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.BufferUsage
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.ClickIdBoundsArray
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.transparency.AttachedDepthPass
import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.mesh.Shapes
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.maps.LazyMap
import org.joml.Vector3i
import org.lwjgl.opengl.GL46C.GL_DRAW_INDIRECT_BUFFER
import org.lwjgl.opengl.GL46C.glDrawArraysIndirect
import org.lwjgl.opengl.GL46C.glDrawElementsIndirect

/**
 * draws globalAABBs onto depth buffer of previous frame to confirm whether a mesh is visible;
 * then compacts instanceId-data on GPU (CPU-latency-free),
 * and draws that data using modern OpenGL API (4.2, so 2011 aka 14 years old)
 * */
class BoxOcclusionCulling : AttachedDepthPass() {

    companion object {
        private val boxShader = Shader(
            "gpuBoxCulling", listOf(
                // attributes per vertex; could be packed into the shader
                Variable(GLSLType.V3F, "coords", VariableMode.ATTR),
                // these attributes are per-instance
                Variable(GLSLType.V3F, "min", VariableMode.ATTR),
                Variable(GLSLType.V1I, "clickIds", VariableMode.ATTR),
                Variable(GLSLType.V3F, "max", VariableMode.ATTR),
                Variable(GLSLType.M4x4, "transform")
            ), "" +
                    // always draw the backside of the AABB
                    "vec3 getVertex(int i){\n" +
                    "   bool minX = (i & 1) == 0;\n" +
                    "   bool minY = (i & 2) == 0;\n" +
                    "   bool minZ = (i & 4) == 0;\n" +
                    "   return vec3(minX ? min.x : max.x, minY ? min.y : max.y, minZ ? min.z : max.z);\n" +
                    "}\n" +
                    // todo limit max depth to reasonable levels. We don't want depth-culling.
                    "void main() {\n" +
                    "   vec3 pos = mix(min, max, coords);\n" +
                    "   gl_Position = matMul(transform, vec4(pos,1.0));\n" +
                    "}\n", listOf(Variable(GLSLType.V1I, "clickId")), listOf(
                Variable(GLSLType.V1I, "frameId"),
                Variable(GLSLType.V4F, "ignoredOutput", VariableMode.OUT)
            ), "" +
                    "layout (binding = 0) buffer isVisible {\n" +
                    "  int values[];\n" +
                    "};" +
                    "void main() {\n" +
                    "   values[clickId] = frameId;\n" +
                    "   ignoredOutput = vec4(1.0);\n" +
                    "}\n"
        )

        /**
         * https://registry.khronos.org/OpenGL-Refpages/gl4/html/glDrawArraysIndirect.xhtml
         * typedef  struct {
         *         uint  count;
         *         uint  instanceCount;
         *         uint  first;
         *         uint  baseInstance;
         *     } DrawArraysIndirectCommand;
         *
         *     const DrawArraysIndirectCommand *cmd = (const DrawArraysIndirectCommand *)indirect;
         *     glDrawArraysInstancedBaseInstance(mode, cmd->first, cmd->count, cmd->instanceCount, cmd->baseInstance);
         * */
        private val writeOutput = "" +
                "layout(std430, binding=0) writeonly buffer indirectBuffer1 { int[] indirectBuffer };\n" +
                "layout(std430, binding=1) writeonly buffer isVisible1 { int[] isVisible; };\n" +
                "void writeOutput(int instanceCount) {\n" +
                "   indirectBuffer[0] = count;\n" + // vertex count
                "   indirectBuffer[1] = instanceCount;\n" +
                "   indirectBuffer[2] = first;\n" + // vertex offset
                "   indirectBuffer[3] = 0;\n" + // instance offset
                "}\n"

        private val writeOutputVars = listOf(
            Variable(GLSLType.V1I, "count"),
            Variable(GLSLType.V1I, "first"),
        )

        private val compacting1Shader = ComputeShader(
            "cullCompact",
            Vector3i(1), writeOutputVars + listOf(
                Variable(GLSLType.V1I, "frameId"),
                Variable(GLSLType.V1I, "clickId")
            ), "" +
                    writeOutput +
                    "void main() {\n" +
                    "   mappedIds[0] = 0;\n" + // doesn't really matter
                    "   bool isDrawn = isVisible[clickId] == frameId;\n" +
                    "   writeOutput(isDrawn ? 1 : 0);\n" +
                    "}\n"
        )

        private val compactingNShader by lazy {
            val groupSize = 1024
            ComputeShader(
                "cullCompact",
                Vector3i(groupSize, 1, 1), writeOutputVars + listOf(
                    Variable(GLSLType.V1I, "frameId"),
                    Variable(GLSLType.V1I, "clickId"),
                    Variable(GLSLType.V1I, "clickIdIndex"),
                    Variable(GLSLType.V1I, "numAttributes"),
                    Variable(GLSLType.V1I, "numInstances"),
                ), "" +
                        writeOutput +
                        "layout(std430, binding=2)  readonly buffer attributes0 { int[] attributesIn; };\n" +
                        "layout(std430, binding=3) writeonly buffer attributes1 { int[] attributesOut; };\n" +

                        "void main() {\n" +
                        // find and validate worker
                        "   uint localWorker = gl_LocalInvocationID.x;\n" +
                        "   uint globalWorker = localWorker + gl_GlobalInvocationID.x * $groupSize;\n" +
                        "   if(globalWorker == 0) {\n" +
                        "       writeOutput(0);\n" +
                        "   }\n" +
                        "   memoryBarrierBuffer(); barrier();\n" + // ensure all threads see the initialized value
                        "   if(globalWorker >= numInstances) return;\n" + // check bounds

                        // check whether entry is valid
                        "   uint srcI = globalWorker * numAttributes;\n" +
                        "   int clickId = attributesIn[srcI + clickIdIndex];\n" +
                        "   if (isVisible[clickId] != frameId) return;\n" +

                        // find output index and compact attributes
                        "   uint dstI = atomicAdd(indirectBuffer[1],1) * numAttributes;\n" +
                        "   for (int i=0;i<numAttributes;i++) {\n" +
                        "       attributesOut[dstI+i] = attributesIn[srcI+i];\n" +
                        "   }\n" +
                        "}\n"
            )
        }
    }

    private val mesh get() = Shapes.smoothCube.back

    private var frameId = 0

    fun clearIsVisible() {
        // instead of clearing the data, we just use a frameId-comparison-check
        frameId++
    }

    /**
     * stores all AABBs to be drawn
     * */
    private val boxBuffer = StaticBuffer(
        "boxBuffer", listOf( // 4 * 8 = 32 bytes each
            Attribute("min", 3),
            Attribute("clickIds", AttributeType.UINT32, 1, true),
            Attribute("max", 3),
            Attribute("padding", 1) // to get nice data alignment
        ), 16 * 1024, BufferUsage.DYNAMIC
    )

    /**
     * stores on what frame a clickId was last visible
     * */
    private val visibilityBuffer = ComputeBuffer(
        "isVisible", listOf(Attribute("isVisible", AttributeType.UINT32, 1, true)),
        16 * 1024
    )

    private val indirectBuffer = ComputeBuffer(
        "indirect", listOf(Attribute("stats", AttributeType.UINT32, 4, true)),
        1, GL_DRAW_INDIRECT_BUFFER
    )

    private val mappedAttributes = LazyMap { attr: List<Attribute> ->
        StaticBuffer("mappedAttrs", attr, 0)
    }

    private fun fillBoxBuffer(boxes: ClickIdBoundsArray) {
        val buffer = boxBuffer
        if (boxes.size > buffer.elementCount) {
            buffer.destroy()
            buffer.elementCount = boxes.capacity
        }
        if (buffer.nioBuffer == null) buffer.createNioBuffer()
        val data = buffer.nioBuffer!!
        buffer.isUpToDate = false

        val values = boxes.values
        data.asFloatBuffer()
            .put(values, 0, boxes.size)
        assertEquals(data.position(), boxes.size * 6 * 4)
        buffer.ensureBuffer()
    }

    private fun ensureVisibilityBuffer(boxes: ClickIdBoundsArray) {
        val buffer = visibilityBuffer
        if (buffer.elementCount < boxes.size) {
            buffer.destroy()
            buffer.elementCount = boxes.capacity
        }
        // ensure it exists
        buffer.ensureBuffer()
        clearIsVisible()
    }

    /**
     * render a box for every AABB
     * */
    fun renderBoxes(pipeline: Pipeline, depth: Framebuffer, depthMode: DepthMode, boxes: ClickIdBoundsArray) {
        fillBoxBuffer(boxes)
        ensureVisibilityBuffer(boxes)
        val fb = getFramebufferWithAttachedDepth(listOf(TargetType.UInt8x1), depth)
        useFrame(fb) {
            GFXState.blendMode.use(null) { // disable blending
                GFXState.depthMode.use(depthMode) { // configure depth
                    GFXState.depthMask.use(false) {
                        val shader = boxShader
                        shader.use()
                        shader.v1i("frameId", frameId)
                        // using old transform, because we're using the previous depth buffer as a comparison
                        shader.m4x4("transform", RenderState.prevCameraMatrix)
                        shader.bindBuffer(0, visibilityBuffer)
                        mesh.drawInstanced(pipeline, shader, 0, boxBuffer, false)
                    }
                }
            }
        }
    }

    private fun getMappedInstanceBuffer(instanceBuffer: StaticBuffer): StaticBuffer {
        val buffer = mappedAttributes[instanceBuffer.attributes]
        if (instanceBuffer.elementCount > buffer.vertexCount) {
            assertTrue(instanceBuffer.vertexCount <= instanceBuffer.elementCount)
            buffer.destroy()
            buffer.elementCount = instanceBuffer.elementCount
        }
        buffer.ensureBuffer()
        return buffer
    }

    private fun bindCompactUniforms(shader: ComputeShader, first: Int, count: Int) {
        shader.v1i("frameId", frameId)
        shader.v1i("first", first)
        shader.v1i("count", count)
    }

    private fun compactId(clickId: Int, first: Int, count: Int) {
        indirectBuffer.ensureBuffer()
        val shader = compacting1Shader
        shader.use()

        shader.v1i("clickId", clickId)
        bindCompactUniforms(shader, first, count)

        shader.bindBuffer(0, indirectBuffer)
        shader.bindBuffer(1, visibilityBuffer)
        shader.runBySize(1)
    }

    private fun compactIds(
        instanceBuffer: StaticBuffer, clickIdAttribute: Attribute,
        first: Int, count: Int
    ): StaticBuffer {
        indirectBuffer.ensureBuffer()
        assertTrue(clickIdAttribute.offset % 4 == 0)
        assertTrue(clickIdAttribute.stride % 4 == 0)

        val mappedInstances = getMappedInstanceBuffer(instanceBuffer)
        val shader = compactingNShader
        shader.use()

        bindCompactUniforms(shader, first, count)

        // uniform sizes, so we don't write OOB
        shader.v1i("numInstances", instanceBuffer.elementCount)

        shader.bindBuffer(0, indirectBuffer)
        shader.bindBuffer(1, visibilityBuffer)
        shader.bindBuffer(2, instanceBuffer)
        shader.bindBuffer(3, mappedInstances)
        shader.runBySize(instanceBuffer.elementCount)
        return mappedInstances
    }

    private fun bindIndirectBuffer() {
        indirectBuffer.simpleBind()
    }

    fun drawArrays(
        shader: GPUShader, clickId: Int,
        first: Int, count: Int, drawMode: DrawMode
    ) {
        compactId(clickId, first, count)
        shader.use()
        bindIndirectBuffer()
        // last argument is the offset within indirectBuffer
        glDrawArraysIndirect(drawMode.id, 0)
    }

    fun drawElements(
        shader: GPUShader, clickId: Int,
        first: Int, count: Int, drawMode: DrawMode, indexType: Int
    ) {
        compactId(clickId, first, count)
        shader.use()
        bindIndirectBuffer()
        glDrawElementsIndirect(drawMode.id, indexType, 0)
    }

    private fun bindMappedInstances(shader: Shader, instanceBuffer: StaticBuffer) {
        shader.use() // just to be sure
        instanceBuffer.bindAttributes(shader, true)
    }

    fun drawArraysInstanced(
        shader: Shader, instanceBuffer: StaticBuffer, clickIdAttribute: Attribute,
        first: Int, count: Int, drawMode: DrawMode,
    ) {
        compactIds(instanceBuffer, clickIdAttribute, first, count)
        bindMappedInstances(shader, instanceBuffer)
        bindIndirectBuffer()
        glDrawArraysIndirect(drawMode.id, 0)
    }

    fun drawElementsInstanced(
        shader: Shader, instanceBuffer: StaticBuffer, clickIdAttribute: Attribute,
        first: Int, count: Int, drawMode: DrawMode, indexType: Int,
    ) {
        compactIds(instanceBuffer, clickIdAttribute, first, count)
        bindMappedInstances(shader, instanceBuffer)
        bindIndirectBuffer()
        glDrawElementsIndirect(drawMode.id, indexType, 0)
    }

    override fun destroy() {
        super.destroy()
        boxBuffer.destroy()
    }
}