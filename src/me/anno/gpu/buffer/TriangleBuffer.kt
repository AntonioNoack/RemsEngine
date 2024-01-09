package me.anno.gpu.buffer

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
import java.nio.ByteBuffer
import kotlin.math.max

/**
 * a buffer for rapidly drawing thousands of triangles,
 * which change every frame their position without easy pattern
 *
 * this method is much faster than calling Grid.drawLine 1000 times
 * */
object TriangleBuffer {

    private val LOGGER = LogManager.getLogger(TriangleBuffer::class)

    val shader = BaseShader(
        "DebugTriangles", listOf(
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
            Variable(GLSLType.V3F, "position"),
        ), "" +
                "void main(){\n" +
                "   finalColor = vColor.rgb;\n" +
                "   finalAlpha = vColor.a;\n" +
                "   finalNormal = normalize(cross(dFdx(position),dFdy(position)));\n" +
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

    private val buffer = StaticBuffer("lines", attributes, 65536, BufferUsage.STREAM)
    const val triangleSize = 3 * (3 * 4 + 4)

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

    fun putRelativeTriangle(
        x0: Double, y0: Double, z0: Double,
        x1: Double, y1: Double, z1: Double,
        x2: Double, y2: Double, z2: Double,
        cam: org.joml.Vector3d,
        worldScale: Double,
        r: Byte, g: Byte, b: Byte, a: Byte = -1
    ) {
        ensureSize(triangleSize)
        putRelativePoint(x0, y0, z0, cam, worldScale, r, g, b, a)
        putRelativePoint(x1, y1, z1, cam, worldScale, r, g, b, a)
        putRelativePoint(x2, y2, z2, cam, worldScale, r, g, b, a)
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

    fun putRelativeTriangle(
        x0: Double, y0: Double, z0: Double,
        x1: Double, y1: Double, z1: Double,
        x2: Double, y2: Double, z2: Double,
        cam: org.joml.Vector3d,
        worldScale: Double,
        color: Int
    ) {
        putRelativeTriangle(
            x0, y0, z0,
            x1, y1, z1,
            x2, y2, z2,
            cam, worldScale,
            color.r().toByte(),
            color.g().toByte(),
            color.b().toByte(),
            color.a().toByte()
        )
    }

    fun putRelativeTriangle(
        v0: org.joml.Vector3d, v1: org.joml.Vector3d, v2: org.joml.Vector3d,
        cam: org.joml.Vector3d,
        worldScale: Double,
        color: Int
    ) {
        putRelativeTriangle(
            v0.x, v0.y, v0.z,
            v1.x, v1.y, v1.z,
            v2.x, v2.y, v2.z,
            cam, worldScale,
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
        shader.v4f("tint", 1f)
        finish(camTransform, shader)
    }

    private fun finish(transform: Matrix4f, shader: Shader) {

        buffer.upload(allowResize = true, keepLarge = true)
        shader.m4x4("transform", transform)
        buffer.draw(shader)

        // reset the buffer
        buffer.clear()
    }
}