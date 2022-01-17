package me.anno.gpu.buffer

import me.anno.engine.ui.render.RenderView.Companion.camPosition
import me.anno.engine.ui.render.RenderView.Companion.worldScale
import me.anno.gpu.GFX
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.pooling.ByteBufferPool
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL15C.GL_LINES
import org.lwjgl.opengl.GL15C.GL_STREAM_DRAW
import java.nio.ByteBuffer
import javax.vecmath.Vector3d

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
                "}", listOf(Variable(GLSLType.V4F, "vColor")), "" +
                "void main(){\n" +
                "   vec3 finalColor = vColor.rgb;\n" +
                "   float finalAlpha = vColor.a;\n" +
                "}\n"
    )

    // drawing all these lines is horribly slow -> speed it up by caching them
    // we also could calculate their position in 2D on the CPU and just upload them xD
    val attributes = listOf(
        Attribute("position", 3),
        Attribute("color", AttributeType.UINT8_NORM, 4)
    )

    // whenever this is updated, nioBuffer in buffer needs to be updated as well

    private val buffer = StaticBuffer(attributes, 1024, GL_STREAM_DRAW)

    init {
        buffer.drawMode = GL_LINES
    }

    private var bytes: ByteBuffer
        get() = buffer.nioBuffer!!
        set(value) {
            buffer.nioBuffer = value
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

    fun doubleToByte(d: Double): Byte {
        // a function, that must not/does not crash
        return when {
            d < 0.0 -> 0
            d < 1.0 -> (d * 255.0).toInt().toByte()
            else -> -1 // = 255
        }
    }

    fun addLine(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        r: Byte, g: Byte, b: Byte
    ) {
        // we have to ensure a mutex,
        // and this is the only one that makes sense
        GFX.checkIsGFXThread()
        // ensure that there is enough space in bytes
        ensureSize(2 * (3 * 4 + 4))
        // write two data points
        val bytes = bytes
        bytes.putFloat(x0)
        bytes.putFloat(y0)
        bytes.putFloat(z0)
        bytes.put(r)
        bytes.put(g)
        bytes.put(b)
        bytes.put(-1)
        bytes.putFloat(x1)
        bytes.putFloat(y1)
        bytes.putFloat(z1)
        bytes.put(r)
        bytes.put(g)
        bytes.put(b)
        bytes.put(-1)
    }

    fun addLine(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        r: Double, g: Double, b: Double
    ) {
        addLine(
            x0, y0, z0,
            x1, y1, z1,
            doubleToByte(r),
            doubleToByte(g),
            doubleToByte(b)
        )
    }

    fun addLine(
        v0: Vector3f, v1: Vector3f,
        r: Double, g: Double, b: Double
    ) {
        addLine(
            v0.x, v0.y, v0.z,
            v1.x, v1.y, v1.z,
            doubleToByte(r),
            doubleToByte(g),
            doubleToByte(b)
        )
    }

    fun putRelativeLine(
        x0: Double, y0: Double, z0: Double,
        x1: Double, y1: Double, z1: Double,
        cam: org.joml.Vector3d,
        r: Byte, g: Byte, b: Byte
    ) {
        ensureSize(2 * (3 * 4 + 4))
        val bytes = bytes
        bytes.putFloat((x0 - cam.x).toFloat())
        bytes.putFloat((y0 - cam.y).toFloat())
        bytes.putFloat((z0 - cam.z).toFloat())
        bytes.put(r)
        bytes.put(g)
        bytes.put(b)
        bytes.put(-1)
        bytes.putFloat((x1 - cam.x).toFloat())
        bytes.putFloat((y1 - cam.y).toFloat())
        bytes.putFloat((z1 - cam.z).toFloat())
        bytes.put(r)
        bytes.put(g)
        bytes.put(b)
        bytes.put(-1)
    }

    fun putRelativeLine(
        v0: Vector3d, v1: Vector3d,
        cam: org.joml.Vector3d,
        worldScale: Double,
        r: Byte, g: Byte, b: Byte
    ) {
        putRelativeLine(
            v0.x, v0.y, v0.z,
            v1.x, v1.y, v1.z,
            cam, worldScale,
            r, g, b
        )
    }

    fun putRelativeLine(
        v0: Vector3d, v1: Vector3d,
        cam: org.joml.Vector3d,
        worldScale: Double,
        r: Double, g: Double, b: Double
    ) {
        putRelativeLine(
            v0.x, v0.y, v0.z,
            v1.x, v1.y, v1.z,
            cam, worldScale,
            doubleToByte(r),
            doubleToByte(g),
            doubleToByte(b)
        )
    }

    fun putRelativeLine(
        x0: Double, y0: Double, z0: Double,
        x1: Double, y1: Double, z1: Double,
        cam: org.joml.Vector3d,
        worldScale: Double,
        r: Byte, g: Byte, b: Byte
    ) {
        ensureSize(2 * (3 * 4 + 4))
        val bytes = bytes
        bytes.putFloat(((x0 - cam.x) * worldScale).toFloat())
        bytes.putFloat(((y0 - cam.y) * worldScale).toFloat())
        bytes.putFloat(((z0 - cam.z) * worldScale).toFloat())
        bytes.put(r)
        bytes.put(g)
        bytes.put(b)
        bytes.put(-1)
        bytes.putFloat(((x1 - cam.x) * worldScale).toFloat())
        bytes.putFloat(((y1 - cam.y) * worldScale).toFloat())
        bytes.putFloat(((z1 - cam.z) * worldScale).toFloat())
        bytes.put(r)
        bytes.put(g)
        bytes.put(b)
        bytes.put(-1)
    }

    fun putRelativeLine(
        v0: org.joml.Vector3d,
        x1: Double, y1: Double, z1: Double,
        cam: org.joml.Vector3d, worldScale: Double,
        color: Int
    ) {
        putRelativeLine(
            v0.x, v0.y, v0.z,
            x1, y1, z1,
            cam,
            worldScale,
            color.r().toByte(),
            color.g().toByte(),
            color.b().toByte()
        )
    }

    fun putRelativeVector(
        v0: org.joml.Vector3d,
        dir: org.joml.Vector3d,
        length: Double,
        cam: org.joml.Vector3d, worldScale: Double,
        color: Int
    ) {
        putRelativeLine(
            v0.x, v0.y, v0.z,
            v0.x + dir.x * length,
            v0.y + dir.y * length,
            v0.z + dir.z * length,
            cam,
            worldScale,
            color.r().toByte(),
            color.g().toByte(),
            color.b().toByte()
        )
    }

    fun putRelativeLine(
        x0: Double, y0: Double, z0: Double,
        x1: Double, y1: Double, z1: Double,
        cam: org.joml.Vector3d,
        worldScale: Double,
        color: Int
    ) {
        putRelativeLine(
            x0, y0, z0,
            x1, y1, z1,
            cam, worldScale,
            color.r().toByte(),
            color.g().toByte(),
            color.b().toByte()
        )
    }

    fun putRelativeLine(
        v0: Vector3d, v1: Vector3d,
        cam: org.joml.Vector3d,
        worldScale: Double,
        color: Int
    ) {
        putRelativeLine(
            v0, v1,
            cam, worldScale,
            color.r().toByte(),
            color.g().toByte(),
            color.b().toByte()
        )
    }

    fun putRelativeLine(
        v0: org.joml.Vector3d, v1: org.joml.Vector3d,
        cam: org.joml.Vector3d,
        worldScale: Double,
        r: Double, g: Double, b: Double
    ) {
        putRelativeLine(
            v0.x, v0.y, v0.z,
            v1.x, v1.y, v1.z,
            cam, worldScale,
            doubleToByte(r),
            doubleToByte(g),
            doubleToByte(b)
        )
    }

    fun putRelativeLine(
        v0: org.joml.Vector3d, v1: org.joml.Vector3d,
        cam: org.joml.Vector3d,
        worldScale: Double,
        color: Int
    ) {
        putRelativeLine(
            v0.x, v0.y, v0.z,
            v1.x, v1.y, v1.z,
            cam, worldScale,
            color.r().toByte(),
            color.g().toByte(),
            color.b().toByte()
        )
    }

    fun putRelativeLine(
        v0: org.joml.Vector3d,
        v1: org.joml.Vector3d,
        color: Int
    ) {
        putRelativeLine(
            v0.x, v0.y, v0.z,
            v1.x, v1.y, v1.z,
            camPosition, worldScale,
            color.r().toByte(),
            color.g().toByte(),
            color.b().toByte()
        )
    }

    fun drawIf1M(camTransform: Matrix4f) {
        if (bytes.position() >= 32_000_000) {
            // more than 1M points have been collected
            finish(camTransform)
        }
    }

    fun finish(camTransform: Matrix4f) {
        val shader = shader.value
        shader.use()
        shader.v4f("tint", -1)
        finish(camTransform, shader)
    }

    private fun finish(camTransform: Matrix4f, shader: Shader) {

        buffer.upload()
        shader.m4x4("transform", camTransform)
        buffer.draw(shader)

        // reset the buffer
        buffer.clear()

    }

}