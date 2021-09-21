package me.anno.gpu.buffer

import me.anno.engine.ui.render.RenderView.Companion.camPosition
import me.anno.engine.ui.render.RenderView.Companion.worldScale
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.pooling.ByteBufferPool
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL15
import javax.vecmath.Vector3d
import kotlin.math.roundToInt

/**
 * a buffer for rapidly drawing thousands of lines,
 * which change every frame their position without easy pattern
 *
 * this method is much faster than calling Grid.drawLine 1000 times
 * */
object LineBuffer {

    private val LOGGER = LogManager.getLogger(LineBuffer::class)

    val shader = BaseShader(
        "DebugLines", "" +
                "attribute vec3 position;\n" +
                "attribute vec4 color;\n" +
                "uniform mat4 transform;\n" +
                "void main(){" +
                "   gl_Position = transform * vec4(position, 1);\n" +
                "   vColor = color;\n" +
                "}", listOf(Variable("vec4", "vColor")), "" +
                "void main(){\n" +
                "   vec3 finalColor = vColor.rgb;\n" +
                "   float finalAlpha = vColor.a;\n" +
                "}\n"
    )

    //  drawing all these lines is horribly slow -> speed it up by caching them
    // we also could calculate their position in 2D on the CPU and just upload them xD
    val attributes = listOf(
        Attribute("position", 3),
        Attribute("color", AttributeType.UINT8_NORM, 4)
    )

    // whenever this is updated, nioBuffer in buffer needs to be updated as well
    private var bytes = ByteBufferPool
        .allocateDirect(1024 * attributes.sumOf { it.byteSize })

    private val buffer = object : Buffer(attributes, GL15.GL_DYNAMIC_DRAW) {
        init {
            drawMode = GL15.GL_LINES
        }

        override fun createNioBuffer() {
            nioBuffer = bytes
        }
    }

    fun ensureSize(extraSize: Int) {
        val position = bytes.position()
        val newSize = extraSize + position
        val size = bytes.capacity()
        if (newSize > size) {
            val newBytes = ByteBufferPool.allocateDirect(size * 2)
            // copy over
            LOGGER.info("Increased size of buffer $size*2")
            val oldBytes = bytes
            oldBytes.position(0)
            newBytes.position(0)
            newBytes.put(oldBytes)
            newBytes.position(position)
            ByteBufferPool.free(oldBytes)
            bytes = newBytes
            buffer.nioBuffer = newBytes
        }
    }

    fun addLine(
        v0: Vector3f, v1: Vector3f,
        r: Double, g: Double, b: Double
    ) {
        ensureSize(2 * (3 * 4 + 4))
        val r0 = (r * 255).roundToInt().toByte()
        val g0 = (g * 255).roundToInt().toByte()
        val b0 = (b * 255).roundToInt().toByte()
        val bytes = bytes
        bytes.putFloat(v0.x)
        bytes.putFloat(v0.y)
        bytes.putFloat(v0.z)
        bytes.put(r0)
        bytes.put(g0)
        bytes.put(b0)
        bytes.put(-1)
        bytes.putFloat(v1.x)
        bytes.putFloat(v1.y)
        bytes.putFloat(v1.z)
        bytes.put(r0)
        bytes.put(g0)
        bytes.put(b0)
        bytes.put(-1)
    }

    fun putRelativeLine(
        v0: Vector3d, v1: Vector3d,
        cam: org.joml.Vector3d,
        r: Double, g: Double, b: Double
    ) {
        ensureSize(2 * (3 * 4 + 4))
        val r0 = (r * 255).roundToInt().toByte()
        val g0 = (g * 255).roundToInt().toByte()
        val b0 = (b * 255).roundToInt().toByte()
        val bytes = bytes
        bytes.putFloat((v0.x - cam.x).toFloat())
        bytes.putFloat((v0.y - cam.y).toFloat())
        bytes.putFloat((v0.z - cam.z).toFloat())
        bytes.put(r0)
        bytes.put(g0)
        bytes.put(b0)
        bytes.put(-1)
        bytes.putFloat((v1.x - cam.x).toFloat())
        bytes.putFloat((v1.y - cam.y).toFloat())
        bytes.putFloat((v1.z - cam.z).toFloat())
        bytes.put(r0)
        bytes.put(g0)
        bytes.put(b0)
        bytes.put(-1)
    }

