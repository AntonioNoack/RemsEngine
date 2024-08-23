package me.anno.engine.ui.input

import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Range.Companion.maxByte
import me.anno.ecs.annotations.Range.Companion.maxDouble
import me.anno.ecs.annotations.Range.Companion.maxFloat
import me.anno.ecs.annotations.Range.Companion.maxInt
import me.anno.ecs.annotations.Range.Companion.maxShort
import me.anno.ecs.annotations.Range.Companion.minByte
import me.anno.ecs.annotations.Range.Companion.minDouble
import me.anno.ecs.annotations.Range.Companion.minFloat
import me.anno.ecs.annotations.Range.Companion.minInt
import me.anno.ecs.annotations.Range.Companion.minShort
import me.anno.engine.inspector.IProperty
import me.anno.engine.ui.input.ComponentUI.askForReset
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.clamp
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.TitledListY
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.FloatInput
import me.anno.ui.input.FloatVectorInput
import me.anno.ui.input.IntInput
import me.anno.ui.input.NumberType
import me.anno.utils.types.AnyToDouble
import me.anno.utils.types.AnyToFloat
import me.anno.utils.types.AnyToLong
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4d
import org.joml.Vector4f

object ComponentUIImpl {

    fun createBooleanInput(
        nameDesc: NameDesc, value: Any?, default: Any?,
        property: IProperty<Any?>, style: Style,
    ): Panel {
        return BooleanInput(nameDesc, value as Boolean, default as? Boolean ?: false, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setResetListener { property.reset(this) as Boolean }
            askForReset(property) { setValue(it as Boolean, false) }
            setChangeListener {
                property.set(this, it)
            }
        }
    }

    fun createByteInput(
        nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
        property: IProperty<Any?>, range: Range?, style: Style,
    ): Panel {
        val type = NumberType(default as Byte,
            { clamp(AnyToLong.getLong(it, 0), range.minByte().toLong(), range.maxByte().toLong()).toByte() },
            { it })
        return createAnyIntInput(nameDesc, visibilityKey, value, property, style, type) { it.toByte() }
    }

    fun createShortInput(
        nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
        property: IProperty<Any?>, range: Range?, style: Style,
    ): Panel {
        val type = NumberType(default as Short,
            { clamp(AnyToLong.getLong(it, 0), range.minShort().toLong(), range.maxShort().toLong()).toShort() },
            { it })
        return createAnyIntInput(nameDesc, visibilityKey, value, property, style, type) { it.toShort() }
    }

    fun createIntInput(
        nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
        property: IProperty<Any?>, range: Range?, style: Style,
    ): Panel {
        val type = NumberType(default as Int,
            { clamp(AnyToLong.getLong(it, 0), range.minInt().toLong(), range.maxInt().toLong()).toInt() },
            { it })
        return createAnyIntInput(nameDesc, visibilityKey, value, property, style, type) { it.toInt() }
    }

    private fun createAnyIntInput(
        nameDesc: NameDesc, visibilityKey: String, value: Any?,
        property: IProperty<Any?>, style: Style, type: NumberType,
        toType: (Long) -> Any
    ): Panel {
        return IntInput(nameDesc, visibilityKey, type, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setValue(AnyToLong.getLong(value, 0), false)
            askForReset(property) { setValue(AnyToLong.getLong(it, 0), false) }
            setResetListener { property.reset(this).toString() }
            setChangeListener {
                property.set(this, toType(it))
            }
        }
    }

    fun createFloatInput(
        nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
        property: IProperty<Any?>, range: Range?, style: Style,
    ): Panel {
        val type = NumberType(
            AnyToFloat.getFloat(default, 0f),
            { clamp(AnyToFloat.getFloat(it, 0f), range.minFloat(), range.maxFloat()).toDouble() }, { it })
        return FloatInput(nameDesc, visibilityKey, type, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setValue(value as Float, false)
            setResetListener { property.reset(this).toString() }
            askForReset(property) { setValue(it as Float, false) }
            setChangeListener {
                property.set(this, it.toFloat())
            }
        }
    }

    fun createDoubleInput(
        nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
        property: IProperty<Any?>, range: Range?, style: Style,
    ): Panel {
        val type = NumberType(
            AnyToDouble.getDouble(default, 0.0),
            { clamp(AnyToDouble.getDouble(it, 0.0), range.minDouble(), range.maxDouble()) }, { it })
        return FloatInput(nameDesc, visibilityKey, type, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setValue(value as Double, false)
            setResetListener { property.reset(this).toString() }
            askForReset(property) { setValue(it as Double, false) }
            setChangeListener {
                property.set(this, it)
            }
        }
    }

