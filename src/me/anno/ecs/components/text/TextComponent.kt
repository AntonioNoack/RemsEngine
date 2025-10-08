package me.anno.ecs.components.text

import me.anno.ecs.annotations.Range
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

    /**
     * The default shall be center
     * */
    var blockAlignmentX: AxisAlignment

    /**
     * The default shall be center
     * */
    var blockAlignmentY: TextAlignmentY

    /**
     * todo make relative width limit the total width of the text?
     * Left/middle/right alignment of each line;
     * left = 0, middle = 0.5, right = 1;
     * The default shall be 0 (left).
     * */
    @Range(0.0, 1.0)
    var lineAlignmentX: Float

    /**
     * How wide the text may be, relative to font.size;
     * 0f is the default and <= 0 means unlimited.
     * */
    @Range(0.0, 1e38)
    var relativeWidthLimit: Float

    /**
     * Maximum number of lines, typically 1 or Int.MAX_INT;
     * The default shall be Int.MAX_INT
     * */
    @Range(1.0, 2e9)
    var maxNumLines: Int

    fun onTextOrFontChange()

    companion object {
        val defaultFont = Font("Verdana", 50)
    }
}