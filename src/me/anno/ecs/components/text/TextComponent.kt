package me.anno.ecs.components.text

import me.anno.fonts.Font
import me.anno.ui.base.components.AxisAlignment

/**
 * TextTextureComponent is much cheaper to calculate than SDFTextComponent, but also a bit lower quality.
 * TextMeshComponent has the highest quality, and has medium effort to calculate.
 * The downside is triangles, which may become expensive, if there is lots of text.
 * */
interface TextComponent {

    var text: String
    var font: Font

    var alignmentX: AxisAlignment
    var alignmentY: TextAlignmentY

    /**
     * How wide the text may be, relative to font.size;
     * 0f is the default and <= 0 means unlimited.
     * */
    var relativeWidthLimit: Float

    /**
     * Maximum number of lines, typically 1 or Int.MAX_INT;
     * The default shall be Int.MAX_INT
     * */
    var maxNumLines: Int

    fun onTextOrFontChange()

    companion object {
        val defaultFont = Font("Verdana", 50)
    }
}