    fun createVector2fInput(
        nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
        property: IProperty<Any?>, style: Style,
    ): Panel {
        val type = NumberType.VEC2.withDefault(default as? Vector2f ?: Vector2f())
        return FloatVectorInput(nameDesc, visibilityKey, value as Vector2f, type, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setResetListener { property.reset(this) }
            askForReset(property) { setValue(it as Vector2f, false) }
            addChangeListener { x, y, _, _, mask ->
                property.set(this, Vector2f(x.toFloat(), y.toFloat()), mask)
            }
        }
    }

    fun createVector3fInput(
        nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
        property: IProperty<Any?>, style: Style,
    ): Panel {
        val type = NumberType.VEC3.withDefault(default as? Vector3f ?: Vector3f())
        return FloatVectorInput(nameDesc, visibilityKey, value as Vector3f, type, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setResetListener { property.reset(this) }
            askForReset(property) { setValue(it as Vector3f, false) }
            addChangeListener { x, y, z, _, mask ->
                property.set(this, Vector3f(x.toFloat(), y.toFloat(), z.toFloat()), mask)
            }
        }
    }

    fun createVector4fInput(
        nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
        property: IProperty<Any?>, style: Style,
    ): Panel {
        val type = NumberType.VEC4.withDefault(default as? Vector4f ?: Vector4f())
        return FloatVectorInput(nameDesc, visibilityKey, value as Vector4f, type, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setResetListener { property.reset(this) }
            askForReset(property) { setValue(it as Vector4f, false) }
            addChangeListener { x, y, z, w, mask ->
                property.set(this, Vector4f(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat()), mask)
            }
        }
    }

    fun createVector2dInput(
        nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
        property: IProperty<Any?>, style: Style,
    ): Panel {
        val type = NumberType.VEC2D.withDefault(default as? Vector2d ?: Vector2d())
        return FloatVectorInput(nameDesc, visibilityKey, value as Vector2d, type, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setResetListener { property.reset(this) }
            askForReset(property) { setValue(it as Vector2d, false) }
            addChangeListener { x, y, _, _, mask ->
                property.set(this, Vector2d(x, y), mask)
            }
        }
    }

    fun createVector3dInput(
        nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
        property: IProperty<Any?>, style: Style,
    ): Panel {
        val type = NumberType.VEC3D.withDefault(default as? Vector3d ?: Vector3d())
        return FloatVectorInput(nameDesc, visibilityKey, value as Vector3d, type, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setResetListener { property.reset(this) }
            askForReset(property) { setValue(it as Vector3d, false) }
            addChangeListener { x, y, z, _, mask ->
                property.set(this, Vector3d(x, y, z), mask)
            }
        }
    }

    fun createVector4dInput(
        nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
        property: IProperty<Any?>, style: Style,
    ): Panel {
        val type = NumberType.VEC4D.withDefault(default as? Vector4d ?: Vector4d())
        return FloatVectorInput(nameDesc, visibilityKey, value as Vector4d, type, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setResetListener { property.reset(this) }
            askForReset(property) { setValue(it as Vector4d, -1, false) }
            addChangeListener { x, y, z, w, mask ->
                property.set(this, Vector4d(x, y, z, w), mask)
            }
        }
    }

    fun createAABBfInput(
        nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
        property: IProperty<Any?>, style: Style,
    ): Panel {
        value as AABBf
        default as AABBf
        val typeMin = NumberType.VEC3.withDefault(default.getMin())
        val panel = TitledListY(nameDesc, visibilityKey, style)
        panel.add(FloatVectorInput(NameDesc.EMPTY, visibilityKey, value.getMin(), typeMin, style).apply {
            property.init(this)
            setResetListener { property.reset(this) }
            askForReset(property) { setValue((it as AABBf).getMin(), false) }
            addChangeListener { x, y, z, _, mask ->
                value.setMin(x.toFloat(), y.toFloat(), z.toFloat())
                property.set(this, value, mask.and(7))
            }
        })
        val typeMax = NumberType.VEC3D.withDefault(default.getMax())
        panel.add(FloatVectorInput(NameDesc.EMPTY, visibilityKey, value.getMax(), typeMax, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setResetListener { property.reset(this) }
            askForReset(property) { setValue((it as AABBf).getMax(), false) }
            addChangeListener { x, y, z, _, mask ->
                value.setMax(x.toFloat(), y.toFloat(), z.toFloat())
                property.set(this, value, mask.and(7).shl(3))
            }
        })
        return panel
    }

