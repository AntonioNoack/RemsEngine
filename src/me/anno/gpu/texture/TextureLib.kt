package me.anno.gpu.texture

import me.anno.ui.UIColors.magenta
import me.anno.utils.Color.black
import me.anno.utils.Color.white
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toInt

/**
 * library of standard textures like white, black, transparent, striped
 * */
object TextureLib {

    private val transparent = ByteArray(4)
    private val white1 = byteArrayOf(-1, -1, -1, -1)
    private val black1 = byteArrayOf(0, 0, 0, -1)

    val invisibleTexture = IndestructibleTexture2D("invisible", 1, 1, transparent)
    val invisibleTex3d = IndestructibleTexture3D("invisible", 1, 1, 1, transparent)
    val whiteTexture = IndestructibleTexture2D("white", 1, 1, white1)
    val grayTexture = IndestructibleTexture2D("gray", 1, 1, 0x999999 or black)
    val whiteTex3d = IndestructibleTexture3D("white3d", 1, 1, 1, white1)
    val whiteTex2da = IndestructibleTexture2DArray("white2da", 1, 1, 1, white1)
    val whiteCube = IndestructibleCubemap("whiteCube", 1, white1)
    val depthTexture = IndestructibleTexture2DArray("depth", 1, 1, 1, "depth")
    val depthCube = IndestructibleCubemap("depth", 1, "depth")
    val colorShowTexture =
        IndestructibleTexture2D("color-show", 2, 2, intArrayOf(0xccffffff.toInt(), -1, -1, 0xccffffff.toInt()))
    val normalTexture = IndestructibleTexture2D("normal", 1, 1, byteArrayOf(127, 127, -1, -1))
    val gradientXTex = IndestructibleTexture2D("gradientX", 5, 1, byteArrayOf(0, 63, 127, -64, -1))
    val blackTexture = IndestructibleTexture2D("black", 1, 1, black1)
    val blackCube = IndestructibleCubemap("blackCube", 1, black1)
    val missingColors = intArrayOf(magenta, black, black, magenta)
    val missingTexture = IndestructibleTexture2D("missing", 2, 2, missingColors)

    val chess8x8Texture = IndestructibleTexture2D("chess", 8, 8, IntArray(64) {
        val y = it.shr(3)
        (it + y).hasFlag(1).toInt(white, black)
    })

    fun bindWhite(index: Int): Boolean {
        return whiteTexture.bind(index, whiteTexture.filtering, whiteTexture.clamping)
    }
}