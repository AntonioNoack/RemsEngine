package me.anno.gpu.framebuffer

import me.anno.Build
import me.anno.cache.ICacheData
import me.anno.gpu.Blitting
import me.anno.gpu.ContextPointer
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.GLNames
import me.anno.gpu.debug.DebugGPUStorage
import me.anno.gpu.framebuffer.AttachToDepth.attachFramebufferToDepthImpl
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.LazyTexture
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib
import me.anno.maths.Maths
import me.anno.utils.assertions.assertEquals
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Booleans.withoutFlag
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL46C.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.GL46C.GL_DEPTH_ATTACHMENT
import org.lwjgl.opengl.GL46C.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL46C.GL_FRAMEBUFFER_COMPLETE
import org.lwjgl.opengl.GL46C.GL_NONE
import org.lwjgl.opengl.GL46C.GL_RENDERBUFFER
import org.lwjgl.opengl.GL46C.GL_TEXTURE_2D
import org.lwjgl.opengl.GL46C.GL_TEXTURE_2D_MULTISAMPLE
import org.lwjgl.opengl.GL46C.glBindFramebuffer
import org.lwjgl.opengl.GL46C.glCheckFramebufferStatus
import org.lwjgl.opengl.GL46C.glDeleteFramebuffers
import org.lwjgl.opengl.GL46C.glDrawBuffers
import org.lwjgl.opengl.GL46C.glFramebufferRenderbuffer
import org.lwjgl.opengl.GL46C.glFramebufferTexture2D
import org.lwjgl.opengl.GL46C.glGenFramebuffers
import org.lwjgl.opengl.GL46C.glObjectLabel
import org.lwjgl.opengl.GL46C.glUseProgram

/**
 * Creates textures to render onto; can be instantiated before GFX/on any thread;
 * can only be rendered to or read by GFX thread
 * */