    fun putRelativeLine(
        v0: Vector3d, v1: Vector3d,
        cam: org.joml.Vector3d,
        worldScale: Double,
        r: Double, g: Double, b: Double
    ) {
        ensureSize(2 * (3 * 4 + 4))
        val r0 = (r * 255).roundToInt().toByte()
        val g0 = (g * 255).roundToInt().toByte()
        val b0 = (b * 255).roundToInt().toByte()
        val bytes = bytes
        bytes.putFloat(((v0.x - cam.x) * worldScale).toFloat())
        bytes.putFloat(((v0.y - cam.y) * worldScale).toFloat())
        bytes.putFloat(((v0.z - cam.z) * worldScale).toFloat())
        bytes.put(r0)
        bytes.put(g0)
        bytes.put(b0)
        bytes.put(-1)
        bytes.putFloat(((v1.x - cam.x) * worldScale).toFloat())
        bytes.putFloat(((v1.y - cam.y) * worldScale).toFloat())
        bytes.putFloat(((v1.z - cam.z) * worldScale).toFloat())
        bytes.put(r0)
        bytes.put(g0)
        bytes.put(b0)
        bytes.put(-1)
    }

    fun putRelativeLine(
        v0: Vector3d, v1: Vector3d,
        cam: org.joml.Vector3d,
        worldScale: Double,
        color: Int
    ) {
        putRelativeLine(v0, v1, cam, worldScale, color.r() / 255.0, color.g() / 255.0, color.b() / 255.0)
    }


    fun putRelativeLine(
        v0: org.joml.Vector3d, v1: org.joml.Vector3d,
        cam: org.joml.Vector3d,
        worldScale: Double,
        r: Double, g: Double, b: Double
    ) {
        ensureSize(2 * (3 * 4 + 4))
        val r0 = (r * 255).roundToInt().toByte()
        val g0 = (g * 255).roundToInt().toByte()
        val b0 = (b * 255).roundToInt().toByte()
        val bytes = bytes
        bytes.putFloat(((v0.x - cam.x) * worldScale).toFloat())
        bytes.putFloat(((v0.y - cam.y) * worldScale).toFloat())
        bytes.putFloat(((v0.z - cam.z) * worldScale).toFloat())
        bytes.put(r0)
        bytes.put(g0)
        bytes.put(b0)
        bytes.put(-1)
        bytes.putFloat(((v1.x - cam.x) * worldScale).toFloat())
        bytes.putFloat(((v1.y - cam.y) * worldScale).toFloat())
        bytes.putFloat(((v1.z - cam.z) * worldScale).toFloat())
        bytes.put(r0)
        bytes.put(g0)
        bytes.put(b0)
        bytes.put(-1)
    }

    fun putRelativeLine(
        v0: org.joml.Vector3d, v1: org.joml.Vector3d,
        cam: org.joml.Vector3d,
        worldScale: Double,
        color: Int
    ) {
        putRelativeLine(v0, v1, cam, worldScale, color.r() / 255.0, color.g() / 255.0, color.b() / 255.0)
    }

    fun putRelativeLine(
        v0: org.joml.Vector3d, v1: org.joml.Vector3d,
        color: Int
    ) {
        putRelativeLine(v0, v1, camPosition, worldScale, color.r() / 255.0, color.g() / 255.0, color.b() / 255.0)
    }

    fun finish(stack: Matrix4f) {
        val shader = shader.value
        shader.use()
        /*if (isKeyDown('x')) {
            shader.printLocationsAndValues()
            shader.invalidateCacheForTests()
            LOGGER.info(RenderState.blendMode.currentValue)
            LOGGER.info(RenderState.depthMode.currentValue)
            LOGGER.info(RenderState.cullMode.currentValue)
            LOGGER.info(RenderState.depthMask.currentValue)
            LOGGER.info(RenderState.scissorTest.currentValue)
        }*/
        shader.v4("tint", -1)
        finish(stack, shader)
    }

    private fun finish(stack: Matrix4f, shader: Shader) {

        // prepare for upload
        val bytes = bytes
        val limit = bytes.position()
        if (limit <= 0) return
        bytes.limit(limit) // we only need so many lines

        val buffer = buffer
        buffer.upload()
        buffer.isUpToDate = true
        shader.m4x4("transform", stack)
        buffer.draw(shader)
        // LOGGER.info("${buffer.drawLength} for limit $limit")

        // reset the buffer
        bytes.position(0)
        bytes.limit(bytes.capacity())

    }

}