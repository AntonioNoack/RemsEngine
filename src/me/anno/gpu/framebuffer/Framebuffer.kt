package me.anno.gpu.framebuffer

import me.anno.Build
import me.anno.cache.ICacheData
import me.anno.gpu.ContextPointer
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.debug.DebugGPUStorage
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib
import me.anno.maths.Maths
import me.anno.utils.structures.lists.Lists.createList
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL46C.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.GL46C.GL_DEPTH_ATTACHMENT
import org.lwjgl.opengl.GL46C.GL_DEPTH_COMPONENT24
import org.lwjgl.opengl.GL46C.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL46C.GL_FRAMEBUFFER_COMPLETE
import org.lwjgl.opengl.GL46C.GL_NONE
import org.lwjgl.opengl.GL46C.GL_RENDERBUFFER
import org.lwjgl.opengl.GL46C.GL_RGBA8
import org.lwjgl.opengl.GL46C.GL_TEXTURE_2D
import org.lwjgl.opengl.GL46C.GL_TEXTURE_2D_MULTISAMPLE
import org.lwjgl.opengl.GL46C.glBindFramebuffer
import org.lwjgl.opengl.GL46C.glBindRenderbuffer
import org.lwjgl.opengl.GL46C.glCheckFramebufferStatus
import org.lwjgl.opengl.GL46C.glDeleteFramebuffers
import org.lwjgl.opengl.GL46C.glDeleteRenderbuffers
import org.lwjgl.opengl.GL46C.glDrawBuffer
import org.lwjgl.opengl.GL46C.glDrawBuffers
import org.lwjgl.opengl.GL46C.glFramebufferRenderbuffer
import org.lwjgl.opengl.GL46C.glFramebufferTexture2D
import org.lwjgl.opengl.GL46C.glGenFramebuffers
import org.lwjgl.opengl.GL46C.glGenRenderbuffers
import org.lwjgl.opengl.GL46C.glObjectLabel
import org.lwjgl.opengl.GL46C.glReadBuffer
import org.lwjgl.opengl.GL46C.glRenderbufferStorage
import org.lwjgl.opengl.GL46C.glRenderbufferStorageMultisample
import org.lwjgl.opengl.GL46C.glUseProgram

