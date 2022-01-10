package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import me.anno.gpu.OpenGL
import me.anno.gpu.debug.DebugGPUStorage
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.utils.maths.Maths
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL13C.GL_MULTISAMPLE
import org.lwjgl.opengl.GL30C.*
import org.lwjgl.opengl.GL32C.GL_TEXTURE_2D_MULTISAMPLE
import org.lwjgl.opengl.GL45C.glCheckNamedFramebufferStatus
import kotlin.system.exitProcess

class Framebuffer(
    var name: String,
    override var w: Int, override var h: Int,
    samples: Int, val targets: Array<TargetType>,
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

    override val samples: Int = Maths.clamp(samples, 1, GFX.maxSamples)

    // todo test this, does this work?
    /**
     * attach another framebuffer, which shares the depth buffer
     * this can be used to draw 3D ui without deferred-rendering,
     * but using the same depth values
     * */
    fun attachFramebufferToDepth(targetCount: Int, fpTargets: Boolean): Framebuffer {
        val fb = Framebuffer(name, w, h, samples, targetCount, fpTargets, DepthBufferType.ATTACHMENT)
        fb.depthAttachment = this
        return fb
    }

    var offsetX = 0
    var offsetY = 0

    // the source of our depth texture
    var depthAttachment: Framebuffer? = null

    val target = if (withMultisampling) GL_TEXTURE_2D_MULTISAMPLE else GL_TEXTURE_2D

    // multiple targets, layout=x require shader version 330+
    // use glBindFragDataLocation instead

    var needsBlit = true

    val withMultisampling get() = samples > 1
    var msBuffer = if (withMultisampling)
        Framebuffer("$name.ms", w, h, 1, targets, depthBufferType) else null

    override var pointer = -1
    var session = 0

    var internalDepthTexture = -1
    override var depthTexture: Texture2D? = null

    lateinit var textures: Array<Texture2D>

    fun checkSession() {
        if (pointer > 0 && session != OpenGL.session) {
            GFX.check()
            session = OpenGL.session
            pointer = -1
            needsBlit = true
            msBuffer?.checkSession()
            depthTexture?.checkSession()
            for (texture in textures) {
                texture.checkSession()
            }
            GFX.check()
            // validate it
            create()
        }
    }

    override fun ensure() {
        checkSession()
        if (pointer < 0) create()
    }

    override fun bindDirectly(viewport: Boolean) = bind(viewport)
    override fun bindDirectly(w: Int, h: Int, viewport: Boolean) = bind(w, h, viewport)

    private fun bind(viewport: Boolean) {
        needsBlit = true
        ensure()
        glBindFramebuffer(GL_FRAMEBUFFER, pointer)
        Frame.lastPtr = pointer
        if (viewport) glViewport(0, 0, w, h)
        //stack.push(this)
        if (withMultisampling) {
            glEnable(GL_MULTISAMPLE)
        } else {
            glDisable(GL_MULTISAMPLE)
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
        val pointer = glGenFramebuffers()
        if (pointer <= 0) throw OutOfMemoryError("Could not generate OpenGL framebuffer")
        LOGGER.debug("Creating $pointer: $name $w $h $samples ${targets.joinToString { it.name }} $depthBufferType")
        session = OpenGL.session
        DebugGPUStorage.fbs.add(this)
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
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + index, texture.target, texture.pointer, 0)
        }
        GFX.check()
        when (targets.size) {
            0 -> glDrawBuffer(GL_NONE)
            1 -> glDrawBuffer(GL_COLOR_ATTACHMENT0)
            else -> glDrawBuffers(textures.indices.map { GL_COLOR_ATTACHMENT0 + it }.toIntArray())
        }
        GFX.check()
        when (depthBufferType) {
            DepthBufferType.NONE -> {
            }
            DepthBufferType.ATTACHMENT -> {
                val texPointer = depthAttachment!!.depthTexture!!.pointer
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, target, texPointer, 0)
            }
            DepthBufferType.INTERNAL -> createDepthBuffer()
            DepthBufferType.TEXTURE, DepthBufferType.TEXTURE_16 -> {
                val depthTexture = Texture2D("$name-depth", w, h, samples)
                depthTexture.createDepth(depthBufferType == DepthBufferType.TEXTURE_16)
                glFramebufferTexture2D(
                    GL_FRAMEBUFFER,
                    GL_DEPTH_ATTACHMENT,
                    depthTexture.target,
                    depthTexture.pointer,
                    0
                )
                this.depthTexture = depthTexture
            }
        }
        GFX.check()
        check(pointer)
        this.pointer = pointer
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
        if (renderBuffer < 0) throw RuntimeException("Failed to create renderbuffer")
        internalDepthTexture = renderBuffer
        glBindRenderbuffer(GL_RENDERBUFFER, renderBuffer)
        val format = GL_DEPTH_COMPONENT // application chooses bytes/pixel
        if (withMultisampling) {
            glRenderbufferStorageMultisample(GL_RENDERBUFFER, samples, format, w, h)
        } else {
            glRenderbufferStorage(GL_RENDERBUFFER, format, w, h)
        }
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, renderBuffer)
        val bytesPerPixel = 4 // a guess for the internal format; worst case
        depthAllocated = Texture2D.allocate(depthAllocated, w * h * bytesPerPixel.toLong())
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
            var bits = 0
            if (targets.isNotEmpty()) bits = bits or GL_COLOR_BUFFER_BIT
            if (depthBufferType != DepthBufferType.NONE) bits = bits or GL_DEPTH_BUFFER_BIT
            glBlitFramebuffer(
                0, 0, w, h,
                0, 0, w, h,
                // we may want to GL_STENCIL_BUFFER_BIT, if present
                bits,
                GL_NEAREST
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

    private fun check(pointer: Int) {
        val state = glCheckNamedFramebufferStatus(pointer, GL_FRAMEBUFFER)
        if (state != GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("Framebuffer is incomplete: ${GFX.getErrorTypeName(state)}")
        }
    }

    fun bindTexture0(shader: Shader, texName: String, nearest: GPUFiltering, clamping: Clamping) {
        val index = shader.getTextureIndex(texName)
        if (index >= 0) {
            checkSession()
            bindTextureI(0, index, nearest, clamping)
        }
    }

    fun bindTexture0(offset: Int = 0, nearest: GPUFiltering, clamping: Clamping) {
        checkSession()
        bindTextureI(0, offset, nearest, clamping)
    }

    fun bindTextureI(index: Int, offset: Int) {
        checkSession()
        bindTextureI(index, offset, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
    }

    fun bindTextureI(index: Int, offset: Int, nearest: GPUFiltering, clamping: Clamping) {
        checkSession()
        if (withMultisampling) {
            val msBuffer = msBuffer!!
            resolveTo(msBuffer)
            msBuffer.bindTextureI(index, offset, nearest, clamping)
        } else {
            textures[index].bind(offset, nearest, clamping)
        }
    }

    fun resolve() {
        if (withMultisampling) {
            resolveTo(msBuffer!!)
            GFX.check()
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
        if (msBuffer != null) {
            msBuffer?.destroyExceptTextures(deleteDepth)
            msBuffer = null
            destroy()
        } else {
            msBuffer?.destroy()
            destroyFramebuffer()
            destroyInternalDepth()
        }
    }

    fun destroyFramebuffer() {
        if (pointer > -1) {
            glDeleteFramebuffers(pointer)
            Frame.invalidate()
            DebugGPUStorage.fbs.remove(this)
            pointer = -1
            for (it in textures) {
                it.destroy()
            }
            depthTexture?.destroy()
        }
    }

    fun destroyInternalDepth() {
        if (internalDepthTexture > -1) {
            glDeleteRenderbuffers(internalDepthTexture)
            depthAllocated = Texture2D.allocate(depthAllocated, 0L)
            internalDepthTexture = -1
        }
    }

    var depthAllocated = 0L

    override fun destroy() {
        if (pointer >= 0) {
            LOGGER.debug("Destroying framebuffer $pointer")
            msBuffer?.destroy()
            destroyFramebuffer()
            destroyInternalDepth()
        }
    }

    fun getColor0(): Texture2D {
        return if (samples > 1) {
            bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
            msBuffer!!.textures.first()
        } else textures.first()
    }

    companion object {

        private val LOGGER = LogManager.getLogger(Framebuffer::class)

        fun bindNullDirectly() = bindNull()

        private fun bindNull() {
            glBindFramebuffer(GL_FRAMEBUFFER, 0)
            Frame.lastPtr = 0
        }

    }

    override fun toString(): String =
        "FB[n=$name, i=$pointer, w=$w h=$h s=$samples t=${targets.joinToString()} d=$depthBufferType]"

}