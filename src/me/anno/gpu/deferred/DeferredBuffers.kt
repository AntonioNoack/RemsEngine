package me.anno.gpu.deferred

import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.Shader
import org.lwjgl.opengl.GL20

/**
 * used by some game tests of me
 * I use VideoStudio as a ui engine
 * maybe I should split it off some time...
 * */
object DeferredBuffers {

    fun getBaseBuffer(settings: DeferredSettings): Framebuffer {
        val layers = settings.layers
        return Framebuffer("main", 1, 1, 1,
            Array(layers.size) { layers[it].type },
            Framebuffer.DepthBufferType.TEXTURE)
    }

    fun getLightBuffer(settings: DeferredSettings): Framebuffer {
        return Framebuffer("light", 1, 1, 1, 1, settings.fpLights, Framebuffer.DepthBufferType.NONE)
    }

    val defaultLayers = listOf(
        DeferredLayer("vec4", "finalColor", TargetType.UByteTarget4), // rgb + reflectivity?
        DeferredLayer("vec3", "finalNormal", TargetType.FloatTarget4), // [-1,+1]*0.5+0.5
        DeferredLayer("vec3", "finalPosition", TargetType.FloatTarget4) // world space? or relative to the player for better precision?
    )

    fun createDeferredShader(
        settings: DeferredSettings,
        shaderName: String, v3D: String, y3D: String, f3D: String, textures: List<String>): Shader {
        val shader = Shader(shaderName, v3D, y3D, settings.f3D + f3D, true)
        shader.glslVersion = 330
        shader.use()
        textures.forEachIndexed { index, textureName ->
            GL20.glUniform1i(shader[textureName], index)
        }
        return shader
    }

}