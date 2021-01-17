package me.anno.ui.base.buttons

import me.anno.cache.instances.ImageCache.getInternalTexture
import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFXx2D
import me.anno.gpu.blending.BlendDepth
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.input.Input.mouseDownX
import me.anno.input.Input.mouseDownY
import me.anno.input.Input.mouseKeysDown
import me.anno.ui.base.Panel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.style.Style
import kotlin.math.max
import kotlin.math.roundToInt

class ImageButton(
    val path: String,
    val size: Int,
    val padding: Padding,
    var square: Boolean = true,
    style: Style
) : Panel(style) {

    // private val path = //if (imageName.startsWith("textures/")) imageName else "textures/ui/$imageName"

    var guiScale = 1f

    private val icon get() = getInternalTexture(path, true, 10_000)

    init {
        add(WrapAlign.LeftTop)
    }

    override fun getVisualState(): Any? = icon
    override fun getLayoutState(): Any? = icon?.w

    override fun calculateSize(w: Int, h: Int) {
        val icon = icon ?: return
        minW = ((size + padding.width) * guiScale).toInt()
        minH = if (square) ((size) * guiScale + padding.height).toInt() else {
            ((icon.h * size / icon.w) * guiScale + padding.height).toInt()
        }
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val tint = when {
            0 in mouseKeysDown && contains(mouseDownX, mouseDownY) -> black or 0x777777
            isHovered -> black or 0xaaaaaa
            else -> -1
        }
        drawBackground()
        val icon = icon ?: return
        BlendDepth(BlendMode.DEFAULT, false) {
            icon.bind(0, GPUFiltering.LINEAR, Clamping.CLAMP)
            val scale = ((w - padding.width).toFloat() / max(icon.w, icon.h))
            val iw = (icon.w * scale).roundToInt()
            val ih = (icon.h * scale).roundToInt()
            GFXx2D.drawTexture(x + (w - iw) / 2, y + (h - ih) / 2, iw, ih, icon, tint, null)
        }
    }

}