    fun createAABBdInput(
        nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
        property: IProperty<Any?>, style: Style,
    ): Panel {
        value as AABBd
        default as AABBd
        val typeMin = NumberType.VEC3D.withDefault(default.getMin())
        val panel = TitledListY(nameDesc, visibilityKey, style)
        panel.add(FloatVectorInput(NameDesc.EMPTY, visibilityKey, value.getMin(), typeMin, style).apply {
            property.init(this)
            setResetListener { property.reset(this) }
            askForReset(property) { setValue((it as AABBd).getMin(), false) }
            addChangeListener { x, y, z, _, mask ->
                value.setMin(x, y, z)
                property.set(this, value, mask.and(3))
            }
        })
        val typeMax = NumberType.VEC3D.withDefault(default.getMax())
        panel.add(FloatVectorInput(NameDesc.EMPTY, visibilityKey, value.getMax(), typeMax, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setResetListener { property.reset(this) }
            askForReset(property) { setValue((it as AABBd).getMax(), false) }
            addChangeListener { x, y, z, _, mask ->
                value.setMax(x, y, z)
                property.set(this, value, mask.and(7).shl(3))
            }
        })
        return panel
    }

    fun createByteArrayInput(
        nameDesc: NameDesc, visibilityKey: String, value: Any?,
        property: IProperty<Any?>, style: Style,
    ): Panel {
        return createNumberArrayInput(
            nameDesc, visibilityKey, property, style,
            "Byte", (value as? ByteArray)?.asList() ?: emptyList()
        ) { values -> ByteArray(values.size) { values[it] as Byte } }
    }

    fun createShortArrayInput(
        nameDesc: NameDesc, visibilityKey: String, value: Any?,
        property: IProperty<Any?>, style: Style,
    ): Panel {
        return createNumberArrayInput(
            nameDesc, visibilityKey, property, style,
            "Short", (value as? ShortArray)?.asList() ?: emptyList()
        ) { values -> ShortArray(values.size) { values[it] as Short } }
    }

    fun createIntArrayInput(
        nameDesc: NameDesc, visibilityKey: String, value: Any?,
        property: IProperty<Any?>, style: Style,
    ): Panel {
        return createNumberArrayInput(
            nameDesc, visibilityKey, property, style,
            "Int", (value as? IntArray)?.asList() ?: emptyList()
        ) { values -> IntArray(values.size) { values[it] as Int } }
    }

    fun createLongArrayInput(
        nameDesc: NameDesc, visibilityKey: String, value: Any?,
        property: IProperty<Any?>, style: Style,
    ): Panel {
        return createNumberArrayInput(
            nameDesc, visibilityKey, property, style,
            "Long", (value as? LongArray)?.asList() ?: emptyList()
        ) { values -> LongArray(values.size) { values[it] as Long } }
    }

    fun createFloatArrayInput(
        nameDesc: NameDesc, visibilityKey: String, value: Any?,
        property: IProperty<Any?>, style: Style,
    ): Panel {
        return createNumberArrayInput(
            nameDesc, visibilityKey, property, style,
            "Float", (value as? FloatArray)?.asList() ?: emptyList()
        ) { values -> FloatArray(values.size) { values[it] as Float } }
    }

    fun createDoubleArrayInput(
        nameDesc: NameDesc, visibilityKey: String, value: Any?,
        property: IProperty<Any?>, style: Style,
    ): Panel {
        return createNumberArrayInput(
            nameDesc, visibilityKey, property, style,
            "Double", (value as? DoubleArray)?.asList() ?: emptyList()
        ) { values -> DoubleArray(values.size) { values[it] as Double } }
    }

    fun createNumberArrayInput(
        nameDesc: NameDesc, visibilityKey: String,
        property: IProperty<Any?>, style: Style,
        childType: String, valueAsList: List<Any?>, valuesToArray: (List<Any?>) -> Any,
    ): Panel {
        return object : AnyArrayPanel(nameDesc, visibilityKey, childType, style) {
            override fun onChange() {
                property.set(this, valuesToArray(values))
            }
        }.apply {
            property.init(this)
            setValues(valueAsList)
        }
    }
}