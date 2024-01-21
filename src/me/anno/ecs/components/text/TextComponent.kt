package me.anno.ecs.components.text

import me.anno.ecs.components.mesh.ProceduralMesh
import me.anno.engine.serialization.SerializedProperty
import me.anno.fonts.Font
import me.anno.ui.base.components.AxisAlignment

/**
 * TextTextureComponent is much cheaper to calculate than SDFTextComponent, but also a bit lower quality.
 * TextMeshComponent has the highest quality, and has medium effort to calculate. The downside is triangles, which may become expensive,
 * if there is tons of text.
 * */
abstract class TextComponent(
    text: String,
    font: Font,
    alignment: AxisAlignment,
    widthLimit: Int = -1
) : ProceduralMesh() {

    constructor(text: String, font: Font, alignment: AxisAlignment) : this(text, font, alignment, -1)
    constructor() : this("Text", defaultFont, AxisAlignment.CENTER, -1)

    @SerializedProperty
    var text = text
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    @SerializedProperty
    var font = font
        set(value) {
            field = value
            invalidate()
        }

    @SerializedProperty
    var alignmentX = alignment
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    @SerializedProperty
    var widthLimit = widthLimit
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    open fun invalidate() {
        invalidateMesh()
    }

    companion object {
        val defaultFont = Font("Verdana", 20)
    }
}