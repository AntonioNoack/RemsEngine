package me.anno.gpu.drawing

import me.anno.gpu.GFX
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.buffer.SimpleBuffer.Companion.circle
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.utils.Color.white4
import me.anno.utils.types.Floats.toRadians
import me.anno.video.formats.gpu.GPUFrame
import org.joml.Matrix4f
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import kotlin.math.round

@Suppress("unused")
object GFXx3D {

    // used in Rem's Studio
    fun getScale(w: Int, h: Int) = 1f / h
    fun getScale(w: Float, h: Float) = 1f / h

    val shader3DCircle = ShaderLib.createShader(
        "3dCircle", listOf(
            Variable(GLSLType.V2F, "positions", VariableMode.ATTR),// angle, inner/outer
            Variable(GLSLType.M4x4, "transform"),
            Variable(GLSLType.V3F, "circleParams"), // 1 - inner r, start, end
        ), "" +
                "void main(){\n" +
                "   float angle = mix(circleParams.y, circleParams.z, positions.x);\n" +
                "   vec2 betterUV = vec2(cos(angle), -sin(angle)) * (1.0 - circleParams.x * positions.y);\n" +
                "   finalPosition = vec3(betterUV, 0.0);\n" +
                "   gl_Position = matMul(transform, vec4(finalPosition, 1.0));\n" +
                ShaderLib.flatNormal +
                "}", ShaderLib.y3D, listOf(
            Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
            Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT)
        ),
        "void main(){\n" +
                "   finalColor = vec3(1.0);\n" +
                "   finalAlpha = 1.0;\n" +
                "}", listOf()
    )

    val shader3DBoxBlur = Shader(
        "3d-blur", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
        listOf(
            Variable(GLSLType.S2D, "tex"),
            Variable(GLSLType.V2F, "stepSize"),
            Variable(GLSLType.V1I, "steps")
        ), "" +
                "void main(){\n" +
                "   vec4 color;\n" +
                "   if(steps < 2){\n" +
                "       color = texture(tex, uv);\n" +
                "   } else {\n" +
                "       color = vec4(0.0);\n" +
                "       for(int i=-steps/2;i<(steps+1)/2;i++){\n" +
                "           color += texture(tex, uv + float(i) * stepSize);\n" +
                "       }\n" +
                "       color /= float(steps);\n" +
                "   }\n" +
                "   gl_FragColor = color;\n" +
                "}"
    )

    fun shader3DUniforms(
        shader: Shader, stack: Matrix4f,
        w: Int, h: Int, tiling: Vector4f?
    ) {
        transformUniform(shader, stack)
        shader.v2f("textureDeltaUV", 1f / w, 1f / h)
        GFXx2D.tiling(shader, tiling)
    }

    fun shader3DUniforms(
        shader: Shader, stack: Matrix4f,
        w: Int, h: Int, color: Vector4f?,
        tiling: Vector4f?
    ) {
        shader3DUniforms(shader, stack, w, h, tiling)
        shader.v4f("tint", color ?: white4)
    }

    fun shader3DUniforms(
        shader: Shader, stack: Matrix4f,
        w: Int, h: Int, color: Int,
        tiling: Vector4f?
    ) {
        shader3DUniforms(shader, stack, w, h, tiling)
        shader.v4f("tint", color)
    }

    fun shader3DUniforms(shader: Shader, transform: Matrix4f?, color: Int) {
        transformUniform(shader, transform)
        shader.v4f("tint", color)
        shader.v4f("tiling", 1f, 1f, 0f, 0f)
    }

    fun transformUniform(shader: Shader, transform: Matrix4f?) {
        GFX.check()
        shader.m4x4("transform", transform)
    }

    fun colorGradingUniforms(shader: Shader) {
        shader.v3f("cgOffset", 0f)
        shader.v3f("cgSlope", 1f)
        shader.v3f("cgPower", 1f)
        shader.v1f("cgSaturation", 1f)
    }

    fun draw3DPlanar(
        stack: Matrix4fArrayList, texture: GPUFrame, color: Int,
        filtering: Filtering, clamping: Clamping, tiling: Vector4f?
    ) {
        if (!texture.isCreated) throw RuntimeException("Frame must be loaded to be rendered!")
        val shader = texture.get3DShaderPlanar().value
        shader.use()
        shader3DUniforms(shader, stack, texture.width, texture.height, color, tiling)
        texture.bind(0, filtering, clamping)
        texture.bindUVCorrection(shader)
        SimpleBuffer.flat01.draw(shader)
        GFX.check()
    }

    fun draw3DPlanar(
        stack: Matrix4fArrayList, texture: ITexture2D, w: Int, h: Int, color: Int,
        filtering: Filtering, clamping: Clamping, tiling: Vector4f?
    ) {
        val shader = ShaderLib.shader3DPlanar.value
        shader.use()
        shader3DUniforms(shader, stack, w, h, color, tiling)
        texture.bind(0, filtering, clamping)
        SimpleBuffer.flat01.draw(shader)
        GFX.check()
    }

    fun draw3DCircle(
        stack: Matrix4fArrayList,
        innerRadius: Float,
        startDegrees: Float,
        endDegrees: Float,
        color: Vector4f
    ) {
        val shader = shader3DCircle.value
        shader.use()
        shader3DUniforms(shader, stack, 1, 1, color, null)
        circleParams(innerRadius, startDegrees, endDegrees, shader)
        circle.draw(shader)
        GFX.check()
    }

    fun circleParams(
        innerRadius: Float,
        startDegrees: Float,
        endDegrees: Float,
        shader: Shader
    ) {
        val inv = round((startDegrees + endDegrees) / 180f) * 360f
        var a0 = startDegrees - inv
        var a1 = endDegrees - inv
        // if the two arrows switch sides, flip the circle
        if (a0 > a1) {// first start for checker pattern
            val tmp = a0
            a0 = a1
            a1 = tmp - 360f
        }
        // fix edge resolution loss
        if (a0 > a1 + 360) {
            a0 = a1 + 360
        } else if (a1 > a0 + 360) {
            a1 = a0 + 360
        }
        val angle0 = a0.toRadians()
        val angle1 = a1.toRadians()
        shader.v3f("circleParams", 1f - innerRadius, angle0, angle1)
    }
}