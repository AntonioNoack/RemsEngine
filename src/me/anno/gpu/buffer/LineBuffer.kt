package me.anno.gpu.buffer

import me.anno.engine.ui.render.RenderState
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3f
import org.lwjgl.opengl.GL46C.GL_LINE_SMOOTH
import org.lwjgl.opengl.GL46C.glDisable
import org.lwjgl.opengl.GL46C.glEnable

/**
 * a buffer for rapidly drawing thousands of lines,
 * which change every frame their position without easy pattern
 *
 * this method is much faster than calling Grid.drawLine 1000 times
 * */
object LineBuffer {

    val shader = BaseShader(
        "DebugLines", listOf(
            Variable(GLSLType.V3F, "position", VariableMode.ATTR),
            Variable(GLSLType.V4F, "color", VariableMode.ATTR),
            Variable(GLSLType.V3F, "cameraDelta"),
            Variable(GLSLType.M4x4, "transform")
        ), "" +
                "void main(){\n" +
                "   gl_Position = matMul(transform, vec4(position + cameraDelta, 1.0));\n" +
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
    )

    // drawing all these lines is horribly slow -> speed it up by caching them
    // we also could calculate their position in 2D on the CPU and just upload them xD
    val attributes = bind(
        Attribute("position", 3),
        Attribute("color", AttributeType.UINT8_NORM, 4)
    )

    private val buffer = StaticBuffer("lines", attributes, 65536, BufferUsage.STREAM).apply {
        drawMode = DrawMode.LINES
    }

    val bytesPerLine: Int = 2 * attributes.stride

    fun hasLinesToDraw(): Boolean {
        val nioBuffer = buffer.nioBuffer
        return nioBuffer != null && nioBuffer.position() > 0
    }

    fun ensureSize(extraSize: Int) {
        buffer.ensureHasExtraSpace(extraSize)
    }

    fun addLine(
        x0: Double, y0: Double, z0: Double,
        x1: Double, y1: Double, z1: Double,
        color: Int
    ) {
        // ensure that there is enough space in bytes
        ensureSize(bytesPerLine)

        val r = color.r().toByte()
        val g = color.g().toByte()
        val b = color.b().toByte()
        val a = color.a().toByte()

        val cam = cameraPosition
        buffer.getOrCreateNioBuffer()
            .putFloat((x0 - cam.x).toFloat())
            .putFloat((y0 - cam.y).toFloat())
            .putFloat((z0 - cam.z).toFloat())
            .put(r).put(g).put(b).put(a)
            .putFloat((x1 - cam.x).toFloat())
            .putFloat((y1 - cam.y).toFloat())
            .putFloat((z1 - cam.z).toFloat())
            .put(r).put(g).put(b).put(a)
    }

    fun addLine(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        color: Int
    ): Unit = addLine(
        x0.toDouble(), y0.toDouble(), z0.toDouble(),
        x1.toDouble(), y1.toDouble(), z1.toDouble(), color
    )

    fun addLine(
        v0: Vector3f, v1: Vector3f,
        color: Int
    ): Unit = addLine(v0.x, v0.y, v0.z, v1.x, v1.y, v1.z, color)

    fun addLine(
        v0: Vector3d,
        x1: Double, y1: Double, z1: Double,
        color: Int
    ): Unit = addLine(v0.x, v0.y, v0.z, x1, y1, z1, color)

    fun addVector(
        v0: Vector3d,
        dir: Vector3d,
        length: Double,
        color: Int
    ) {
        addLine(
            v0.x, v0.y, v0.z,
            v0.x + dir.x * length,
            v0.y + dir.y * length,
            v0.z + dir.z * length,
            color
        )
    }

    fun addLine(v0: Vector3d, v1: Vector3d, color: Int): Unit =
        addLine(v0.x, v0.y, v0.z, v1.x, v1.y, v1.z, color)

    fun drawIf1M(camTransform: Matrix4f) {
        val nioBuffer = buffer.nioBuffer
        if (nioBuffer != null && nioBuffer.position() >= 32 * 256 * 1024) {
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

    private val cameraPosition = Vector3d()
    private val cameraDelta = Vector3d()
    private fun finish(transform: Matrix4f, shader: Shader) {
        if (!hasLinesToDraw()) return

        val newPosition = RenderState.cameraPosition
        cameraPosition.sub(newPosition, cameraDelta)
        cameraPosition.set(newPosition)

        val buffer = buffer
        buffer.upload(allowResize = true, keepLarge = true)
        shader.m4x4("transform", transform)
        shader.v3f("cameraDelta", cameraDelta)
        if (enableLineSmoothing) glEnable(GL_LINE_SMOOTH)
        buffer.draw(shader)
        if (enableLineSmoothing) glDisable(GL_LINE_SMOOTH)

        // reset the buffer
        buffer.clear()
    }

    var enableLineSmoothing = true
}