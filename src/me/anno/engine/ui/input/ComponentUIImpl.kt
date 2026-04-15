package me.anno.engine.ui.input

import me.anno.ecs.annotations.Range
import me.anno.engine.inspector.IProperty
import me.anno.engine.ui.input.ComponentUI.askForReset
import me.anno.engine.ui.input.ComponentUI.fileInputRightClickOptions
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.clamp
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.groups.TitledListY
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.ColorInput
import me.anno.ui.input.FileInput
import me.anno.ui.input.FloatInput
import me.anno.ui.input.FloatVectorInput
import me.anno.ui.input.IntInput
import me.anno.ui.input.IntVectorInput
import me.anno.ui.input.NumberType
import me.anno.utils.Color
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.types.AnyToBool
import me.anno.utils.types.AnyToDouble
import me.anno.utils.types.AnyToFloat
import me.anno.utils.types.AnyToInt
import me.anno.utils.types.AnyToLong
import me.anno.utils.types.AnyToVector
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toInt
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Matrix4f
import org.joml.Planed
import org.joml.Planef
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4d
import org.joml.Vector4f
import org.joml.Vector4i
import kotlin.math.ln
import kotlin.math.pow

object ComponentUIImpl {

    val createBooleanInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                            property: IProperty<Any?>, range: Range?, style: Style ->
        BooleanInput(
            nameDesc, AnyToBool.getBool(value),
            AnyToBool.getBool(default), style
        ).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setResetListener { property.reset(this) as Boolean }
            askForReset(property) { setValue(it as Boolean, false) }
            setChangeListener {
                property.set(this, it)
            }
        }
    }

    val createByteInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                         property: IProperty<Any?>, range: Range?, style: Style ->
        val type = NumberType(AnyToInt.getInt(default, 0).toByte(), null) { it }
            .withRange(range)
        createAnyIntInput(nameDesc, visibilityKey, value, property, style, type) { it.toByte() }
    }

    val createShortInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                          property: IProperty<Any?>, range: Range?, style: Style ->
        val type = NumberType(AnyToInt.getInt(default, 0).toShort(), null) { it }
            .withRange(range)
        createAnyIntInput(nameDesc, visibilityKey, value, property, style, type) { it.toShort() }
    }

    val createIntInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                        property: IProperty<Any?>, range: Range?, style: Style ->
        if (nameDesc.englishName.endsWith("color", true)) {
            ColorInput(nameDesc, visibilityKey, (value as? Int ?: 0).toVecRGBA(), true, style).apply {
                alignmentX = AxisAlignment.FILL
                property.init(this)
                askForReset(property) { it as Int; setValue(it.toVecRGBA(), -1, false) }
                setResetListener { (property.reset(this) as Int).toVecRGBA() }
                setChangeListener { r, g, b, a, mask ->
                    property.set(this, Color.rgba(r, g, b, a), mask)
                }
            }
        } else {
            val type = NumberType.INT
                .withDefaultValue(AnyToInt.getInt(default))
                .withRange(range)
            createAnyIntInput(nameDesc, visibilityKey, value, property, style, type) { it.toInt() }
        }
    }

    val createLongInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                         property: IProperty<Any?>, range: Range?, style: Style ->
        val type = NumberType.LONG
            .withDefaultValue(AnyToLong.getLong(default))
            .withRange(range)
        IntInput(nameDesc, visibilityKey, type, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setValue(value as Long, false)
            askForReset(property) { setValue(it as Long, false) }
            setResetListener { property.reset(this).toString() }
            setChangeListener {
                property.set(this, it)
            }
        }
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

    val createFloatInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                          property: IProperty<Any?>, range: Range?, style: Style ->
        val type = NumberType.FLOAT
            .withDefaultValue(AnyToFloat.getFloat(default))
            .withRange(range)
        FloatInput(nameDesc, visibilityKey, type, style).apply {
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

    val createDoubleInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                           property: IProperty<Any?>, range: Range?, style: Style ->
        val type = NumberType.DOUBLE
            .withDefault(AnyToDouble.getDouble(default))
            .withRange(range)
        FloatInput(nameDesc, visibilityKey, type, style).apply {
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

    val createVector2fInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                             property: IProperty<Any?>, range: Range?, style: Style ->
        val type = NumberType.VEC2
            .withDefault(default as? Vector2f ?: Vector2f())
            .withRange(range)
        FloatVectorInput(nameDesc, visibilityKey, value as Vector2f, type, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setResetListener { property.reset(this) }
            askForReset(property) { setValue(it as Vector2f, false) }
            addChangeListener { x, y, _, _, mask ->
                property.set(this, Vector2f(x.toFloat(), y.toFloat()), mask)
            }
        }
    }

    val createVector3fInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                             property: IProperty<Any?>, range: Range?, style: Style ->
        val type = NumberType.VEC3
            .withDefault(default as? Vector3f ?: Vector3f())
            .withRange(range)
        FloatVectorInput(nameDesc, visibilityKey, value as Vector3f, type, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setResetListener { property.reset(this) }
            askForReset(property) { setValue(it as Vector3f, false) }
            addChangeListener { x, y, z, _, mask ->
                property.set(this, Vector3f(x.toFloat(), y.toFloat(), z.toFloat()), mask)
            }
        }
    }

    val createTilingInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                           property: IProperty<Any?>, range: Range?, style: Style ->
        val typeXY = NumberType.VEC2
            .withDefault(Vector2f(1f))
            .withRange(range)

        val typeZW = NumberType.VEC2
            .withDefault(Vector2f(0f))
            .withRange(range)

        value as Vector4f
        val ui = PanelListY(style)
        val xy = FloatVectorInput(nameDesc, visibilityKey, Vector2f(value.x, value.y), typeXY, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setResetListener { property.reset(this) }
            addChangeListener { x, y, _, _, mask ->
                val v0 = property.get() as Vector4f
                val v1 = Vector4f(x.toFloat(), y.toFloat(), v0.z, v0.w)
                property.set(this, v1, mask and 3)
            }
        }
        val zw = FloatVectorInput(nameDesc, visibilityKey, Vector2f(value.z, value.w), typeZW, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setResetListener { property.reset(this) }
            addChangeListener { z, w, _, _, mask ->
                val v0 = property.get() as Vector4f
                val v1 = Vector4f(v0.x, v0.y, z.toFloat(), w.toFloat())
                property.set(this, v1, (mask and 3).shl(2))
            }
        }
        val reset = { it: Any? ->
            it as Vector4f
            xy.setValue(Vector2f(it.x, it.y), false)
            zw.setValue(Vector2f(it.z, it.w), false)
        }
        xy.askForReset(property, reset)
        zw.askForReset(property, reset)
        ui.apply {
            add(xy)
            add(zw)
        }
    }

    val createVector4fInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                             property: IProperty<Any?>, range: Range?, style: Style ->
        val type = NumberType.VEC4
            .withDefault(default as? Vector4f ?: Vector4f())
            .withRange(range)
        FloatVectorInput(nameDesc, visibilityKey, value as Vector4f, type, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setResetListener { property.reset(this) }
            askForReset(property) { setValue(it as Vector4f, false) }
            addChangeListener { x, y, z, w, mask ->
                property.set(this, Vector4f(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat()), mask)
            }
        }
    }

    val createVector2dInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                             property: IProperty<Any?>, range: Range?, style: Style ->
        val type = NumberType.VEC2D
            .withDefault(default as? Vector2d ?: Vector2d())
            .withRange(range)
        FloatVectorInput(nameDesc, visibilityKey, value as Vector2d, type, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setResetListener { property.reset(this) }
            askForReset(property) { setValue(it as Vector2d, false) }
            addChangeListener { x, y, _, _, mask ->
                property.set(this, Vector2d(x, y), mask)
            }
        }
    }

    val createVector3dInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                             property: IProperty<Any?>, range: Range?, style: Style ->
        val type = NumberType.VEC3D
            .withDefault(default as? Vector3d ?: Vector3d())
            .withRange(range)
        FloatVectorInput(nameDesc, visibilityKey, value as Vector3d, type, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setResetListener { property.reset(this) }
            askForReset(property) { setValue(it as Vector3d, false) }
            addChangeListener { x, y, z, _, mask ->
                property.set(this, Vector3d(x, y, z), mask)
            }
        }
    }

    val createVector4dInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                             property: IProperty<Any?>, range: Range?, style: Style ->
        val type = NumberType.VEC4D
            .withDefault(default as? Vector4d ?: Vector4d())
            .withRange(range)
        FloatVectorInput(nameDesc, visibilityKey, value as Vector4d, type, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setResetListener { property.reset(this) }
            askForReset(property) { setValue(it as Vector4d, -1, false) }
            addChangeListener { x, y, z, w, mask ->
                property.set(this, Vector4d(x, y, z, w), mask)
            }
        }
    }

    val createPlanefInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                           property: IProperty<Any?>, range: Range?, style: Style ->
        val type = NumberType.PLANE4.withDefault(default as? Planef ?: Planef())
        FloatVectorInput(nameDesc, visibilityKey, value as Planef, type, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setResetListener { property.reset(this) }
            askForReset(property) { setValue(it as Planef, false) }
            addChangeListener { x, y, z, w, mask ->
                property.set(this, Planef(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat()), mask)
            }
        }
    }

    val createPlanedInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                           property: IProperty<Any?>, range: Range?, style: Style ->
        val type = NumberType.PLANE4D
            .withDefault(default as? Planed ?: Planed())
            .withRange(range)
        FloatVectorInput(nameDesc, visibilityKey, value as Planed, type, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setResetListener { property.reset(this) }
            askForReset(property) { setValue(it as Planed, false) }
            addChangeListener { x, y, z, w, mask ->
                property.set(this, Planed(x, y, z, w), mask)
            }
        }
    }

    val createVector2iInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                             property: IProperty<Any?>, range: Range?, style: Style ->
        val type = NumberType(default as? Vector2i ?: Vector2i(), 2)
            .withRange(range)
        IntVectorInput(nameDesc, visibilityKey, value as Vector2i, type, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setResetListener { property.reset(this) }
            askForReset(property) { setValue(it as Vector2i, false) }
            addChangeListener { x, y, _, _, mask ->
                property.set(this, Vector2i(x.toInt(), y.toInt()), mask)
            }
        }
    }

    val createVector3iInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                             property: IProperty<Any?>, range: Range?, style: Style ->
        val type = NumberType(default as? Vector3i ?: Vector3i(), 3)
            .withRange(range)
        IntVectorInput(nameDesc, visibilityKey, value as Vector3i, type, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setResetListener { property.reset(this) }
            askForReset(property) { setValue(it as Vector3i, false) }
            addChangeListener { x, y, z, _, mask ->
                property.set(this, Vector3i(x.toInt(), y.toInt(), z.toInt()), mask)
            }
        }
    }

    val createVector4iInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                             property: IProperty<Any?>, range: Range?, style: Style ->
        val type = NumberType(default as? Vector4i ?: Vector4i(), 4)
            .withRange(range)
        IntVectorInput(nameDesc, visibilityKey, value as Vector4i, type, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setResetListener { property.reset(this) }
            askForReset(property) { setValue(it as Vector4i, -1, false) }
            addChangeListener { x, y, z, w, mask ->
                property.set(this, Vector4i(x.toInt(), y.toInt(), z.toInt(), w.toInt()), mask)
            }
        }
    }

    val createAABBfInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                          property: IProperty<Any?>, range: Range?, style: Style ->
        value as AABBf
        default as AABBf
        val typeMin = NumberType.VEC3
            .withDefault(default.getMin())
            .withRange(range)
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
        val typeMax = NumberType.VEC3D
            .withDefault(default.getMax())
            .withRange(range)
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
        panel
    }

    val createAABBdInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                          property: IProperty<Any?>, range: Range?, style: Style ->
        value as AABBd
        default as AABBd
        val typeMin = NumberType.VEC3D
            .withDefault(default.getMin())
            .withRange(range)
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
        val typeMax = NumberType.VEC3D
            .withDefault(default.getMax())
            .withRange(range)
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
        panel
    }

    val createByteArrayInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                              property: IProperty<Any?>, range: Range?, style: Style ->
        createNumberArrayInput(
            nameDesc, visibilityKey, property, style,
            "Byte", (value as? ByteArray)?.asList() ?: emptyList()
        ) { values -> ByteArray(values.size) { values[it] as Byte } }
    }

    val createShortArrayInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                               property: IProperty<Any?>, range: Range?, style: Style ->
        createNumberArrayInput(
            nameDesc, visibilityKey, property, style,
            "Short", (value as? ShortArray)?.asList() ?: emptyList()
        ) { values -> ShortArray(values.size) { values[it] as Short } }
    }

    val createIntArrayInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                             property: IProperty<Any?>, range: Range?, style: Style ->
        createNumberArrayInput(
            nameDesc, visibilityKey, property, style,
            "Int", (value as? IntArray)?.asList() ?: emptyList()
        ) { values -> IntArray(values.size) { values[it] as Int } }
    }

    val createLongArrayInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                              property: IProperty<Any?>, range: Range?, style: Style ->
        createNumberArrayInput(
            nameDesc, visibilityKey, property, style,
            "Long", (value as? LongArray)?.asList() ?: emptyList()
        ) { values -> LongArray(values.size) { values[it] as Long } }
    }

    val createFloatArrayInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                               property: IProperty<Any?>, range: Range?, style: Style ->
        createNumberArrayInput(
            nameDesc, visibilityKey, property, style,
            "Float", (value as? FloatArray)?.asList() ?: emptyList()
        ) { values -> FloatArray(values.size) { values[it] as Float } }
    }

    val createDoubleArrayInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                                property: IProperty<Any?>, range: Range?, style: Style ->
        createNumberArrayInput(
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

    val createQuaternionfInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                                property: IProperty<Any?>, range: Range?, style: Style ->
        value as Quaternionf
        val type = NumberType.ROT_YXZ
            .withDefault((default as Quaternionf).toEulerAnglesDegrees())
            .withRange(range)
        FloatVectorInput(nameDesc, visibilityKey, value.toEulerAnglesDegrees(), type, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            askForReset(property) { setValue((it as Quaternionf).toEulerAnglesDegrees(), false) }
            setResetListener { property.reset(this) }
            addChangeListener { x, y, z, _, mask ->
                val q = Vector3f(x.toFloat(), y.toFloat(), z.toFloat()).toQuaternionDegrees()
                property.set(this, q, mask)
            }
        }
    }

    val createQuaterniondInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                                property: IProperty<Any?>, range: Range?, style: Style ->
        value as Quaterniond
        val type = NumberType.ROT_YXZ
            .withDefault((default as Quaterniond).toEulerAnglesDegrees())
            .withRange(range)
        FloatVectorInput(nameDesc, visibilityKey, value.toEulerAnglesDegrees(), type, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            askForReset(property) { setValue((it as Quaterniond).toEulerAnglesDegrees(), false) }
            setResetListener { property.reset(this) }
            addChangeListener { x, y, z, _, mask ->
                val q = Vector3d(x, y, z).toQuaternionDegrees()
                property.set(this, q, mask)
            }
        }
    }

    val createColor3Input = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                           property: IProperty<Any?>, range: Range?, style: Style ->
        val value0 = AnyToVector.getVector4f(value)
        ColorInput(nameDesc, visibilityKey, value0, false, style).apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            setResetListener { AnyToVector.getVector4f(property.reset(this)) }
            askForReset(property) {
                setValue(AnyToVector.getVector4f(value), -1, false)
            }
            setChangeListener { r, g, b, _, mask ->
                property.set(this, Vector3f(r, g, b), mask)
            }
        }
    }

    val createColor3HDRInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                              property: IProperty<Any?>, range: Range?, style: Style ->
        value as Vector3f
        val maxPower = 1e3f

        // logarithmic brightness scale
        fun b2l(b: Vector3f): Vector4f {
            var length = b.length()
            if (length == 0f) length = 1f
            val power = clamp(ln(length) / ln(maxPower) * 0.5f + 0.5f, 0f, 1f)
            val scale = maxPower.pow(power * 2f - 1f)
            return Vector4f(b.x / scale, b.y / scale, b.z / scale, power)
        }

        fun l2b(l: Vector4f): Vector3f {
            val power = maxPower.pow(l.w * 2f - 1f)
            return Vector3f(l.x, l.y, l.z).mul(power)
        }
        object : ColorInput(nameDesc, visibilityKey, b2l(value), true, style) {
            override fun onCopyRequested(x: Float, y: Float): String {
                val v = l2b(this.value)
                return "vec3(${v.x},${v.y},${v.z})"
            }
        }.apply {
            alignmentX = AxisAlignment.FILL
            property.init(this)
            // todo brightness should have different background than alpha
            setResetListener { b2l(property.reset(this) as Vector3f) }
            askForReset(property) { setValue(b2l(it as Vector3f), -1, false) }
            setChangeListener { r, g, b, a, mask ->
                val rgbMask = mask.and(7) or mask.hasFlag(8).toInt(7)
                property.set(this, l2b(Vector4f(r, g, b, a)), rgbMask)
            }
        }
    }

    val createColor4Input = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                           property: IProperty<Any?>, range: Range?, style: Style ->
        value as Vector4f
        // todo hdr colors per color amplitude
        ColorInput(nameDesc, visibilityKey, value, true, style)
            .apply {
                alignmentX = AxisAlignment.FILL
                property.init(this)
                setResetListener { property.reset(this) as Vector4f }
                askForReset(property) { setValue(it as Vector4f, -1, false) }
                setChangeListener { r, g, b, a, mask ->
                    property.set(this, Vector4f(r, g, b, a), mask)
                }
            }
    }

    val createMatrix4fInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                             property: IProperty<Any?>, range: Range?, style: Style ->
        value as Matrix4f
        default as Matrix4f
        val panel = TitledListY(nameDesc, visibilityKey, style)
        property.init(panel)
        // todo special types
        // todo operations: translate, rotate, scale
        for (i in 0 until 4) {
            panel.add(
                FloatVectorInput(
                    NameDesc.EMPTY,
                    visibilityKey,
                    value.getRow(i, Vector4f()),
                    NumberType.VEC4,
                    style
                )
                    .addChangeListener { x, y, z, w, _ ->// todo correct change listener
                        value.setRow(i, Vector4f(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat()))
                    }
            )
        }
        // todo reset listener
        panel.askForReset(property) {
            it as Matrix4f
            for (i in 0 until 4) {
                (panel.children[i + 1] as FloatVectorInput)
                    .setValue(it.getRow(i, Vector4f()), false)
            }
        }
        panel
    }

    val createFileReferenceInput = InputCreator { nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
                                                  property: IProperty<Any?>, range: Range?, style: Style ->
        value as FileReference
        // todo if resource is located here, and we support the type, allow editing here directly
        //  (Materials), #fileInputRightClickOptions
        FileInput(nameDesc, style, value, fileInputRightClickOptions).apply {
            property.init(this)
            setResetListener {
                property.reset(this) as? FileReference
                    ?: InvalidRef
            }
            addChangeListener {
                property.set(this, it)
            }
        }
    }
}