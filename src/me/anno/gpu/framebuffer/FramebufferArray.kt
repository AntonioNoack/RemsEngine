package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.Framebuffer.Companion.bindFramebuffer
import me.anno.gpu.framebuffer.Framebuffer.Companion.drawBuffersN
import me.anno.gpu.framebuffer.IFramebuffer.Companion.createTargets
import me.anno.gpu.shader.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2DArray
import org.lwjgl.opengl.GL30C.*

class FramebufferArray(
    override var name: String,
    override var width: Int,
    override var height: Int,
    var layers: Int,
    override val samples: Int, // todo when we support multi-sampled 2d-arrays, also support them here
    val targets: Array<TargetType>,
    val depthBufferType: DepthBufferType
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
        name, width, height, layers, samples, if (fpTargets)
            Array(targetCount) { TargetType.FloatTarget4 } else
            Array(targetCount) { TargetType.UByteTarget4 }, depthBufferType
    )

    // multiple targets, layout=x require shader version 330+
    // use glBindFragDataLocation instead

    override var pointer = 0
    var session = 0
    var depthRenderBuffer = 0
    override var depthTexture: Texture2DArray? = null
    var depthAttachment: FramebufferArray? = null

    override val numTextures: Int = targets.size

    lateinit var textures: Array<Texture2DArray>

    var autoUpdateMipmaps = true

    val isCreated get() = pointer != 0

    override fun getTargetType(slot: Int) = targets[slot]

    override fun ensure() {
        if (pointer == 0) create()
    }

    override fun checkSession() {
        if (pointer != 0 && session != GFXState.session) {
            GFX.check()
            session = GFXState.session
            pointer = 0
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

    override fun getTextureI(index: Int) = textures[index]
    override fun getTexture0() = textures[0] // overridden for the result type

    override fun bindDirectly() = bind()
    override fun bindDirectly(w: Int, h: Int) {
        checkSize(w, h)
        bind()
    }

    private fun bind() {
        if (pointer == 0) create()
        bindFramebuffer(GL_FRAMEBUFFER, pointer)
        Frame.lastPtr = pointer
        glDisable(GL_MULTISAMPLE)
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
        if (pointer == 0) throw RuntimeException()
        bindFramebuffer(GL_FRAMEBUFFER, pointer)
        Frame.lastPtr = pointer
        //stack.push(this)
        GFX.check()
        textures = Array(targets.size) { index ->
            val texture = Texture2DArray("$name-$index", width, height, layers)
            texture.autoUpdateMipmaps = autoUpdateMipmaps
            texture.create(targets[index])
            GFX.check()
            texture
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
            DepthBufferType.INTERNAL -> createDepthBuffer()
            DepthBufferType.TEXTURE, DepthBufferType.TEXTURE_16 -> {
                GFX.check()
                val texture = Texture2DArray("$name-depth", width, height, layers)
                texture.autoUpdateMipmaps = autoUpdateMipmaps
                texture.createDepth(depthBufferType == DepthBufferType.TEXTURE_16)
                GFX.check()
                glFramebufferTextureLayer(
                    GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT,
                    texture.pointer, 0, 0
                )
                GFX.check()
                this.depthTexture = texture
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
        val renderBuffer = glGenRenderbuffers()
        depthRenderBuffer = renderBuffer
        if (renderBuffer < 0) throw RuntimeException()
        glBindRenderbuffer(GL_RENDERBUFFER, renderBuffer)
        if (samples > 1) glRenderbufferStorageMultisample(GL_RENDERBUFFER, samples, GL_DEPTH_COMPONENT, width, height)
        else glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, width, height)
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, renderBuffer)
    }

    private fun check() {
        val state = glCheckFramebufferStatus(GL_FRAMEBUFFER)
        if (state != GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("Framebuffer is incomplete: ${GFX.getErrorTypeName(state)}")
        }
    }

    override fun bindTextureI(index: Int, offset: Int, nearest: GPUFiltering, clamping: Clamping) {
        checkSession()
        textures[index].bind(offset, nearest, clamping)
    }

    override fun bindTextures(offset: Int, nearest: GPUFiltering, clamping: Clamping) {
        checkSession()
        for (textureIndex in textures.indices) {
            textures[textureIndex].bind(offset + textureIndex, nearest, clamping)
        }
    }

    override fun destroy() {
        if (pointer != 0) {
            glDeleteFramebuffers(pointer)
            Frame.invalidate()
            pointer = 0
            for (it in textures) {
                it.destroy()
            }
            depthTexture?.destroy()
        }
        if (depthRenderBuffer != 0) {
            glDeleteRenderbuffers(depthRenderBuffer)
            depthRenderBuffer = 0
        }
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

    private fun renderSides(render: (layer: Int) -> Unit) {
        Frame.bind()
        for (layer in 0 until layers) {
            // update all attachments, updating the framebuffer texture targets
            updateAttachments(layer)
            val status = glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER)
            if (status != GL_FRAMEBUFFER_COMPLETE) throw IllegalStateException("Framebuffer incomplete $status")
            render(layer)
        }
        depthTexture?.needsMipmaps = true
        for (i in textures.indices) textures[i].needsMipmaps = true
    }

    override fun attachFramebufferToDepth(name: String, targets: Array<TargetType>): IFramebuffer {
        return if (targets.size <= GFX.maxColorAttachments) {
            val buffer = FramebufferArray(name, width, height, layers, samples, targets, DepthBufferType.ATTACHMENT)
            buffer.depthAttachment = this
            buffer
        } else {
            TODO("Framebuffer arrays with attachment depth not yet implemented for ${targets.size} > ${GFX.maxColorAttachments}")
            /*val buffer = MultiFramebuffer(name, size, samples, targetCount, fpTargets, DepthBufferType.ATTACHMENT)
            for (it in buffer.targetsI) it.depthAttachment = this
            buffer*/
        }
    }

    override fun toString(): String =
        "FBArray[n=$name, i=$pointer, w=$width, h=$height, d=$layers, t=${targets.joinToString()} d=$depthBufferType]"
}