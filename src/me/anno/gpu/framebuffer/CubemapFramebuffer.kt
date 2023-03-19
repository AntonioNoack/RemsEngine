package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.Framebuffer.Companion.bindFramebuffer
import me.anno.gpu.shader.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.CubemapTexture
import me.anno.gpu.texture.GPUFiltering
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL13C
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL30C

class CubemapFramebuffer(
    override var name: String, var size: Int,
    override val samples: Int, // todo when we support multi-sampled cubemaps, also support them here
    val targets: Array<TargetType>,
    val depthBufferType: DepthBufferType
) : IFramebuffer {

    constructor(
        name: String, size: Int,
        samples: Int,
        targetCount: Int,
        fpTargets: Boolean,
        depthBufferType: DepthBufferType
    ) : this(
        name, size, samples, if (fpTargets)
            Array(targetCount) { TargetType.FloatTarget4 } else
            Array(targetCount) { TargetType.UByteTarget4 }, depthBufferType
    )

    // multiple targets, layout=x require shader version 330+
    // use glBindFragDataLocation instead

    override var pointer = 0
    var session = 0
    var depthRenderBuffer = 0
    override var depthTexture: CubemapTexture? = null
    var depthAttachment: CubemapFramebuffer? = null

    override val w: Int get() = size
    override val h: Int get() = size
    override val numTextures: Int = targets.size

    lateinit var textures: Array<CubemapTexture>

    var autoUpdateMipmaps = true

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
        GL11C.glDisable(GL13C.GL_MULTISAMPLE)
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
        if (pointer == 0) throw RuntimeException()
        bindFramebuffer(GL_FRAMEBUFFER, pointer)
        Frame.lastPtr = pointer
        //stack.push(this)
        GFX.check()
        textures = Array(targets.size) { index ->
            val texture = CubemapTexture("$name-$index", size, samples)
            texture.autoUpdateMipmaps = autoUpdateMipmaps
            texture.create(targets[index])
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
                val texture = CubemapTexture("$name-depth", size, samples)
                texture.autoUpdateMipmaps = autoUpdateMipmaps
                texture.createDepth(depthBufferType == DepthBufferType.TEXTURE_16)
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
                GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT, target, texPointer, 0)
                // throw IllegalArgumentException("attachment depth not yet supported for cubemaps")
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
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, size, size)
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, renderBuffer)
    }

    private fun check() {
        val state = glCheckFramebufferStatus(GL_FRAMEBUFFER)
        if (state != GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("Framebuffer is incomplete: ${GFX.getErrorTypeName(state)}")
        }
    }

    /*fun bindTexture0(offset: Int = 0, nearest: GPUFiltering, clamping: Clamping) {
        bindTextureI(0, offset, nearest, clamping)
    }

    fun bindTextureI(index: Int, offset: Int) {
        bindTextureI(index, offset, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
    }

    fun bindTextureI(index: Int, offset: Int, nearest: GPUFiltering, clamping: Clamping) {
        textures[index].bind(offset, nearest, clamping)
    }

    fun bindTextures(offset: Int = 0, nearest: GPUFiltering, clamping: Clamping) {
        GFX.check()
        for ((index, texture) in textures.withIndex()) {
            texture.bind(offset + index, nearest, clamping)
        }
        GFX.check()
    }*/

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
        when (targets.size) {
            0 -> glDrawBuffer(GL_NONE)
            1 -> glDrawBuffer(GL_COLOR_ATTACHMENT0)
            else -> glDrawBuffers(textures.indices.map { it + GL_COLOR_ATTACHMENT0 }.toIntArray())
        }
        GFX.check()
        if (depthBufferType == DepthBufferType.TEXTURE || depthBufferType == DepthBufferType.TEXTURE_16) {
            val depthTexture = depthTexture!!
            glFramebufferTexture2D(
                GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT,
                tex2D, depthTexture.pointer, 0
            )
        }
        GFX.check()
    }

    fun draw(size: Int, renderer: Renderer, render: (side: Int) -> Unit) {
        useFrame(size, size, true, this, renderer) {
            Frame.bind()
            for (side in 0 until 6) {
                // update all attachments, updating the framebuffer texture targets
                updateAttachments(side)
                val status = glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER)
                if (status != GL_FRAMEBUFFER_COMPLETE) throw IllegalStateException("Framebuffer incomplete $status")
                render(side)
            }
        }
    }

    fun draw(renderer: Renderer, render: (side: Int) -> Unit) {
        useFrame(this, renderer) {
            Frame.bind()
            for (side in 0 until 6) {
                // update all attachments, updating the framebuffer texture targets
                updateAttachments(side)
                val status = glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER)
                if (status != GL_FRAMEBUFFER_COMPLETE) throw IllegalStateException("Framebuffer incomplete $status")
                render(side)
            }
        }
    }

    override fun attachFramebufferToDepth(name: String, targetCount: Int, fpTargets: Boolean): IFramebuffer {
        return if (targetCount <= GFX.maxColorAttachments) {
            val buffer = CubemapFramebuffer(name, size, samples, targetCount, fpTargets, DepthBufferType.ATTACHMENT)
            buffer.depthAttachment = this
            buffer
        } else {
            TODO("Cubemaps with attachment depth not yet implemented for $targetCount > ${GFX.maxColorAttachments}")
            /*val buffer = MultiFramebuffer(name, size, samples, targetCount, fpTargets, DepthBufferType.ATTACHMENT)
            for (it in buffer.targetsI) it.depthAttachment = this
            buffer*/
        }
    }

    override fun attachFramebufferToDepth(name: String, targets: Array<TargetType>): IFramebuffer {
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
        "FBCubemap[n=$name, i=$pointer, size=$size t=${targets.joinToString()} d=$depthBufferType]"

}