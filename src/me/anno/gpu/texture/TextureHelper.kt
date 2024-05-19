package me.anno.gpu.texture

import me.anno.gpu.GFX
import me.anno.utils.Color.a01
import me.anno.utils.Color.b01
import me.anno.utils.Color.g01
import me.anno.utils.Color.r01
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL46C.GL_BYTE
import org.lwjgl.opengl.GL46C.GL_CLAMP_TO_BORDER
import org.lwjgl.opengl.GL46C.GL_CLAMP_TO_EDGE
import org.lwjgl.opengl.GL46C.GL_FLOAT
import org.lwjgl.opengl.GL46C.GL_HALF_FLOAT
import org.lwjgl.opengl.GL46C.GL_INT
import org.lwjgl.opengl.GL46C.GL_R16
import org.lwjgl.opengl.GL46C.GL_R16F
import org.lwjgl.opengl.GL46C.GL_R16I
import org.lwjgl.opengl.GL46C.GL_R16UI
import org.lwjgl.opengl.GL46C.GL_R32F
import org.lwjgl.opengl.GL46C.GL_R32I
import org.lwjgl.opengl.GL46C.GL_R32UI
import org.lwjgl.opengl.GL46C.GL_R8
import org.lwjgl.opengl.GL46C.GL_R8I
import org.lwjgl.opengl.GL46C.GL_R8UI
import org.lwjgl.opengl.GL46C.GL_RG16
import org.lwjgl.opengl.GL46C.GL_RG16F
import org.lwjgl.opengl.GL46C.GL_RG16I
import org.lwjgl.opengl.GL46C.GL_RG16UI
import org.lwjgl.opengl.GL46C.GL_RG32F
import org.lwjgl.opengl.GL46C.GL_RG32I
import org.lwjgl.opengl.GL46C.GL_RG32UI
import org.lwjgl.opengl.GL46C.GL_RG8
import org.lwjgl.opengl.GL46C.GL_RG8I
import org.lwjgl.opengl.GL46C.GL_RG8UI
import org.lwjgl.opengl.GL46C.GL_RGB16
import org.lwjgl.opengl.GL46C.GL_RGB16F
import org.lwjgl.opengl.GL46C.GL_RGB16I
import org.lwjgl.opengl.GL46C.GL_RGB16UI
import org.lwjgl.opengl.GL46C.GL_RGB32F
import org.lwjgl.opengl.GL46C.GL_RGB32I
import org.lwjgl.opengl.GL46C.GL_RGB32UI
import org.lwjgl.opengl.GL46C.GL_RGB8
import org.lwjgl.opengl.GL46C.GL_RGB8I
import org.lwjgl.opengl.GL46C.GL_RGB8UI
import org.lwjgl.opengl.GL46C.GL_RGBA16
import org.lwjgl.opengl.GL46C.GL_RGBA16F
import org.lwjgl.opengl.GL46C.GL_RGBA16I
import org.lwjgl.opengl.GL46C.GL_RGBA16UI
import org.lwjgl.opengl.GL46C.GL_RGBA32F
import org.lwjgl.opengl.GL46C.GL_RGBA32I
import org.lwjgl.opengl.GL46C.GL_RGBA32UI
import org.lwjgl.opengl.GL46C.GL_RGBA8
import org.lwjgl.opengl.GL46C.GL_RGBA8I
import org.lwjgl.opengl.GL46C.GL_RGBA8UI
import org.lwjgl.opengl.GL46C.GL_SHORT
import org.lwjgl.opengl.GL46C.GL_TEXTURE_2D_ARRAY
import org.lwjgl.opengl.GL46C.GL_TEXTURE_3D
import org.lwjgl.opengl.GL46C.GL_TEXTURE_BORDER_COLOR
import org.lwjgl.opengl.GL46C.GL_TEXTURE_SWIZZLE_RGBA
import org.lwjgl.opengl.GL46C.GL_TEXTURE_WRAP_R
import org.lwjgl.opengl.GL46C.GL_TEXTURE_WRAP_S
import org.lwjgl.opengl.GL46C.GL_TEXTURE_WRAP_T
import org.lwjgl.opengl.GL46C.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL46C.GL_UNSIGNED_INT
import org.lwjgl.opengl.GL46C.GL_UNSIGNED_SHORT
import org.lwjgl.opengl.GL46C.glTexParameterfv
import org.lwjgl.opengl.GL46C.glTexParameteri
import org.lwjgl.opengl.GL46C.glTexParameteriv

object TextureHelper {

    private val tmp4i = IntArray(4)

    private val tmp4f = FloatArray(4)

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
            val tmp = tmp4f
            tmp[0] = borderColor.r01()
            tmp[1] = borderColor.g01()
            tmp[2] = borderColor.b01()
            tmp[3] = borderColor.a01()
            glTexParameterfv(target, GL_TEXTURE_BORDER_COLOR, tmp)
        }
    }

    /**
     * This is a helper to define swizzling on a texture.
     * Don't use this if possible, because it's not supported on Android, WebGL, nor DirectX.
     * Probably Vulkan either, but idk.
     * */
    fun swizzle(target: Int, r: Int, g: Int, b: Int, a: Int) {
        val tmp = tmp4i
        tmp[0] = r
        tmp[1] = g
        tmp[2] = b
        tmp[3] = a
        GFX.check()
        glTexParameteriv(target, GL_TEXTURE_SWIZZLE_RGBA, tmp)
        GFX.check()
    }

    fun getNumChannels(format: Int): Int {
        return when (format) {
            0 -> 0
            GL_R8, GL_R8I, GL_R8UI, GL_R16F, GL_R16I, GL_R16UI, GL_R32F, GL_R32I, GL_R32UI -> 1
            GL_RG8, GL_RG8I, GL_RG8UI, GL_RG16F, GL_RG16I, GL_RG16UI, GL_RG32F, GL_RG32I, GL_RG32UI -> 2
            GL_RGB8, GL_RGB8I, GL_RGB8UI, GL_RGB16F, GL_RGB16I, GL_RGB16UI, GL_RGB32F, GL_RGB32I, GL_RGB32UI -> 3
            else -> 4
        }
    }

    fun getNumberType(format: Int): Int {
        return when (format) {
            GL_R8, GL_RG8, GL_RGB8, GL_RGBA8 -> GL_UNSIGNED_BYTE.inv()
            GL_R8UI, GL_RG8UI, GL_RGB8UI, GL_RGBA8UI -> GL_UNSIGNED_BYTE
            GL_R8I, GL_RG8I, GL_RGB8I, GL_RGBA8I -> GL_BYTE
            GL_R16, GL_RG16, GL_RGB16, GL_RGBA16 -> GL_UNSIGNED_SHORT.inv()
            GL_R16UI, GL_RG16UI, GL_RGB16UI, GL_RGBA16UI -> GL_UNSIGNED_SHORT
            GL_R16I, GL_RG16I, GL_RGB16I, GL_RGBA16I -> GL_SHORT
            GL_R32I, GL_RG32I, GL_RGB32I, GL_RGBA32I -> GL_INT
            GL_R32UI, GL_RG32UI, GL_RGB32UI, GL_RGBA32UI -> GL_UNSIGNED_INT
            GL_R16F, GL_RG16F, GL_RGB16F, GL_RGBA16F -> GL_HALF_FLOAT
            GL_R32F, GL_RG32F, GL_RGB32F, GL_RGBA32F -> GL_FLOAT
            else -> 0
        }
    }
}