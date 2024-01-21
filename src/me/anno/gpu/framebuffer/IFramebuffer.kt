package me.anno.gpu.framebuffer

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
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.opengl.GL46C.GL_COLOR
import org.lwjgl.opengl.GL46C.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL46C.GL_DEPTH_BUFFER_BIT
import org.lwjgl.opengl.GL46C.GL_STENCIL_BUFFER_BIT
import org.lwjgl.opengl.GL46C.glClear
import org.lwjgl.opengl.GL46C.glClearBufferfv
import org.lwjgl.opengl.GL46C.glClearColor
import org.lwjgl.opengl.GL46C.glClearDepth
import org.lwjgl.opengl.GL46C.glClearStencil
import java.nio.ByteBuffer
import java.nio.ByteOrder

interface IFramebuffer {

    val name: String

    val pointer: Int

    val width: Int
    val height: Int

    val samples: Int

    val numTextures: Int

    fun ensure()

    fun bindDirectly()

    fun bindDirectly(w: Int, h: Int)

    fun destroy()

    fun attachFramebufferToDepth(name: String, targetCount: Int, fpTargets: Boolean): IFramebuffer {
        return attachFramebufferToDepth(name, createTargets(targetCount, fpTargets))
    }

    fun attachFramebufferToDepth(name: String, targets: Array<TargetType>): IFramebuffer

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
    fun getTextureIMS(index: Int): ITexture2D = getTextureI(index)

    val depthTexture: ITexture2D?

    fun createImage(flipY: Boolean, withAlpha: Boolean) =
        getTexture0().createImage(flipY, withAlpha)

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
            glClearColor(r, g, b, a)
            glClearDepth(if (GFXState.depthMode.currentValue.reversedDepth) 0.0 else 1.0)
            glClear(GL_COLOR_BUFFER_BIT or depth.toInt(GL_DEPTH_BUFFER_BIT))
        } else {
            useFrame(this) {
                clearColor(r, g, b, a, depth)
            }
        }
    }

    fun clearColor(colors: Array<Vector4f>, depth: Boolean = false) {
        if (isBound()) {
            Frame.bind()
            val tmp = tmp4f
            for (i in colors.indices) {
                tmp.put(0, colors[i].x)
                tmp.put(1, colors[i].y)
                tmp.put(2, colors[i].z)
                tmp.put(3, colors[i].w)
                glClearBufferfv(GL_COLOR, i, tmp)
            }
            if (depth) clearDepth()
        } else {
            useFrame(this) {
                clearColor(colors, depth)
            }
        }
    }

    fun clearColor(colors: IntArray, depth: Boolean = false) {
        if (isBound()) {
            Frame.bind()
            val tmp = tmp4f
            for (i in colors.indices) {
                tmp.put(0, colors[i].r01())
                tmp.put(1, colors[i].g01())
                tmp.put(2, colors[i].b01())
                tmp.put(3, colors[i].a01())
                glClearBufferfv(GL_COLOR, i, tmp)
            }
            if (depth) clearDepth()
        } else {
            useFrame(this) {
                clearColor(colors, depth)
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
            glClearStencil(stencil)
            glClearColor(r, g, b, a)
            glClearDepth(if (GFXState.depthMode.currentValue.reversedDepth) 0.0 else 1.0)
            glClear(GL_COLOR_BUFFER_BIT or GL_STENCIL_BUFFER_BIT or depth.toInt(GL_DEPTH_BUFFER_BIT))
        } else {
            useFrame(this) {
                clearColor(r, g, b, a, stencil, depth)
            }
        }
    }

    fun clearDepth() {
        if (isBound()) {
            Frame.bind()
            glClearDepth(if (GFXState.depthMode.currentValue.reversedDepth) 0.0 else 1.0)
            glClear(GL_DEPTH_BUFFER_BIT)
        } else {
            useFrame(this) {
                clearDepth()
            }
        }
    }

    fun isBound(): Boolean {
        val curr = GFXState.currentBuffer
        return curr == this
    }

    fun use(index: Int, renderer: Renderer, render: () -> Unit) {
        GFXState.renderers[index] = renderer
        GFXState.framebuffer.use(this, render)
    }

    companion object {
        private val tmp4f = ByteBuffer.allocateDirect(16).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        fun createTargets(targetCount: Int, fpTargets: Boolean): Array<TargetType> {
            val target = if (fpTargets) TargetType.Float32x4 else TargetType.UInt8x4
            return Array(targetCount) { target }
        }
    }
}