class Framebuffer(
    override var name: String,
    override var width: Int, override var height: Int,
    samples: Int, val targets: List<TargetType>,
    var depthBufferType: DepthBufferType
) : IFramebuffer, ICacheData {

    constructor(
        name: String, w: Int, h: Int, samples: Int,
        targetCount: Int, fpTargets: Boolean,
        depthBufferType: DepthBufferType
    ) : this(
        name, w, h, samples,
        createList(targetCount, if (fpTargets) TargetType.Float32x4 else TargetType.UInt8x4),
        depthBufferType
    )

    constructor(
        name: String, w: Int, h: Int, targets: List<TargetType>,
        depthBufferType: DepthBufferType = DepthBufferType.NONE
    ) : this(name, w, h, 1, targets, depthBufferType)

    constructor(
        name: String, w: Int, h: Int, target: TargetType,
        depthBufferType: DepthBufferType = DepthBufferType.NONE
    ) : this(name, w, h, 1, listOf(target), depthBufferType)

    fun clone() = Framebuffer(name, width, height, samples, targets, depthBufferType)

    override val samples: Int = Maths.clamp(samples, 1, GFX.maxSamples)
    override val numTextures: Int = targets.size

    override fun getTargetType(slot: Int) = targets[slot]

    /**
     * attach another framebuffer, which shares the depth buffer
     * this can be used to draw 3D ui without deferred-rendering,
     * but using the same depth values
     * */
    override fun attachFramebufferToDepth(name: String, targets: List<TargetType>): IFramebuffer {
        if (depthBufferType == DepthBufferType.NONE)
            throw IllegalStateException("Cannot attach depth to framebuffer without depth buffer")
        return if (targets.size <= GFX.maxColorAttachments) {
            val buffer = Framebuffer(name, width, height, samples, targets, DepthBufferType.ATTACHMENT)
            buffer.depthAttachment = this
            buffer.ssBuffer?.depthAttachment = ssBuffer
            buffer
        } else {
            val buffer = MultiFramebuffer(name, width, height, samples, targets, DepthBufferType.ATTACHMENT)
            for (it in buffer.targetsI) {
                it.depthAttachment = this
                it.ssBuffer?.depthAttachment = ssBuffer
            }
            buffer
        }
    }

    var offsetX = 0
    var offsetY = 0
    var lastDrawn = 0L

    // the source of our depth texture
    var depthAttachedPtr = 0
    var depthAttachment: Framebuffer? = null

    val target = if (withMultisampling) GL_TEXTURE_2D_MULTISAMPLE else GL_TEXTURE_2D

    // multiple targets, layout=x require shader version 330+
    // use bindFragDataLocation instead

    var needsBlit = true

    val withMultisampling get() = samples > 1

    /**
     * Framebuffer with single sample for blitting;
     * null, if this Framebuffer already just has a single sample
     * */
    val ssBuffer = if (withMultisampling)
        Framebuffer("$name.ss", width, height, 1, targets, depthBufferType)
    else null

    override var pointer by ContextPointer()

    var session = 0

    var internalDepthRenderbuffer = 0
    override var depthTexture: Texture2D? = null

    var textures: List<Texture2D>? = null

    override fun checkSession() {
        if (pointer != 0 && session != GFXState.session) {
            GFX.check()
            session = GFXState.session
            pointer = 0
            needsBlit = true
            ssBuffer?.checkSession()
            depthTexture?.checkSession()
            depthAttachment?.checkSession()
            renderBufferAllocated = 0L
            internalDepthRenderbuffer = 0
            colorRenderBuffers = null
            val textures = textures
            if (textures != null) {
                for (i in textures.indices) {
                    textures[i].checkSession()
                }
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
            val dtp = da.depthTexture?.pointer ?: da.internalDepthRenderbuffer
            if (dtp != depthAttachedPtr) {
                destroy()
                wasDestroyed = true
            }
            if ((width != da.width || height != da.height)) {
                throw IllegalStateException("Depth is not matching dimensions, $width x $height vs ${da.width} x ${da.height}")
            }
        }

        ensure()

        if (da != null) {
            val dtp =
                (if (GFX.supportsDepthTextures) da.depthTexture?.pointer else null) ?: da.internalDepthRenderbuffer
            if (dtp != depthAttachedPtr) {
                throw IllegalStateException(
                    "Depth attachment could not be recreated! ${da.pointer}, ${da.depthTexture!!.pointer} != $depthAttachedPtr, " +
                            "was destroyed? $wasDestroyed"
                )
            }
        }

        bindFramebuffer(GL_FRAMEBUFFER, pointer)

        val textures = textures
        if (textures != null) {
            for (i in textures.indices) {
                invalidateTexture(textures[i])
            }
        }
        invalidateTexture(depthTexture)
    }

    private fun invalidateTexture(texture: Texture2D?) {
        texture ?: return
        texture.hasMipmap = false
        texture.filtering = Filtering.TRULY_NEAREST
    }

    private fun ensureSize(newWidth: Int, newHeight: Int) {
        if (newWidth != width || newHeight != height) {
            width = newWidth
            height = newHeight
            GFX.check()
            destroy()
            GFX.check()
            create()
            GFX.check()
        }
    }

    private fun checkDepthTextureSupport() {
        if (!GFX.supportsDepthTextures &&
            (depthBufferType == DepthBufferType.TEXTURE ||
                    depthBufferType == DepthBufferType.TEXTURE_16)
        ) {
            LOGGER.warn("Depth textures aren't supported")
            depthBufferType = DepthBufferType.INTERNAL
        }
    }

    fun create() {

        checkDepthTextureSupport()

        depthAttachment?.ensure()
        Frame.invalidate()
        GFX.check()
        val pointer = glGenFramebuffers()
        if (pointer == 0) throw OutOfMemoryError("Could not generate OpenGL framebuffer")
        session = GFXState.session
        if (Build.isDebug) {
            DebugGPUStorage.fbs.add(this)
        }
        bindFramebuffer(GL_FRAMEBUFFER, pointer)
        Frame.lastPtr = pointer
        val w = width
        val h = height
        if (w * h < 1) throw RuntimeException("Invalid framebuffer size $w x $h")
        GFX.check()
        if (textures == null) {
            textures = targets.mapIndexed { index, target ->
                val texture = Texture2D("$name-tex[$index]", w, h, samples)
                texture.owner = this
                texture.create(target)
                GFX.check()
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + index, texture.target, texture.pointer, 0)
                texture
            }
        } else {
            for (index in targets.indices) {
                val texture = textures!![index]
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + index, texture.target, texture.pointer, 0)
            }
        }
        GFX.check()
        drawBuffersN(targets.size)
        GFX.check()
        // cannot use depth-texture with color render buffers... why ever...
        val depthBufferType = depthBufferType
        depthAttachedPtr = 0
        when (depthBufferType) {
            DepthBufferType.NONE -> {
            }
            DepthBufferType.ATTACHMENT -> {
                val da = depthAttachment ?: throw IllegalStateException("Depth Attachment was not found in $name, null")
                if (da.session != GFXState.session || (da.depthTexture == null && da.internalDepthRenderbuffer == 0)) {
                    da.ensure()
                    bindFramebuffer(GL_FRAMEBUFFER, pointer)
                }
                var texPointer = if (GFX.supportsDepthTextures) da.depthTexture?.pointer else null
                if (texPointer == null) {
                    texPointer = da.internalDepthRenderbuffer
                    if (texPointer == 0) throw IllegalStateException("Depth Attachment was not found in $name, $da.${da.depthTexture}")
                    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, texPointer)
                } else {
                    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, target, texPointer, 0)
                }
                depthAttachedPtr = texPointer
            }
            DepthBufferType.INTERNAL -> {
                if (internalDepthRenderbuffer == 0) {
                    internalDepthRenderbuffer =
                        createAndAttachRenderbuffer(GL_DEPTH_ATTACHMENT, GL_DEPTH_COMPONENT24, 4)
                }
            }
            DepthBufferType.TEXTURE, DepthBufferType.TEXTURE_16 -> {
                if (GFX.supportsDepthTextures) {
                    val depthTexture = this.depthTexture ?: Texture2D("$name-depth", w, h, samples).apply {
                        createDepth(depthBufferType == DepthBufferType.TEXTURE_16)
                        owner = this@Framebuffer
                    }
                    glFramebufferTexture2D(
                        GL_FRAMEBUFFER,
                        GL_DEPTH_ATTACHMENT,
                        depthTexture.target,
                        depthTexture.pointer,
                        0
                    )
                    this.depthTexture = depthTexture
                } else if (internalDepthRenderbuffer == 0) {
                    // 4 is worst-case assumed
                    internalDepthRenderbuffer =
                        createAndAttachRenderbuffer(GL_DEPTH_ATTACHMENT, GL_DEPTH_COMPONENT24, 4)
                }
            }
        }
        GFX.check()
        checkIsComplete()
        if (Build.isDebug) {
            glObjectLabel(GL_FRAMEBUFFER, pointer, name)
        }
        this.pointer = pointer
    }

    // could be used in the future :)
    // we don't read multisampled textures currently anyway
    var colorRenderBuffers: IntArray? = null
    fun createAndAttachRenderbuffer(
        attachment: Int = GL_COLOR_ATTACHMENT0,
        format: Int = GL_RGBA8,
        bytesPerPixel: Int,
    ): Int {
        val renderBuffer = glGenRenderbuffers()
        glBindRenderbuffer(GL_RENDERBUFFER, renderBuffer)
        if (withMultisampling) glRenderbufferStorageMultisample(GL_RENDERBUFFER, samples, format, width, height)
        else glRenderbufferStorage(GL_RENDERBUFFER, format, width, height)
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, attachment, GL_RENDERBUFFER, renderBuffer)
        GFX.check()
        renderBufferAllocated = Texture2D.allocate(
            renderBufferAllocated,
            renderBufferAllocated + width * height * bytesPerPixel.toLong() * samples
        )
        return renderBuffer
    }

    fun copyIfNeeded(dst: IFramebuffer) {

        if (!needsBlit) return
        needsBlit = false

        val w = width
        val h = height

        GFX.check()

        // ensure that we exist
        ensure()

        // ensure that it exists
        // + bind it, it seems important
        if (dst is Framebuffer) {
            dst.ensureSize(w, h)
        }

        dst.ensure()

        GFX.check()

        if (pointer == 0 || (dst.pointer == 0 && dst != NullFramebuffer))
            throw RuntimeException("Something went wrong $this -> $dst")

        copyTo(dst)
    }

    fun copyTo(dst: IFramebuffer) {
        // save previous shader
        val prevShader = GPUShader.lastProgram
        useFrame(dst, Renderer.copyRenderer) {
            GFX.check()
            for (i in targets.indices) {
                glDrawBuffers(GL_COLOR_ATTACHMENT0 + i)
                if (i == 0) {
                    val depth = depthTexture ?: TextureLib.depthTexture
                    GFX.copyColorAndDepth(textures!![i], depth)
                    GFX.check()
                } else {
                    GFXState.depthMask.use(false) { // don't copy depth
                        GFX.copy(textures!![i])
                    }
                    GFX.check()
                }
            }
            val depth = depthTexture
            if (targets.isEmpty() && depth != null) {
                GFX.copyColorAndDepth(TextureLib.whiteTexture, depth)
            }
            // reset state, just in case
            drawBuffersN(textures!!.size)
            GFX.check()
        }
        GPUShader.lastProgram = prevShader
        glUseProgram(prevShader)
    }

    fun checkIsComplete() {
        val state = glCheckFramebufferStatus(GL_FRAMEBUFFER)
        if (state != GL_FRAMEBUFFER_COMPLETE) {
            throw IllegalStateException(
                "Framebuffer is incomplete: ${GFX.getErrorTypeName(state)}, " +
                        "$width x $height x $samples, [${targets.joinToString { it.name }}], $depthBufferType, ${depthAttachment?.samples}"
            )
        }
    }

    override fun bindTextureI(index: Int, offset: Int, nearest: Filtering, clamping: Clamping) {
        checkSession()
        val ssBuffer = ssBuffer
        if (ssBuffer != null) {
            copyIfNeeded(ssBuffer)
            ssBuffer.bindTextureI(index, offset, nearest, clamping)
        } else {
            textures!![index].bind(offset, nearest, clamping)
        }
    }

    override fun bindTextures(offset: Int, nearest: Filtering, clamping: Clamping) {
        GFX.check()
        val ssBuffer = ssBuffer
        if (ssBuffer != null) {
            copyIfNeeded(ssBuffer)
            ssBuffer.bindTextures(offset, nearest, clamping)
        } else {
            val textures = textures!!
            for (i in textures.indices) {
                textures[i].bind(offset + i, nearest, clamping)
            }
        }
        GFX.check()
    }

    override fun bindTrulyNearestMS(offset: Int) {
        if (withMultisampling) {
            val textures = textures!!
            for (i in textures.indices) {
                textures[i].bindTrulyNearest(offset + i)
            }
        } else super.bindTrulyNearestMS(offset)
    }

    override fun getTextureIMS(index: Int): ITexture2D {
        return if (samples > 1) textures!![index]
        else super.getTextureIMS(index)
    }

    fun destroyExceptTextures(deleteDepth: Boolean) {
        if (ssBuffer != null) {
            ssBuffer.destroyExceptTextures(deleteDepth)
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
                colorRenderBuffers = null
            }
        }
    }

    fun destroyInternalDepth() {
        if (internalDepthRenderbuffer > 0) {
            glDeleteRenderbuffers(internalDepthRenderbuffer)
            renderBufferAllocated = Texture2D.allocate(renderBufferAllocated, 0L)
            internalDepthRenderbuffer = 0
        }
    }

    var renderBufferAllocated = 0L

    fun destroyTextures(deleteDepth: Boolean) {
        val textures = textures
        if (textures != null) for (i in textures.indices) {
            textures[i].destroy()
        }
        if (deleteDepth) destroyDepthTexture()
        this.textures = null
    }

    fun destroyDepthTexture() {
        depthTexture?.destroy()
        depthTexture = null
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
        } else {
            val textures = textures ?: throw IllegalStateException("Framebuffer hasn't been initialized")
            textures[index]
        }
    }

    companion object {

        private val LOGGER = LogManager.getLogger(Framebuffer::class)

        // more than 16 attachments would be pretty insane, and probably not supported
        private val attachments = Array(14) { size ->
            IntArray(size + 2) { GL_COLOR_ATTACHMENT0 + it }
        }

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
        "FB['$name', #$pointer, $width x $height x $samples, t=[${targets.joinToString { it.name }}] d=$depthBufferType]"
}