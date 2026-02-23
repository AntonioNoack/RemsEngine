package me.anno.gpu.framebuffer

import me.anno.gpu.ContextPointer
import me.anno.gpu.GFX
import me.anno.gpu.GFX.INVALID_POINTER
import me.anno.gpu.GFX.INVALID_SESSION
import me.anno.gpu.GFX.isPointerValid
import me.anno.gpu.GFXState
import me.anno.gpu.GLNames
import me.anno.gpu.framebuffer.Framebuffer.Companion.bindFramebuffer
import me.anno.gpu.framebuffer.Framebuffer.Companion.drawBuffersN
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture3D
import me.anno.gpu.texture.TextureLib
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import org.lwjgl.opengl.GL46C.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.GL46C.GL_DEPTH_ATTACHMENT
import org.lwjgl.opengl.GL46C.GL_DRAW_FRAMEBUFFER
import org.lwjgl.opengl.GL46C.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL46C.GL_FRAMEBUFFER_COMPLETE
import org.lwjgl.opengl.GL46C.GL_TEXTURE_3D
import org.lwjgl.opengl.GL46C.glCheckFramebufferStatus
import org.lwjgl.opengl.GL46C.glDeleteFramebuffers
import org.lwjgl.opengl.GL46C.glFramebufferTexture3D
import org.lwjgl.opengl.GL46C.glGenFramebuffers

