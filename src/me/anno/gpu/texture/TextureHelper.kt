package me.anno.gpu.texture

import me.anno.gpu.GFX
import me.anno.utils.Color.a01
import me.anno.utils.Color.b01
import me.anno.utils.Color.g01
import me.anno.utils.Color.r01
import org.lwjgl.opengl.GL33C.*

object TextureHelper {

    fun clamping(target: Int, type: Int, border: Int) {
        glTexParameteri(target, GL_TEXTURE_WRAP_S, type)
        glTexParameteri(target, GL_TEXTURE_WRAP_T, type)
        if (target == GL_TEXTURE_3D || target == GL_TEXTURE_2D_ARRAY)
            glTexParameteri(target, GL_TEXTURE_WRAP_R, type)
        if (type == GL_CLAMP_TO_BORDER) {
            val tmp = Texture2D.tmp4f
            tmp[0] = border.r01()
            tmp[1] = border.g01()
            tmp[2] = border.b01()
            tmp[3] = border.a01()
            glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, tmp)
        }
    }

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