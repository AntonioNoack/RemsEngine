package me.anno.ecs.components.light

import me.anno.gpu.framebuffer.CubemapFramebuffer
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.image.ImageGPUCache
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef

// todo or render from shader
// todo always find closest to object
// todo bake surrounding lighting for reflections
// todo blur
// todo hdr

// todo is this a light component?
//  - only 1 per object
//  - closest to object
//  - few per scene (?)

// todo or we could say, that only elements in this AABB are valid receivers :)

/**
 * environment map for reflections,
 * radiance map, sky map, ...
 * */
class EnvironmentMap : LightComponentBase() {

    enum class SourceType {
        TEXTURE, // could have different projections...
        SHADER,
        ENVIRONMENT
    }

    var resolution = 1024

    var near = 0.01
    var type = SourceType.ENVIRONMENT

    var shader: BaseShader? = null
    var textureSource: FileReference = InvalidRef

    var buffer: CubemapFramebuffer? = null

    var needsUpdate = true
    var autoUpdate = true

    override fun onUpdate() {
        super.onUpdate()
        if (type != SourceType.TEXTURE || buffer?.size != resolution) {
            buffer?.destroy()
            buffer = CubemapFramebuffer("", resolution, 1, true, DepthBufferType.TEXTURE_16)
            needsUpdate = true
        } else {
            buffer?.destroy()
            buffer = null
        }
        val buffer = buffer
        if (buffer != null && (needsUpdate || autoUpdate)) {
            needsUpdate = false
            // todo draw update
            // todo simple color renderer
            buffer.draw(Renderer.copyRenderer) { side ->
                // todo build camera
                // todo fill pipeline
                // todo draw scene

                // todo theoretically, this is the same as with the shadow maps, just not depth rendering

            }
        }
    }

    fun canBind(): Boolean {
        return when (type) {
            SourceType.TEXTURE -> ImageGPUCache.getImage(textureSource, textureTimeout, true) != null
            else -> buffer != null
        }
    }

    fun bind(index: Int) {
        when (type) {
            SourceType.TEXTURE -> {
                val texture = ImageGPUCache.getImage(textureSource, textureTimeout, true)
                texture!!.bind(index)
            }
            else -> {
                val buffer = buffer
                buffer!!.bindTexture0(0, GPUFiltering.LINEAR, Clamping.CLAMP)
            }
        }
    }

    override fun clone(): EnvironmentMap {
        val clone = EnvironmentMap()
        copy(clone)
        return EnvironmentMap()
    }

    companion object {
        val textureTimeout = 10000L
    }

}