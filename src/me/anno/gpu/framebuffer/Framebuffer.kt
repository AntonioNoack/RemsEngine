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
    ) : this(name, w, h, 1, arrayOf(target), depthBufferType)

    fun clone() = Framebuffer(name, w, h, samples, targets, depthBufferType)

    override val samples: Int = Maths.clamp(samples, 1, GFX.maxSamples)
    override val numTextures: Int = targets.size

    override fun getTargetType(slot: Int) = targets[slot]

    /**
     * attach another framebuffer, which shares the depth buffer
     * this can be used to draw 3D ui without deferred-rendering,
     * but using the same depth values
     * */
    override fun attachFramebufferToDepth(name: String, targetCount: Int, fpTargets: Boolean): IFramebuffer {
        return if (targetCount <= GFX.maxColorAttachments) {
            val buffer = Framebuffer(name, w, h, samples, targetCount, fpTargets, DepthBufferType.ATTACHMENT)
            buffer.depthAttachment = this
            buffer.ssBuffer?.depthAttachment = ssBuffer
            buffer
        } else {
            val buffer = MultiFramebuffer(name, w, h, samples, targetCount, fpTargets, DepthBufferType.ATTACHMENT)
            for (it in buffer.targetsI) {
                it.depthAttachment = this
                it.ssBuffer?.depthAttachment = ssBuffer
            }
            buffer
        }
    }

    /**
     * attach another framebuffer, which shares the depth buffer
     * this can be used to draw 3D ui without deferred-rendering,
     * but using the same depth values
     * */
    override fun attachFramebufferToDepth(name: String, targets: Array<TargetType>): IFramebuffer {
        if (depthBufferType != DepthBufferType.TEXTURE && depthBufferType != DepthBufferType.TEXTURE_16)
            throw IllegalStateException("Cannot attach depth to framebuffer without depth texture")
        return if (targets.size <= GFX.maxColorAttachments) {
            val buffer = Framebuffer(name, w, h, samples, targets, DepthBufferType.ATTACHMENT)
            buffer.depthAttachment = this
            buffer.ssBuffer?.depthAttachment = ssBuffer
            buffer
        } else {
            val buffer = MultiFramebuffer(name, w, h, samples, targets, DepthBufferType.ATTACHMENT)
            for (it in buffer.targetsI) {
                it.depthAttachment = this
                it.ssBuffer?.depthAttachment = ssBuffer
            }
            buffer
        }
    }

    var offsetX = 0
    var offsetY = 0
    var lastDraw = 0L

    // the source of our depth texture
    var depthAttachedPtr = 0
    var depthAttachment: Framebuffer? = null

    val target = if (withMultisampling) GL_TEXTURE_2D_MULTISAMPLE else GL_TEXTURE_2D

    var autoUpdateMipmaps = true

    // multiple targets, layout=x require shader version 330+
    // use bindFragDataLocation instead

    var needsBlit = true

    val withMultisampling get() = samples > 1
    var ssBuffer = if (withMultisampling)
        Framebuffer("$name.ss", w, h, 1, targets, depthBufferType) else null

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
        val da = if (depthBufferType == DepthBufferType.ATTACHMENT) depthAttachment else null
        var wasDestroyed = false
        if (da != null) {
            val dtp = da.depthTexture?.pointer ?: da.internalDepthTexture
            if (dtp != depthAttachedPtr) {
                destroy()
                wasDestroyed = true
            }
            if ((w != da.w || h != da.h)) {
                throw IllegalStateException("Depth is not matching dimensions, $w x $h vs ${da.w} x ${da.h}")
            }
        }

        ensure()

        if (da != null) {
            val dtp = da.depthTexture?.pointer ?: da.internalDepthTexture
            if (dtp != depthAttachedPtr) {
                throw IllegalStateException(
                    "Depth attachment could not be recreated! ${da.pointer}, ${da.depthTexture!!.pointer} != $depthAttachedPtr, " +
                            "was destroyed? $wasDestroyed"
                )
            }
        }

        bindFramebuffer(GL_FRAMEBUFFER, pointer)
        if (!OS.isWeb) {// not defined in WebGL
            if (withMultisampling) {
                glEnable(GL_MULTISAMPLE)
            } else {
                glDisable(GL_MULTISAMPLE)
            }
        }

        // todo if this fine? might cost a lof ot performance...
        for (texture in textures) {
            texture.hasMipmap = false
            texture.filtering = GPUFiltering.TRULY_NEAREST
        }
        depthTexture?.hasMipmap = false
        depthTexture?.filtering = GPUFiltering.TRULY_NEAREST

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

    val usesCRBs = false // samples > 1
    // if you need multi-sampled textures, write me :) ->
    // lol, I need them myself for MSAA x deferred rendering 😂

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
                createRenderbuffer(GL_COLOR_ATTACHMENT0 + it, target.internalFormat, target.bytesPerPixel)
            }
        } else textures = Array(targets.size) { index ->
            val texture = Texture2D("$name-tex[$index]", w, h, samples)
            texture.autoUpdateMipmaps = autoUpdateMipmaps
            texture.create(targets[index])
            texture.fb = this
            GFX.check()
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + index, texture.target, texture.pointer, 0)
            texture
        }
        GFX.check()
        drawBuffersN(targets.size)
        GFX.check()
        // cannot use depth-texture with color render buffers... why ever...
        val depthBufferType = if (usesCRBs && depthBufferType == DepthBufferType.TEXTURE)
            DepthBufferType.INTERNAL else depthBufferType
        depthAttachedPtr = 0
        when (depthBufferType) {
            DepthBufferType.NONE -> {
            }
            DepthBufferType.ATTACHMENT -> {
                val da = depthAttachment ?: throw IllegalStateException("Depth Attachment was not found in $name, null")
                depthAttachedPtr = if (da.usesCRBs && da.internalDepthTexture != 0) {
                    val texPointer = da.internalDepthTexture
                    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, texPointer)
                    // println("set attached depth ptr to $texPointer")
                    texPointer
                } else {
                    val texPointer = da.depthTexture?.pointer
                        ?: throw IllegalStateException("Depth Attachment was not found in $name, $da.${da.depthTexture}")
                    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, target, texPointer, 0)
                    // println("set attached depth ptr to $texPointer")
                    texPointer
                }
            }
            DepthBufferType.INTERNAL -> createDepthBuffer()
            DepthBufferType.TEXTURE, DepthBufferType.TEXTURE_16 -> {
                val depthTexture = Texture2D("$name-depth", w, h, samples)
                depthTexture.autoUpdateMipmaps = autoUpdateMipmaps
                depthTexture.createDepth(depthBufferType == DepthBufferType.TEXTURE_16)
                depthTexture.fb = this
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
    fun createRenderbuffer(
        attachment: Int = GL_COLOR_ATTACHMENT0,
        format: Int = GL_RGBA8,
        bytesPerPixel: Int,
    ): Int {
        val renderBuffer = glGenRenderbuffers()
        glBindRenderbuffer(GL_RENDERBUFFER, renderBuffer)
        if (withMultisampling) glRenderbufferStorageMultisample(GL_RENDERBUFFER, samples, format, w, h)
        else glRenderbufferStorage(GL_RENDERBUFFER, format, w, h)
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, attachment, GL_RENDERBUFFER, renderBuffer)
        GFX.check()
        renderBufferAllocated = Texture2D.allocate(
            renderBufferAllocated,
            renderBufferAllocated + w * h * bytesPerPixel.toLong() * samples
        )
        return renderBuffer
    }

    fun createRenderbuffer(
        attachment: Int = GL_COLOR_ATTACHMENT0,
        targetType: TargetType,
    ): Int = createRenderbuffer(attachment, targetType.internalFormat, targetType.bytesPerPixel)

    fun createDepthBuffer() {
        internalDepthTexture = when (depthBufferType) {
            // these texture types MUST be the same as for the texture creation process
            DepthBufferType.TEXTURE -> createRenderbuffer(GL_DEPTH_ATTACHMENT, TargetType.DEPTH32F)
            DepthBufferType.TEXTURE_16 -> createRenderbuffer(GL_DEPTH_ATTACHMENT, TargetType.DEPTH16)
            else -> createRenderbuffer(GL_DEPTH_ATTACHMENT, GL_DEPTH_COMPONENT24, 4) // 4 is worst-case assumed
        }
    }

    fun copyIfNeeded(dst: IFramebuffer) {

        if (!needsBlit) return
        needsBlit = false

        val w = w
        val h = h

        GFX.check()

        // ensure that we exist
        ensure()

        // ensure that it exists
        // + bind it, it seems important
        if (dst is Framebuffer) dst.ensureSize(w, h)
        dst.ensure()

        GFX.check()

        if (pointer == 0 || (dst.pointer == 0 && dst != NullFramebuffer))
            throw RuntimeException("Something went wrong $this -> $dst")

        bindFramebuffer(GL_DRAW_FRAMEBUFFER, dst.pointer)
        bindFramebuffer(GL_READ_FRAMEBUFFER, pointer)

        GFX.check()

        var bits = 0
        if (targets.isNotEmpty()) bits = bits or GL_COLOR_BUFFER_BIT
        if (depthBufferType != DepthBufferType.NONE) bits = bits or GL_DEPTH_BUFFER_BIT

        if (targets.size > 1) {

            for (i in targets.indices) {

                glDrawBuffers(GL_COLOR_ATTACHMENT0 + i)
                glReadBuffer(GL_COLOR_ATTACHMENT0 + i)

                glBlitFramebuffer(
                    0, 0, w, h,
                    0, 0, w, h,
                    // we may want to add GL_STENCIL_BUFFER_BIT, if present
                    if (i == 0) bits else GL_COLOR_BUFFER_BIT, // correct???
                    GL_NEAREST
                )

            }

            // reset state, just in case
            glDrawBuffers(GL_COLOR_ATTACHMENT0)
            glReadBuffer(GL_COLOR_ATTACHMENT0)

        } else {

            glBlitFramebuffer(
                0, 0, w, h,
                0, 0, w, h,
                // we may want to add GL_STENCIL_BUFFER_BIT, if present
                bits, GL_NEAREST
            )

            GFX.check()

        }

        GFX.check()

        // restore the old binding
        Frame.invalidate()
        Frame.bind()

    }

    fun copyTo(dst: IFramebuffer, mask: Int) {

        GFX.check()

        ensure()
        dst.ensure()

        GFX.check()

        bindFramebuffer(GL_DRAW_FRAMEBUFFER, dst.pointer)
        bindFramebuffer(GL_READ_FRAMEBUFFER, pointer)

        GFX.check()

        glBlitFramebuffer(
            0, 0, w, h,
            0, 0, dst.w, dst.h,
            mask, GL_NEAREST
        )

        GFX.check()

        // restore the old binding
        Frame.invalidate()
        Frame.bind()

        GFX.check()

    }

    fun copyColorTo(dst: Framebuffer, srcI: Int, dstI: Int, mask: Int) {

        GFX.check()

        ensure()
        dst.ensure()

        bindFramebuffer(GL_FRAMEBUFFER, pointer)
        drawBuffers1(srcI)

        bindFramebuffer(GL_FRAMEBUFFER, dst.pointer)
        drawBuffers1(dstI)

        bindFramebuffer(GL_DRAW_FRAMEBUFFER, dst.pointer)
        bindFramebuffer(GL_READ_FRAMEBUFFER, pointer)

        glBlitFramebuffer(
            0, 0, w, h,
            0, 0, dst.w, dst.h,
            mask, GL_NEAREST
        )

        bindFramebuffer(GL_FRAMEBUFFER, pointer)
        drawBuffersN(targets.size)

        bindFramebuffer(GL_FRAMEBUFFER, dst.pointer)
        drawBuffersN(dst.targets.size)

        // restore the old binding
        Frame.invalidate()
        Frame.bind()

        GFX.check()

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
            copyIfNeeded(ssBuffer)
            ssBuffer.bindTextureI(index, offset, nearest, clamping)
        } else {
            textures[index].bind(offset, nearest, clamping)
        }
    }

    override fun bindTextures(offset: Int, nearest: GPUFiltering, clamping: Clamping) {
        GFX.check()
        if (withMultisampling) {
            val ssBuffer = ssBuffer!!
            copyIfNeeded(ssBuffer)
            ssBuffer.bindTextures(offset, nearest, clamping)
        } else {
            val textures = textures
            for (i in textures.indices) {
                textures[i].bind(offset + i, nearest, clamping)
            }
        }
        GFX.check()
    }

    override fun bindTrulyNearestMS(offset: Int) {
        if (withMultisampling) {
            val tex = textures
            for (i in tex.indices) {
                tex[i].bindTrulyNearest(offset + i)
            }
        } else super.bindTrulyNearestMS(offset)
    }

    override fun getTextureIMS(index: Int): ITexture2D {
        return if (samples > 1) textures[index]
        else super.getTextureIMS(index)
    }

    fun resolve() {
        if (withMultisampling) {
            copyIfNeeded(ssBuffer!!)
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
        else {
            val crb = colorRenderBuffers
            if (crb != null) glDeleteRenderbuffers(crb)
            colorRenderBuffers = null
        }
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
            renderBufferAllocated = Texture2D.allocate(renderBufferAllocated, 0L)
        }
    }

    override fun getTextureI(index: Int): ITexture2D {
        checkSession()
        return if (withMultisampling) {
            val ssBuffer = ssBuffer!!
            copyIfNeeded(ssBuffer)
            ssBuffer.getTextureI(index)
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

        fun drawBuffers1(slot: Int) {
            glDrawBuffer(if (slot < 0) GL_NONE else GL_COLOR_ATTACHMENT0 + slot)
        }

        fun drawBuffersN(size: Int) {
            when (size) {
                0, 1 -> drawBuffers1(size - 1)
                else -> glDrawBuffers(attachments[size - 2])
            }
        }

    }

    override fun toString(): String =
        "FB[n=$name, i=$pointer, w=$w h=$h s=$samples t=${targets.joinToString()} d=$depthBufferType]"

}