class Framebuffer(
    override var name: String,
    override var width: Int, override var height: Int,
    samples: Int, val targets: List<TargetType>,
    override var depthBufferType: DepthBufferType
) : IFramebuffer, ICacheData {

    constructor(
        name: String, w: Int, h: Int, samples: Int, targetType: TargetType,
        depthBufferType: DepthBufferType
    ) : this(name, w, h, samples, listOf(targetType), depthBufferType)

    constructor(
        name: String, w: Int, h: Int, targetType: TargetType,
        depthBufferType: DepthBufferType = DepthBufferType.NONE
    ) : this(name, w, h, 1, listOf(targetType), depthBufferType)

    override val samples: Int = Maths.clamp(samples, 1, GFX.maxSamples)
    override val numTextures: Int get() = targets.size

    override fun getTargetType(slot: Int) = targets[slot]

    override fun attachFramebufferToDepth(name: String, targets: List<TargetType>): IFramebuffer {
        return attachFramebufferToDepthImpl(name, targets)
    }

    var offsetX = 0
    var offsetY = 0

    // the source of our depth texture
    var depthAttachedPtr = 0
    var depthAttachment: Framebuffer? = null

    val target = if (withMultisampling) GL_TEXTURE_2D_MULTISAMPLE else GL_TEXTURE_2D

    // multiple targets, layout=x require shader version 330+
    // use bindFragDataLocation instead

    var needsBlit = -1

    override var isSRGBMask = 0
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

    var renderBufferAllocated = 0L
    var depthRenderbuffer: Renderbuffer? = null
    override var depthTexture: Texture2D? = null
    override var depthMask: Int = 0

    var textures: List<Texture2D> = emptyList()

    fun isCreated(): Boolean {
        return pointer != 0
    }

    override fun checkSession() {
        if (pointer != 0 && session != GFXState.session) {
            GFX.check()
            session = GFXState.session
            pointer = 0
            needsBlit = -1
            ssBuffer?.checkSession()
            depthTexture?.checkSession()
            depthAttachment?.checkSession()
            renderBufferAllocated = 0L
            depthRenderbuffer = null
            val textures = textures
            for (i in textures.indices) {
                textures[i].checkSession()
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
        ensureSize(w, h, 0)
        bind()
    }

    private fun getDepthPointer(da: Framebuffer?): Int {
        da ?: return -1
        val depTex = da.depthTexture
        if (depTex != null && GFX.supportsDepthTextures) return depTex.pointer
        val depBuf = da.depthRenderbuffer
        if (depBuf != null) return depBuf.pointer
        return -1
    }

    fun bind() {

        needsBlit = -1

        // if the depth-attachment base changed, we need to recreate this texture
        val da = if (depthBufferType == DepthBufferType.ATTACHMENT) depthAttachment else null
        if (da != null) {
            val dtp = getDepthPointer(da)
            if (dtp != depthAttachedPtr) destroy()
            assertEquals(width, da.width)
            assertEquals(height, da.height)
        }

        ensure()

        if (da != null) {
            assertEquals(getDepthPointer(da), depthAttachedPtr)
        }

        bindFramebuffer(GL_FRAMEBUFFER, pointer)

        val textures = textures
        for (i in textures.indices) {
            invalidateTexture(textures[i])
        }
        invalidateTexture(depthTexture)
    }

    private fun invalidateTexture(texture: Texture2D?) {
        texture ?: return
        texture.hasMipmap = false
        texture.filtering = Filtering.TRULY_NEAREST
    }

    override fun ensureSize(newWidth: Int, newHeight: Int, newDepth: Int) {
        if (newWidth != width || newHeight != height) {
            destroy()
            width = newWidth
            height = newHeight
            create()
        } else ensure()
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
        if (textures.size != targets.size) {
            textures = targets.mapIndexed { index, target ->
                val texture = Texture2D("$name-tex[$index]", w, h, samples)
                texture.owner = this
                texture.create(target)
                GFX.check()
                // println("Attaching $target (${texture.pointer}) onto $pointer.$index")
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + index, texture.target, texture.pointer, 0)
                texture
            }
        } else {
            for (index in targets.indices) {
                val texture = textures[index]
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
                if (da.session != GFXState.session || (da.depthTexture == null && da.depthRenderbuffer == null)) {
                    da.ensure()
                    bindFramebuffer(GL_FRAMEBUFFER, pointer)
                }
                var texPointer = if (GFX.supportsDepthTextures) da.depthTexture?.pointer else null
                if (texPointer == null) {
                    texPointer = da.depthRenderbuffer?.pointer ?: 0
                    if (texPointer == 0) throw IllegalStateException("Depth Attachment was not found in $name, $da.${da.depthTexture}")
                    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, texPointer)
                } else {
                    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, target, texPointer, 0)
                }
                depthAttachedPtr = texPointer
            }
            DepthBufferType.INTERNAL -> ensureRenderbuffer()
            DepthBufferType.TEXTURE, DepthBufferType.TEXTURE_16 -> {
                if (GFX.supportsDepthTextures) {
                    val depthTexture = this.depthTexture ?: Texture2D("$name-depth", w, h, samples).apply {
                        create(depthBufferType.chooseDepthFormat())
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
                } else ensureRenderbuffer()
            }
        }
        GFX.check()
        checkIsComplete()
        if (Build.isDebug) {
            glObjectLabel(GL_FRAMEBUFFER, pointer, name)
        }
        this.pointer = pointer
    }

    private fun ensureRenderbuffer() {
        if (depthRenderbuffer == null) {
            depthRenderbuffer = createAndAttachRenderbuffer()
        }
    }

    private fun createAndAttachRenderbuffer(): Renderbuffer {
        val renderbuffer = Renderbuffer()
        renderbuffer.createDepthBuffer(width, height, samples)
        renderbuffer.attachToFramebuffer(false)
        GFX.check()
        val bytesPerPixel = 24
        renderBufferAllocated = Texture2D.allocate(
            renderBufferAllocated,
            renderBufferAllocated + width * height * bytesPerPixel.toLong() * samples
        )
        return renderbuffer
    }

    fun copyIfNeeded(dst: IFramebuffer, layerMask: Int) {
        if (needsBlit.hasFlag(layerMask)) {
            needsBlit = needsBlit.withoutFlag(layerMask)
            val w = width
            val h = height
            ensure()
            dst.ensureSize(w, h, 0)
            copyTo(dst, layerMask)
        }
    }

    fun copyTo(dst: IFramebuffer, copyColor: Boolean, copyDepth: Boolean) {
        val texturesSize = targets.size
        val mask = copyColor.toInt((1 shl texturesSize) - 1) or copyDepth.toInt(1 shl texturesSize)
        copyTo(dst, mask)
    }

    fun copyTo(dst: IFramebuffer, layerMask: Int) {
        // save previous shader
        val prevShader = GPUShader.lastProgram
        dst.ensure()
        val depthTexture = depthTexture
        var needsToCopyDepth = layerMask.hasFlag(1 shl targets.size)
        val tex0 = Texture2D.getBindState(0)
        val tex1 = Texture2D.getBindState(1)
        useFrame(dst, Renderer.copyRenderer) {
            renderPurely {
                val textures = textures
                val dstFramebuffer = dst as? Framebuffer ?: (dst as MultiFramebuffer).targetsI[0]
                var remainingMask = layerMask and ((1 shl targets.size) - 1)
                while (remainingMask != 0) {
                    // find next to-process index
                    val i = remainingMask.countTrailingZeroBits()
                    val isSRGB = isSRGBMask.hasFlag(1 shl i)
                    remainingMask = remainingMask.withoutFlag(1 shl i)

                    // execute blit
                    val dstColor = dst.getTextureIMS(i) as Texture2D
                    val srcColor = textures[i]
                    if (needsToCopyDepth && depthTexture != null) {
                        needsToCopyDepth = false
                        useFrame(dstColor, dstFramebuffer) {
                            Blitting.copyColorAndDepth(srcColor, depthTexture, depthMask, isSRGB)
                        }
                    } else {
                        useFrame(dstColor) {
                            Blitting.copyColor(srcColor, isSRGB)
                        }
                    }
                }
                // execute depth blit
                if (needsToCopyDepth && depthTexture != null) {
                    useFrame(null, dstFramebuffer) {
                        Blitting.copyColorAndDepth(TextureLib.blackTexture, depthTexture, depthMask, false)
                    }
                }
            }
        }
        Texture2D.restoreBindState(1, tex1)
        Texture2D.restoreBindState(0, tex0)
        if (prevShader >= 0) {
            GPUShader.lastProgram = prevShader
            glUseProgram(prevShader)
        }
    }

    fun checkIsComplete() {
        val state = glCheckFramebufferStatus(GL_FRAMEBUFFER)
        if (state != GL_FRAMEBUFFER_COMPLETE) {
            throw IllegalStateException(
                "Framebuffer is incomplete: ${GLNames.getErrorTypeName(state)}, " +
                        "$width x $height x $samples, [${targets.joinToString { it.name }}], $depthBufferType, ${depthAttachment?.samples}"
            )
        }
    }

    override fun bindTextureI(index: Int, offset: Int, nearest: Filtering, clamping: Clamping) {
        checkSession()
        val ssBuffer = ssBuffer
        if (ssBuffer != null) {
            copyIfNeeded(ssBuffer, 1 shl index)
            ssBuffer.bindTextureI(index, offset, nearest, clamping)
        } else {
            textures[index].bind(offset, nearest, clamping)
        }
    }

    override fun bindTextures(offset: Int, nearest: Filtering, clamping: Clamping) {
        GFX.check()
        val ssBuffer = ssBuffer
        if (ssBuffer != null) {
            copyIfNeeded(ssBuffer, (1 shl targets.size) - 1)
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
            val textures = textures
            for (i in textures.indices) {
                textures[i].bindTrulyNearest(offset + i)
            }
        } else super.bindTrulyNearestMS(offset)
    }

    override fun getTextureIMS(index: Int): ITexture2D {
        return if (samples > 1) textures[index]
        else super.getTextureIMS(index)
    }

    fun destroyExceptTextures(deleteDepth: Boolean) {
        GFX.checkIsGFXThread()
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
            GFX.checkIsGFXThread()
            bindFramebuffer(GL_FRAMEBUFFER, 0)
            glDeleteFramebuffers(pointer)
            Frame.invalidate()
            if (Build.isDebug) DebugGPUStorage.fbs.remove(this)
            pointer = 0
        }
    }

    fun destroyInternalDepth() {
        depthRenderbuffer?.destroy()
        depthRenderbuffer = null
        renderBufferAllocated = Texture2D.allocate(renderBufferAllocated, 0L)
    }

    fun destroyTextures(deleteDepth: Boolean) {
        val textures = textures
        for (i in textures.indices) {
            textures[i].destroy()
        }
        if (deleteDepth) destroyDepthTexture()
        this.textures = emptyList()
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

    override fun getTextureILazy(index: Int): ITexture2D {
        checkSession()
        return if (withMultisampling) {
            val ssBuffer = ssBuffer!!
            ssBuffer.ensureSize(width, height, 0)
            val isDepthTexture = index == numTextures
            return LazyTexture(
                if (isDepthTexture) ssBuffer.depthTexture!!
                else ssBuffer.textures[index],
                if (isDepthTexture) depthTexture!!
                else textures[index], lazy {
                    copyIfNeeded(ssBuffer, 1 shl index)
                })
        } else getTextureI(index)
    }

    override fun getTextureI(index: Int): ITexture2D {
        checkSession()
        return if (withMultisampling) {
            val ssBuffer = ssBuffer!!
            copyIfNeeded(ssBuffer, 1 shl index)
            ssBuffer.getTextureI(index)
        } else {
            val textures = textures
            if (index == numTextures) depthTexture ?: TextureLib.depthTexture
            else textures.getOrNull(index) ?: TextureLib.missingTexture
        }
    }

    companion object {

        private val LOGGER = LogManager.getLogger(Framebuffer::class)

        // more than 16 attachments are probably not supported
        private val attachments = createArrayList(14) { size ->
            IntArray(size + 2) { GL_COLOR_ATTACHMENT0 + it }
        }

        fun bindFramebuffer(target: Int, pointer: Int) {
            glBindFramebuffer(target, pointer)
            Frame.lastPtr = pointer
        }

        fun drawBuffersN(size: Int) {
            when (size) {
                0 -> glDrawBuffers(GL_NONE)
                1 -> glDrawBuffers(GL_COLOR_ATTACHMENT0)
                else -> glDrawBuffers(attachments[size - 2])
            }
        }
    }

    override fun toString(): String =
        "FB['$name', #$pointer, $width x $height x $samples, t=[${targets.joinToString { it.name }}] d=$depthBufferType]"
}