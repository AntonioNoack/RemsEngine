package me.anno.engine.ui

import me.anno.animation.Type
import me.anno.ecs.Component
import me.anno.ecs.annotations.ListType
import me.anno.ecs.annotations.MapType
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Range.Companion.maxByte
import me.anno.ecs.annotations.Range.Companion.maxDouble
import me.anno.ecs.annotations.Range.Companion.maxFloat
import me.anno.ecs.annotations.Range.Companion.maxInt
import me.anno.ecs.annotations.Range.Companion.maxLong
import me.anno.ecs.annotations.Range.Companion.maxShort
import me.anno.ecs.annotations.Range.Companion.minByte
import me.anno.ecs.annotations.Range.Companion.minDouble
import me.anno.ecs.annotations.Range.Companion.minFloat
import me.anno.ecs.annotations.Range.Companion.minInt
import me.anno.ecs.annotations.Range.Companion.minLong
import me.anno.ecs.annotations.Range.Companion.minShort
import me.anno.ecs.prefab.Change0
import me.anno.ecs.prefab.PrefabComponent1
import me.anno.engine.IProperty
import me.anno.io.ISaveable
import me.anno.io.serialization.CachedProperty
import me.anno.language.translation.NameDesc
import me.anno.objects.inspectable.Inspectable
import me.anno.ui.base.Panel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.groups.TitledListY
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.SettingCategory
import me.anno.ui.input.*
import me.anno.ui.style.Style
import me.anno.utils.Maths
import me.anno.utils.strings.StringHelper
import me.anno.utils.strings.StringHelper.titlecase
import me.anno.utils.types.Quaternions.toEulerAnglesDegrees
import me.anno.utils.types.Quaternions.toQuaternionDegrees
import org.apache.logging.log4j.LogManager
import org.joml.*
import java.io.Serializable

object ComponentUI {

    private val LOGGER = LogManager.getLogger(ComponentUI::class)

    /**
     * a massive function, which provides editors for all types of variables
     * */
    fun createUI(name: String, property: CachedProperty, c: Component, style: Style): Panel? {
        if (property.hideInInspector) return null

        // todo mesh input, skeleton selection, animation selection, ...

        return createUI2(name, name, object : IProperty<Any?> {

            override fun getDefault(): Any? {
                return c.getDefaultValue(name)
            }

            override fun set(value: Any?) {
                property[c] = value
                c.changedPropertiesInInstance.add(name)
            }

            override fun get(): Any? {
                return property[c]
            }

            override fun reset(): Any? {
                c.changedPropertiesInInstance.remove(name)
                val value = getDefault()
                property[c] = value
                return value
            }

            override val annotations: List<Annotation>
                get() = property.annotations

        }, property.range, style)
    }

    fun createUI(name: String, property: CachedProperty, c: PrefabComponent1, cSample: Component, style: Style): Panel? {

        if (property.hideInInspector) return null

        // todo mesh input, skeleton selection, animation selection, ...

        return createUI2(name, name, object : IProperty<Any?> {

            override fun getDefault(): Any? {
                // this may be difficult
                return cSample.getDefaultValue(name)
            }

            override fun set(value: Any?) {
                c.changes[name] = Change0(value)
            }

            override fun get(): Any? {
                // use the changes
                return c.changes[name]?.value ?: getDefault()
            }

            override fun reset(): Any? {
                c.changes.remove(name)
                return getDefault()
            }

            override val annotations: List<Annotation>
                get() = property.annotations

        }, property.range, style)
    }

