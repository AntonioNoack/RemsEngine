package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.framebuffer.Framebuffer.Companion.drawBuffersN
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture3D
import org.lwjgl.opengl.GL30C.*

class Framebuffer3D(
    override var name: String,
    override var width: Int,
    override var height: Int,
    val d: Int,
    val targets: Array<TargetType>,
    val depthBufferType: DepthBufferType
) : IFramebuffer {

    override var pointer = 0
    var session = 0

    override val samples = 1
    override val numTextures = 1
    override var depthTexture: Texture3D? = null

    lateinit var textures: Array<Texture3D>

    var depthAttachedPtr = 0
    var depthAttachment: Framebuffer3D? = null

    override fun getTargetType(slot: Int) = targets[slot]

    fun create() {
        Frame.invalidate()
        GFX.check()
        val pointer = glGenFramebuffers()
        if (pointer == 0) throw OutOfMemoryError("Could not generate OpenGL framebuffer")
        session = GFXState.session
        // if (Build.isDebug) DebugGPUStorage.fbs.add(this)
        Framebuffer.bindFramebuffer(GL_FRAMEBUFFER, pointer)
        Frame.lastPtr = pointer
        val w = width
        val h = height
        val d = d
        if (w * h * d < 1) throw RuntimeException("Invalid framebuffer size $w x $h x $d")
        GFX.check()
        textures = Array(targets.size) { index ->
            val texture = Texture3D("$name-tex[$index]", w, h, d)
            // texture.autoUpdateMipmaps = autoUpdateMipmaps
            texture.create(targets[index])
            GFX.check()
            texture
        }
        GFX.check()
        val textures = textures
        for (index in targets.indices) {
            val texture = textures[index]
            glFramebufferTexture3D(
                GL_FRAMEBUFFER,
                GL_COLOR_ATTACHMENT0 + index,
                texture.target,
                texture.pointer,
                0,
                0 // todo change the layer dynamically like for cubemaps
            )
        }
        GFX.check()
        drawBuffersN(targets.size)
        GFX.check()
        when (depthBufferType) {
            DepthBufferType.NONE -> {
            }
            DepthBufferType.ATTACHMENT -> {
                val texPointer = depthAttachment?.depthTexture?.pointer
                    ?: throw IllegalStateException("Depth Attachment was not found in $name, ${depthAttachment}.${depthAttachment?.depthTexture}")
                glFramebufferTexture3D(
                    GL_FRAMEBUFFER,
                    GL_DEPTH_ATTACHMENT,
                    GL_TEXTURE_3D,
                    texPointer,
                    0, 0
                )
                depthAttachedPtr = texPointer
            }
            DepthBufferType.INTERNAL -> throw NotImplementedError()// createDepthBuffer()
            DepthBufferType.TEXTURE, DepthBufferType.TEXTURE_16 -> {
                val depthTexture = Texture3D("$name-depth", w, h, d)
                // depthTexture.autoUpdateMipmaps = autoUpdateMipmaps
                depthTexture.create(if (depthBufferType == DepthBufferType.TEXTURE_16) TargetType.DEPTH16 else TargetType.DEPTH32F)
                glFramebufferTexture3D(
                    GL_FRAMEBUFFER,
                    GL_DEPTH_ATTACHMENT,
                    depthTexture.target,
                    depthTexture.pointer,
                    0, 0
                )
                this.depthTexture = depthTexture
            }
        }
        GFX.check()
        check()
        this.pointer = pointer
    }

    fun check() {
        val state = glCheckFramebufferStatus(GL_FRAMEBUFFER)
        if (state != GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("Framebuffer is incomplete: ${GFX.getErrorTypeName(state)}")
        }
    }

    override fun ensure() {
        checkSession()
        if (pointer == 0) create()
    }

    override fun checkSession() {
        if (pointer != 0 && session != GFXState.session) {
            GFX.check()
            session = GFXState.session
            pointer = 0
            // needsBlit = true
            // ssBuffer?.checkSession()
            // depthTexture?.checkSession()
            for (texture in textures) {
                texture.checkSession()
            }
            GFX.check()
            // validate it
            create()
        }
    }

    override fun bindDirectly() {
        bindDirectly(width, height)
    }

    override fun bindDirectly(w: Int, h: Int) {

    }

    override fun destroy() {
        if (pointer != 0) {
            GFX.checkIsGFXThread()
            // ssBuffer?.destroy()
            destroyFramebuffer()
            destroyInternalDepth()
            destroyTextures(true)
        }
    }

    fun destroyFramebuffer() {
        if (pointer != 0) {
            glDeleteFramebuffers(pointer)
            Frame.invalidate()
            // if (Build.isDebug) DebugGPUStorage.fbs.remove(this)
            pointer = 0
        }
    }

    fun destroyInternalDepth() {
        // not implemented
        /*if (internalDepthTexture > -1) {
            glDeleteRenderbuffers(internalDepthTexture)
            depthAllocated = Texture2D.allocate(depthAllocated, 0L)
            internalDepthTexture = -1
        }*/
    }

    fun destroyTextures(deleteDepth: Boolean) {
        for (tex in textures) tex.destroy()
        if (deleteDepth) destroyDepthTexture()
    }

    fun destroyDepthTexture() {
        depthTexture?.destroy()
    }

    override fun bindTextureI(index: Int, offset: Int, nearest: Filtering, clamping: Clamping) {
        textures[index].bind(offset, nearest, clamping)
    }

    override fun bindTextures(offset: Int, nearest: Filtering, clamping: Clamping) {
        for (index in textures.indices) {
            textures[index].bind(index + offset, nearest, clamping)
        }
    }

    fun draw(renderer: Renderer, render: (side: Int) -> Unit) {
        GFXState.useFrame(this, renderer) {
            Frame.bind()
            for (slice in 0 until d) {
                // update all attachments, updating the framebuffer texture targets
                updateAttachments(slice)
                val status = glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER)
                if (status != GL_FRAMEBUFFER_COMPLETE) throw IllegalStateException("Framebuffer incomplete $status")
                render(slice)
            }
        }
    }

    fun updateAttachments(layer: Int) {
        val target = GL_TEXTURE_3D
        val textures = textures
        for (index in textures.indices) {
            val texture = textures[index]
            glFramebufferTexture3D(
                GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + index,
                target, texture.pointer, 0, layer
            )
        }
        GFX.check()
        drawBuffersN(targets.size)
        GFX.check()
        if (depthBufferType == DepthBufferType.TEXTURE || depthBufferType == DepthBufferType.TEXTURE_16) {
            val depthTexture = depthTexture!!
            glFramebufferTexture3D(
                GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT,
                target, depthTexture.pointer, 0, layer
            )
        }
        GFX.check()
    }

    fun destroyExceptTextures(deleteDepth: Boolean) {
        /*if (ssBuffer != null) {
            ssBuffer?.destroyExceptTextures(deleteDepth)
            ssBuffer = null
            destroy()
        } else {*/
        destroyFramebuffer()
        destroyInternalDepth()
        if (deleteDepth) destroyDepthTexture()
        //}
    }

    override fun getTexture0() = textures[0]
    override fun getTextureI(index: Int) = textures[index]

    override fun attachFramebufferToDepth(name: String, targets: Array<TargetType>): IFramebuffer {
        throw NotImplementedError()
    }

}