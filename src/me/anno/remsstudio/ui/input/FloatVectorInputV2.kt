package me.anno.remsstudio.ui.input

import me.anno.animation.Type
import me.anno.io.text.TextReader
import me.anno.remsstudio.RemsStudio
import me.anno.remsstudio.Selection
import me.anno.remsstudio.animation.AnimatedProperty
import me.anno.studio.StudioBase
import me.anno.ui.base.Visibility
import me.anno.ui.input.FloatVectorInput
import me.anno.ui.style.Style
import org.joml.*

class FloatVectorInputV2(
    title: String,
    visibilityKey: String,
    type: Type,
    private val owningProperty: AnimatedProperty<*>,
    style: Style
) : FloatVectorInput(
    title,
    visibilityKey,
    type,
    style,
    FloatInputV2(style, title, visibilityKey, type, owningProperty)
) {

    constructor(title: String, property: AnimatedProperty<*>, time: Double, style: Style) :
            this(title, title, property.type, property, style) {
        when (val value = property[time]) {
            is Vector2f -> setValue(value, false)
            is Vector3f -> setValue(value, false)
            is Vector4f -> setValue(value, false)
            is Quaternionf -> setValue(value, false)
            else -> throw RuntimeException("Type $value not yet supported!")
        }
    }

    constructor(
        title: String, visibilityKey: String, value: Vector2fc, type: Type,
        owningProperty: AnimatedProperty<*>, style: Style
    ) : this(title, visibilityKey, type, owningProperty, style) {
        setValue(value, false)
    }

    constructor(
        title: String, visibilityKey: String, value: Vector2dc, type: Type,
        owningProperty: AnimatedProperty<*>, style: Style
    ) : this(title, visibilityKey, type, owningProperty, style) {
        setValue(value, false)
    }

    constructor(
        title: String, visibilityKey: String, value: Vector3dc, type: Type,
        owningProperty: AnimatedProperty<*>, style: Style
    ) : this(title, visibilityKey, type, owningProperty, style) {
        setValue(value, false)
    }

    constructor(
        title: String, visibilityKey: String, value: Vector4dc, type: Type,
        owningProperty: AnimatedProperty<*>, style: Style
    ) : this(title, visibilityKey, type, owningProperty, style) {
        setValue(value, false)
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        pasteVector(data)
            ?: pasteScalar(data)
            ?: pasteColor(data)
            ?: pasteAnimated(data)
            ?: super.onPaste(x, y, data, type)
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val focused1 = titleView?.isInFocus == true
        if (RemsStudio.hideUnusedProperties) {
            val focused2 = focused1 || owningProperty == Selection.selectedProperty
            valueList.visibility = if (focused2) Visibility.VISIBLE else Visibility.GONE
        }
        super.onDraw(x0, y0, x1, y1)
    }

    override fun onCopyRequested(x: Float, y: Float) = owningProperty.toString()

    private fun pasteAnimated(data: String): Unit? {
        return try {
            val editorTime = RemsStudio.editorTime
            val animProperty = TextReader.read(data, true)
                .firstOrNull() as? AnimatedProperty<*>
            if (animProperty != null) {
                owningProperty.copyFrom(animProperty)
                when (val value = owningProperty[editorTime]) {
                    is Vector2f -> setValue(value, true)
                    is Vector3f -> setValue(value, true)
                    is Vector4f -> setValue(value, true)
                    is Quaternionf -> setValue(value, true)
                    else -> StudioBase.warn("Unknown pasted data type $value")
                }
            }
            Unit
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onEmpty(x: Float, y: Float) {
        onEmpty2(owningProperty.defaultValue ?: type.defaultValue)
    }

    override fun clone(): FloatVectorInputV2 {
        val clone = FloatVectorInputV2(title, visibilityKey, type, owningProperty, style)
        copy(clone)
        return clone
    }

}