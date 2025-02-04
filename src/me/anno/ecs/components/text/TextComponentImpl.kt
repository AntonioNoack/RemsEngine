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
    alignmentX: AxisAlignment,
    alignmentY: TextAlignmentY,
    widthLimit: Float = -1f
) : ProceduralMesh(), TextComponent {

    constructor() : this("Text", defaultFont, AxisAlignment.CENTER)
    constructor(text: String, font: Font, alignment: AxisAlignment) : this(
        text, font, alignment, TextAlignmentY.CENTER, -1f
    )

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
    override var alignmentX = alignmentX
        set(value) {
            if (field != value) {
                field = value
                onTextOrFontChange()
            }
        }

    @SerializedProperty
    override var alignmentY = alignmentY
        set(value) {
            if (field != value) {
                field = value
                onTextOrFontChange()
            }
        }

    @SerializedProperty
    override var widthLimit = widthLimit
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
        dst.alignmentX = alignmentX
        dst.alignmentY = alignmentY
        dst.widthLimit = widthLimit
    }
}