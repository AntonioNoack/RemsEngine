package me.anno.gpu.framebuffer

import me.anno.gpu.ContextPointer
import me.anno.gpu.GFX
import me.anno.gpu.GFX.INVALID_POINTER
import me.anno.gpu.GFX.INVALID_SESSION
import me.anno.gpu.GFX.isPointerValid
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.GLNames
import me.anno.gpu.framebuffer.Framebuffer.Companion.bindFramebuffer
import me.anno.gpu.framebuffer.Framebuffer.Companion.drawBuffersN
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2DArray
import me.anno.gpu.texture.TextureLib.missingTexture
import me.anno.utils.assertions.assertEquals
import me.anno.utils.structures.lists.Lists.createList
import org.lwjgl.opengl.GL46C.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.GL46C.GL_DEPTH_ATTACHMENT
import org.lwjgl.opengl.GL46C.GL_DRAW_FRAMEBUFFER
import org.lwjgl.opengl.GL46C.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL46C.GL_FRAMEBUFFER_COMPLETE
import org.lwjgl.opengl.GL46C.glCheckFramebufferStatus
import org.lwjgl.opengl.GL46C.glDeleteFramebuffers
import org.lwjgl.opengl.GL46C.glFramebufferTextureLayer
import org.lwjgl.opengl.GL46C.glGenFramebuffers

