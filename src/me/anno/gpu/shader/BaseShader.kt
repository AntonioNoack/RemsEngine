package me.anno.gpu.shader

import me.anno.gpu.GFX
import me.anno.gpu.RenderSettings
import me.anno.gpu.deferred.DeferredSettings2

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
        GFX.check()
        shader
    }

    val value: Shader
        get() {
            val renderer = RenderSettings.currentRenderer
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

    private val deferredShaders = HashMap<DeferredSettings2, Shader>()
    operator fun get(settings: DeferredSettings2): Shader {
        return deferredShaders.getOrPut(settings) {
            val shader =
                settings.createPostProcessingShader(name, vertexSource, varyingSource, fragmentSource, textures)
            shader.ignoreUniformWarnings(ignoredUniforms)
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

    // todo converts a shader with color, normal, tint and such into
    //  a) a clickable / depth-able shader
    //  b) a flat color shader
    //  c) a deferred shader
    // todo the render engine decides how it is rendered...
    // todo this could include multiple passes as well...


}