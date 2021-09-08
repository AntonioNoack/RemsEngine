package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL32
import kotlin.system.exitProcess

class Framebuffer(
    var name: String,
    override var w: Int, override var h: Int,
    val samples: Int, val targets: Array<TargetType>,
    val depthBufferType: DepthBufferType
) : IFramebuffer {

    constructor(
        name: String, w: Int, h: Int, samples: Int,
        targetCount: Int,
        fpTargets: Boolean,
        depthBufferType: DepthBufferType
    ) : this(
        name, w, h, samples, if (fpTargets)
            Array(targetCount) { TargetType.FloatTarget4 } else
            Array(targetCount) { TargetType.UByteTarget4 }, depthBufferType
    )

    // multiple targets, layout=x require shader version 330+
    // use glBindFragDataLocation instead

    var needsBlit = true

    val withMultisampling get() = samples > 1
    var msBuffer = if (withMultisampling)
        Framebuffer("$name.ms", w, h, 1, targets, depthBufferType) else null

    override var pointer = -1
    var depthRenderBuffer = -1
    override var depthTexture: Texture2D? = null

    lateinit var textures: Array<Texture2D>

    override fun ensure() {
        if (pointer < 0) create()
    }

    override fun bindDirectly(viewport: Boolean) = bind(viewport)
    override fun bindDirectly(w: Int, h: Int, viewport: Boolean) = bind(w, h, viewport)

    private fun bind(viewport: Boolean) {
        needsBlit = true
        if (pointer < 0) create()
        glBindFramebuffer(GL_FRAMEBUFFER, pointer)
        Frame.lastPtr = pointer
        if (viewport) glViewport(0, 0, w, h)
        //stack.push(this)
        if (withMultisampling) {
            GL11.glEnable(GL13.GL_MULTISAMPLE)
        } else {
            GL11.glDisable(GL13.GL_MULTISAMPLE)
        }
    }

    private fun bind(newWidth: Int, newHeight: Int, viewport: Boolean = true) {
        needsBlit = true
        if (newWidth != w || newHeight != h) {
            w = newWidth
            h = newHeight
            GFX.check()
            destroy()
            GFX.check()
            create()
            if (viewport) {
                // not done by create...
                glViewport(0, 0, newWidth, newHeight)
            }
            GFX.check()
        } else {
            GFX.check()
            bind(viewport)
            GFX.check()
        }
    }

    private fun create() {
        Frame.invalidate()
        // LOGGER.info("w: $w, h: $h, samples: $samples, targets: $targetCount x fp32? $fpTargets")
        GFX.check()
        val tex2D = if (withMultisampling) GL32.GL_TEXTURE_2D_MULTISAMPLE else GL_TEXTURE_2D
        pointer = glGenFramebuffers()
        if (pointer < 0) throw RuntimeException()
        glBindFramebuffer(GL_FRAMEBUFFER, pointer)
        Frame.lastPtr = pointer
        //stack.push(this)
        GFX.check()
        textures = Array(targets.size) { index ->
            val texture = Texture2D("$name-tex[$index]", w, h, samples)
            texture.create(targets[index])
            // if (fpTargets) texture.createFP32()
            // else texture.create()
            // LOGGER.info("create/textures-array $w $h $samples $fpTargets")
            GFX.check()
            texture
        }
        GFX.check()
        val textures = textures
        for (index in textures.indices) {
            val texture = textures[index]
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + index, tex2D, texture.pointer, 0)
        }
        GFX.check()
        when (targets.size) {
            0 -> glDrawBuffer(GL_NONE)
            1 -> glDrawBuffer(GL_COLOR_ATTACHMENT0)
            else -> glDrawBuffers(textures.indices.map { it + GL_COLOR_ATTACHMENT0 }.toIntArray())
        }
        GFX.check()
        when (depthBufferType) {
            DepthBufferType.NONE -> {
            }
            DepthBufferType.INTERNAL -> createDepthBuffer()
            DepthBufferType.TEXTURE, DepthBufferType.TEXTURE_16 -> {
                val depthTexture = Texture2D("$name-depth", w, h, samples) // xD
                depthTexture.createDepth(depthBufferType == DepthBufferType.TEXTURE_16)
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, tex2D, depthTexture.pointer, 0)
                this.depthTexture = depthTexture
            }
        }
        GFX.check()
        check()
    }

    /*fun createColorBuffer(){
        if(!withMultisampling) throw RuntimeException()
        val renderBuffer = glGenRenderbuffers()
        colorRenderBuffer = renderBuffer
        if(renderBuffer < 0) throw RuntimeException()
        glBindRenderbuffer(GL_RENDERBUFFER, renderBuffer)
        glRenderbufferStorageMultisample(GL_RENDERBUFFER, samples, GL_RGBA8, w, h)
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, renderBuffer)
    }*/

    private fun createDepthBuffer() {
        val renderBuffer = glGenRenderbuffers()
        depthRenderBuffer = renderBuffer
        if (renderBuffer < 0) throw RuntimeException()
        glBindRenderbuffer(GL_RENDERBUFFER, renderBuffer)
        if (withMultisampling) {
            glRenderbufferStorageMultisample(GL_RENDERBUFFER, samples, GL_DEPTH_COMPONENT, w, h)
        } else {
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, w, h)
        }
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, renderBuffer)
    }

    private fun resolveTo(target: Framebuffer) {
        if (!needsBlit) return
        needsBlit = false
        try {

            val w = w
            val h = h

            GFX.check()

            // ensure that we exist
            if (pointer < 0) {
                bind(w, h)
            }

            // ensure that it exists
            // + bind it, it seems important
            target.bind(w, h)

            GFX.check()

            // LOGGER.info("Blit: $pointer -> ${target.pointer}")
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, target.pointer)
            glBindFramebuffer(GL_READ_FRAMEBUFFER, pointer)
            // if(target == null) glDrawBuffer(GL_BACK)?

            GFX.check()

            // LOGGER.info("Blit $w $h into target $target")
            glBlitFramebuffer(
                0, 0, w, h,
                0, 0, w, h,
                // we may want to GL_STENCIL_BUFFER_BIT, if present
                GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT,
                GL11.GL_NEAREST
            )

            GFX.check()

            // restore the old binding
            Frame.invalidate()
            Frame.bind()

        } catch (e: Exception) {
            e.printStackTrace()
            exitProcess(1)
        }
    }

    private fun check() {
        val state = glCheckFramebufferStatus(GL_FRAMEBUFFER)
        if (state != GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("Framebuffer is incomplete: $state")
        }
    }

    fun bindTexture0(offset: Int = 0, nearest: GPUFiltering, clamping: Clamping) {
        bindTextureI(0, offset, nearest, clamping)
    }

    fun bindTextureI(index: Int, offset: Int) {
        bindTextureI(index, offset, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
    }

    fun bindTextureI(index: Int, offset: Int, nearest: GPUFiltering, clamping: Clamping) {
        if (withMultisampling) {
            val msBuffer = msBuffer!!
            resolveTo(msBuffer)
            msBuffer.bindTextureI(index, offset, nearest, clamping)
        } else {
            textures[index].bind(offset, nearest, clamping)
        }
    }

    fun bindTextures(offset: Int = 0, nearest: GPUFiltering, clamping: Clamping) {
        GFX.check()
        if (withMultisampling) {
            val msBuffer = msBuffer!!
            resolveTo(msBuffer)
            GFX.check()
            msBuffer.bindTextures(offset, nearest, clamping)
        } else {
            for ((index, texture) in textures.withIndex()) {
                texture.bind(offset + index, nearest, clamping)
            }
        }
        GFX.check()
    }

    fun destroyExceptTextures(deleteDepth: Boolean) {
        if(msBuffer != null){
            msBuffer?.destroyExceptTextures(deleteDepth)
            msBuffer = null
            destroy()
        } else {
            msBuffer?.destroy()
            if (pointer > -1) {
                glDeleteFramebuffers(pointer)
                Frame.invalidate()
                pointer = -1
                if(deleteDepth) depthTexture?.destroy()
            }
            if (depthRenderBuffer > -1) {
                glDeleteRenderbuffers(depthRenderBuffer)
                depthRenderBuffer = -1
            }
        }
    }

    override fun destroy() {
        msBuffer?.destroy()
        if (pointer > -1) {
            glDeleteFramebuffers(pointer)
            Frame.invalidate()
            pointer = -1
            for (it in textures) {
                it.destroy()
            }
            depthTexture?.destroy()
        }
        if (depthRenderBuffer > -1) {
            glDeleteRenderbuffers(depthRenderBuffer)
            depthRenderBuffer = -1
        }
    }

    fun getColor0(): Texture2D {
        return if (samples > 1) {
            bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
            msBuffer!!.textures.first()
        } else textures.first()
    }

    companion object {

        // todo keep track of the allocated bytes
        // todo render buffers
        // todo internal depth textures

        // private val LOGGER = LogManager.getLogger(Framebuffer::class)!!

        fun bindNullDirectly() = bindNull()

        private fun bindNull() {
            glBindFramebuffer(GL_FRAMEBUFFER, 0)
            Frame.lastPtr = 0
        }

    }

    override fun toString(): String =
        "FB[n=$name, i=$pointer, w=$w h=$h s=$samples t=${targets.joinToString()} d=$depthBufferType]"

}