    fun createUI2(
        name: String?,
        visibilityKey: String,
        property: IProperty<Any?>,
        range: Range?,
        style: Style
    ): Panel? {

        val title = if (name == null) "" else StringHelper.splitCamelCase(name.titlecase())

        val type0 = property.annotations.filterIsInstance<me.anno.ecs.annotations.Type>().firstOrNull()?.type
        val type1 = type0 ?: when (val value = property.get()) {// todo better, not sample-depending check for the type

            // native types
            is Boolean, is Byte, is Short,
            is Int, is Long, is Float, is Double,
            is String,
                // float vectors
            is Vector2f, is Vector3f, is Vector4f,
            is Vector2d, is Vector3d, is Vector4d,
                // int vectors
            is Vector2i, is Vector3i, is Vector4i,
                // rotations
            is Quaternionf,
            is Quaterniond,
                // matrices
            is Matrix2f, is Matrix2d,
            is Matrix3x2f, is Matrix3x2d,
            is Matrix3f, is Matrix3d,
            is Matrix4x3f, is Matrix4x3d,
            is Matrix4f, is Matrix4d,
                // native arrays
            is ByteArray, is ShortArray,
            is CharArray,
            is IntArray, is LongArray,
            is FloatArray, is DoubleArray,
            -> value.javaClass.simpleName

            is Inspectable -> "Inspectable"

            // todo edit native arrays (byte/short/int/float/...) as images


            // collections and maps
            is Array<*> -> {
                val arrayType = getArrayType(property, value.iterator(), name) ?: return null
                return object : AnyArrayPanel(title, visibilityKey, arrayType, style) {
                    override fun onChange() {
                        property.set(content.toTypedArray())
                    }
                }.apply { setValues(value.toList()) }
            }

            is List<*> -> {
                val arrayType = getArrayType(property, value.iterator(), name) ?: return null
                return object : AnyArrayPanel(title, visibilityKey, arrayType, style) {
                    override fun onChange() {
                        property.set(content)
                    }
                }.apply { setValues(value.toList()) }
            }

            is Set<*> -> {
                val arrayType = getArrayType(property, value.iterator(), name) ?: return null
                return object : AnyArrayPanel(title, visibilityKey, arrayType, style) {
                    override fun onChange() {
                        property.set(content.toSet())
                    }
                }.apply { setValues(value.toList()) }
            }

            is Map<*, *> -> {
                val annotation = property.annotations.filterIsInstance<MapType>().firstOrNull()
                val keyType = annotation?.keyType ?: getType(value.keys.iterator(), name) ?: return null
                val valueType = annotation?.valueType ?: getType(value.values.iterator(), name) ?: return null
                return object : AnyMapPanel(title, visibilityKey, keyType, valueType, style) {
                    override fun onChange() {
                        property.set(content.associate { it.first to it.first }.toMutableMap())
                    }
                }.apply {
                    setValues(value.map { AnyMapPanel.MutablePair(it.key, it.value) })
                }
            }

            // todo sort list/map by key or property of the users choice
            // todo array & list inputs of any kind
            // todo map inputs: a list of pairs of key & value
            // todo tables for structs?
            // todo show changed values with bold font
            // todo ISaveable-s


            // is ISaveable -> { list all child properties }
            else -> {
                return TextPanel("?? $title", style)
            }
        }

        return createUI3(name, visibilityKey, property, type1, range, style)

    }

    fun warnDetectionIssue(name: String?) {
        LOGGER.warn("Could not detect type of $name")
    }

