package me.anno.gpu.framebuffer

import me.anno.gpu.GFX.supportsF16Targets
import me.anno.gpu.GFX.supportsF32Targets
import me.anno.gpu.GLNames.getName
import me.anno.utils.GFXFeatures
import org.lwjgl.opengl.GL46C.GL_DEPTH_COMPONENT
import org.lwjgl.opengl.GL46C.GL_DEPTH_COMPONENT16
import org.lwjgl.opengl.GL46C.GL_DEPTH_COMPONENT32
import org.lwjgl.opengl.GL46C.GL_DEPTH_COMPONENT32F
import org.lwjgl.opengl.GL46C.GL_FLOAT
import org.lwjgl.opengl.GL46C.GL_HALF_FLOAT
import org.lwjgl.opengl.GL46C.GL_R16
import org.lwjgl.opengl.GL46C.GL_R16F
import org.lwjgl.opengl.GL46C.GL_R32F
import org.lwjgl.opengl.GL46C.GL_R8
import org.lwjgl.opengl.GL46C.GL_RED
import org.lwjgl.opengl.GL46C.GL_RG
import org.lwjgl.opengl.GL46C.GL_RG16
import org.lwjgl.opengl.GL46C.GL_RG16F
import org.lwjgl.opengl.GL46C.GL_RG32F
import org.lwjgl.opengl.GL46C.GL_RG8
import org.lwjgl.opengl.GL46C.GL_RGB
import org.lwjgl.opengl.GL46C.GL_RGB32F
import org.lwjgl.opengl.GL46C.GL_RGBA
import org.lwjgl.opengl.GL46C.GL_RGBA16
import org.lwjgl.opengl.GL46C.GL_RGBA16F
import org.lwjgl.opengl.GL46C.GL_RGBA32F
import org.lwjgl.opengl.GL46C.GL_RGBA8
import org.lwjgl.opengl.GL46C.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL46C.GL_UNSIGNED_INT
import org.lwjgl.opengl.GL46C.GL_UNSIGNED_SHORT

class TargetType(
    val name: String,
    val internalFormat: Int,
    val uploadFormat: Int,
    val fillType: Int,
    val bytesPerPixel: Int,
    val channels: Int,
    val isHDR: Boolean
) {

    override fun toString(): String {
        return "TargetType($name, internalFormat=${getName(internalFormat)}, uploadFormat=${getName(uploadFormat)}, " +
                "fillType=${getName(fillType)}, bytesPerPixel=$bytesPerPixel, channels=$channels, isHDR=$isHDR)"
    }

    /**
     * The following constants are types as they are available on the target platform.
     * If a format is not available, it should be replaced (by the engine) with a matching replacement,
     * so you can use these without worrying about support.
     * */
    @Suppress("unused")
    companion object {

        // luminance counts as a compressed format, so it can't be used
        // luminance_alpha neither
        val UInt8x1 = TargetType("u8x1", GL_R8, GL_RED, GL_UNSIGNED_BYTE, 1, 1, false)
        val UInt8x2 = TargetType("u8x2", GL_RG8, GL_RG, GL_UNSIGNED_BYTE, 2, 2, false)
        val UInt8x4 = TargetType("u8x4", GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, 4, 4, false)
        val UInt8x3 = UInt8x4 // 3 isn't supported well
        val UInt8xI = listOf(UInt8x1, UInt8x2, UInt8x3, UInt8x4)

        val Float32x1 = if (!supportsF32Targets) UInt8x1
        else TargetType("f1", GL_R32F, GL_RED, GL_FLOAT, 1 * 4, 1, true)
        val Float32x2 = if (!supportsF32Targets) UInt8x2
        else TargetType("f2", GL_RG32F, GL_RG, GL_FLOAT, 2 * 4, 2, true)
        val Float32x4 = if (!supportsF32Targets) UInt8x4
        else TargetType("f4", GL_RGBA32F, GL_RGBA, GL_FLOAT, 4 * 4, 4, true)
        val Float32x3 = if (!supportsF32Targets) UInt8x3
        else if (GFXFeatures.isOpenGLES) Float32x4 // f32x3 isn't color-renderable on Web, and Android threw an error, too
        else TargetType("f3", GL_RGB32F, GL_RGB, GL_FLOAT, 3 * 4, 3, true)
        val Float32xI = listOf(Float32x1, Float32x2, Float32x3, Float32x4)

        val Float16x1 = if (!supportsF16Targets) Float32x1
        else TargetType("h1", GL_R16F, GL_RED, GL_HALF_FLOAT, 2, 1, true)
        val Float16x2 = if (!supportsF16Targets) Float32x2
        else TargetType("h2", GL_RG16F, GL_RG, GL_HALF_FLOAT, 2 * 2, 2, true)
        val Float16x4 = if (!supportsF16Targets) Float32x4
        else TargetType("h4", GL_RGBA16F, GL_RGBA, GL_HALF_FLOAT, 2 * 4, 4, true)
        val Float16x3 = if (!supportsF16Targets) Float32x3
        else Float16x4 // fp16x3 has a bad memory layout, and is not color-renderable on Web
        val Float16xI = listOf(Float16x1, Float16x2, Float16x3, Float16x4)

        val Normal12Target4 = // not working, because compressed formats are not color-renderable :/, why ever...
            Float16x4 // TargetType(GL_UNSIGNED_INT_10_10_10_2, GL_RGBA, GL_UNSIGNED_BYTE, 4, true)

        // the following formats are only available, where depth textures are supported
        val DEPTH16 = TargetType("depth16", GL_DEPTH_COMPONENT16, GL_DEPTH_COMPONENT, GL_UNSIGNED_SHORT, 2, 1, false)
        val DEPTH32 = TargetType("depth32", GL_DEPTH_COMPONENT32, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, 4, 1, false)
        val DEPTH32F = TargetType("depth32f", GL_DEPTH_COMPONENT32F, GL_DEPTH_COMPONENT, GL_FLOAT, 4, 1, false)

        // their support is quite limited, e.g. not available on Android...
        //   to do what is available is the integer format... we kind of need to support that...
        //   -> but then we wouldn't be able to support MSAA -> just stay with these non-integer formats
        val UInt16x1 = if (GFXFeatures.isOpenGLES) Float16x1
        else TargetType("u16x1", GL_R16, GL_RED, GL_UNSIGNED_SHORT, 2, 1, false)
        val UInt16x2 = if (GFXFeatures.isOpenGLES) Float16x2
        else TargetType("u16x2", GL_RG16, GL_RG, GL_UNSIGNED_SHORT, 4, 2, false)
        val UInt16x4 = if (GFXFeatures.isOpenGLES) Float16x3
        else TargetType("u16x4", GL_RGBA16, GL_RGBA, GL_UNSIGNED_SHORT, 8, 4, false)
        val UInt16xI = listOf(UInt16x1, UInt16x2, UInt16x4, UInt16x4)
    }
}