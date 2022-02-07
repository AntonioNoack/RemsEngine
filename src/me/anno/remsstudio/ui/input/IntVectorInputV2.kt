package me.anno.remsstudio.ui.input

import me.anno.animation.Type
import me.anno.io.text.TextReader
import me.anno.remsstudio.RemsStudio
import me.anno.remsstudio.Selection
import me.anno.remsstudio.animation.AnimatedProperty
import me.anno.studio.StudioBase
import me.anno.ui.base.Visibility
import me.anno.ui.input.IntVectorInput
import me.anno.ui.style.Style
import org.joml.*

class IntVectorInputV2(
    style: Style, title: String, visibilityKey: String, type: Type,
    private val owningProperty: AnimatedProperty<*>
) : IntVectorInput(style, title, visibilityKey, type) {

    constructor(title: String, visibilityKey: String, property: AnimatedProperty<*>, time: Double, style: Style) :
            this(style, title, visibilityKey, property.type, property) {
        when (val value = property[time]) {
            is Vector2i -> setValue(value, false)
            is Vector3i -> setValue(value, false)
            is Vector4i -> setValue(value, false)
            else -> throw RuntimeException("Type $value not yet supported!")
        }
    }

    constructor(
        style: Style, title: String, visibilityKey: String, value: Vector2ic, type: Type,
        owningProperty: AnimatedProperty<*>
    ) : this(style, title, visibilityKey, type, owningProperty) {
        setValue(value, false)
    }

    constructor(
        style: Style, title: String, visibilityKey: String, value: Vector3ic, type: Type,
        owningProperty: AnimatedProperty<*>
    ) : this(style, title, visibilityKey, type, owningProperty) {
        setValue(value, false)
    }

    constructor(
        style: Style, title: String, visibilityKey: String, value: Vector4ic, type: Type,
        owningProperty: AnimatedProperty<*>
    ) : this(style, title, visibilityKey, type, owningProperty) {
        setValue(value, false)
    }


    fun pasteAnimated(data: String): Unit? {
        return try {
            val editorTime = RemsStudio.editorTime
            val animProperty = TextReader.read(data, true)
                .firstOrNull() as? AnimatedProperty<*>
            if (animProperty != null) {
                owningProperty.copyFrom(animProperty)
                when (val value = owningProperty[editorTime]) {
                    is Vector2i -> setValue(value, true)
                    is Vector3i -> setValue(value, true)
                    is Vector4i -> setValue(value, true)
                    else -> StudioBase.warn("Unknown pasted data type $value")
                }
            }
            Unit
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        pasteVector(data)
            ?: pasteScalar(data)
            ?: pasteColor(data)
            ?: pasteAnimated(data)
            ?: super.onPaste(x, y, data, type)
    }

    override fun onEmpty(x: Float, y: Float) {
        onEmpty2(owningProperty.defaultValue ?: type.defaultValue)
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val focused1 = titleView?.isInFocus == true
        if (RemsStudio.hideUnusedProperties) {
            val focused2 = focused1 || owningProperty == Selection.selectedProperty
            valueList.visibility = if (focused2) Visibility.VISIBLE else Visibility.GONE
        }
        super.onDraw(x0, y0, x1, y1)
    }

    override fun onCopyRequested(x: Float, y: Float): String = owningProperty.toString()

    override fun clone(): IntVectorInputV2 {
        val clone = IntVectorInputV2(style, title, visibilityKey, type, owningProperty)
        copy(clone)
        return clone
    }

    override val className: String = "IntVectorInputV2"

}