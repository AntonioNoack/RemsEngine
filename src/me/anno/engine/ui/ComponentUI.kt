package me.anno.engine.ui

import me.anno.animation.Type
import me.anno.ecs.annotations.ListType
import me.anno.ecs.annotations.MapType
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Range.Companion.maxByte
import me.anno.ecs.annotations.Range.Companion.maxDouble
import me.anno.ecs.annotations.Range.Companion.maxFloat
import me.anno.ecs.annotations.Range.Companion.maxInt
import me.anno.ecs.annotations.Range.Companion.maxLong
import me.anno.ecs.annotations.Range.Companion.maxShort
import me.anno.ecs.annotations.Range.Companion.maxUByte
import me.anno.ecs.annotations.Range.Companion.maxUInt
import me.anno.ecs.annotations.Range.Companion.maxULong
import me.anno.ecs.annotations.Range.Companion.maxUShort
import me.anno.ecs.annotations.Range.Companion.minByte
import me.anno.ecs.annotations.Range.Companion.minDouble
import me.anno.ecs.annotations.Range.Companion.minFloat
import me.anno.ecs.annotations.Range.Companion.minInt
import me.anno.ecs.annotations.Range.Companion.minLong
import me.anno.ecs.annotations.Range.Companion.minShort
import me.anno.ecs.annotations.Range.Companion.minUByte
import me.anno.ecs.annotations.Range.Companion.minUInt
import me.anno.ecs.annotations.Range.Companion.minULong
import me.anno.ecs.annotations.Range.Companion.minUShort
import me.anno.ecs.prefab.PrefabCache.loadPrefab
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.IProperty
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.io.ISaveable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.language.translation.NameDesc
import me.anno.objects.inspectable.Inspectable
import me.anno.ui.base.Panel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.groups.TitledListY
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.Menu.msg
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.files.FileExplorerOption
import me.anno.ui.input.*
import me.anno.ui.style.Style
import me.anno.utils.maths.Maths
import me.anno.utils.strings.StringHelper
import me.anno.utils.strings.StringHelper.titlecase
import me.anno.utils.structures.tuples.MutablePair
import me.anno.utils.types.Quaternions.toEulerAnglesDegrees
import me.anno.utils.types.Quaternions.toQuaternionDegrees
import org.apache.logging.log4j.LogManager
import org.joml.*
import java.io.Serializable

object ComponentUI {

    private val LOGGER = LogManager.getLogger(ComponentUI::class)

    val fileInputRightClickOptions = listOf(
        // todo create menu of files with same type in project, e.g. image files, meshes or sth like that
        FileExplorerOption(NameDesc("Open Scene")) { ECSSceneTabs.open(it) },
        // create mutable scene, = import
        FileExplorerOption(NameDesc("Import")) { it ->
            val prefab = loadPrefab(it)
            if (prefab == null) msg(NameDesc("Cannot import ${it.name}", "Because it cannot be loaded as a scene", ""))
            else {
                // todo import options: place it into which folder?
                // todo create sub folders? for materials / meshes / animations (if that is relevant to the resource)
                // todo if it already exists: ask for replace
                // todo for all to-generate-files: ask for replace / new-rename / abort all
                // todo then open the scene
            }
        }
    )

    // todo mesh input, skeleton selection, animation selection, ...

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
            is Boolean,
            is Byte, is Short, is Int, is Long,
            is UByte, is UShort, is UInt, is ULong,
            is Float, is Double,
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
            is PrefabSaveable -> "PrefabSaveable"
            is FileReference -> "FileReference"

            // todo edit native arrays (byte/short/int/float/...) as images


            // collections and maps
            is Array<*> -> {
                val arrayType = getArrayType(property, value.iterator(), name) ?: return null
                return object : AnyArrayPanel(title, visibilityKey, arrayType, style) {
                    override fun onChange() {
                        property.set(this, content.toTypedArray())
                    }
                }.apply { setValues(value.toList()) }
            }

            is List<*> -> {
                val arrayType = getArrayType(property, value.iterator(), name) ?: return null
                return object : AnyArrayPanel(title, visibilityKey, arrayType, style) {
                    override fun onChange() {
                        property.set(this, content)
                    }
                }.apply { setValues(value.toList()) }
            }

            is Set<*> -> {
                val arrayType = getArrayType(property, value.iterator(), name) ?: return null
                return object : AnyArrayPanel(title, visibilityKey, arrayType, style) {
                    override fun onChange() {
                        property.set(this, content.toSet())
                    }
                }.apply { setValues(value.toList()) }
            }

