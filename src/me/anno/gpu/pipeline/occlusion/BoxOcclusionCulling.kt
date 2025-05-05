package me.anno.gpu.pipeline.occlusion

import me.anno.engine.ui.render.RenderState
import me.anno.gpu.DepthMode
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeLayout
import me.anno.gpu.buffer.AttributeLayout.Companion.bind
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.buffer.BufferUsage
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.framebuffer.IFramebuffer
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
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.maps.LazyMap
import org.joml.Vector3f
import org.joml.Vector3i
import org.lwjgl.opengl.GL46C.GL_DRAW_INDIRECT_BUFFER
import org.lwjgl.opengl.GL46C.glDrawArraysIndirect
import org.lwjgl.opengl.GL46C.glDrawElementsIndirect
import kotlin.math.max

/**
 * draws globalAABBs onto depth buffer of previous frame to confirm whether a mesh is visible;
 * then compacts instanceId-data on GPU (CPU-latency-free),
 * and draws that data using modern OpenGL API (4.2, so 2011 aka 14 years old)
 *
 * todo potential optimization
 *  - generate hierarchical Z pyramid,
 *  - check against in a compute shader instead of rasterizing at original quality
 * */
class BoxOcclusionCulling : AttachedDepthPass() {

    companion object {

        private val mesh = Shapes.smoothCube.linear(Vector3f(0.5f), Vector3f(0.5f)).back

        private val boxShader = LazyMap { reverseDepth: Boolean ->
            val condition =
                if (reverseDepth) "prevFrameDepth < 0.0 || boxDepth < prevFrameDepth*epsilon"
                else "boxDepth*epsilon > prevFrameDepth"
            Shader(
                "gpuBoxCulling", listOf(
                    // attributes per vertex; could be packed into the shader
                    Variable(GLSLType.V3F, "positions", VariableMode.ATTR),
                    // these attributes are per-instance
                    Variable(GLSLType.V3F, "minBox", VariableMode.ATTR),
                    Variable(GLSLType.V3F, "maxBox", VariableMode.ATTR),
                    Variable(GLSLType.V3F, "cameraDirection"),
                    Variable(GLSLType.M4x4, "transform"),
                ), "" +
                        // todo when we don't have reverse depth, limit depth to back plane somehow...
                        "void main() {\n" +
                        // back-side point for culling and geometry;
                        // must be clamped to avoid numeric issues for huge meshes
                        "   vec3 minBox1 = max(minBox, vec3(-1e15));\n" +
                        "   vec3 maxBox1 = min(maxBox, vec3(+1e15));\n" +
                        "   vec3 back = mix(minBox1, maxBox1, positions);\n" +
                        // find the point on the front-side for depth-comparison values
                        "   vec3 dist = (maxBox1-minBox1)/abs(cameraDirection);\n" +
                        "   float n = min(dist.x,min(dist.y,dist.z));\n" +
                        "   n = min(n,1e30);\n" +
                        "   vec3 front = back - n * cameraDirection;\n" +
                        "   gl_Position = matMul(transform, vec4(back, 1.0));\n" +
                        "   frontPosition = matMul(transform, vec4(front, 1.0));\n" +
                        "   clickId = gl_InstanceID;\n" + // not accessible from fragment shader
                        "}\n", listOf(
                    Variable(GLSLType.V1I, "clickId"),
                    Variable(GLSLType.V4F, "frontPosition")
                ), listOf(
                    Variable(GLSLType.V1I, "frameId"),
                    Variable(GLSLType.S2D, "depthTex"),
                    Variable(GLSLType.V4F, "ignoredOutput", VariableMode.OUT)
                ), "" +
                        "layout (binding = 0) writeonly buffer isVisible {\n" +
                        "  int values[];\n" +
                        "};\n" +
                        "void main() {\n" +
                        "   float epsilon = 1.0001;\n" +
                        "   float boxDepth = texelFetch(depthTex,ivec2(gl_FragCoord.xy),0).x;\n" +
                        "   float prevFrameDepth = frontPosition.z / frontPosition.w;\n" +
                        // todo correct check depends on reverseDepth
                        "   if($condition) values[clickId] = frameId;\n" +
                        "   ignoredOutput = vec4(1.0);\n" +
                        "}\n"
            ).apply { glslVersion = 430 }
        }

        /**
         * https://registry.khronos.org/OpenGL-Refpages/gl4/html/glDrawArraysIndirect.xhtml
         *     typedef struct {
         *         uint  count;
         *         uint  instanceCount;
         *         uint  first;
         *         uint  baseInstance;
         *     } DrawArraysIndirectCommand;
         *      typedef  struct {
         *         uint  count;
         *         uint  instanceCount;
         *         uint  firstIndex;
         *         int  baseVertex;
         *         uint  baseInstance;
         *     } DrawElementsIndirectCommand;
         *
         *     const DrawArraysIndirectCommand *cmd = (const DrawArraysIndirectCommand *)indirect;
         *     glDrawArraysInstancedBaseInstance(mode, cmd->first, cmd->count, cmd->instanceCount, cmd->baseInstance);
         * */
        private val writeOutput = "" +
                "layout(std430, binding=0) writeonly buffer indirectBuffer1 { int indirectBuffer[]; };\n" +
                "layout(std430, binding=1)  readonly buffer isVisible1 { int isVisible[]; };\n" +
                "void writeOutput(int instanceCount) {\n" +
                "   indirectBuffer[0] = count;\n" + // vertex count
                "   indirectBuffer[1] = instanceCount;\n" +
                "   indirectBuffer[2] = first;\n" + // vertex offset
                "   indirectBuffer[3] = 0;\n" + // instance/vertex offset
                "   indirectBuffer[4] = 0;\n" + // instance offset
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
                        "layout(std430, binding=2)  readonly buffer attributes0 { int attributesIn[]; };\n" +
                        "layout(std430, binding=3) writeonly buffer attributes1 { int attributesOut[]; };\n" +

                        "void main() {\n" +
                        // find and validate worker
                        "   uint localWorker = gl_LocalInvocationID.x;\n" +
                        "   uint globalWorker = gl_GlobalInvocationID.x;\n" +
                        "   if(globalWorker == 0) {\n" +
                        "       writeOutput(0);\n" +
                        "   }\n" +
                        "   memoryBarrierBuffer(); barrier();\n" + // ensure all threads see the initialized value
                        "   if(globalWorker >= numInstances) return;\n" + // check bounds

                        // check whether entry is valid
                        "   uint srcI = globalWorker * numAttributes;\n" +
                        "   int gfxId = attributesIn[srcI + clickIdIndex];\n" +
                        //  this is lowest3 bytes X endian-swap
                        "   int clickId = ((gfxId >> 16) & 0xff) | (gfxId & 0xff00) | ((gfxId & 0xff) << 16);\n" +
                        "   if (isVisible[clickId] != frameId) return;\n" +

                        // find output index and compact attributes
                        "   uint dstI = atomicAdd(indirectBuffer[1],1);\n" +
                        "   if (dstI >= numInstances) return;\n" + // safety-check
                        "   dstI = dstI * numAttributes;\n" +
                        "   for (int i=0;i<numAttributes;i++) {\n" +
                        "       attributesOut[dstI+i] = attributesIn[srcI+i];\n" +
                        "   }\n" +
                        "}\n"
            )
        }
    }

    private var frameId = 0

    fun clearIsVisible() {
        // instead of clearing the data, we just use a frameId-comparison-check
        frameId++
    }

    /**
     * stores all AABBs to be drawn
     * */
    private val boxBuffer = StaticBuffer(
        "boxBuffer",
        bind(
            // 4 * 6 = 24 bytes each
            Attribute("minBox", 3),
            Attribute("maxBox", 3),
        ),
        16 * 1024,
        BufferUsage.DYNAMIC
    )

    /**
     * stores on what frame a clickId was last visible
     * */
    private val visibilityBuffer = ComputeBuffer(
        "isVisible", bind(Attribute("isVisible", AttributeType.UINT32, 1)),
        16 * 1024
    )

    private val indirectBuffer = ComputeBuffer(
        "indirect", bind(Attribute("stats", AttributeType.UINT32, 1)),
        5, GL_DRAW_INDIRECT_BUFFER
    )

    private val mappedAttributes = LazyMap { attr: AttributeLayout ->
        StaticBuffer("mappedAttrs", attr, 16, BufferUsage.STATIC)
    }

    private fun fillBoxBuffer(boxes: ClickIdBoundsArray) {
        val buffer = boxBuffer
        if (boxes.size > buffer.vertexCount) {
            buffer.destroy()
            buffer.vertexCount = boxes.capacity
            buffer.elementCount = boxes.capacity
        }
        buffer.clear()
        buffer.put(boxes.values, 0, boxes.size * 6)
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
    fun renderBoxes(pipeline: Pipeline, depth: IFramebuffer, depthMode: DepthMode, boxes: ClickIdBoundsArray) {
        fillBoxBuffer(boxes)
        ensureVisibilityBuffer(boxes)
        val fb = getFramebufferWithAttachedDepth(listOf(TargetType.UInt8x1), depth)
        useFrame(fb) {
            GFXState.blendMode.use(null) { // disable blending
                GFXState.depthMode.use(depthMode) { // configure depth
                    GFXState.depthMask.use(false) {
                        val shader = boxShader[depthMode.reversedDepth]
                        shader.use()
                        shader.v1i("frameId", frameId)
                        shader.v3f("cameraDirection", RenderState.cameraDirection)
                        depth.depthTexture!!.bindTrulyNearest(shader, "depthTex")
                        // using old transform, because we're using the previous depth buffer as a comparison
                        shader.m4x4("transform", RenderState.prevCameraMatrix)
                        shader.bindBuffer(0, visibilityBuffer)
                        mesh.drawInstanced(pipeline, shader, 0, boxBuffer, false)
                    }
                }
            }
        }
    }

    private fun getMappedInstanceBuffer(instanceBuffer: Buffer): StaticBuffer {
        val buffer = mappedAttributes[instanceBuffer.attributes]
        if (instanceBuffer.elementCount > buffer.vertexCount) {
            buffer.destroy()
            buffer.vertexCount = max(instanceBuffer.elementCount, buffer.vertexCount * 2)
            buffer.createNioBuffer()
        }
        if (!buffer.isUpToDate) {
            // create buffer properly
            val buffer1 = buffer.getOrCreateNioBuffer()
            buffer1.position(buffer.vertexCount * buffer.stride)
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
        instanceBuffer: Buffer, clickIdAttribute: Int,
        first: Int, count: Int
    ): Buffer {
        indirectBuffer.ensureBuffer()
        val layout = instanceBuffer.attributes
        assertTrue(layout.offset(clickIdAttribute) % 4 == 0)
        assertTrue(layout.stride % 4 == 0)

        val mappedInstances = getMappedInstanceBuffer(instanceBuffer)
        val shader = compactingNShader
        shader.use()

        bindCompactUniforms(shader, first, count)

        // uniform sizes, so we don't write OOB
        shader.v1i("numInstances", instanceBuffer.elementCount)
        shader.v1i("clickIdIndex", layout.offset(clickIdAttribute).shr(2))
        shader.v1i("numAttributes", layout.stride.shr(2))

        shader.bindBuffer(0, indirectBuffer)
        shader.bindBuffer(1, visibilityBuffer)
        shader.bindBuffer(2, instanceBuffer)
        shader.bindBuffer(3, mappedInstances)
        shader.runBySize(instanceBuffer.elementCount)
        return mappedInstances
    }

    private fun bindIndirectBuffer() {
        indirectBuffer.bind()
    }

    // todo use them (?)
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

    // todo use them (?)
    fun drawElements(
        shader: GPUShader, clickId: Int,
        first: Int, count: Int, drawMode: DrawMode, indexType: Int
    ) {
        compactId(clickId, first, count)
        shader.use()
        bindIndirectBuffer()
        glDrawElementsIndirect(drawMode.id, indexType, 0)
    }

    private fun bindMappedInstances(shader: Shader, mapped: Buffer) {
        shader.use() // just to be sure
        mapped.bindAttributes(shader, true)
    }

    fun drawArraysInstanced(
        shader: Shader, instanceBuffer: Buffer, clickIdAttribute: Int,
        first: Int, count: Int, drawMode: DrawMode,
    ) {
        val mapped = compactIds(instanceBuffer, clickIdAttribute, first, count)
        bindMappedInstances(shader, mapped)
        bindIndirectBuffer()
        glDrawArraysIndirect(drawMode.id, 0)
    }

    fun drawElementsInstanced(
        shader: Shader, instanceBuffer: Buffer, clickIdAttribute: Int,
        first: Int, count: Int, drawMode: DrawMode, indexType: AttributeType,
    ) {
        val mapped = compactIds(instanceBuffer, clickIdAttribute, first, count)
        bindMappedInstances(shader, mapped)
        bindIndirectBuffer()
        glDrawElementsIndirect(drawMode.id, indexType.glslId, 0)
    }

    override fun destroy() {
        super.destroy()
        boxBuffer.destroy()
        visibilityBuffer.destroy()
        indirectBuffer.destroy()
        for ((_, v) in mappedAttributes) {
            v.destroy()
        }
        mappedAttributes.clear()
    }
}