package me.anno.gpu.framebuffer

import me.anno.gpu.GFXState
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D
import me.anno.utils.Color.a01
import me.anno.utils.Color.b01
import me.anno.utils.Color.g01
import me.anno.utils.Color.r01
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector3fc
import org.joml.Vector4fc
import org.lwjgl.opengl.GL11C.*
import org.lwjgl.opengl.GL45

interface IFramebuffer {

    val name: String

    val pointer: Int

    val w: Int
    val h: Int

    val samples: Int

    val numTextures: Int

    fun ensure()

    fun bindDirectly()

    fun bindDirectly(w: Int, h: Int)

    fun destroy()

    fun attachFramebufferToDepth(targetCount: Int, fpTargets: Boolean): IFramebuffer

    fun attachFramebufferToDepth(targets: Array<TargetType>): IFramebuffer

    fun checkSession()

    fun bindTexture0(shader: Shader, texName: String, nearest: GPUFiltering, clamping: Clamping) {
        val index = shader.getTextureIndex(texName)
        if (index >= 0) {
            checkSession()
            bindTextureI(index, 0, nearest, clamping)
        }
    }

    fun bindTexture0(offset: Int = 0, nearest: GPUFiltering, clamping: Clamping) {
        bindTextureI(0, offset, nearest, clamping)
    }

    fun bindTextureI(index: Int, offset: Int) {
        checkSession()
        bindTextureI(index, offset, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
    }

    fun bindTextureI(index: Int, offset: Int, nearest: GPUFiltering, clamping: Clamping)

    fun bindTextures(offset: Int = 0, nearest: GPUFiltering, clamping: Clamping)

    fun getTexture0() = getTextureI(0)

    fun bindTrulyNearest(dstIndex: Int = 0) =
        getTexture0().bindTrulyNearest(dstIndex)

    fun getTextureI(index: Int): ITexture2D

    val depthTexture: ITexture2D?

    fun createBufferedImage(flipY: Boolean, withAlpha: Boolean) =
        getTexture0().createBufferedImage(flipY, withAlpha)

    fun createImage(flipY: Boolean, withAlpha: Boolean) =
        getTexture0().createImage(flipY, withAlpha)

    fun clearColor(color: Int, depth: Boolean = false) =
        clearColor(color.r01(), color.g01(), color.b01(), color.a01(), depth)

    fun clearColor(color: Vector3fc, alpha: Float, depth: Boolean = false) =
        clearColor(color.x(), color.y(), color.z(), alpha, depth)

    fun clearColor(color: Vector4fc, depth: Boolean = false) =
        clearColor(color.x(), color.y(), color.z(), color.w(), depth)

    fun clearColor(r: Float, g: Float, b: Float, a: Float, depth: Boolean = false) {
        Frame.bind()
        glClearColor(r, g, b, a)
        glClearDepth(if (GFXState.depthMode.currentValue.reversedDepth) 0.0 else 1.0)
        glClear(GL_COLOR_BUFFER_BIT or depth.toInt(GL_DEPTH_BUFFER_BIT))
    }

    fun clearColor(color: Int, stencil: Int, depth: Boolean) =
        clearColor(color.r01(), color.g01(), color.b01(), color.a01(), stencil, depth)

    fun clearColor(color: Vector3fc, alpha: Float, stencil: Int, depth: Boolean = false) =
        clearColor(color.x(), color.y(), color.z(), alpha, stencil, depth)


    fun clearColor(color: Vector4fc, stencil: Int, depth: Boolean = false) =
        clearColor(color.x(), color.y(), color.z(), color.w(), stencil, depth)

    fun clearColor(r: Float, g: Float, b: Float, a: Float, stencil: Int, depth: Boolean = false) {
        Frame.bind()
        glClearStencil(stencil)
        glClearColor(r, g, b, a)
        glClearDepth(if (GFXState.depthMode.currentValue.reversedDepth) 0.0 else 1.0)
        glClear(GL_COLOR_BUFFER_BIT or GL_STENCIL_BUFFER_BIT or depth.toInt(GL_DEPTH_BUFFER_BIT))
    }

    fun clearDepth() {
        Frame.bind()
        glClearDepth(if (GFXState.depthMode.currentValue.reversedDepth) 0.0 else 1.0)
        glClear(GL_DEPTH_BUFFER_BIT)
    }

}