class Framebuffer3D(
    override var name: String,
    override var width: Int,
    override var height: Int,
    var depth: Int,
    val targets: List<TargetType>,
    override val depthBufferType: DepthBufferType
) : IFramebuffer {

    override var pointer by ContextPointer()

    var session = INVALID_SESSION

    override val samples get() = 1
    override val numTextures get() = 1
    override var depthTexture: Texture3D? = null
    override val depthMask: Int get() = 0
    override var isSRGBMask: Int = 0

    var textures: List<Texture3D> = emptyList()

    var depthAttachedPtr = 0
    var depthAttachment: Framebuffer3D? = null

    override fun getTargetType(slot: Int) = targets[slot]

    fun create() {
        Frame.invalidate()
        GFX.check()
        val pointer = glGenFramebuffers()
        if (!isPointerValid(pointer)) throw OutOfMemoryError("Could not generate OpenGL framebuffer")
        session = GFXState.session
        // if (Build.isDebug) DebugGPUStorage.fbs.add(this)
        bindFramebuffer(GL_FRAMEBUFFER, pointer)
        Frame.lastPtr = pointer
        val w = width
        val h = height
        val d = depth
        assertTrue(w > 0 && h > 0 && d > 0, "Invalid framebuffer size")
        GFX.check()
        if (textures.size != targets.size) {
            textures = targets.mapIndexed { index, target ->
                val texture = Texture3D("$name-tex[$index]", w, h, d)
                // texture.autoUpdateMipmaps = autoUpdateMipmaps
                texture.create(target)
                GFX.check()
                texture
            }
        }
        GFX.check()
        val textures = textures
        for (index in targets.indices) {
            val texture = textures[index]
            glFramebufferTexture3D(
                GL_FRAMEBUFFER,
                GL_COLOR_ATTACHMENT0 + index,
                texture.target, texture.pointer,
                0, 0 // todo change the layer dynamically like for cubemaps
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
                if (depthTexture == null) {
                    val depthTexture = Texture3D("$name-depth", w, h, d)
                    // depthTexture.autoUpdateMipmaps = autoUpdateMipmaps
                    depthTexture.create(depthBufferType.chooseDepthFormat())
                    this.depthTexture = depthTexture
                }
                val depthTexture = depthTexture!!
                glFramebufferTexture3D(
                    GL_FRAMEBUFFER,
                    GL_DEPTH_ATTACHMENT,
                    depthTexture.target,
                    depthTexture.pointer,
                    0, 0
                )
            }
        }
        GFX.check()
        check()
        this.pointer = pointer
    }

    fun check() {
        val state = glCheckFramebufferStatus(GL_FRAMEBUFFER)
        if (state != GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("Framebuffer is incomplete: ${GLNames.getErrorTypeName(state)}")
        }
    }

    override fun ensure() {
        checkSession()
        if (!isPointerValid(pointer)) create()
    }

    override fun ensureSize(newWidth: Int, newHeight: Int, newDepth: Int) {
        if (width != newWidth || height != newHeight) {
            destroy()
            width = newWidth
            height = newHeight
            create()
        } else ensure()
    }

    override fun checkSession() {
        if (isPointerValid(pointer) && session != GFXState.session) {
            GFX.check()
            session = GFXState.session
            pointer = INVALID_POINTER
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
        ensureSize(w, h, depth)
        bind()
    }

    fun bind() {

        // if the depth-attachment base changed, we need to recreate this texture
        val da = if (depthBufferType == DepthBufferType.ATTACHMENT) depthAttachment else null
        if (da != null) {
            val dtp = da.depthTexture?.pointer
            if (dtp != depthAttachedPtr) {
                destroy()
            }
            assertEquals(width, da.width)
            assertEquals(height, da.height)
        }

        ensure()

        if (da != null) {
            val dtp = if (GFX.supportsDepthTextures) da.depthTexture?.pointer else null
            assertEquals(dtp, depthAttachedPtr)
        }

        bindFramebuffer(GL_FRAMEBUFFER, pointer)

        val textures = textures
        for (i in textures.indices) {
            invalidateTexture(textures[i])
        }
        invalidateTexture(depthTexture)
    }

    private fun invalidateTexture(texture: Texture3D?) {
        texture ?: return
        texture.filtering = Filtering.TRULY_NEAREST
    }

    override fun destroy() {
        if (isPointerValid(pointer)) {
            GFX.checkIsGFXThread()
            // ssBuffer?.destroy()
            destroyFramebuffer()
            destroyInternalDepth()
            destroyTextures(true)
        }
    }

    fun destroyFramebuffer() {
        if (isPointerValid(pointer)) {
            bindFramebuffer(GL_FRAMEBUFFER, 0)
            glDeleteFramebuffers(pointer)
            Frame.invalidate()
            // if (Build.isDebug) DebugGPUStorage.fbs.remove(this)
            pointer = INVALID_POINTER
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

    fun draw(renderer: Renderer, render: (z: Int) -> Unit) {
        GFXState.useFrame(this, renderer) {
            Frame.bind()
            bindDirectly()
            repeat(depth) { z ->
                // update all attachments, updating the framebuffer texture targets
                updateAttachments(z)
                val status = glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER)
                assertEquals(GL_FRAMEBUFFER_COMPLETE, status) { "Framebuffer incomplete $status" }
                render(z)
            }
        }
    }

    fun updateAttachments(z: Int) {
        GFX.check()
        val target = GL_TEXTURE_3D
        val textures = textures
        val level = 0
        val layer = z
        for (index in textures.indices) {
            val texture = textures[index]
            glFramebufferTexture3D(
                GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + index,
                target, texture.pointer, level, layer
            )
        }
        GFX.check()
        drawBuffersN(targets.size)
        GFX.check()
        when (depthBufferType) {
            DepthBufferType.TEXTURE, DepthBufferType.TEXTURE_16 -> {
                val depthTexture = depthTexture!!
                glFramebufferTexture3D(
                    GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT,
                    target, depthTexture.pointer, level, layer
                )
            }
            else -> {}
        }
        GFX.check()
    }

    fun destroyExceptTextures(deleteDepth: Boolean) {
        destroyFramebuffer()
        destroyInternalDepth()
        if (deleteDepth) destroyDepthTexture()
    }

    override fun getTexture0(): Texture3D = getTextureI(0)
    override fun getTextureI(index: Int): Texture3D {
        return textures.getOrNull(index) ?: TextureLib.missingTexture3d
    }

    override fun attachFramebufferToDepth(name: String, targets: List<TargetType>): IFramebuffer {
        throw NotImplementedError()
    }
}