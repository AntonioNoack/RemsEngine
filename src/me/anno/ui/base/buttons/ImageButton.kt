package me.anno.ui.base.buttons

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.GFXState.renderDefault
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureCache
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.ui.Style
import me.anno.ui.base.components.Padding
import me.anno.utils.types.Floats.roundToIntOr
import kotlin.math.max

open class ImageButton(
    var path: FileReference,
    var size: Int,
    var padding: Padding,
    isSquare: Boolean = true,
    style: Style
) : Button(isSquare, style) {

    constructor(style: Style) : this(InvalidRef, 16, Padding(5), true, style)

    var guiScale = 1f

    private val icon get() = TextureCache[path, 10_000].value

    override fun calculateSize(w: Int, h: Int) {
        val icon = TextureCache[path, 10_000].waitFor()
        minW = ((size + padding.width) * guiScale).toInt()
        minH = if (icon != null && !isSquare) {
            ((icon.height * size / icon.width) * guiScale + padding.height).toInt()
        } else ((size) * guiScale + padding.height).toInt()
    }

    override fun onUpdate() {
        super.onUpdate()
        if (canBeSeen) icon // ensure it stays loaded
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawBackground(x0, y0, x1, y1)
        val icon = icon ?: return
        renderDefault {
            icon.bind(0, Filtering.LINEAR, Clamping.CLAMP)
            val scale = ((width - padding.width).toFloat() / max(icon.width, icon.height))
            val iw = (icon.width * scale).roundToIntOr()
            val ih = (icon.height * scale).roundToIntOr()
            DrawTextures.drawTexture(x + (width - iw) / 2, y + (height - ih) / 2, iw, ih, icon, tintColor, null)
        }
    }

    override fun clone(): ImageButton {
        val clone = ImageButton(style)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is ImageButton) return
        dst.path = path
        dst.padding = padding
        dst.size = size
        dst.isSquare = isSquare
    }
}