    fun createUI3(
        name: String?,
        visibilityKey: String,
        property: IProperty<Any?>,
        type0: String,
        range: Range?,
        style: Style
    ): Panel {

        val title = if (name == null) "" else StringHelper.splitCamelCase(name.titlecase())
        val ttt = "" // we could use annotations for that :)
        val value = property.get()

        when (type0) {
            // native types
            "Bool", "Boolean" -> return BooleanInput(title, ttt, value as Boolean, false, style)
                // reset listener / option to reset it
                .apply {
                    setResetListener { property.reset() as Boolean }
                    askForReset(property) { setValue(it as Boolean, false) }
                    setChangeListener {
                        property.set(it)
                    }
                }
            // todo char
            // todo unsigned types
            "Byte" -> {
                val type = Type(property.getDefault() as Byte,
                    { Maths.clamp(it.toLong(), range.minByte().toLong(), range.maxByte().toLong()).toByte() }, { it })
                return IntInput(style, title, visibilityKey, type, null, 0)
                    .apply {
                        setValue((value as Byte).toInt(), false)
                        askForReset(property) { setValue((it as Byte).toInt(), false) }
                        setResetListener { property.reset().toString() }
                        setChangeListener {
                            property.set(it.toByte())
                        }
                    }
            }
            "Short" -> {
                val type = Type(property.getDefault() as Short,
                    { Maths.clamp(it.toLong(), range.minShort().toLong(), range.maxShort().toLong()).toShort() },
                    { it })
                return IntInput(style, title, visibilityKey, type, null, 0)
                    .apply {
                        setValue((value as Short).toInt(), false)
                        askForReset(property) { setValue((it as Short).toInt(), false) }
                        setResetListener { property.reset().toString() }
                        setChangeListener {
                            property.set(it.toShort())
                        }
                    }
            }
            "Int", "Integer" -> {
                val type = Type(property.getDefault() as Int,
                    { Maths.clamp(it.toLong(), range.minInt().toLong(), range.maxInt().toLong()).toInt() }, { it })
                return IntInput(style, title, visibilityKey, type, null, 0)
                    .apply {
                        setValue(value as Int, false)
                        askForReset(property) { setValue(it as Int, false) }
                        setResetListener { property.reset().toString() }
                        setChangeListener {
                            property.set(it.toInt())
                        }
                    }
            }
            "Long" -> {
                val type = Type(property.getDefault() as Long,
                    { Maths.clamp(it.toLong(), range.minLong(), range.maxLong()) }, { it })
                return IntInput(style, title, visibilityKey, type, null, 0)
                    .apply {
                        setValue(value as Long, false)
                        askForReset(property) { setValue(it as Long, false) }
                        setResetListener { property.reset().toString() }
                        setChangeListener {
                            property.set(it.toInt())
                        }
                    }

            }
            "Float" -> {
                val type = Type(property.getDefault() as Float,
                    { Maths.clamp(it as Float, range.minFloat(), range.maxFloat()) }, { it })
                return FloatInput(style, title, visibilityKey, type, null, 0)
                    .apply {
                        setValue(value as Float, false)
                        setResetListener { property.reset().toString() }
                        askForReset(property) { setValue(it as Float, false) }
                        setChangeListener {
                            property.set(it.toFloat())
                        }
                    }
            }
            "Double" -> {
                val type = Type(property.getDefault() as Double,
                    { Maths.clamp(it as Double, range.minDouble(), range.maxDouble()) }, { it })
                return FloatInput(style, title, visibilityKey, type, null, 0)
                    .apply {
                        setValue(value as Double, false)
                        setResetListener { property.reset().toString() }
                        askForReset(property) { setValue(it as Double, false) }
                        setChangeListener {
                            property.set(it)
                        }
                    }
            }
            "String" -> {
                return TitledListY(title, visibilityKey, style).add(
                    TextInput(title, visibilityKey, style.getChild("deep"), value as String)
                        .apply {
                            setResetListener { property.reset().toString() }
                            askForReset(property) { setValue(it as String, false) }
                            setChangeListener {
                                property.set(it)
                            }
                        }
                )
            }

            // float vectors
            // todo ranges for vectors (?)
            "Vector2f" -> {
                return VectorInput(style, title, visibilityKey, value as Vector2f, Type.VEC2)
                    .apply {
                        setResetListener { property.reset() }
                        askForReset(property) { setValue(it as Vector2f, false) }
                        setChangeListener { x, y, _, _ ->
                            value.set(x, y)
                        }
                    }
            }
            "Vector3f" -> {
                return VectorInput(style, title, visibilityKey, value as Vector3f, Type.VEC3)
                    .apply {
                        setResetListener { property.reset() }
                        askForReset(property) { setValue(it as Vector3f, false) }
                        setChangeListener { x, y, z, _ ->
                            value.set(x, y, z)
                        }
                    }
            }
            "Vector4f" -> {
                return VectorInput(style, title, visibilityKey, value as Vector4f, Type.VEC4)
                    .apply {
                        setResetListener { property.reset() }
                        askForReset(property) { setValue(it as Vector4f, false) }
                        setChangeListener { x, y, z, w ->
                            value.set(x, y, z, w)
                        }
                    }
            }
            "Vector2d" -> {
                return VectorInput(style, title, visibilityKey, value as Vector2d, Type.VEC2D)
                    .apply {
                        setResetListener { property.reset() }
                        askForReset(property) { setValue(it as Vector2d, false) }
                        setChangeListener { x, y, _, _ ->
                            value.set(x, y)
                        }
                    }
            }
            "Vector3d" -> {
                return VectorInput(style, title, visibilityKey, value as Vector3d, Type.VEC3D)
                    .apply {
                        setResetListener { property.reset() }
                        askForReset(property) { setValue(it as Vector3d, false) }
                        setChangeListener { x, y, z, _ ->
                            value.set(x, y, z)
                        }
                    }
            }
            "Vector4d" -> {
                return VectorInput(style, title, visibilityKey, value as Vector4d, Type.VEC4D)
                    .apply {
                        setResetListener { property.reset() }
                        askForReset(property) { setValue(it as Vector4d, false) }
                        setChangeListener { x, y, z, w ->
                            value.set(x, y, z, w)
                        }
                    }
            }

            // int vectors
            "Vector2i" -> {
                val type = Type(Vector2i(), 2)
                return VectorIntInput(style, title, visibilityKey, value as Vector2i, type)
                    .apply {
                        setResetListener { property.reset() }
                        askForReset(property) { setValue(it as Vector2i, false) }
                        setChangeListener { x, y, _, _ ->
                            value.set(x, y)
                        }
                    }
            }
            "Vector3i" -> {
                val type = Type(Vector3i(), 3)
                return VectorIntInput(style, title, visibilityKey, value as Vector3i, type)
                    .apply {
                        setResetListener { property.reset() }
                        askForReset(property) { setValue(it as Vector3i, false) }
                        setChangeListener { x, y, z, _ ->
                            value.set(x, y, z)
                        }
                    }
            }
            "Vector4i" -> {
                val type = Type(Vector4i(), 4)
                return VectorIntInput(style, title, visibilityKey, value as Vector4i, type)
                    .apply {
                        setResetListener { property.reset() }
                        askForReset(property) { setValue(it as Vector4i, false) }
                        setChangeListener { x, y, z, w ->
                            value.set(x, y, z, w)
                        }
                    }
            }

            // quaternions
            // entered using yxz angle, because that's much more intuitive
            "Quaternionf" -> {
                value as Quaternionf
                return VectorInput(style, title, visibilityKey, value.toEulerAnglesDegrees(), Type.ROT_YXZ).apply {
                    askForReset(property) { setValue((it as Quaternionf).toEulerAnglesDegrees(), false) }
                    setResetListener { property.reset() }
                    setChangeListener { x, y, z, _ ->
                        value.set(Vector3f(x.toFloat(), y.toFloat(), z.toFloat()).toQuaternionDegrees())
                    }
                }
            }
            "Quaterniond" -> {
                value as Quaterniond
                return VectorInput(style, title, visibilityKey, value.toEulerAnglesDegrees(), Type.ROT_YXZ).apply {
                    setResetListener { property.reset() }
                    askForReset(property) { setValue((it as Quaterniondc).toEulerAnglesDegrees(), false) }
                    setChangeListener { x, y, z, _ ->
                        value.set(Vector3d(x, y, z).toQuaternionDegrees())
                    }
                }
            }

            // matrices
            "Matrix4f" -> {
                value as Matrix4f
                val panel = TitledListY(title, visibilityKey, style)
                for (i in 0 until 4) {
                    panel.add(VectorInput(style, "", visibilityKey, value.getRow(i, Vector4f()), Type.VEC4)
                        .apply {
                            setChangeListener { x, y, z, w ->
                                value.setRow(i, Vector4f(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat()))
                            }
                        }
                    )
                }
                // todo reset listener
                panel.askForReset(property) {
                    it as Matrix4f
                    for (i in 0 until 4) {
                        (panel.children[i + 1] as VectorInput)
                            .setValue(it.getRow(i, Vector4f()), false)
                    }
                }
                return panel
            }
            // todo smaller matrices, and for double
            // todo when editing a matrix, maybe add a second mode for translation x rotation x scale

            // todo edit native arrays (byte/short/int/float/...) as images

            // native arrays
            // todo char array?
            "ByteArray", "Byte[]", "byte[]" -> {
                return object : AnyArrayPanel(title, visibilityKey, "Byte", style) {
                    override fun onChange() {
                        property.set(ByteArray(content.size) { content[it] as Byte })
                    }
                }.apply { setValues((value as ByteArray).toList()) }
            }
            "ShortArray", "Short[]", "short[]" -> {
                return object : AnyArrayPanel(title, visibilityKey, "Short", style) {
                    override fun onChange() {
                        property.set(ShortArray(content.size) { content[it] as Short })
                    }
                }.apply { setValues((value as ShortArray).toList()) }
            }
            "IntArray", "IntegerArray", "Integer[]", "integer[]", "Int[]", "int[]" -> {
                return object : AnyArrayPanel(title, visibilityKey, "Int", style) {
                    override fun onChange() {
                        property.set(IntArray(content.size) { content[it] as Int })
                    }
                }.apply { setValues((value as IntArray).toList()) }
            }
            "LongArray", "Long[]", "long[]" -> {
                return object : AnyArrayPanel(title, visibilityKey, "Long", style) {
                    override fun onChange() {
                        property.set(LongArray(content.size) { content[it] as Long })
                    }
                }.apply { setValues((value as LongArray).toList()) }
            }
            "FloatArray", "Float[]", "float[]" -> {
                return object : AnyArrayPanel(title, visibilityKey, "Float", style) {
                    override fun onChange() {
                        property.set(FloatArray(content.size) { content[it] as Float })
                    }
                }.apply { setValues((value as FloatArray).toList()) }
            }
            "DoubleArray", "Double[]", "double[]" -> {
                return object : AnyArrayPanel(title, visibilityKey, "Double", style) {
                    override fun onChange() {
                        property.set(DoubleArray(content.size) { content[it] as Double })
                    }
                }.apply { setValues((value as DoubleArray).toList()) }
            }

            "Inspectable" -> {
                value as Inspectable
                val list = PanelListY(style)
                val groups = HashMap<String, SettingCategory>()
                value.createInspector(list, style) { title2, description, path ->
                    groups.getOrPut(path.ifEmpty { title2 }) {
                        SettingCategory(title2, description, path, style)
                    }
                }
                return list
            }

            // todo nice ui inputs for array types and maps

            /*is Map<*, *> -> {



            }*/

            // todo sort list/map by key or property of the users choice
            // todo map inputs: a list of pairs of key & value
            // todo tables for structs?
            // todo show changed values with bold font or a different text color
            // todo ISaveable-s


            // is ISaveable -> { list all child properties }
            else -> {

                if ('<' in type0) {
                    val index0 = type0.indexOf('<')
                    val index1 = type0.lastIndexOf('>')
                    val mainType = type0.substring(index0).trim()
                    val generics = type0.substring(index0 + 1, index1).trim()
                    when (mainType) {
                        "Array" -> {
                            value as Array<*>
                            return object : AnyArrayPanel(title, visibilityKey, generics, style) {
                                override fun onChange() {
                                    property.set(content.toTypedArray())
                                }
                            }.apply { setValues(value.toList()) }
                        }
                        "List" -> {
                            value as List<*>
                            return object : AnyArrayPanel(title, visibilityKey, generics, style) {
                                override fun onChange() {
                                    property.set(content)
                                }
                            }.apply { setValues(value.toList()) }
                        }
                        "Set" -> {
                            value as Set<*>
                            return object : AnyArrayPanel(title, visibilityKey, generics, style) {
                                override fun onChange() {
                                    property.set(content.toHashSet())
                                }
                            }.apply { setValues(value.toList()) }
                        }
                        "Map" -> {
                            val (genericKey, genericValue) = generics.split(',')
                            // types must not be generic themselves for this to work... we should fix that...
                            value as Map<*, *>
                            return object : AnyMapPanel(title, visibilityKey, genericKey, genericValue, style) {
                                override fun onChange() {
                                    property.set(content.associate { it.first to it.second }.toMutableMap())
                                }
                            }.apply { setValues(value.map { AnyMapPanel.MutablePair(it.key, it.value) }) }
                        }
                        // todo pair and triple
                        // other generic types? stacks, queues, ...
                    }
                }

                LOGGER.warn("Missing knowledge to edit $type0, $title")

                return TextPanel("?? $title", style)

            }
        }

    }

