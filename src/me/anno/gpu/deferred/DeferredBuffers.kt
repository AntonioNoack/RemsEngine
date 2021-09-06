package me.anno.gpu.deferred

import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable

/**
 * used by some game tests of me
 * I use VideoStudio as a ui engine
 * maybe I should split it off some time...
 * */
object DeferredBuffers {

    fun getBaseBuffer(settings: DeferredSettingsV1): Framebuffer {
        val layers = settings.layers
        return Framebuffer(
            "main", 1, 1, 1,
            Array(layers.size) { layers[it].type },
            DepthBufferType.TEXTURE
        )
    }

    fun getLightBuffer(settings: DeferredSettingsV1): Framebuffer {
        return Framebuffer("light", 1, 1, 1, 1, settings.fpLights, DepthBufferType.NONE)
    }

    val defaultLayers = listOf(
        // rgb + reflectivity?
        DeferredLayer("vec4", DeferredLayerType.COLOR.glslName, TargetType.UByteTarget4),
        // [-1,+1]*0.5+0.5
        DeferredLayer("vec3", DeferredLayerType.NORMAL.glslName, TargetType.FloatTarget4),
        // world space? or relative to the player for better precision?
        DeferredLayer("vec3", DeferredLayerType.POSITION.glslName, TargetType.FloatTarget4)
    )

    fun createDeferredShader(
        settings: DeferredSettingsV1,
        shaderName: String, g3D: String?, v3D: String, y3D: List<Variable>, f3D: String, textures: List<String>
    ): Shader {
        val shader = Shader(shaderName, g3D, v3D, y3D, settings.f3D + f3D, true)
        shader.glslVersion = 330
        shader.setTextureIndices(textures)
        return shader
    }

    fun createDeferredShader(
        settings: DeferredSettingsV1,
        shaderName: String, v3D: String, y3D: List<Variable>, f3D: String, textures: List<String>
    ): Shader {
        return createDeferredShader(settings, shaderName, null, v3D, y3D, f3D, textures)
    }

}