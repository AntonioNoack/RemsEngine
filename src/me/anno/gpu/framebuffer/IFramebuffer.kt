package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.utils.Color.a01
import me.anno.utils.Color.b01
import me.anno.utils.Color.g01
import me.anno.utils.Color.r01
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.lists.Lists.createList
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.opengl.GL46C.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL46C.GL_DEPTH_BUFFER_BIT
import org.lwjgl.opengl.GL46C.GL_STENCIL_BUFFER_BIT
import org.lwjgl.opengl.GL46C.glClear
import org.lwjgl.opengl.GL46C.glClearColor
import org.lwjgl.opengl.GL46C.glClearDepth
import org.lwjgl.opengl.GL46C.glClearStencil

interface IFramebuffer {

    var name: String

    val pointer: Int

    val width: Int
    val height: Int

    val samples: Int
    val numTextures: Int

    var isSRGBMask: Int // 32 layers should be enough for any buffer...

    val depthBufferType: DepthBufferType

    fun ensure()
    fun ensureSize(newWidth: Int, newHeight: Int, newDepth: Int)

    fun bindDirectly()

    fun bindDirectly(w: Int, h: Int)

    fun destroy()

    fun attachFramebufferToDepth(name: String, targetCount: Int, fpTargets: Boolean): IFramebuffer {
        return attachFramebufferToDepth(name, createTargets(targetCount, fpTargets))
    }

    /**
     * attach another framebuffer, which shares the depth buffer
     * this can be used to draw 3D ui without deferred-rendering,
     * but using the same depth values
     * */
    fun attachFramebufferToDepth(name: String, targets: List<TargetType>): IFramebuffer

    fun checkSession()

    fun getTargetType(slot: Int): TargetType

    fun bindTexture0(shader: Shader, texName: String, nearest: Filtering, clamping: Clamping) {
        val index = shader.getTextureIndex(texName)
        if (index >= 0) {
            checkSession()
            bindTextureI(index, 0, nearest, clamping)
        }
    }

    fun bindTexture0(offset: Int = 0, nearest: Filtering, clamping: Clamping) {
        bindTextureI(0, offset, nearest, clamping)
    }

    fun bindTextureI(index: Int, offset: Int) {
        bindTextureI(index, offset, Filtering.TRULY_NEAREST, Clamping.CLAMP)
    }

    fun bindTextureI(index: Int, offset: Int, nearest: Filtering, clamping: Clamping)

    fun bindTextures(offset: Int = 0, nearest: Filtering, clamping: Clamping)

    fun getTexture0() = getTextureI(0)
    fun getTexture0MS() = getTextureIMS(0)

    fun bindTrulyNearest(offset: Int = 0) = bindTextures(offset, Filtering.TRULY_NEAREST, Clamping.CLAMP)

    fun bindTrulyNearestMS(offset: Int = 0) {
        bindTrulyNearest(offset)
    }

    fun getTextureI(index: Int): ITexture2D
    fun getTextureILazy(index: Int): ITexture2D = getTextureI(index)
    fun getTextureIMS(index: Int): ITexture2D = getTextureI(index)

    val depthTexture: ITexture2D?
    val depthMask: Int

    fun createImage(flipY: Boolean, withAlpha: Boolean) =
        getTexture0().createdOrNull()?.createImage(flipY, withAlpha)

    fun clearColor(color: Int, depth: Boolean = false) =
        clearColor(color.r01(), color.g01(), color.b01(), color.a01(), depth)

    fun clearColor(color: Int, alpha: Float, depth: Boolean = false) =
        clearColor(color.r01(), color.g01(), color.b01(), alpha, depth)

    fun clearColor(color: Vector3f, alpha: Float, depth: Boolean = false) =
        clearColor(color.x, color.y, color.z, alpha, depth)

    fun clearColor(color: Vector4f, depth: Boolean = false) =
        clearColor(color.x, color.y, color.z, color.w, depth)

    fun clearColor(r: Float, g: Float, b: Float, a: Float, depth: Boolean = false) {
        if (isBound()) {
            Frame.bind()
            GFXState.depthMask.use(true) {
                glClearColor(r, g, b, a)
                setClearValue()
                glClear(GL_COLOR_BUFFER_BIT or depth.toInt(GL_DEPTH_BUFFER_BIT))
            }
        } else {
            useFrame(this) {
                assertTrue(isBound())
                clearColor(r, g, b, a, depth)
            }
        }
    }

    fun clearColor(color: Int, stencil: Int, depth: Boolean) =
        clearColor(color.r01(), color.g01(), color.b01(), color.a01(), stencil, depth)

    fun clearColor(color: Vector3f, alpha: Float, stencil: Int, depth: Boolean = false) =
        clearColor(color.x, color.y, color.z, alpha, stencil, depth)

    fun clearColor(color: Vector4f, stencil: Int, depth: Boolean = false) =
        clearColor(color.x, color.y, color.z, color.w, stencil, depth)

    fun clearColor(r: Float, g: Float, b: Float, a: Float, stencil: Int, depth: Boolean = false) {
        if (isBound()) {
            Frame.bind()
            GFXState.depthMask.use(true) {
                glClearStencil(stencil)
                glClearColor(r, g, b, a)
                setClearValue()
                glClear(GL_COLOR_BUFFER_BIT or GL_STENCIL_BUFFER_BIT or depth.toInt(GL_DEPTH_BUFFER_BIT))
            }
        } else {
            useFrame(this) {
                clearColor(r, g, b, a, stencil, depth)
            }
        }
    }

    fun clearDepth() {
        if (isBound()) {
            Frame.bind()
            GFXState.depthMask.use(true) {
                setClearValue()
                glClear(GL_DEPTH_BUFFER_BIT)
            }
        } else {
            useFrame(this) {
                clearDepth()
            }
        }
    }

    private fun setClearValue() {
        GFXState.bindDepthMask()
        glClearDepth(GFXState.depthMode.currentValue.skyDepth)
    }

    fun isBound(): Boolean = GFXState.currentBuffer == this

    fun use(index: Int, renderer: Renderer, render: () -> Unit) {
        GFXState.renderers[index] = renderer
        GFXState.framebuffer.use(this, render)
    }

    companion object {
        fun createTargets(targetCount: Int, fpTargets: Boolean): List<TargetType> {
            val target = if (fpTargets) TargetType.Float32x4 else TargetType.UInt8x4
            return createList(targetCount, target)
        }

        fun createFramebuffer(
            name: String, width: Int, height: Int, samples: Int,
            targetTypes: List<TargetType>, depthBufferType: DepthBufferType
        ): IFramebuffer {
            return if (targetTypes.size <= GFX.maxColorAttachments) {
                Framebuffer(
                    name, width, height, samples,
                    targetTypes, depthBufferType
                )
            } else {
                MultiFramebuffer(
                    name, width, height, samples,
                    targetTypes, depthBufferType
                )
            }
        }
    }
}