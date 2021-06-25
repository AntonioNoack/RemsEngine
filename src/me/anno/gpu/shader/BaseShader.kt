package me.anno.gpu.shader

import me.anno.gpu.GFX
import me.anno.gpu.RenderState
import me.anno.gpu.deferred.DeferredSettingsV2

/**
 * converts a shader with color, normal, tint and such into
 *  a) a clickable / depth-able shader
 *  b) a flat color shader
 *  c) a deferred shader
 * */
class BaseShader(
    val name: String,
    val vertexSource: String,
    val varyingSource: String,
    val fragmentSource: String
) {

    var textures: List<String>? = null
    var ignoredUniforms = HashSet<String>()

    private val flatShader: Lazy<Shader> = lazy {
        // todo if it does not have tint, then add it?
        // todo what do we do if it writes glFragColor?
        // todo option to use flat shading independent of rendering mode (?)
        var fragment = if ("gl_FragColor" !in fragmentSource) {
            fragmentSource.substring(0, fragmentSource.lastIndexOf('}')) +
                    "gl_FragColor = vec4(finalColor, finalAlpha);\n"
        } else {
            fragmentSource.substring(0, fragmentSource.lastIndexOf('}'))
        }
        fragment += "}"
        GFX.check()
        val shader = ShaderPlus.create(name, vertexSource, varyingSource, fragment)
        shader.setTextureIndices(textures)
        shader.ignoreUniformWarnings(ignoredUniforms)
        shader.v1("drawMode", ShaderPlus.DrawMode.COLOR.id)
        shader.v4("tint", 1f, 1f, 1f, 1f)
        GFX.check()
        shader
    }

    val value: Shader
        get() {
            val renderer = RenderState.currentRenderer
            return when (renderer.deferredSettings) {
                null -> flatShader.value
                else -> get(renderer.deferredSettings)
            }
        }

    fun ignoreUniformWarnings(warnings: Collection<String>) {
        ignoredUniforms += warnings
    }

    fun setTextureIndices(textures: List<String>) {
        this.textures = textures
    }

    private val deferredShaders = HashMap<DeferredSettingsV2, Shader>()
    operator fun get(settings: DeferredSettingsV2): Shader {
        return deferredShaders.getOrPut(settings) {
            val shader = settings.createShader(name, vertexSource, varyingSource, fragmentSource, textures)
            shader.setTextureIndices(textures)
            shader.ignoreUniformWarnings(ignoredUniforms)
            shader.v1("drawMode", ShaderPlus.DrawMode.COLOR.id)
            shader.v4("tint", 1f, 1f, 1f, 1f)
            shader
        }
    }

    fun destroy() {
        if (flatShader.isInitialized()) {
            flatShader.value.destroy()
        }
        for (shader in deferredShaders) shader.value.destroy()
        // deferredShaders.clear()
    }

}