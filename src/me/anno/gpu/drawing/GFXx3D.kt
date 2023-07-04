package me.anno.gpu.drawing

import me.anno.gpu.GFX
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.buffer.SimpleBuffer.Companion.circleBuffer
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01Cube
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01CubeX10
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.drawing.GFXx2D.defineAdvancedGraphicalFeatures
import me.anno.gpu.drawing.GFXx2D.disableAdvancedGraphicalFeatures
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderFuncLib
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.maxOutlineColors
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.*
import me.anno.utils.Color.toARGB
import me.anno.utils.types.Floats.toRadians
import me.anno.video.formats.gpu.GPUFrame
import org.joml.*
import org.lwjgl.BufferUtils
import java.nio.FloatBuffer
import kotlin.math.min
import kotlin.math.round

@Suppress("unused")
object GFXx3D {

    // used in Rem's Studio
    fun getScale(w: Int, h: Int) = 1f / h
    fun getScale(w: Float, h: Float) = 1f / h

    val shader3DText = ShaderLib.createShader(
        "3d-text", ShaderLib.v3Dl,
        "uniform vec3 offset;\n" +
                ShaderLib.getUVForceFieldLib +
                "void main(){\n" +
                "   vec3 localPos0 = coords + offset;\n" +
                "   vec2 pseudoUV2 = getForceFieldUVs(localPos0.xy*.5+.5);\n" +
                "   finalPosition = ${ShaderLib.hasForceFieldUVs} ? vec3(pseudoUV2*2.0-1.0, coords.z + offset.z) : localPos0;\n" +
                "   gl_Position = matMul(transform, vec4(finalPosition, 1.0));\n" +
                ShaderLib.flatNormal +
                ShaderLib.positionPostProcessing +
                "   vertexId = gl_VertexID;\n" +
                "}", ShaderLib.y3D + listOf(Variable(GLSLType.V1I, "vertexId").flat()), listOf(
            Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
            Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT)
        ), "" +
                ShaderFuncLib.noiseFunc +
                ShaderLib.getTextureLib +
                ShaderLib.getColorForceFieldLib +
                "void main(){\n" +
                "   vec4 finalColor2 = (${ShaderLib.hasForceFieldColor}) ? getForceFieldColor(finalPosition) : vec4(1.0);\n" +
                "   finalColor = finalColor2.rgb;\n" +
                "   finalAlpha = finalColor2.a;\n" +
                "}", listOf(), "tiling", "forceFieldUVCount"
    )

    val shader3DCircle = ShaderLib.createShader(
        "3dCircle", listOf(
            Variable(GLSLType.V2F, "coords", VariableMode.ATTR),// angle, inner/outer
            Variable(GLSLType.M4x4, "transform"),
            Variable(GLSLType.V3F, "circleParams"), // 1 - inner r, start, end
        ), "" +
                "void main(){\n" +
                "   float angle = mix(circleParams.y, circleParams.z, coords.x);\n" +
                "   vec2 betterUV = vec2(cos(angle), -sin(angle)) * (1.0 - circleParams.x * coords.y);\n" +
                "   finalPosition = vec3(betterUV, 0.0);\n" +
                "   gl_Position = matMul(transform, vec4(finalPosition, 1.0));\n" +
                ShaderLib.flatNormal +
                ShaderLib.positionPostProcessing +
                "}", ShaderLib.y3D, listOf(
            Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
            Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT)
        ), ShaderLib.getColorForceFieldLib +
                "void main(){\n" +
                "   vec4 finalColor2 = (${ShaderLib.hasForceFieldColor}) ? getForceFieldColor(finalPosition) : vec4(1);\n" +
                "   finalColor = finalColor2.rgb;\n" +
                "   finalAlpha = finalColor2.a;\n" +
                "}", listOf(),
        "filtering", "textureDeltaUV", "tiling", "uvProjection", "forceFieldUVCount",
        "cgOffset", "cgSlope", "cgPower", "cgSaturation"
    )

    val shader3DBoxBlur = Shader(
        "3d-blur", ShaderLib.coordsList, ShaderLib.coordsVShader, ShaderLib.uvList,
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

    val shader3DGaussianBlur = Shader(
        "3d-blur", ShaderLib.v3DlMasked, ShaderLib.v3DMasked, ShaderLib.y3DMasked, listOf(
            Variable(GLSLType.S2D, "tex"),
            Variable(GLSLType.V2F, "stepSize"),
            Variable(GLSLType.V1F, "steps"),
            Variable(GLSLType.V1F, "threshold")
        ), "" +
                ShaderLib.brightness +
                "void main(){\n" +
                "   vec2 uv2 = uv.xy/uv.z * 0.5 + 0.5;\n" +
                "   vec4 color;\n" +
                "   float sum = 0.0;\n" +
                // test all steps for -pixelating*2 .. pixelating*2, then average
                "   int iSteps = max(0, int(2.7 * steps));\n" +
                "   if(iSteps == 0){\n" +
                "       color = texture(tex, uv2);\n" +
                "   } else {\n" +
                "       color = vec4(0.0);\n" +
                "       for(int i=-iSteps;i<=iSteps;i++){\n" +
                "           float fi = float(i);\n" +
                "           float relativeX = fi/steps;\n" +
                "           vec4 colorHere = texture(tex, uv2 + fi * stepSize);\n" +
                "           float weight = exp(-relativeX*relativeX);\n" +
                "           sum += weight;\n" +
                "           color += vec4(max(vec3(0.0), colorHere.rgb - threshold), colorHere.a) * weight;\n" +
                "       }\n" +
                "       color /= sum;\n" +
                "   }\n" +
                "   gl_FragColor = color;\n" +
                "}"
    )

    fun shader3DUniforms(
        shader: Shader, stack: Matrix4fArrayList,
        w: Int, h: Int,
        tiling: Vector4f?, filtering: Filtering,
        uvProjection: UVProjection?
    ) {

        stack.pushMatrix()

        val doScale2 = (uvProjection?.doScale ?: true) && w != h
        if (doScale2) {
            val scale = getScale(w, h)
            val sx = w * scale
            val sy = h * scale
            stack.scale(sx, -sy, 1f)
        } else {
            stack.scale(1f, -1f, 1f)
        }

        transformUniform(shader, stack)
        shader.v1i("filtering", filtering.id)
        shader.v2f("textureDeltaUV", 1f / w, 1f / h)

        stack.popMatrix()

        GFXx2D.tiling(shader, tiling)
        shader.v1i("uvProjection", uvProjection?.id ?: UVProjection.Planar.id)

    }

    fun shader3DUniforms(
        shader: Shader, stack: Matrix4fArrayList,
        w: Int, h: Int, color: Vector4f?,
        tiling: Vector4f?, filtering: Filtering,
        uvProjection: UVProjection?
    ) {
        shader3DUniforms(shader, stack, w, h, tiling, filtering, uvProjection)
        GFX.shaderColor(shader, "tint", color)
    }

    fun shader3DUniforms(
        shader: Shader, stack: Matrix4fArrayList,
        w: Int, h: Int, color: Int,
        tiling: Vector4f?, filtering: Filtering,
        uvProjection: UVProjection?
    ) {
        shader3DUniforms(shader, stack, w, h, tiling, filtering, uvProjection)
        GFX.shaderColor(shader, "tint", color)
    }

    fun shader3DUniforms(
        shader: Shader, stack: Matrix4fArrayList,
        w: Int, h: Int, color: Int,
        tiling: Vector4f?, uvProjection: UVProjection?
    ) {
        shader3DUniforms(shader, stack, w, h, tiling, Filtering.NEAREST, uvProjection)
        GFX.shaderColor(shader, "tint", color)
    }

    fun drawDebugCube(matrix: Matrix4fArrayList, size: Float, color: Vector4f?) {
        matrix.scale(0.5f * size, -0.5f * size, 0.5f * size) // flip inside out
        val tex = TextureLib.whiteTexture
        draw3D(
            matrix, tex, color,
            Filtering.NEAREST, tex.clamping!!, null, UVProjection.TiledCubemap
        )
    }

    fun shader3DUniforms(shader: Shader, transform: Matrix4f?, color: Int) {
        transformUniform(shader, transform)
        GFX.shaderColor(shader, "tint", color)
        shader.v4f("tiling", 1f, 1f, 0f, 0f)
    }

    fun shader3DUniforms(shader: Shader, transform: Matrix4f, color: Vector4f) {
        transformUniform(shader, transform)
        GFX.shaderColor(shader, "tint", color)
        shader.v4f("tiling", 1f, 1f, 0f, 0f)
    }

    fun transformUniform(shader: Shader, transform: Matrix4f?) {
        GFX.check()
        shader.m4x4("transform", transform)
    }

    fun draw3DText(
        offset: Vector3f,
        stack: Matrix4fArrayList, buffer: StaticBuffer, color: Vector4f
    ) = draw3DText(offset, stack, buffer, color.toARGB())

    fun draw3DText(
        offset: Vector3f,
        stack: Matrix4fArrayList, buffer: StaticBuffer, color: Int
    ) {
        val shader = shader3DText.value
        shader.use()
        shader3DUniforms(shader, stack, color)
        shader.v3f("offset", offset)
        uploadAttractors0(shader)
        buffer.draw(shader)
        GFX.check()
    }

    fun draw3DTextWithOffset(
        buffer: StaticBuffer,
        offset: Vector3f
    ) {
        val shader = shader3DText.value
        shader.use()
        shader.v3f("offset", offset)
        buffer.draw(shader)
    }

    fun colorGradingUniforms(shader: Shader) {
        shader.v3f("cgOffset", 0f)
        shader.v3f("cgSlope", 1f)
        shader.v3f("cgPower", 1f)
        shader.v1f("cgSaturation", 1f)
    }

    fun draw3D(
        stack: Matrix4fArrayList, texture: GPUFrame, color: Vector4f,
        filtering: Filtering, clamping: Clamping, tiling: Vector4f?, uvProjection: UVProjection
    ) {
        if (!texture.isCreated) throw RuntimeException("Frame must be loaded to be rendered!")
        val shader0 = texture.get3DShader()
        val shader = shader0.value
        shader.use()
        shader3DUniforms(shader, stack, texture.w, texture.h, color, tiling, filtering, uvProjection)
        disableAdvancedGraphicalFeatures(shader)
        texture.bind(0, filtering, clamping)
        texture.bindUVCorrection(shader)
        uvProjection.getBuffer().draw(shader)
        GFX.check()
    }

    fun draw3D(
        stack: Matrix4fArrayList, texture: GPUFrame, color: Int,
        filtering: Filtering, clamping: Clamping, tiling: Vector4f?, uvProjection: UVProjection
    ) {
        if (!texture.isCreated) throw RuntimeException("Frame must be loaded to be rendered!")
        val shader = texture.get3DShader().value
        shader.use()
        shader3DUniforms(shader, stack, texture.w, texture.h, color, tiling, filtering, uvProjection)
        defineAdvancedGraphicalFeatures(shader)
        texture.bind(0, filtering, clamping)
        texture.bindUVCorrection(shader)
        uvProjection.getBuffer().draw(shader)
        GFX.check()
    }

    fun draw3D(
        stack: Matrix4fArrayList, texture: GPUFrame, color: Int,
        filtering: GPUFiltering, clamping: Clamping, tiling: Vector4f?, uvProjection: UVProjection
    ) {
        if (!texture.isCreated) throw RuntimeException("Frame must be loaded to be rendered!")
        val shader = texture.get3DShader().value
        shader.use()
        shader3DUniforms(shader, stack, texture.w, texture.h, color, tiling, uvProjection)
        defineAdvancedGraphicalFeatures(shader)
        texture.bind(0, filtering, clamping)
        texture.bindUVCorrection(shader)
        uvProjection.getBuffer().draw(shader)
        GFX.check()
    }

    fun draw3D(
        stack: Matrix4fArrayList, texture: Texture2D, color: Vector4f?,
        filtering: Filtering, clamping: Clamping, tiling: Vector4f?, uvProjection: UVProjection
    ) = draw3D(stack, texture, texture.w, texture.h, color, filtering, clamping, tiling, uvProjection)

    fun draw3D(
        stack: Matrix4fArrayList, texture: Texture2D, color: Int,
        filtering: Filtering, clamping: Clamping, tiling: Vector4f?, uvProjection: UVProjection
    ) = draw3D(stack, texture, texture.w, texture.h, color, filtering, clamping, tiling, uvProjection)

    fun draw3D(
        stack: Matrix4fArrayList, texture: Texture2D, w: Int, h: Int, color: Vector4f?,
        filtering: Filtering, clamping: Clamping, tiling: Vector4f?, uvProjection: UVProjection
    ) {
        val shader = ShaderLib.shader3D.value
        shader.use()
        defineAdvancedGraphicalFeatures(shader)
        shader3DUniforms(shader, stack, w, h, color, tiling, filtering, uvProjection)
        texture.bind(0, filtering, clamping)
        uvProjection.getBuffer().draw(shader)
        GFX.check()
    }

    fun draw3D(
        stack: Matrix4fArrayList, texture: Texture2D, w: Int, h: Int, color: Int,
        filtering: Filtering, clamping: Clamping, tiling: Vector4f?, uvProjection: UVProjection
    ) {
        val shader = ShaderLib.shader3D.value
        shader.use()
        defineAdvancedGraphicalFeatures(shader)
        shader3DUniforms(shader, stack, w, h, color, tiling, filtering, uvProjection)
        texture.bind(0, filtering, clamping)
        uvProjection.getBuffer().draw(shader)
        GFX.check()
    }

    val outlineStatsBuffer: FloatBuffer = BufferUtils.createFloatBuffer(maxOutlineColors * 4)
    fun drawOutlinedText(
        stack: Matrix4fArrayList,
        offset: Vector2f,
        scale: Vector2f,
        texture: Texture2D,
        color: Vector4f,
        colorCount: Int,
        colors: Array<Vector4f>,
        distances: FloatArray,
        smoothness: FloatArray,
        depth: Float,
        hasUVAttractors: Boolean
    ) {

        val shader = ShaderLib.shaderSDFText.value
        shader.use()

        defineAdvancedGraphicalFeatures(shader)

        GFX.shaderColor(shader, "tint", color)

        val cc = min(colorCount, maxOutlineColors)

        /**
         * u4[ maxColors ] colors
         * u2[ maxColors ] distSmooth
         * uniform int colorCount
         * */
        val buffer = outlineStatsBuffer
        buffer.position(0)
        for (i in 0 until cc) {
            val colorI = colors[i]
            buffer.put(colorI.x)
            buffer.put(colorI.y)
            buffer.put(colorI.z)
            buffer.put(colorI.w)
        }
        buffer.position(0)
        shader.v4Array("colors", buffer)
        buffer.position(0)
        for (i in 0 until cc) {
            buffer.put(distances[i])
            buffer.put(smoothness[i])
        }
        buffer.position(0)
        shader.v2Array("distSmoothness", buffer)
        shader.v1i("colorCount", cc)
        shader.v1f("depth", depth * 0.00001f)

        drawOutlinedText(stack, offset, scale, texture, hasUVAttractors)

    }

    fun drawOutlinedText(
        stack: Matrix4fArrayList,
        offset: Vector2f,
        scale: Vector2f,
        texture: Texture2D,
        hasUVAttractors: Boolean
    ) {
        val shader = ShaderLib.shaderSDFText.value
        shader.use()
        transformUniform(shader, stack)
        shader.v2f("offset", offset)
        shader.v2f("scale", scale)
        texture.bind(0, GPUFiltering.LINEAR, Clamping.CLAMP)
        // if we have a force field applied, subdivide the geometry
        val buffer = if (hasUVAttractors) flat01CubeX10 else flat01Cube
        buffer.draw(shader)
        GFX.check()
    }

    fun draw3DGaussianBlur(
        stack: Matrix4fArrayList,
        size: Float, w: Int, h: Int,
        threshold: Float, isFirst: Boolean,
        isFullscreen: Boolean
    ) {
        val shader = shader3DGaussianBlur
        shader.use()
        transformUniform(shader, stack)
        if (isFirst) shader.v2f("stepSize", 0f, 1f / h)
        else shader.v2f("stepSize", 1f / w, 0f)
        shader.v1f("steps", size * h)
        shader.v1f("threshold", threshold)
        val buffer = if (isFullscreen) SimpleBuffer.flatLarge else SimpleBuffer.flat11
        buffer.draw(shader)
        GFX.check()
    }

    fun draw3DBoxBlur(
        stack: Matrix4fArrayList,
        steps: Int, w: Int, h: Int,
        isFirst: Boolean
    ) {
        val shader = shader3DBoxBlur
        shader.use()
        transformUniform(shader, stack)
        if (isFirst) {
            shader.v2f("stepSize", 0f, 1f / h)
            shader.v1i("steps", steps)
        } else {
            shader.v2f("stepSize", 1f / w, 0f)
            shader.v1i("steps", steps)
        }
        GFX.flat01.draw(shader)
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
        defineAdvancedGraphicalFeatures(shader)
        shader3DUniforms(shader, stack, 1, 1, color, null, Filtering.NEAREST, null)
        circleParams(innerRadius, startDegrees, endDegrees, shader)
        circleBuffer.draw(shader)
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

    fun uploadAttractors0(shader: Shader) {

        // localScale, localOffset not needed
        shader.v1i("forceFieldColorCount", 0)
        shader.v1i("forceFieldUVCount", 0)

    }

}