            is Map<*, *> -> {
                val annotation = property.annotations.filterIsInstance<MapType>().firstOrNull()
                val keyType = annotation?.keyType ?: getType(value.keys.iterator(), name) ?: return null
                val valueType = annotation?.valueType ?: getType(value.values.iterator(), name) ?: return null
                return object : AnyMapPanel(title, visibilityKey, keyType, valueType, style) {
                    override fun onChange() {
                        property.set(this, content.associate { it.first to it.first }.toMutableMap())
                    }
                }.apply {
                    setValues(value.map { MutablePair(it.key, it.value) })
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
                return TextPanel("?? $title, ${value?.javaClass}", style)
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
        val default = property.getDefault()

        when (type0) {
            // native types
            "Bool", "Boolean" -> return BooleanInput(
                title,
                ttt,
                value as Boolean,
                default as? Boolean ?: false,
                style
            ).apply {
                property.init(this)
                setResetListener { property.reset(this) as Boolean }
                askForReset(property) { setValue(it as Boolean, false) }
                setChangeListener {
                    property.set(this, it)
                }
            }
            // todo char
            "Byte" -> {
                val type = Type(default as Byte,
                    { Maths.clamp(it.toLong(), range.minByte().toLong(), range.maxByte().toLong()).toByte() }, { it })
                return IntInput(style, title, visibilityKey, type, null, 0).apply {
                    property.init(this)
                    setValue((value as Byte).toInt(), false)
                    askForReset(property) { setValue((it as Byte).toInt(), false) }
                    setResetListener { property.reset(this).toString() }
                    setChangeListener {
                        property.set(this, it.toByte())
                    }
                }
            }
            "UByte" -> {
                val type = Type(default as UByte,
                    { Maths.clamp(it.toLong(), range.minUByte().toLong(), range.maxUByte().toLong()).toUByte() },
                    { it })
                return IntInput(style, title, visibilityKey, type, null, 0).apply {
                    property.init(this)
                    setValue((value as UByte).toInt(), false)
                    askForReset(property) { setValue((it as UByte).toInt(), false) }
                    setResetListener { property.reset(this).toString() }
                    setChangeListener {
                        property.set(this, it.toUByte())
                    }
                }
            }
            "Short" -> {
                val type = Type(default as Short,
                    { Maths.clamp(it.toLong(), range.minShort().toLong(), range.maxShort().toLong()).toShort() },
                    { it })
                return IntInput(style, title, visibilityKey, type, null, 0).apply {
                    property.init(this)
                    setValue((value as Short).toInt(), false)
                    askForReset(property) { setValue((it as Short).toInt(), false) }
                    setResetListener { property.reset(this).toString() }
                    setChangeListener {
                        property.set(this, it.toShort())
                    }
                }
            }
            "UShort" -> {
                val type = Type(default as UShort,
                    { Maths.clamp(it.toLong(), range.minUShort().toLong(), range.maxUShort().toLong()).toUShort() },
                    { it })
                return IntInput(style, title, visibilityKey, type, null, 0).apply {
                    property.init(this)
                    setValue((value as UShort).toInt(), false)
                    askForReset(property) { setValue((it as UShort).toInt(), false) }
                    setResetListener { property.reset(this).toString() }
                    setChangeListener {
                        property.set(this, it.toUShort())
                    }
                }
            }
            "Int", "Integer" -> {
                val type = Type(default as Int,
                    { Maths.clamp(it.toLong(), range.minInt().toLong(), range.maxInt().toLong()).toInt() }, { it })
                return IntInput(style, title, visibilityKey, type, null, 0).apply {
                    property.init(this)
                    setValue(value as Int, false)
                    askForReset(property) { setValue(it as Int, false) }
                    setResetListener { property.reset(this).toString() }
                    setChangeListener {
                        property.set(this, it.toInt())
                    }
                }
            }
            "UInt" -> {
                val type = Type(default as UInt,
                    { Maths.clamp(it.toLong(), range.minUInt().toLong(), range.maxUInt().toLong()).toUInt() }, { it })
                return IntInput(style, title, visibilityKey, type, null, 0).apply {
                    property.init(this)
                    setValue((value as UInt).toLong(), false)
                    askForReset(property) { setValue((it as UInt).toLong(), false) }
                    setResetListener { property.reset(this).toString() }
                    setChangeListener {
                        property.set(this, it.toUInt())
                    }
                }
            }
            "Long" -> {
                val type = Type(default as Long,
                    { Maths.clamp(it.toLong(), range.minLong(), range.maxLong()) }, { it })
                return IntInput(style, title, visibilityKey, type, null, 0).apply {
                    property.init(this)
                    setValue(value as Long, false)
                    askForReset(property) { setValue(it as Long, false) }
                    setResetListener { property.reset(this).toString() }
                    setChangeListener {
                        property.set(this, it)
                    }
                }
            }
            "ULong" -> {// not fully supported
                val type = Type(default as ULong,
                    { Maths.clamp(it.toULong2(), range.minULong(), range.maxULong()) }, { it })
                return IntInput(style, title, visibilityKey, type, null, 0).apply {
                    property.init(this)
                    setValue((value as ULong).toLong(), false)
                    askForReset(property) { setValue((it as ULong).toLong(), false) }
                    setResetListener { property.reset(this).toString() }
                    setChangeListener {
                        property.set(this, it.toULong())
                    }
                }
            }
            "Float" -> {
                val type = Type(default as Float,
                    { Maths.clamp(it as Float, range.minFloat(), range.maxFloat()) }, { it })
                return FloatInput(style, title, visibilityKey, type, null, 0).apply {
                    property.init(this)
                    setValue(value as Float, false)
                    setResetListener { property.reset(this).toString() }
                    askForReset(property) { setValue(it as Float, false) }
                    setChangeListener {
                        property.set(this, it.toFloat())
                    }
                }
            }
            "Double" -> {
                val type = Type(default as Double,
                    { Maths.clamp(it as Double, range.minDouble(), range.maxDouble()) }, { it })
                return FloatInput(style, title, visibilityKey, type, null, 0).apply {
                    property.init(this)
                    setValue(value as Double, false)
                    setResetListener { property.reset(this).toString() }
                    askForReset(property) { setValue(it as Double, false) }
                    setChangeListener {
                        property.set(this, it)
                    }
                }
            }
            "String" -> {
                return TitledListY(title, visibilityKey, style).add(
                    TextInput(title, visibilityKey, value as String, style.getChild("deep")).apply {
                        property.init(this)
                        setResetListener { property.reset(this).toString() }
                        askForReset(property) { setValue(it as String, false) }
                        setChangeListener {
                            property.set(this, it)
                        }
                    }
                )
            }

            // float vectors
            // todo ranges for vectors (?)
            "Vector2f" -> {
                val type = Type.VEC2.withDefault(default!!)
                return VectorInput(title, visibilityKey, value as Vector2f, type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector2f, false) }
                    setChangeListener { x, y, _, _ ->
                        property.set(this, Vector2f(x.toFloat(), y.toFloat()))
                    }
                }
            }
            "Vector3f" -> {
                val type = Type.VEC3.withDefault(default!!)
                return VectorInput(title, visibilityKey, value as Vector3f, type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector3f, false) }
                    setChangeListener { x, y, z, _ ->
                        property.set(this, Vector3f(x.toFloat(), y.toFloat(), z.toFloat()))
                    }
                }
            }
            "Vector4f" -> {
                val type = Type.VEC4.withDefault(default!!)
                return VectorInput(title, visibilityKey, value as Vector4f, type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector4f, false) }
                    setChangeListener { x, y, z, w ->
                        property.set(this, Vector4f(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat()))
                    }
                }
            }
            "Vector2d" -> {
                val type = Type.VEC2D.withDefault(default!!)
                return VectorInput(title, visibilityKey, value as Vector2d, type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector2d, false) }
                    setChangeListener { x, y, _, _ ->
                        property.set(this, Vector2d(x, y))
                    }
                }
            }
            "Vector3d" -> {
                val type = Type.VEC3D.withDefault(default!!)
                return VectorInput(title, visibilityKey, value as Vector3d, type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector3d, false) }
                    setChangeListener { x, y, z, _ ->
                        property.set(this, Vector3d(x, y, z))
                    }
                }
            }
            "Vector4d" -> {
                val type = Type.VEC4D.withDefault(default!!)
                return VectorInput(title, visibilityKey, value as Vector4d, type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector4d, false) }
                    setChangeListener { x, y, z, w ->
                        property.set(this, Vector4d(x, y, z, w))
                    }
                }
            }

            // int vectors
            "Vector2i" -> {
                val type = Type(default!!, 2)
                return VectorIntInput(style, title, visibilityKey, value as Vector2i, type).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector2i, false) }
                    setChangeListener { x, y, _, _ ->
                        property.set(this, Vector2i(x, y))
                    }
                }
            }
            "Vector3i" -> {
                val type = Type(default!!, 3)
                return VectorIntInput(style, title, visibilityKey, value as Vector3i, type).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector3i, false) }
                    setChangeListener { x, y, z, _ ->
                        property.set(this, Vector3i(x, y, z))
                    }
                }
            }
            "Vector4i" -> {
                val type = Type(default!!, 4)
                return VectorIntInput(style, title, visibilityKey, value as Vector4i, type).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector4i, false) }
                    setChangeListener { x, y, z, w ->
                        property.set(this, Vector4i(x, y, z, w))
                    }
                }
            }

            // quaternions
            // entered using yxz angle, because that's much more intuitive
            "Quaternionf" -> {
                value as Quaternionf
                val type = Type.ROT_YXZ.withDefault((default as Quaternionf).toEulerAnglesDegrees())
                return VectorInput(title, visibilityKey, value.toEulerAnglesDegrees(), type, style).apply {
                    property.init(this)
                    askForReset(property) { setValue((it as Quaternionf).toEulerAnglesDegrees(), false) }
                    setResetListener { property.reset(this) }
                    setChangeListener { x, y, z, _ ->
                        val q = Vector3f(x.toFloat(), y.toFloat(), z.toFloat()).toQuaternionDegrees()
                        property.set(this, q)
                    }
                }
            }
            "Quaterniond" -> {
                value as Quaterniond
                val type = Type.ROT_YXZ64.withDefault((default as Quaterniond).toEulerAnglesDegrees())
                return VectorInput(title, visibilityKey, value.toEulerAnglesDegrees(), type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue((it as Quaterniondc).toEulerAnglesDegrees(), false) }
                    setChangeListener { x, y, z, _ ->
                        property.set(this, Vector3d(x, y, z).toQuaternionDegrees())
                    }
                }
            }

            // matrices
            "Matrix4f" -> {
                value as Matrix4f
                default as Matrix4f
                val panel = TitledListY(title, visibilityKey, style)
                property.init(panel)
                // todo special types
                for (i in 0 until 4) {
                    panel.add(VectorInput("", visibilityKey, value.getRow(i, Vector4f()), Type.VEC4, style)
                        .apply {
                            // todo correct change listener
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
                        property.set(this, ByteArray(content.size) { content[it] as Byte })
                    }
                }.apply {
                    property.init(this)
                    setValues((value as ByteArray).toList())
                }
            }
            "ShortArray", "Short[]", "short[]" -> {
                return object : AnyArrayPanel(title, visibilityKey, "Short", style) {
                    override fun onChange() {
                        property.set(this, ShortArray(content.size) { content[it] as Short })
                    }
                }.apply {
                    property.init(this)
                    setValues((value as ShortArray).toList())
                }
            }
            "IntArray", "IntegerArray", "Integer[]", "integer[]", "Int[]", "int[]" -> {
                return object : AnyArrayPanel(title, visibilityKey, "Int", style) {
                    override fun onChange() {
                        property.set(this, IntArray(content.size) { content[it] as Int })
                    }
                }.apply {
                    property.init(this)
                    setValues((value as IntArray).toList())
                }
            }
            "LongArray", "Long[]", "long[]" -> {
                return object : AnyArrayPanel(title, visibilityKey, "Long", style) {
                    override fun onChange() {
                        property.set(this, LongArray(content.size) { content[it] as Long })
                    }
                }.apply {
                    property.init(this)
                    setValues((value as LongArray).toList())
                }
            }
            "FloatArray", "Float[]", "float[]" -> {
                return object : AnyArrayPanel(title, visibilityKey, "Float", style) {
                    override fun onChange() {
                        property.set(this, FloatArray(content.size) { content[it] as Float })
                    }
                }.apply {
                    property.init(this)
                    setValues((value as FloatArray).toList())
                }
            }
            "DoubleArray", "Double[]", "double[]" -> {
                return object : AnyArrayPanel(title, visibilityKey, "Double", style) {
                    override fun onChange() {
                        property.set(this, DoubleArray(content.size) { content[it] as Double })
                    }
                }.apply {
                    property.init(this)
                    setValues((value as DoubleArray).toList())
                }
            }

            "File", "FileReference", "InvalidReference" -> {
                value as FileReference
                return FileInput(title, style, value, fileInputRightClickOptions).apply {
                    property.init(this)
                    setResetListener {
                        property.reset(this) as? FileReference
                            ?: InvalidRef
                    }
                    setChangeListener {
                        property.set(this, it)
                    }
                }
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

            "PrefabSaveable", "Material", "Mesh", "MorphTarget", "Skeleton" -> {
                value as? PrefabSaveable
                // todo just a reference with a type limiter

                return TextPanel("todo: prefab saveable, ${value?.javaClass?.name}", style)
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

                when {
                    '<' in type0 -> {
                        val index0 = type0.indexOf('<')
                        val index1 = type0.lastIndexOf('>')
                        val mainType = type0.substring(0, index0).trim()
                        val generics = type0.substring(index0 + 1, index1).trim()
                        when (mainType) {
                            "Array" -> {
                                value as Array<*>
                                return object : AnyArrayPanel(title, visibilityKey, generics, style) {
                                    override fun onChange() {
                                        property.set(this, content.toTypedArray())
                                    }
                                }.apply {
                                    property.init(this)
                                    setValues(value.toList())
                                }
                            }
                            "List" -> {
                                value as List<*>
                                return object : AnyArrayPanel(title, visibilityKey, generics, style) {
                                    override fun onChange() {
                                        property.set(this, content)
                                    }
                                }.apply {
                                    property.init(this)
                                    setValues(value.toList())
                                }
                            }
                            "Set" -> {
                                value as Set<*>
                                return object : AnyArrayPanel(title, visibilityKey, generics, style) {
                                    override fun onChange() {
                                        property.set(this, content.toHashSet())
                                    }
                                }.apply {
                                    property.init(this)
                                    setValues(value.toList())
                                }
                            }
                            "Map" -> {
                                val (genericKey, genericValue) = generics.split(',')
                                // types must not be generic themselves for this to work... we should fix that...
                                value as Map<*, *>
                                return object : AnyMapPanel(title, visibilityKey, genericKey, genericValue, style) {
                                    override fun onChange() {
                                        property.set(this, content.associate { it.first to it.second }.toMutableMap())
                                    }
                                }.apply {
                                    property.init(this)
                                    setValues(value.map { MutablePair(it.key, it.value) })
                                }
                            }
                            // todo pair and triple
                            // other generic types? stacks, queues, ...
                            else -> {
                                LOGGER.warn("Unknown generic type: $mainType<$generics>")
                            }
                        }
                    }
                    type0.endsWith("/Reference", true) -> {
                        val type1 = type0.substring(0, type0.lastIndexOf('/'))
                        // todo filter the types
                        // todo index all files of this type in the current project (plus customizable extra directories)
                        // todo and show them here, with their nice icons
                        value as FileReference
                        return FileInput(title, style, value, fileInputRightClickOptions).apply {
                            property.init(this)
                            setResetListener {
                                property.reset(this) as? FileReference
                                    ?: InvalidRef
                            }
                            setChangeListener {
                                property.set(this, it)
                            }
                        }
                    }
                }

                LOGGER.warn("Missing knowledge to edit $type0, $title")

                return TextPanel("?? $title : ${value?.javaClass?.simpleName}", style)
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
        addOnClickListener { _, _, button, _ ->
            if (button.isRight) {
                // todo option to edit the parent... how will that work?
                Menu.openMenu(listOf(MenuOption(NameDesc("Reset")) {
                    callback(property.reset(this))
                }))
                true
            } else false
        }
        return this
    }

    fun Any?.toLong(): Long {
        return when (this) {
            is Byte -> toLong()
            is UByte -> toLong()
            is Short -> toLong()
            is UShort -> toLong()
            is Int -> toLong()
            is UInt -> toLong()
            is Long -> this
            is ULong -> toLong()
            else -> throw RuntimeException()
        }
    }

    fun Any?.toULong2(): ULong {
        return when (this) {
            is Byte -> toULong()
            is UByte -> toULong()
            is Short -> toULong()
            is UShort -> toULong()
            is Int -> toULong()
            is UInt -> toULong()
            is Long -> toULong()
            is ULong -> this
            else -> throw RuntimeException()
        }
    }

}