    fun getType(value: Iterator<Any?>, warnName: String? = null): String? {
        var type: String? = null
        while (type == null && value.hasNext()) type = getTypeFromSample(value.next())
        if (warnName != null && type == null) warnDetectionIssue(warnName)
        return type
    }

    // get what type it is
    // if we were using a language, which doesn't discard type information at runtime, we would not have this
    // issue
    fun getArrayType(property: IProperty<Any?>, value: Iterator<Any?>, warnName: String? = null): String? {
        var arrayType = property.annotations.filterIsInstance<ListType>().firstOrNull()?.valueType
        while (arrayType == null && value.hasNext()) arrayType = getTypeFromSample(value.next())
        if (warnName != null && arrayType == null) warnDetectionIssue(warnName)
        return arrayType
    }

    fun getTypeFromSample(value: Any?): String? {
        value ?: return null
        return when (value) {
            is ISaveable -> value.className
            is Serializable -> value.javaClass.simpleName
            else -> value.javaClass.simpleName
        }
    }

    fun getDefault(type: String): Any? {
        return when (type) {
            "Byte" -> 0.toByte()
            "Short" -> 0.toShort()
            "Char" -> ' '
            "Int", "Integer" -> 0
            "Long" -> 0L
            "Float" -> 0f
            "Double" -> 0.0
            "String" -> ""
            "String?" -> null
            "Vector2f" -> Vector2f()
            "Vector3f" -> Vector3f()
            "Vector4f" -> Vector4f()
            "Vector2d" -> Vector2d()
            "Vector3d" -> Vector3d()
            "Vector4d" -> Vector4d()
            "Vector2i" -> Vector2i()
            "Vector3i" -> Vector3i()
            "Vector4i" -> Vector4i()
            "Quaternionf" -> Quaternionf()
            "Quaterniond" -> Quaterniond()
            "Matrix4f" -> Matrix4f()
            "Matrix4d" -> Matrix4d()
            else -> null
        }
    }

    private fun Panel.askForReset(property: IProperty<Any?>, callback: (Any?) -> Unit): Panel {
        setOnClickListener { _, _, button, _ ->
            if (button.isRight) {
                // todo option to edit the parent... how will that work?
                Menu.openMenu(listOf(MenuOption(NameDesc("Reset")) {
                    callback(property.reset())
                }))
            }
        }
        return this
    }

    fun Any?.toLong(): Long {
        return when (this) {
            is Byte -> toLong()
            is Short -> toLong()
            is Int -> toLong()
            is Long -> toLong()
            else -> throw RuntimeException()
        }
    }

}