class FramebufferArray(
    override var name: String,
    override var width: Int,
    override var height: Int,
    var layers: Int,
    override val samples: Int, // todo when we support multi-sampled 2d-arrays, also support them here
    val targets: List<TargetType>,
    override val depthBufferType: DepthBufferType
) : IFramebuffer {

    constructor(
        name: String,
        width: Int,
        height: Int,
        layers: Int,
        samples: Int,
        targetCount: Int,
        fpTargets: Boolean,
        depthBufferType: DepthBufferType
    ) : this(
        name, width, height, layers, samples,
        createList(targetCount, if (fpTargets) TargetType.Float32x4 else TargetType.UInt8x4),
        depthBufferType
    )

    // multiple targets, layout=x require shader version 330+
    // use glBindFragDataLocation instead

    override var pointer by ContextPointer()

    var session = INVALID_SESSION
    var depthRenderBuffer: Renderbuffer? = null
    override var depthTexture: Texture2DArray? = null
    override var depthMask: Int = 0
    var depthAttachment: FramebufferArray? = null
    override var isSRGBMask: Int = 0

    override val numTextures: Int = targets.size

    var textures: List<Texture2DArray> = emptyList()

    var autoUpdateMipmaps = true

    val isCreated get() = isPointerValid(pointer)

    override fun getTargetType(slot: Int) = targets[slot]

    override fun ensure() {
        if (!isPointerValid(pointer)) create()
    }

    override fun ensureSize(newWidth: Int, newHeight: Int, newDepth: Int) {
        if (width != newWidth || height != newHeight || layers != newDepth) {
            destroy()
            width = newWidth
            height = newHeight
            layers = newDepth
            create()
        } else ensure()
    }

    override fun checkSession() {
        if (isPointerValid(pointer) && session != GFXState.session) {
            GFX.check()
            session = GFXState.session
            pointer = INVALID_POINTER
            // needsBlit = true
            // msBuffer?.checkSession()
            depthTexture?.checkSession()
            for (texture in textures) {
                texture.checkSession()
            }
            GFX.check()
            // validate it
            create()
        }
    }

    override fun getTextureI(index: Int): ITexture2D = textures.getOrNull(index) ?: missingTexture

    override fun bindDirectly() = bind()
    override fun bindDirectly(w: Int, h: Int) {
        checkSize(w, h)
        bind()
    }

    private fun bind() {
        if (!isPointerValid(pointer)) create()
        bindFramebuffer(GL_FRAMEBUFFER, pointer)
        Frame.lastPtr = pointer
    }

    private fun checkSize(w: Int, h: Int) {
        if (w != width || h != height) {
            width = w
            height = h
            GFX.check()
            destroy()
            GFX.check()
            create()
            GFX.check()
        }
    }

    private fun create() {
        Frame.invalidate()
        // LOGGER.info("w: $w, h: $h, samples: $samples, targets: $targetCount x fp32? $fpTargets")
        GFX.check()
        pointer = glGenFramebuffers()
        if (!isPointerValid(pointer)) throw RuntimeException()
        bindFramebuffer(GL_FRAMEBUFFER, pointer)
        Frame.lastPtr = pointer
        //stack.push(this)
        GFX.check()
        if (textures.size != targets.size) {
            textures = targets.mapIndexed { index, target ->
                val texture = Texture2DArray("$name-$index", width, height, layers)
                texture.autoUpdateMipmaps = autoUpdateMipmaps
                texture.create(target)
                GFX.check()
                texture
            }
        }
        GFX.check()
        val textures = textures
        for (index in textures.indices) {
            val texture = textures[index]
            glFramebufferTextureLayer(
                GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + index,
                texture.pointer, 0, 0
            )
        }
        GFX.check()
        drawBuffersN(targets.size)
        GFX.check()
        when (depthBufferType) {
            DepthBufferType.NONE -> {
            }
            DepthBufferType.INTERNAL -> {
                val renderBuffer = depthRenderBuffer
                if (renderBuffer == null) {
                    createDepthBuffer()
                } else {
                    renderBuffer.attachToFramebuffer(true)
                }
            }
            DepthBufferType.TEXTURE, DepthBufferType.TEXTURE_16 -> {
                if (depthTexture == null) {
                    GFX.check()
                    val texture = Texture2DArray("$name-depth", width, height, layers)
                    texture.autoUpdateMipmaps = autoUpdateMipmaps
                    texture.createDepth(depthBufferType == DepthBufferType.TEXTURE_16)
                    depthTexture = texture
                }
                GFX.check()
                glFramebufferTextureLayer(
                    GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT,
                    depthTexture!!.pointer, 0, 0
                )
                GFX.check()
            }
            DepthBufferType.ATTACHMENT -> {
                GFX.check()
                val texPointer = depthAttachment?.depthTexture?.pointer
                    ?: throw IllegalStateException("Depth Attachment was not found in $name, ${depthAttachment}.${depthAttachment?.depthTexture}")
                GFX.check()
                glFramebufferTextureLayer(
                    GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT,
                    texPointer, 0, 0
                )
                GFX.check()
                // throw IllegalArgumentException("attachment depth not yet supported for cubemaps")
            }
        }
        GFX.check()
        check()
    }

    // todo we would need multiple of them, right?
    private fun createDepthBuffer() {
        depthRenderBuffer?.destroy()
        val buffer = Renderbuffer()
        buffer.createDepthBuffer(width, height, samples)
        buffer.attachToFramebuffer(false)
        depthRenderBuffer = buffer
    }

    private fun check() {
        val state = glCheckFramebufferStatus(GL_FRAMEBUFFER)
        if (state != GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("Framebuffer is incomplete: ${GLNames.getErrorTypeName(state)}")
        }
    }

    override fun bindTextureI(index: Int, offset: Int, nearest: Filtering, clamping: Clamping) {
        checkSession()
        textures[index].bind(offset, nearest, clamping)
    }

    override fun bindTextures(offset: Int, nearest: Filtering, clamping: Clamping) {
        checkSession()
        for (textureIndex in textures.indices) {
            textures[textureIndex].bind(offset + textureIndex, nearest, clamping)
        }
    }

    override fun destroy() {
        if (isPointerValid(pointer)) {
            bindFramebuffer(GL_FRAMEBUFFER, 0)
            glDeleteFramebuffers(pointer)
            Frame.invalidate()
            pointer = INVALID_POINTER
            for (it in textures) {
                it.destroy()
            }
            depthTexture?.destroy()
        }
        depthRenderBuffer?.destroy()
        depthRenderBuffer = null
    }

    fun updateAttachments(layer: Int) {
        val textures = textures
        for (index in textures.indices) {
            val texture = textures[index]
            glFramebufferTextureLayer(
                GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + index,
                texture.pointer, 0, layer
            )
        }
        GFX.check()
        drawBuffersN(targets.size)
        GFX.check()
        if (depthBufferType == DepthBufferType.TEXTURE || depthBufferType == DepthBufferType.TEXTURE_16) {
            val depthTexture = depthTexture!!
            glFramebufferTextureLayer(
                GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT,
                depthTexture.pointer, 0, layer
            )
        }
        GFX.check()
    }

    fun draw(renderer: Renderer, renderSide: (layer: Int) -> Unit) {
        useFrame(this, renderer) {
            renderSides(renderSide)
        }
    }

    fun draw(renderer: Renderer, layer: Int, renderSide: () -> Unit) {
        useFrame(this, renderer) {
            renderSide(layer, renderSide)
        }
    }

    private fun renderSides(render: (layer: Int) -> Unit) {
        Frame.bind()
        for (layer in 0 until layers) {
            updateBind(layer)
            render(layer)
            textures.getOrNull(layer)?.needsMipmaps = true
        }
        depthTexture?.needsMipmaps = true
    }

    private fun renderSide(layer: Int, render: () -> Unit) {
        Frame.bind()
        updateBind(layer)
        render()
        depthTexture?.needsMipmaps = true
        textures.getOrNull(layer)?.needsMipmaps = true
    }

    private fun updateBind(layer: Int) {
        // update all attachments, updating the framebuffer texture targets
        updateAttachments(layer)
        val status = glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER)
        assertEquals(GL_FRAMEBUFFER_COMPLETE, status, "Framebuffer incomplete")
    }

    override fun attachFramebufferToDepth(name: String, targets: List<TargetType>): IFramebuffer {
        return if (targets.size <= GFX.maxColorAttachments) {
            val buffer = FramebufferArray(
                name, width, height, layers,
                samples, targets, DepthBufferType.ATTACHMENT
            )
            buffer.depthAttachment = this
            buffer
        } else {
            val buffer = MultiFramebufferArray(
                name, width, height, layers,
                samples, targets, DepthBufferType.ATTACHMENT
            )
            for (it in buffer.targetsI) it.depthAttachment = this
            buffer
        }
    }

    override fun toString(): String =
        "FBArray[n=$name, i=$pointer, w=$width, h=$height, d=$layers, t=${targets.joinToString()} d=$depthBufferType]"
}