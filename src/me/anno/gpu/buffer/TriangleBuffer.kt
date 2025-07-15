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
import kotlin.math.max

/**
 * a buffer for rapidly drawing thousands of triangles,
 * which change every frame their position without easy pattern
 *
 * this method is much faster than calling Grid.drawLine 1000 times
 * */
object TriangleBuffer {

    val shader = BaseShader(
        "DebugTriangles", listOf(
            Variable(GLSLType.V3F, "position", VariableMode.ATTR),
            Variable(GLSLType.V4F, "color", VariableMode.ATTR),
            Variable(GLSLType.V3F, "cameraDelta"),
            Variable(GLSLType.M4x4, "transform")
        ), "" +
                "void main(){" +
                "   gl_Position = matMul(transform, vec4(position + cameraDelta, 1.0));\n" +
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
    }

    // drawing all these lines is horribly slow -> speed it up by caching them
    // we also could calculate their position in 2D on the CPU and just upload them xD
    val attributes = bind(
        Attribute("position", 3),
        Attribute("color", AttributeType.UINT8_NORM, 4)
    )

    private val buffer = StaticBuffer("triangles", attributes, 65536, BufferUsage.STREAM)
    private val bytesPerTriangle: Int = 3 * attributes.stride

    fun hasTrianglesToDraw(): Boolean {
        val nioBuffer = buffer.nioBuffer
        return nioBuffer != null && nioBuffer.position() > 0
    }

    fun addTriangle(
        x0: Double, y0: Double, z0: Double,
        x1: Double, y1: Double, z1: Double,
        x2: Double, y2: Double, z2: Double,
        color: Int
    ) {
        buffer.ensureHasExtraSpace(bytesPerTriangle)
        addPoint(x0, y0, z0, color)
        addPoint(x1, y1, z1, color)
        addPoint(x2, y2, z2, color)
    }

    private fun addPoint(x0: Double, y0: Double, z0: Double, color: Int) {
        val cam = cameraPosition
        buffer.getOrCreateNioBuffer()
            .putFloat((x0 - cam.x).toFloat())
            .putFloat((y0 - cam.y).toFloat())
            .putFloat((z0 - cam.z).toFloat())
            .put(color.r().toByte())
            .put(color.g().toByte())
            .put(color.b().toByte())
            .put(color.a().toByte())
    }

    fun addTriangle(
        v0: Vector3d, v1: Vector3d, v2: Vector3d,
        color: Int
    ) {
        addTriangle(
            v0.x, v0.y, v0.z,
            v1.x, v1.y, v1.z,
            v2.x, v2.y, v2.z,
            color
        )
    }

    @Suppress("unused")
    fun drawIf1M(camTransform: Matrix4f) {
        val nioBuffer = buffer.nioBuffer
        if (nioBuffer != null && nioBuffer.position() >= bytesPerTriangle * 256 * 1024) {
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
        if (!hasTrianglesToDraw()) return

        val newPosition = RenderState.cameraPosition
        cameraPosition.sub(newPosition, cameraDelta)
        cameraPosition.set(newPosition)

        val buffer = buffer
        buffer.upload(allowResize = true, keepLarge = true)
        shader.m4x4("transform", transform)
        shader.v3f("cameraDelta", cameraDelta)
        buffer.draw(shader)

        // reset the buffer
        buffer.clear()
    }
}