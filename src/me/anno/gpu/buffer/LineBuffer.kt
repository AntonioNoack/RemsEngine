package me.anno.gpu.buffer

import me.anno.engine.ui.render.RenderState.cameraPosition
import me.anno.engine.ui.render.RenderState.worldScale
import me.anno.gpu.GFX
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.pooling.ByteBufferPool
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL15C.*
import java.nio.ByteBuffer
import javax.vecmath.Vector3d
import kotlin.math.max

/**
 * a buffer for rapidly drawing thousands of lines,
 * which change every frame their position without easy pattern
 *
 * this method is much faster than calling Grid.drawLine 1000 times
 * */
object LineBuffer {

    private val LOGGER = LogManager.getLogger(LineBuffer::class)

    val shader = BaseShader(
        "DebugLines", listOf(
            Variable(GLSLType.V3F, "position", VariableMode.ATTR),
            Variable(GLSLType.V4F, "color", VariableMode.ATTR),
            Variable(GLSLType.M4x4, "transform")
        ), "" +
                "void main(){" +
                "   gl_Position = matMul(transform, vec4(position, 1.0));\n" +
                "   vColor = color;\n" +
                "}", listOf(Variable(GLSLType.V4F, "vColor")), listOf(
            Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
            Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT),
            Variable(GLSLType.V3F, "finalNormal", VariableMode.OUT),
        ), "" +
                "void main(){\n" +
                "   finalColor = vColor.rgb;\n" +
                "   finalAlpha = vColor.a;\n" +
                "   finalNormal = vec3(0.0);\n" +
                "}\n"
    ).apply {
        glslVersion = max(glslVersion, 330)
        ignoreNameWarnings("tint")
    }

    // drawing all these lines is horribly slow -> speed it up by caching them
    // we also could calculate their position in 2D on the CPU and just upload them xD
    val attributes = listOf(
        Attribute("position", 3),
        Attribute("color", AttributeType.UINT8_NORM, 4)
    )

    // whenever this is updated, nioBuffer in buffer needs to be updated as well

    private val buffer = StaticBuffer("lines", attributes, 65536, GL_STREAM_DRAW)
    const val lineSize = 2 * (3 * 4 + 4)

    init {
        buffer.drawMode = DrawMode.LINES
    }

    var bytes: ByteBuffer
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

    fun vToByte(d: Double): Byte {
        // a function, that must not/does not crash
        return when {
            d < 0.0 -> 0
            d < 1.0 -> (d * 255.0).toInt().toByte()
            else -> -1 // = 255
        }
    }

    fun vToByte(d: Float): Byte {
        // a function, that must not/does not crash
        return when {
            d < 0f -> 0
            d < 1f -> (d * 255f).toInt().toByte()
            else -> -1 // = 255
        }
    }

    fun addLine(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        r: Byte, g: Byte, b: Byte, a: Byte = -1
    ) {
        // we have to ensure a mutex,
        // and this is the only one, that makes sense
        GFX.checkIsGFXThread()
        // ensure that there is enough space in bytes
        ensureSize(lineSize)
        // write two data points
        val bytes = bytes
        bytes.putFloat(x0)
        bytes.putFloat(y0)
        bytes.putFloat(z0)
        bytes.put(r)
        bytes.put(g)
        bytes.put(b)
        bytes.put(a)
        bytes.putFloat(x1)
        bytes.putFloat(y1)
        bytes.putFloat(z1)
        bytes.put(r)
        bytes.put(g)
        bytes.put(b)
        bytes.put(a)
    }

    @Suppress("unused")
    fun addLine(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        r: Double, g: Double, b: Double, a: Double = 1.0
    ) {
        addLine(
            x0, y0, z0,
            x1, y1, z1,
            vToByte(r),
            vToByte(g),
            vToByte(b),
            vToByte(a)
        )
    }

    @Suppress("unused")
    fun addLine(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        addLine(
            x0, y0, z0,
            x1, y1, z1,
            vToByte(r),
            vToByte(g),
            vToByte(b),
            vToByte(a)
        )
    }

    @Suppress("unused")
    fun addLine(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        color: Int
    ) {
        addLine(
            x0, y0, z0,
            x1, y1, z1,
            color.r().toByte(),
            color.g().toByte(),
            color.b().toByte(),
            color.a().toByte()
        )
    }

    fun addLine(
        x0: Double, y0: Double, z0: Double,
        x1: Double, y1: Double, z1: Double,
        color: Int
    ) {
        addLine(
            x0.toFloat(), y0.toFloat(), z0.toFloat(),
            x1.toFloat(), y1.toFloat(), z1.toFloat(),
            color.r().toByte(),
            color.g().toByte(),
            color.b().toByte(),
            color.a().toByte()
        )
    }

    @Suppress("unused")
    fun addLine(
        v0: Vector3f, v1: Vector3f,
        r: Double, g: Double, b: Double, a: Double = 1.0
    ) {
        addLine(
            v0.x, v0.y, v0.z,
            v1.x, v1.y, v1.z,
            vToByte(r),
            vToByte(g),
            vToByte(b),
            vToByte(a)
        )
    }

    @Suppress("unused")
    fun addLine(
        v0: Vector3f, v1: Vector3f,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        addLine(
            v0.x, v0.y, v0.z,
            v1.x, v1.y, v1.z,
            vToByte(r),
            vToByte(g),
            vToByte(b),
            vToByte(a)
        )
    }

    fun addLine(
        v0: Vector3f, v1: Vector3f,
        color: Int
    ) {
        addLine(
            v0.x, v0.y, v0.z,
            v1.x, v1.y, v1.z,
            color.r().toByte(),
            color.g().toByte(),
            color.b().toByte(),
            color.a().toByte()
        )
    }

    @Suppress("unused")
    fun putRelativeLine(
        x0: Double, y0: Double, z0: Double,
        x1: Double, y1: Double, z1: Double,
        cam: org.joml.Vector3d,
        r: Byte, g: Byte, b: Byte, a: Byte = -1
    ) {
        putRelativeLine(x0, y0, z0, x1, y1, z1, cam, 1.0, r, g, b, a)
    }

    fun putRelativeLine(
        v0: Vector3d, v1: Vector3d,
        cam: org.joml.Vector3d,
        worldScale: Double,
        r: Byte, g: Byte, b: Byte, a: Byte = -1
    ) {
        putRelativeLine(
            v0.x, v0.y, v0.z,
            v1.x, v1.y, v1.z,
            cam, worldScale,
            r, g, b, a
        )
    }

    fun putRelativeLine(
        v0: Vector3d, v1: Vector3d,
        cam: org.joml.Vector3d,
        worldScale: Double,
        r: Double, g: Double, b: Double, a: Double = 1.0
    ) {
        putRelativeLine(
            v0.x, v0.y, v0.z,
            v1.x, v1.y, v1.z,
            cam, worldScale,
            vToByte(r),
            vToByte(g),
            vToByte(b),
            vToByte(a)
        )
    }

    fun putRelativeLine(
        x0: Double, y0: Double, z0: Double,
        x1: Double, y1: Double, z1: Double,
        cam: org.joml.Vector3d,
        worldScale: Double,
        r: Byte, g: Byte, b: Byte, a: Byte = -1
    ) {
        ensureSize(lineSize)
        putRelativePoint(x0, y0, z0, cam, worldScale, r, g, b, a)
        putRelativePoint(x1, y1, z1, cam, worldScale, r, g, b, a)
    }

    private fun putRelativePoint(
        x0: Double, y0: Double, z0: Double,
        cam: org.joml.Vector3d,
        worldScale: Double,
        r: Byte, g: Byte, b: Byte, a: Byte = -1
    ) {
        val bytes = bytes
        bytes.putFloat(((x0 - cam.x) * worldScale).toFloat())
        bytes.putFloat(((y0 - cam.y) * worldScale).toFloat())
        bytes.putFloat(((z0 - cam.z) * worldScale).toFloat())
        bytes.put(r)
        bytes.put(g)
        bytes.put(b)
        bytes.put(a)
    }

    fun putRelativeLine(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        cam: org.joml.Vector3d,
        worldScale: Double,
        r: Byte, g: Byte, b: Byte, a: Byte = -1
    ) {
        putRelativeLine(
            x0.toDouble(), y0.toDouble(), z0.toDouble(),
            x1.toDouble(), y1.toDouble(), z1.toDouble(),
            cam, worldScale, r, g, b, a
        )
    }

    @Suppress("unused")
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
            color.b().toByte(),
            color.a().toByte()
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
            color.b().toByte(),
            color.a().toByte()
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
            color.b().toByte(),
            color.a().toByte()
        )
    }

    fun putRelativeLine(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
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
            color.b().toByte(),
            color.a().toByte()
        )
    }

    @Suppress("unused")
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
            color.b().toByte(),
            color.a().toByte()
        )
    }

    @Suppress("unused")
    fun putRelativeLine(
        v0: org.joml.Vector3d, v1: org.joml.Vector3d,
        cam: org.joml.Vector3d,
        worldScale: Double,
        r: Double, g: Double, b: Double, a: Double = 1.0
    ) {
        putRelativeLine(
            v0.x, v0.y, v0.z,
            v1.x, v1.y, v1.z,
            cam, worldScale,
            vToByte(r),
            vToByte(g),
            vToByte(b),
            vToByte(a)
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
            color
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
            cameraPosition, worldScale,
            color
        )
    }

    fun putRelativeLine(
        v0: Vector3f,
        v1: Vector3f,
        color: Int
    ) {
        putRelativeLine(
            v0.x, v0.y, v0.z,
            v1.x, v1.y, v1.z,
            cameraPosition, worldScale,
            color
        )
    }

    fun drawIf1M(camTransform: Matrix4f) {
        if (bytes.position() >= 32 * 256 * 1024) {
            // more than 256k points have been collected
            finish(camTransform)
        }
    }

    fun finish(camTransform: Matrix4f) {
        val shader = shader.value
        shader.use()
        shader.v4f("tint", -1)
        finish(camTransform, shader)
    }

    private fun finish(transform: Matrix4f, shader: Shader) {

        buffer.upload(allowResize = true, keepLarge = true)
        shader.m4x4("transform", transform)
        if (enableLineSmoothing) glEnable(GL_LINE_SMOOTH)
        buffer.draw(shader)
        if (enableLineSmoothing) glDisable(GL_LINE_SMOOTH)

        // reset the buffer
        buffer.clear()
    }

    var enableLineSmoothing = true
}