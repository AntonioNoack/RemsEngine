package me.anno.ecs.components.text

import me.anno.ecs.components.mesh.ProceduralMesh
import me.anno.ecs.components.text.TextComponent.Companion.defaultFont
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.SerializedProperty
import me.anno.fonts.Font
import me.anno.ui.base.components.AxisAlignment

/**
 * TextTextureComponent is much cheaper to calculate than SDFTextComponent, but also a bit lower quality.
 * TextMeshComponent has the highest quality, and has medium effort to calculate.
 * The downside is triangles, which may become expensive, if there is lots of text.
 * */
abstract class TextComponentImpl(
    text: String, font: Font,
    blockAlignmentX: AxisAlignment,
    blockAlignmentY: TextAlignmentY,
    lineAlignmentX: Float,
    relativeWidthLimit: Float
) : ProceduralMesh(), TextComponent {

    constructor() : this("Text", defaultFont, AxisAlignment.CENTER)
    constructor(text: String, font: Font, blockAlignment: AxisAlignment) :
            this(text, font, blockAlignment, TextAlignmentY.CENTER)

    constructor(text: String, font: Font, blockAlignment: AxisAlignment, blockAlignmentY: TextAlignmentY) :
            this(text, font, blockAlignment, blockAlignmentY, 0f, 0f)

    @SerializedProperty
    override var text = text
        set(value) {
            if (field != value) {
                field = value
                onTextOrFontChange()
            }
        }

    @SerializedProperty
    override var font = font
        set(value) {
            field = value
            onTextOrFontChange()
        }

    @SerializedProperty
    override var lineAlignmentX: Float = lineAlignmentX
        set(value) {
            if (field != value) {
                field = value
                onTextOrFontChange()
            }
        }

    @SerializedProperty
    override var blockAlignmentX: AxisAlignment = blockAlignmentX
        set(value) {
            if (field != value) {
                field = value
                onTextOrFontChange()
            }
        }

    @SerializedProperty
    override var blockAlignmentY: TextAlignmentY = blockAlignmentY
        set(value) {
            if (field != value) {
                field = value
                onTextOrFontChange()
            }
        }

    @SerializedProperty
    override var relativeWidthLimit = relativeWidthLimit
        set(value) {
            if (field != value) {
                field = value
                onTextOrFontChange()
            }
        }

    @SerializedProperty
    override var maxNumLines: Int = Int.MAX_VALUE
        set(value) {
            if (field != value) {
                field = value
                onTextOrFontChange()
            }
        }

    override fun onTextOrFontChange() {
        invalidateMesh()
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is TextComponent) return
        dst.text = text
        dst.font = font
        dst.blockAlignmentX = this@TextComponentImpl.blockAlignmentX
        dst.blockAlignmentY = this@TextComponentImpl.blockAlignmentY
        dst.relativeWidthLimit = this@TextComponentImpl.relativeWidthLimit
        dst.maxNumLines = maxNumLines
    }
}