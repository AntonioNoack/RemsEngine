package me.anno.gpu.texture

import me.anno.gpu.GFX
import me.anno.utils.Color.a01
import me.anno.utils.Color.b01
import me.anno.utils.Color.g01
import me.anno.utils.Color.r01
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL33C.*

object TextureHelper {

    private val LOGGER = LogManager.getLogger(TextureHelper::class)

    fun clamping(target: Int, value0: Int, borderColor: Int) {
        var value = value0
        if (value == GL_CLAMP_TO_BORDER && OS.isWeb) {
            LOGGER.warn("GL_CLAMP_TO_BORDER is not supported in WebGL; using GL_CLAMP_TO_EDGE")
            value = GL_CLAMP_TO_EDGE
        }
        glTexParameteri(target, GL_TEXTURE_WRAP_S, value)
        glTexParameteri(target, GL_TEXTURE_WRAP_T, value)
        if (target == GL_TEXTURE_3D || target == GL_TEXTURE_2D_ARRAY)
            glTexParameteri(target, GL_TEXTURE_WRAP_R, value)
        if (value == GL_CLAMP_TO_BORDER) {
            val tmp = Texture2D.tmp4f
            tmp[0] = borderColor.r01()
            tmp[1] = borderColor.g01()
            tmp[2] = borderColor.b01()
            tmp[3] = borderColor.a01()
            glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, tmp)
        }
    }

    /**
     * This is a helper to define swizzling on a texture.
     * Don't use this if possible, because it's not supported on Android, WebGL, nor DirectX.
     * Probably Vulkan either, but idk.
     * */
    fun swizzle(target: Int, r: Int, g: Int, b: Int, a: Int) {
        val tmp = Texture2D.tmp4i
        tmp[0] = r
        tmp[1] = g
        tmp[2] = b
        tmp[3] = a
        GFX.check()
        glTexParameteriv(target, GL_TEXTURE_SWIZZLE_RGBA, tmp)
        GFX.check()
    }
}