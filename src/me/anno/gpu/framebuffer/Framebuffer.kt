package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL32
import java.util.*
import kotlin.system.exitProcess

class Framebuffer(
    var name: String, var w: Int, var h: Int, val samples: Int,
    val targets: Array<TargetType>,
    val depthBufferType: DepthBufferType
) {

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

    enum class DepthBufferType {
        NONE,
        INTERNAL,
        TEXTURE
    }

    var needsBlit = true

    val withMultisampling get() = samples > 1
    var msBuffer = if (withMultisampling)
        Framebuffer("$name.ms", w, h, 1, targets, depthBufferType) else null

    var pointer = -1
    var depthRenderBuffer = -1
    var colorRenderBuffer = -1
    var depthTexture: Texture2D? = null

    lateinit var textures: Array<Texture2D>

    fun ensure() {
        if (pointer < 0) create()
    }

    fun bindDirectly(viewport: Boolean) = bind(viewport)
    fun bindDirectly(w: Int, h: Int, viewport: Boolean) = bind(w, h, viewport)

    private fun bind(viewport: Boolean) {
        needsBlit = true
        if (pointer < 0) create()
        glBindFramebuffer(GL_FRAMEBUFFER, pointer)
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
        textures.forEachIndexed { index, texture ->
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + index, tex2D, texture.pointer, 0)
        }
        GFX.check()
        if (targets.size > 1) {// skip array alloc otherwise
            glDrawBuffers(textures.indices.map { it + GL_COLOR_ATTACHMENT0 }.toIntArray())
        } else glDrawBuffer(GL_COLOR_ATTACHMENT0)
        GFX.check()
        when (depthBufferType) {
            DepthBufferType.NONE -> {
            }
            DepthBufferType.INTERNAL -> createDepthBuffer()
            DepthBufferType.TEXTURE -> {
                val depthTexture = Texture2D("$name-depth", w, h, samples) // xD
                depthTexture.createDepth()
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
        } else glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, w, h)
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, renderBuffer)
    }

    private fun resolveTo(target: Framebuffer?) {
        if(!needsBlit) return
        needsBlit = true
        try {
            GFX.check()
            if (target != null) {
                // ensure that it exists
                target.bind(w, h)
                target.unbind()
            }
            GFX.check()
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, target?.pointer ?: 0)
            glBindFramebuffer(GL_READ_FRAMEBUFFER, pointer)
            // if(target == null) glDrawBuffer(GL_BACK)?
            GFX.check()
            // LOGGER.info("Blit $w $h into target $target")
            glBlitFramebuffer(
                0, 0, w, h,
                0, 0, target?.w ?: GFX.width, target?.h ?: GFX.height,
                GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT,
                GL11.GL_NEAREST
            )
            GFX.check()
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
            textures.forEachIndexed { index, texture ->
                texture.bind(offset + index, nearest, clamping)
            }
        }
        GFX.check()
    }

    fun destroy() {
        msBuffer?.destroy()
        if (pointer > -1) {
            glDeleteFramebuffers(pointer)
            pointer = -1
            textures.forEach {
                it.destroy()
            }
            depthTexture?.destroy()
        }
        if (depthRenderBuffer > -1) {
            glDeleteRenderbuffers(depthRenderBuffer)
            depthRenderBuffer = -1
        }
    }

    /*fun use(x: Int, y: Int, w: Int, h: Int, rendering: () -> Unit){
        bind(w, h, false)
        glViewport(x, y, w, h)
        rendering()
        unbind()
    }*/

    /*private fun unbindUntil(){
        var popped = stack.pop()
        while(popped !== this) popped = stack.pop()!!
        stack.pop()!!.bind(true)
    }*/

    private fun unbind() {
        /*val popped = stack.pop()
        if (popped !== this) {
            stack.forEach {
                LOGGER.warn(it.toString())
            }
            throw RuntimeException("Unbind is incorrect... why? am $this, got $popped")
        }
        if (stack.isNotEmpty()) {
            stack.pop()?.bind(true) ?: {
                glBindFramebuffer(GL_FRAMEBUFFER, 0)
                glViewport(0, 0, GFX.width, GFX.height)
            }()
        } else {
            glBindFramebuffer(GL_FRAMEBUFFER, 0)
            glViewport(0, 0, GFX.width, GFX.height)
        }*/
    }

    companion object {

        val LOGGER = LogManager.getLogger(Framebuffer::class)!!

        //val stack = Stack<Framebuffer?>()

        fun bindNullDirectly() = bindNull()

        private fun bindNull() {
            glBindFramebuffer(GL_FRAMEBUFFER, 0)
            //stack.push(null)
        }

    }

    override fun toString(): String =
        "FB[n=$name, i=$pointer, w=$w h=$h s=$samples t=${targets.joinToString()} d=$depthBufferType]"

}