package me.anno.gpu.framebuffer

import me.anno.Build
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.debug.DebugGPUStorage
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.maths.Maths
import me.anno.utils.OS
import org.lwjgl.opengl.GL13C.GL_MULTISAMPLE
import org.lwjgl.opengl.GL30C.*
import org.lwjgl.opengl.GL32C.GL_TEXTURE_2D_MULTISAMPLE

class Framebuffer(
    override var name: String,
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

    constructor(
        name: String,
        w: Int,
        h: Int,
        targets: Array<TargetType>,
        depthBufferType: DepthBufferType = DepthBufferType.NONE
    ) : this(name, w, h, 1, targets, depthBufferType)

    constructor(
        name: String,
        w: Int,
        h: Int,
        target: TargetType,
        depthBufferType: DepthBufferType = DepthBufferType.NONE
    ) :
            this(name, w, h, 1, arrayOf(target), depthBufferType)

    override val samples: Int = Maths.clamp(samples, 1, GFX.maxSamples)
    override val numTextures: Int = targets.size

    /**
     * attach another framebuffer, which shares the depth buffer
     * this can be used to draw 3D ui without deferred-rendering,
     * but using the same depth values
     * */
    override fun attachFramebufferToDepth(targetCount: Int, fpTargets: Boolean): IFramebuffer {
        return if (targetCount <= GFX.maxColorAttachments) {
            val buffer = Framebuffer(name, w, h, samples, targetCount, fpTargets, DepthBufferType.ATTACHMENT)
            buffer.depthAttachment = this
            buffer
        } else {
            val buffer = MultiFramebuffer(name, w, h, samples, targetCount, fpTargets, DepthBufferType.ATTACHMENT)
            for (it in buffer.targetsI) it.depthAttachment = this
            buffer
        }
    }

    /**
     * attach another framebuffer, which shares the depth buffer
     * this can be used to draw 3D ui without deferred-rendering,
     * but using the same depth values
     * */
    override fun attachFramebufferToDepth(targets: Array<TargetType>): IFramebuffer {
        if (depthBufferType != DepthBufferType.TEXTURE && depthBufferType != DepthBufferType.TEXTURE_16)
            throw IllegalStateException("Cannot attach depth to framebuffer without depth texture")
        return if (targets.size <= GFX.maxColorAttachments) {
            val buffer = Framebuffer(name, w, h, samples, targets, DepthBufferType.ATTACHMENT)
            buffer.depthAttachment = this
            buffer
        } else {
            val buffer = MultiFramebuffer(name, w, h, samples, targets, DepthBufferType.ATTACHMENT)
            for (it in buffer.targetsI) it.depthAttachment = this
            buffer
        }
    }

    var offsetX = 0
    var offsetY = 0
    var lastDraw = 0L

    // the source of our depth texture
    var depthAttachedPtr = -1
    var depthAttachment: Framebuffer? = null

    val target = if (withMultisampling) GL_TEXTURE_2D_MULTISAMPLE else GL_TEXTURE_2D

    var autoUpdateMipmaps = true

    // multiple targets, layout=x require shader version 330+
    // use bindFragDataLocation instead

    var needsBlit = true

    val withMultisampling get() = samples > 1
    var ssBuffer = if (withMultisampling)
        Framebuffer("$name.ms", w, h, 1, targets, depthBufferType) else null

    override var pointer = 0
    var session = 0

    var internalDepthTexture = 0
    override var depthTexture: Texture2D? = null

    lateinit var textures: Array<Texture2D>

    override fun checkSession() {
        if (pointer != 0 && session != GFXState.session) {
            GFX.check()
            session = GFXState.session
            pointer = 0
            needsBlit = true
            ssBuffer?.checkSession()
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
        if (pointer == 0) create()
    }

    override fun bindDirectly() = bind()
    override fun bindDirectly(w: Int, h: Int) {
        ensureSize(w, h)
        bind()
    }

    fun bind() {

        needsBlit = true

        // if the depth-attachment base changed, we need to recreate this texture
        val da = depthAttachment
        if (da != null) {
            if (da.depthTexture!!.pointer != depthAttachedPtr) {
                destroy()
            }
            if ((w != da.w || h != da.h)) {
                throw IllegalStateException("Depth is not matching dimensions, $w x $h vs ${da.w} x ${da.h}")
            }
        }

        ensure()

        if (da != null && da.depthTexture!!.pointer != depthAttachedPtr) {
            throw IllegalStateException("Depth attachment could not be recreated! ${da.pointer}, ${da.depthTexture!!.pointer} != $depthAttachedPtr")
        }

        bindFramebuffer(GL_FRAMEBUFFER, pointer)
        if (!OS.isWeb) {// not defined in WebGL
            if (withMultisampling) {
                glEnable(GL_MULTISAMPLE)
            } else {
                glDisable(GL_MULTISAMPLE)
            }
        }
    }

    private fun ensureSize(newWidth: Int, newHeight: Int) {
        if (newWidth != w || newHeight != h) {
            w = newWidth
            h = newHeight
            GFX.check()
            destroy()
            GFX.check()
            create()
            GFX.check()
        }
    }

    val usesCRBs = samples > 1 // if you need multi-sampled textures, write me :)

    fun create() {
        Frame.invalidate()
        GFX.check()
        val pointer = glGenFramebuffers()
        if (pointer == 0) throw OutOfMemoryError("Could not generate OpenGL framebuffer")
        session = GFXState.session
        if (Build.isDebug) DebugGPUStorage.fbs.add(this)
        bindFramebuffer(GL_FRAMEBUFFER, pointer)
        Frame.lastPtr = pointer
        val w = w
        val h = h
        if (w * h < 1) throw RuntimeException("Invalid framebuffer size $w x $h")
        GFX.check()
        if (usesCRBs) {
            colorRenderBuffers = IntArray(targets.size) {
                val target = targets[it]
                createColorBuffer(GL_COLOR_ATTACHMENT0 + it, target.internalFormat, target.bytesPerPixel)
            }
        } else textures = Array(targets.size) { index ->
            val texture = Texture2D("$name-tex[$index]", w, h, samples)
            texture.autoUpdateMipmaps = autoUpdateMipmaps
            texture.create(targets[index])
            GFX.check()
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + index, texture.target, texture.pointer, 0)
            texture
        }
        GFX.check()
        when (targets.size) {
            0 -> glDrawBuffer(GL_NONE)
            1 -> glDrawBuffer(GL_COLOR_ATTACHMENT0)
            else -> glDrawBuffers(attachments[targets.size - 2])
        }
        GFX.check()
        // cannot use depth-texture with color render buffers... why ever...
        val depthBufferType = if (usesCRBs && depthBufferType == DepthBufferType.TEXTURE)
            DepthBufferType.INTERNAL else depthBufferType
        when (depthBufferType) {
            DepthBufferType.NONE -> {
            }
            DepthBufferType.ATTACHMENT -> {
                val texPointer = depthAttachment?.depthTexture?.pointer
                    ?: throw IllegalStateException("Depth Attachment was not found in $name, ${depthAttachment}.${depthAttachment?.depthTexture}")
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, target, texPointer, 0)
                depthAttachedPtr = texPointer
            }
            DepthBufferType.INTERNAL -> createDepthBuffer()
            DepthBufferType.TEXTURE, DepthBufferType.TEXTURE_16 -> {
                val depthTexture = Texture2D("$name-depth", w, h, samples)
                depthTexture.autoUpdateMipmaps = autoUpdateMipmaps
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
        check()
        this.pointer = pointer
    }

    // could be used in the future :)
    // we don't read multisampled textures currently anyway
    var colorRenderBuffers: IntArray? = null
    fun createColorBuffer(
        attachment: Int = GL_COLOR_ATTACHMENT0,
        format: Int = GL_RGBA8,
        bytesPerPixel: Int,
    ): Int {
        val renderBuffer = glGenRenderbuffers()
        glBindRenderbuffer(GL_RENDERBUFFER, renderBuffer)
        if (samples > 1) {
            glRenderbufferStorageMultisample(GL_RENDERBUFFER, samples, format, w, h)
        } else {
            glRenderbufferStorage(GL_RENDERBUFFER, format, w, h)
        }
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, attachment, GL_RENDERBUFFER, renderBuffer)
        GFX.check()
        renderBufferAllocated = Texture2D.allocate(
            renderBufferAllocated,
            renderBufferAllocated + w * h * bytesPerPixel.toLong() * samples
        )
        return renderBuffer
    }

    fun createColorBuffer(
        attachment: Int = GL_COLOR_ATTACHMENT0,
        targetType: TargetType,
    ): Int = createColorBuffer(attachment, targetType.internalFormat, targetType.bytesPerPixel)

    fun createDepthBuffer() {
        internalDepthTexture = when (depthBufferType) {
            // these texture types MUST be the same as for the texture creation process
            DepthBufferType.TEXTURE -> createColorBuffer(GL_DEPTH_ATTACHMENT, TargetType.DEPTH32F)
            DepthBufferType.TEXTURE_16 -> createColorBuffer(GL_DEPTH_ATTACHMENT, TargetType.DEPTH16)
            else -> createColorBuffer(GL_DEPTH_ATTACHMENT, GL_DEPTH_COMPONENT, 4) // 4 is worst-case assumed
        }
    }

    fun blitTo(target: IFramebuffer) {
        if (!needsBlit) return
        needsBlit = false

        val w = w
        val h = h

        GFX.check()

        // ensure that we exist
        ensure()

        // ensure that it exists
        // + bind it, it seems important
        if (target is Framebuffer) target.ensureSize(w, h)
        target.ensure()

        GFX.check()

        if (pointer == 0 || (target.pointer == 0 && target != NullFramebuffer))
            throw RuntimeException("Something went wrong $this -> $target")

        // LOGGER.info("Blit: $pointer -> ${target.pointer}")
        bindFramebuffer(GL_DRAW_FRAMEBUFFER, target.pointer)
        bindFramebuffer(GL_READ_FRAMEBUFFER, pointer)
        // if(target == null) glDrawBuffer(GL_BACK)?

        GFX.check()

        // LOGGER.info("Blit $w $h into target $target")
        var bits = 0
        if (targets.isNotEmpty()) bits = bits or GL_COLOR_BUFFER_BIT
        if (depthBufferType != DepthBufferType.NONE) bits = bits or GL_DEPTH_BUFFER_BIT

        glBlitFramebuffer(
            0, 0, w, h,
            0, 0, w, h,
            // we may want to add GL_STENCIL_BUFFER_BIT, if present
            bits,
            GL_NEAREST
        )

        GFX.check()

        // restore the old binding
        Frame.invalidate()
        Frame.bind()

    }

    fun check() {
        val state = glCheckFramebufferStatus(GL_FRAMEBUFFER)
        if (state != GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("Framebuffer is incomplete: ${GFX.getErrorTypeName(state)}")
        }
    }

    override fun bindTextureI(index: Int, offset: Int, nearest: GPUFiltering, clamping: Clamping) {
        checkSession()
        if (withMultisampling) {
            val ssBuffer = ssBuffer!!
            blitTo(ssBuffer)
            ssBuffer.bindTextureI(index, offset, nearest, clamping)
        } else {
            textures[index].bind(offset, nearest, clamping)
        }
    }

    override fun bindTextures(offset: Int, nearest: GPUFiltering, clamping: Clamping) {
        GFX.check()
        if (withMultisampling) {
            val ssBuffer = ssBuffer!!
            blitTo(ssBuffer)
            GFX.check()
            ssBuffer.bindTextures(offset, nearest, clamping)
        } else {
            for ((index, texture) in textures.withIndex()) {
                texture.bind(offset + index, nearest, clamping)
            }
        }
        GFX.check()
    }

    fun resolve() {
        if (withMultisampling) {
            blitTo(ssBuffer!!)
            GFX.check()
        }
    }

    fun destroyExceptTextures(deleteDepth: Boolean) {
        if (ssBuffer != null) {
            ssBuffer?.destroyExceptTextures(deleteDepth)
            ssBuffer = null
            destroy()
        } else {
            destroyFramebuffer()
            destroyInternalDepth()
            if (deleteDepth) destroyDepthTexture()
        }
    }

    fun destroyFramebuffer() {
        if (pointer != 0) {
            glDeleteFramebuffers(pointer)
            Frame.invalidate()
            if (Build.isDebug) DebugGPUStorage.fbs.remove(this)
            pointer = 0
            val buffers = colorRenderBuffers
            if (buffers != null) {
                glDeleteRenderbuffers(buffers)
            }
        }
    }

    fun destroyInternalDepth() {
        if (internalDepthTexture > 0) {
            glDeleteRenderbuffers(internalDepthTexture)
            renderBufferAllocated = Texture2D.allocate(renderBufferAllocated, 0L)
            internalDepthTexture = 0
        }
    }

    var renderBufferAllocated = 0L

    fun destroyTextures(deleteDepth: Boolean) {
        if (!usesCRBs) for (tex in textures) tex.destroy()
        if (deleteDepth) destroyDepthTexture()
    }

    fun destroyDepthTexture() {
        depthTexture?.destroy()
    }

    override fun destroy() {
        if (pointer != 0) {
            GFX.checkIsGFXThread()
            ssBuffer?.destroy()
            destroyFramebuffer()
            destroyInternalDepth()
            destroyTextures(true)
        }
    }

    override fun getTextureI(index: Int): ITexture2D {
        checkSession()
        return if (samples > 1) {
            bindTextureI(index, 0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
            ssBuffer!!.getTextureI(index)
        } else textures[index]
    }

    companion object {

        // more than 16 attachments would be pretty insane, and probably not supported
        private val attachments = Array(14) { size ->
            IntArray(size + 2) { GL_COLOR_ATTACHMENT0 + it }
        }

        // private val LOGGER = LogManager.getLogger(Framebuffer::class)
        fun bindFramebuffer(target: Int, pointer: Int) {
            glBindFramebuffer(target, pointer)
            Frame.lastPtr = pointer
        }
    }

    override fun toString(): String =
        "FB[n=$name, i=$pointer, w=$w h=$h s=$samples t=${targets.joinToString()} d=$depthBufferType]"

}