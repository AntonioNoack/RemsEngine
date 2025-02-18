package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.GLNames
import me.anno.gpu.framebuffer.Framebuffer.Companion.bindFramebuffer
import me.anno.gpu.framebuffer.Framebuffer.Companion.drawBuffersN
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.CubemapTexture
import me.anno.gpu.texture.Filtering
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.structures.lists.Lists.createList
import org.lwjgl.opengl.GL46C.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.GL46C.GL_DEPTH_ATTACHMENT
import org.lwjgl.opengl.GL46C.GL_DRAW_FRAMEBUFFER
import org.lwjgl.opengl.GL46C.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL46C.GL_FRAMEBUFFER_COMPLETE
import org.lwjgl.opengl.GL46C.GL_TEXTURE_CUBE_MAP_POSITIVE_X
import org.lwjgl.opengl.GL46C.glCheckFramebufferStatus
import org.lwjgl.opengl.GL46C.glDeleteFramebuffers
import org.lwjgl.opengl.GL46C.glFramebufferTexture2D
import org.lwjgl.opengl.GL46C.glGenFramebuffers

class CubemapFramebuffer(
    override var name: String, var size: Int,
    override val samples: Int, // todo when we support multi-sampled cubemaps, also support them here
    val targets: List<TargetType>,
    override val depthBufferType: DepthBufferType
) : IFramebuffer {

    constructor(
        name: String, size: Int, samples: Int,
        targetCount: Int, fpTargets: Boolean,
        depthBufferType: DepthBufferType
    ) : this(
        name, size, samples,
        createList(targetCount, if (fpTargets) TargetType.Float32x4 else TargetType.UInt8x4),
        depthBufferType
    )

    // multiple targets, layout=x require shader version 330+
    // use glBindFragDataLocation instead

    override var pointer = 0
    var session = 0
    var depthRenderBuffer: Renderbuffer? = null
    override var isSRGBMask: Int = 0
    override var depthTexture: CubemapTexture? = null
    override val depthMask: Int get() = 0
    var depthAttachment: CubemapFramebuffer? = null

    override val width: Int get() = size
    override val height: Int get() = size
    override val numTextures: Int = targets.size

    var textures: List<CubemapTexture> = emptyList()

    var autoUpdateMipmaps = true

    val isCreated get() = pointer != 0

    override fun getTargetType(slot: Int) = targets[slot]

    override fun ensure() {
        checkSession()
        if (pointer == 0) create()
    }

    override fun ensureSize(newWidth: Int, newHeight: Int, newDepth: Int) {
        if (newWidth != size) {
            destroy()
            size = newWidth
            create()
        } else ensure()
    }

    override fun checkSession() {
        if (session != GFXState.session) {
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
        }
    }

    override fun getTextureI(index: Int) = textures[index]
    override fun getTexture0() = textures[0] // overridden for the result type

    override fun bindDirectly() = bind()
    override fun bindDirectly(w: Int, h: Int) {
        bindDirectly(w)
    }

    fun bindDirectly(size: Int) {
        checkSize(size)
        bind()
    }

    private fun bind() {
        if (pointer == 0) create()
        bindFramebuffer(GL_FRAMEBUFFER, pointer)
        Frame.lastPtr = pointer
    }

    private fun checkSize(newSize: Int) {
        if (newSize != size) {
            size = newSize
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
        assertNotEquals(0, pointer)
        bindFramebuffer(GL_FRAMEBUFFER, pointer)
        Frame.lastPtr = pointer
        //stack.push(this)
        GFX.check()
        textures = targets.mapIndexed { index, target ->
            val texture = CubemapTexture("$name-$index", size, samples)
            texture.autoUpdateMipmaps = autoUpdateMipmaps
            texture.create(target)
            GFX.check()
            texture
        }
        GFX.check()
        val textures = textures
        for (index in textures.indices) {
            val texture = textures[index]
            glFramebufferTexture2D(
                GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + index,
                GL_TEXTURE_CUBE_MAP_POSITIVE_X, texture.pointer, 0
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
                val texture = CubemapTexture("$name-depth", size, samples)
                texture.autoUpdateMipmaps = autoUpdateMipmaps
                texture.create(depthBufferType.chooseDepthFormat())
                glFramebufferTexture2D(
                    GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT,
                    GL_TEXTURE_CUBE_MAP_POSITIVE_X, texture.pointer, 0
                )
                this.depthTexture = texture
            }
            DepthBufferType.ATTACHMENT -> {
                val target = GL_TEXTURE_CUBE_MAP_POSITIVE_X
                val texPointer = depthAttachment?.depthTexture?.pointer
                    ?: throw IllegalStateException("Depth Attachment was not found in $name, ${depthAttachment}.${depthAttachment?.depthTexture}")
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, target, texPointer, 0)
                // throw IllegalArgumentException("attachment depth not yet supported for cubemaps")
            }
        }
        GFX.check()
        check()
    }

    private fun createDepthBuffer() {
        val renderbuffer = Renderbuffer()
        renderbuffer.createDepthBuffer(size, size, samples)
        renderbuffer.attachToFramebuffer(false)
        depthRenderBuffer = renderbuffer
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
        if (pointer != 0) {
            bindFramebuffer(GL_FRAMEBUFFER, 0)
            glDeleteFramebuffers(pointer)
            Frame.invalidate()
            pointer = 0
            for (it in textures) {
                it.destroy()
            }
            depthTexture?.destroy()
        }
        depthRenderBuffer?.destroy()
        depthRenderBuffer = null
    }

    fun updateAttachments(face: Int) {
        val tex2D = GL_TEXTURE_CUBE_MAP_POSITIVE_X + face
        val textures = textures
        for (index in textures.indices) {
            val texture = textures[index]
            glFramebufferTexture2D(
                GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + index,
                tex2D, texture.pointer, 0
            )
        }
        GFX.check()
        drawBuffersN(targets.size)
        GFX.check()
        if (depthBufferType == DepthBufferType.TEXTURE || depthBufferType == DepthBufferType.TEXTURE_16) {
            val depthTexture = depthTexture!!
            assertNotEquals(0, depthTexture.pointer)
            glFramebufferTexture2D(
                GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT,
                tex2D, depthTexture.pointer, 0
            )
        }
        GFX.check()
    }

    fun draw(size: Int, renderer: Renderer, renderSide: (side: Int) -> Unit) {
        useFrame(size, size, true, this, renderer) {
            renderSides(renderSide)
        }
    }

    fun draw(renderer: Renderer, renderSide: (side: Int) -> Unit) {
        useFrame(this, renderer) {
            renderSides(renderSide)
        }
    }

    private fun renderSides(render: (side: Int) -> Unit) {
        GFX.check()
        ensure()
        Frame.bind()
        GFX.check()
        for (side in 0 until 6) {
            // update all attachments, updating the framebuffer texture targets
            updateAttachments(side)
            GFX.check()
            val status = glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER)
            if (status != GL_FRAMEBUFFER_COMPLETE) throw IllegalStateException("${GLNames.getName(status)}, $this")
            GFX.check()
            render(side)
        }
        depthTexture?.needsMipmaps = true
        for (i in textures.indices) textures[i].needsMipmaps = true
        GFX.check()
    }

    override fun attachFramebufferToDepth(name: String, targets: List<TargetType>): IFramebuffer {
        return if (targets.size <= GFX.maxColorAttachments) {
            val buffer = CubemapFramebuffer(name, size, samples, targets, DepthBufferType.ATTACHMENT)
            buffer.depthAttachment = this
            buffer
        } else {
            TODO("Cubemaps with attachment depth not yet implemented for ${targets.size} > ${GFX.maxColorAttachments}")
            /*val buffer = MultiFramebuffer(name, size, samples, targetCount, fpTargets, DepthBufferType.ATTACHMENT)
            for (it in buffer.targetsI) it.depthAttachment = this
            buffer*/
        }
    }

    override fun toString(): String =
        "FBCubemap[\"$name\"@$pointer, $size² x $samples t=$targets d=$depthBufferType]"
}