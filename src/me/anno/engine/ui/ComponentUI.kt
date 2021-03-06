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
import me.anno.ecs.components.script.ScriptComponent
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.IProperty
import me.anno.engine.RemsEngine
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.io.ISaveable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.JsonFormatter
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.clamp
import me.anno.studio.Inspectable
import me.anno.studio.StudioBase
import me.anno.ui.Panel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.groups.TitledListY
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.Menu.msg
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.code.CodeEditor
import me.anno.ui.editor.files.FileExplorerOption
import me.anno.ui.input.*
import me.anno.ui.style.Style
import me.anno.utils.Color.rgba
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.strings.StringHelper.camelCaseToTitle
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull
import me.anno.utils.structures.tuples.MutablePair
import me.anno.utils.types.Quaternions.toEulerAnglesDegrees
import me.anno.utils.types.Quaternions.toQuaternionDegrees
import org.apache.logging.log4j.LogManager
import org.joml.*
import org.luaj.vm2.LuaError
import java.io.Serializable
import kotlin.math.ln
import kotlin.math.pow
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

object ComponentUI {

    private val LOGGER = LogManager.getLogger(ComponentUI::class)

    val fileInputRightClickOptions = listOf(
        // todo create menu of files with same type in project, e.g. image files, meshes or sth like that
        FileExplorerOption(NameDesc("Open Scene")) { _, it -> ECSSceneTabs.open(it, "Entity", PlayMode.EDITING) },
        // create mutable scene, = import
        FileExplorerOption(NameDesc("Import")) { panel, it ->
            val prefab = PrefabCache[it]
            if (prefab == null) msg(
                panel.windowStack,
                NameDesc("Cannot import ${it.name}", "Because it cannot be loaded as a scene", "")
            )
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

    // todo position control+x is not working (reset on right click is working)

    fun List<*>.toTypedArray2(value: Any): Any? {
        val newArray = java.lang.reflect.Array.newInstance(value.javaClass.componentType, size) // Array<V>
        val tmpArray = toTypedArray() // Array<Object>
        // println("old class: ${value::class.java}, or ${value.javaClass}, new class: ${newArray.javaClass}, tmp class: ${tmpArray.javaClass}")
        try {
            // will this work? can we do it via reflections, if it doesn't???
            // at least in my version it works ^^
            System.arraycopy(tmpArray, 0, newArray, 0, tmpArray.size)
        } catch (e: Exception) {
            LOGGER.error(
                "Copy failed, which is a failure by the JVM implementation; please contact Antonio Noack, author of Rem's Engine",
                e
            )
        }
        return newArray
    }

    fun createUI2(
        name: String?,
        visibilityKey: String,
        property: IProperty<Any?>,
        range: Range?,
        style: Style
    ): Panel? {

        val title = name?.camelCaseToTitle() ?: ""

        val type0 = property.annotations.firstInstanceOrNull<me.anno.ecs.annotations.Type>()?.type
        val type1 = type0 ?: when (val value = property.get()) {

            // native types
            is Boolean,
            is Char,
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
                // aabbs
            is AABBf, is AABBd,
                // planes
            is Planef, is Planed,
                // native arrays
            is ByteArray, is ShortArray,
            is CharArray,
            is IntArray, is LongArray,
            is FloatArray, is DoubleArray,
            -> value::class.simpleName ?: "anonymous"

            is PrefabSaveable -> "PrefabSaveable"
            is Inspectable -> "Inspectable"
            is FileReference -> "FileReference"

            // todo edit native arrays (byte/short/int/float/...) as images


            // collections and maps
            is Array<*> -> {
                val arrayType = getArrayType(property, value.iterator(), name) ?: return null
                return object : AnyArrayPanel(title, visibilityKey, arrayType, style) {
                    override fun onChange() {
                        property.set(this, values.toTypedArray2(value))
                    }
                }.apply { setValues(value.toList()) }
            }

            is List<*> -> {
                val arrayType = getArrayType(property, value.iterator(), name) ?: return null
                return object : AnyArrayPanel(title, visibilityKey, arrayType, style) {
                    override fun onChange() {
                        property.set(this, values)
                    }
                }.apply { setValues(value.toList()) }
            }

            is Set<*> -> {
                val arrayType = getArrayType(property, value.iterator(), name) ?: return null
                return object : AnyArrayPanel(title, visibilityKey, arrayType, style) {
                    override fun onChange() {
                        property.set(this, values.toSet())
                    }
                }.apply { setValues(value.toList()) }
            }

            is Map<*, *> -> {
                val annotation = property.annotations.firstInstanceOrNull<MapType>()
                val keyType = annotation?.keyType ?: getType(value.keys.iterator(), name) ?: return null
                val valueType = annotation?.valueType ?: getType(value.values.iterator(), name) ?: return null
                return object : AnyMapPanel(title, visibilityKey, keyType, valueType, style) {
                    override fun onChange() {
                        property.set(this, values.associate { it.first to it.first }.toMutableMap())
                    }
                }.apply {
                    setValues(value.map { MutablePair(it.key, it.value) })
                }
            }

            // todo tables for structs? (database table type)

            // is ISaveable -> { list all child properties }
            else -> {
                if (value != null && value is Enum<*>) {
                    val input = EnumInput.createInput(title, value, style)
                    val values = EnumInput.getEnumConstants(value.javaClass)
                    input.setChangeListener { _, index, _ -> property.set(input, values[index]) }
                    return input
                }
                if (value is ISaveable) {
                    return createISaveableInput(title, value, style, property)
                }
                return TextPanel("?? $title, ${value?.javaClass}", style)
            }
        }

        return createUIByTypeName(name, visibilityKey, property, type1, range, style)

    }

    fun createISaveableInput(title: String, saveable: ISaveable, style: Style, property: IProperty<Any?>): Panel {
        // if saveable is Inspectable, we could use the inspector as well :)
        // runtime: O(n??) where n is number of properties of that class
        // could be improved, but shouldn't matter
        try {
            // force all variables, so we can list all
            // todo: this will get stuck for recursive references...
            val typeMap = HashMap<String, Pair<String, Any?>>()
            val detective = DetectiveWriter(typeMap)
            saveable.save(detective)
            if (typeMap.isNotEmpty()) {
                // outlined, black background or sth like that
                // best a colored padding
                // todo more colorful?
                val panel = PanelListY(style.getChild("deep"))
                // todo border is too wide on y, why???
                panel.padding.set(2) // border
                val panel2 = PanelListY(style)
                panel.add(panel2)
                val title2 = title.ifBlank { saveable.className }
                // todo list/array-views should have their content visibility be toggleable
                panel2.add(object : TextPanel(title2, style) {
                    override fun onCopyRequested(x: Float, y: Float) =
                        TextWriter.toText(saveable, InvalidRef)
                }.apply {
                    // make it look like a title
                    isItalic = true
                    disableFocusColors()
                })
                for ((name, typeValue) in typeMap) {
                    val type = typeValue.first
                    val startValue = typeValue.second
                    panel2.add(
                        createUIByTypeName(
                            name, "${saveable.className}/$name",
                            SIProperty(name, type, saveable, startValue, property, detective),
                            typeValue.first, null, style
                        )
                    )
                }
                return panel
            } else LOGGER.warn("No property was found for class ${saveable.className}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // serialize saveables for now, this is simple
        // a first variant for editing may be a json editor
        val value0 = JsonFormatter.format(TextWriter.toText(saveable, StudioBase.workspace))
        val input = TextInputML(title, value0, style)
        val textColor = input.base.textColor
        input.addChangeListener {
            if (it == "null") {
                property.set(input, null)
            } else {
                try {
                    val value2 = TextReader.read(it, StudioBase.workspace, false).firstOrNull()
                    if (value2 != null) {
                        property.set(input, value2)
                        input.base.textColor = textColor
                    } else {
                        input.base.textColor = 0xffff77 or 0xff.shl(24)
                    }
                } catch (e: Exception) {
                    input.base.textColor = 0xff7733 or 0xff.shl(24)
                }
            }
        }
        return input
    }

    private fun warnDetectionIssue(name: String?) {
        LOGGER.warn("Could not detect type of $name")
    }

    fun createUIByTypeName(
        name: String?,
        visibilityKey: String,
        property: IProperty<Any?>,
        type0: String,
        range: Range?,
        style: Style
    ): Panel {

        // nullable types
        if (type0.endsWith('?') || type0.endsWith("?/PrefabSaveable")) {
            val i0 = type0.lastIndexOf('?')
            val type1 = type0.substring(0, i0) + type0.substring(i0 + 1)
            val notNullable = createUIByTypeName(name, visibilityKey, property, type1, range, style)
            return if (notNullable is InputPanel<*>) {
                NullableInput(notNullable, property, style)
            } else notNullable // some error has happened, e.g. unknown type
        }

        val title = name?.camelCaseToTitle() ?: ""
        val ttt = "" // we could use annotations for that :)
        val value = property.get()
        val default = property.getDefault()
        // LOGGER.debug("Default for $title: $default")

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
                    { clamp(it.toLong(), range.minByte().toLong(), range.maxByte().toLong()).toByte() }, { it })
                return IntInput(style, title, visibilityKey, type).apply {
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
                    { clamp(it.toLong(), range.minUByte().toLong(), range.maxUByte().toLong()).toUByte() },
                    { it })
                return IntInput(style, title, visibilityKey, type).apply {
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
                    { clamp(it.toLong(), range.minShort().toLong(), range.maxShort().toLong()).toShort() },
                    { it })
                return IntInput(style, title, visibilityKey, type).apply {
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
                    { clamp(it.toLong(), range.minUShort().toLong(), range.maxUShort().toLong()).toUShort() },
                    { it })
                return IntInput(style, title, visibilityKey, type).apply {
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
                if (title.endsWith("color", true)) {
                    value as Int
                    return ColorInput(style, title, visibilityKey, value.toVecRGBA(), true).apply {
                        property.init(this)
                        askForReset(property) { it as Int; setValue(it.toVecRGBA(), false) }
                        setResetListener { (property.reset(this) as Int).toVecRGBA() }
                        setChangeListener { r, g, b, a ->
                            property.set(this, rgba(r, g, b, a))
                        }
                    }
                } else {
                    val type = Type(default as Int,
                        { clamp(it.toLong(), range.minInt().toLong(), range.maxInt().toLong()).toInt() }, { it })
                    return IntInput(style, title, visibilityKey, type).apply {
                        property.init(this)
                        setValue(value as Int, false)
                        askForReset(property) { setValue(it as Int, false) }
                        setResetListener { property.reset(this).toString() }
                        setChangeListener {
                            property.set(this, it.toInt())
                        }
                    }
                }
            }
            "UInt" -> {
                val type = Type(default as UInt,
                    { clamp(it.toLong(), range.minUInt().toLong(), range.maxUInt().toLong()).toUInt() }, { it })
                return IntInput(style, title, visibilityKey, type).apply {
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
                val type = Type(default as? Long ?: throw RuntimeException("$title is not long"),
                    { clamp(it.toLong(), range.minLong(), range.maxLong()) }, { it })
                return IntInput(style, title, visibilityKey, type).apply {
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
                    { clamp(it.toULong2(), range.minULong(), range.maxULong()) }, { it })
                return IntInput(style, title, visibilityKey, type).apply {
                    property.init(this)
                    setValue((value as ULong).toLong(), false)
                    askForReset(property) { setValue((it as ULong).toLong(), false) }
                    setResetListener { property.reset(this).toString() }
                    setChangeListener {
                        property.set(this, it.toULong())
                    }
                }
            }
            // todo slider type, which returns a float in 01 range
            "Float" -> {
                val type = Type(default as? Float ?: throw RuntimeException("$title is not float"),
                    { clamp(it as Float, range.minFloat(), range.maxFloat()) }, { it })
                return FloatInput(style, title, visibilityKey, type).apply {
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
                val type = Type(default as? Double ?: throw RuntimeException("$title is not double"),
                    { clamp(it as Double, range.minDouble(), range.maxDouble()) }, { it })
                return FloatInput(style, title, visibilityKey, type).apply {
                    property.init(this)
                    setValue(value as Double, false)
                    setResetListener { property.reset(this).toString() }
                    askForReset(property) { setValue(it as Double, false) }
                    setChangeListener {
                        property.set(this, it)
                    }
                }
            }
            "Char", "Character", "char" -> {
                return TitledListY(title, visibilityKey, style).add(
                    TextInput(title, visibilityKey, value.toString(), style.getChild("deep")).apply {
                        // todo limit length to 1
                        property.init(this)
                        setResetListener { property.reset(this).toString() }
                        askForReset(property) { setValue(it.toString(), false) }
                        addChangeListener {
                            property.set(this, if (it.isNotEmpty()) it[0] else ' ')
                        }
                    }
                )
            }
            "String" -> {
                return TitledListY(title, visibilityKey, style).add(
                    TextInput(title, visibilityKey, value as? String ?: "", style.getChild("deep")).apply {
                        property.init(this)
                        setResetListener { property.reset(this).toString() }
                        askForReset(property) { setValue(it as String, false) }
                        addChangeListener {
                            property.set(this, it)
                        }
                    }
                )
            }

            // float vectors
            // todo ranges for vectors
            "Vector2f" -> {
                val type = Type.VEC2.withDefault(default as? Vector2f ?: Vector2f())
                return FloatVectorInput(title, visibilityKey, value as Vector2f, type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector2f, false) }
                    setChangeListener { x, y, _, _ ->
                        property.set(this, Vector2f(x.toFloat(), y.toFloat()))
                    }
                }
            }
            "Vector3f" -> {
                val type = Type.VEC3.withDefault(default as? Vector3f ?: Vector3f())
                return FloatVectorInput(title, visibilityKey, value as Vector3f, type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector3f, false) }
                    setChangeListener { x, y, z, _ ->
                        property.set(this, Vector3f(x.toFloat(), y.toFloat(), z.toFloat()))
                    }
                }
            }
            "Vector4f" -> {
                val type = Type.VEC4.withDefault(default as? Vector4f ?: Vector4f())
                return FloatVectorInput(title, visibilityKey, value as Vector4f, type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector4f, false) }
                    setChangeListener { x, y, z, w ->
                        property.set(this, Vector4f(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat()))
                    }
                }
            }
            "Planef" -> {
                val type = Type.PLANE4.withDefault(default as? Planef ?: Planef())
                return FloatVectorInput(title, visibilityKey, value as Planef, type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Planef, false) }
                    setChangeListener { x, y, z, w ->
                        property.set(this, Planef(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat()))
                    }
                }
            }
            "Vector2d" -> {
                val type = Type.VEC2D.withDefault(default as? Vector2d ?: Vector2d())
                return FloatVectorInput(title, visibilityKey, value as Vector2d, type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector2d, false) }
                    setChangeListener { x, y, _, _ ->
                        property.set(this, Vector2d(x, y))
                    }
                }
            }
            "Vector3d" -> {
                val type = Type.VEC3D.withDefault(default as? Vector3d ?: Vector3d())
                return FloatVectorInput(title, visibilityKey, value as Vector3d, type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector3d, false) }
                    setChangeListener { x, y, z, _ ->
                        property.set(this, Vector3d(x, y, z))
                    }
                }
            }
            "Vector4d" -> {
                val type = Type.VEC4D.withDefault(default as? Vector4d ?: Vector4d())
                return FloatVectorInput(title, visibilityKey, value as Vector4d, type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector4d, false) }
                    setChangeListener { x, y, z, w ->
                        property.set(this, Vector4d(x, y, z, w))
                    }
                }
            }
            "Planed" -> {
                val type = Type.PLANE4D.withDefault(default as? Planed ?: Planed())
                return FloatVectorInput(title, visibilityKey, value as Planed, type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Planed, false) }
                    setChangeListener { x, y, z, w ->
                        property.set(this, Planed(x, y, z, w))
                    }
                }
            }
            // int vectors
            "Vector2i" -> {
                val type = Type(default as? Vector2i ?: Vector2i(), 2)
                return IntVectorInput(style, title, visibilityKey, value as Vector2i, type).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector2i, false) }
                    addChangeListener { x, y, _, _ ->
                        property.set(this, Vector2i(x.toInt(), y.toInt()))
                    }
                }
            }
            "Vector3i" -> {
                val type = Type(default as? Vector3i ?: Vector3i(), 3)
                return IntVectorInput(style, title, visibilityKey, value as Vector3i, type).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector3i, false) }
                    addChangeListener { x, y, z, _ ->
                        property.set(this, Vector3i(x.toInt(), y.toInt(), z.toInt()))
                    }
                }
            }
            "Vector4i" -> {
                val type = Type(default as? Vector4i ?: Vector4i(), 4)
                return IntVectorInput(style, title, visibilityKey, value as Vector4i, type).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector4i, false) }
                    addChangeListener { x, y, z, w ->
                        property.set(this, Vector4i(x.toInt(), y.toInt(), z.toInt(), w.toInt()))
                    }
                }
            }

            // quaternions
            // entered using yxz angle, because that's much more intuitive
            "Quaternionf" -> {
                value as Quaternionf
                val type = Type.ROT_YXZ.withDefault((default as Quaternionf).toEulerAnglesDegrees())
                return FloatVectorInput(title, visibilityKey, value.toEulerAnglesDegrees(), type, style).apply {
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
                return FloatVectorInput(title, visibilityKey, value.toEulerAnglesDegrees(), type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue((it as Quaterniondc).toEulerAnglesDegrees(), false) }
                    setChangeListener { x, y, z, _ ->
                        property.set(this, Vector3d(x, y, z).toQuaternionDegrees())
                    }
                }
            }

            // colors, e.g. for materials
            "Color3", "Color3HDR" -> {
                value as Vector3f
                val maxPower = 1e3f

                // logarithmic brightness scale
                fun b2l(b: Vector3f): Vector4f {
                    var length = b.length()
                    if (length == 0f) length = 1f
                    val power = clamp(ln(length) / ln(maxPower) * 0.5f + 0.5f, 0f, 1f)
                    return Vector4f(b.x, b.y, b.z, power)
                }

                fun l2b(l: Vector4f): Vector3f {
                    val power = maxPower.pow(l.w * 2f - 1f)
                    return Vector3f(l.x, l.y, l.z).mul(power)
                }
                return ColorInput(style, title, visibilityKey, b2l(value), type0 == "Color3HDR")
                    .apply {
                        property.init(this)
                        // todo reset listener for color inputs
                        // todo brightness should have different background than alpha
                        // setResetListener { property.reset(this) }
                        askForReset(property) { setValue(b2l(it as Vector3f), false) }
                        setChangeListener { r, g, b, a ->
                            property.set(this, l2b(Vector4f(r, g, b, a)))
                        }
                    }
            }
            "Color4", "Color4HDR" -> {
                value as Vector4f
                // todo hdr colors per color amplitude
                return ColorInput(style, title, visibilityKey, value, true)
                    .apply {
                        property.init(this)
                        // todo reset listener for color inputs
                        // setResetListener { property.reset(this) }
                        askForReset(property) { setValue(it as Vector4f, false) }
                        setChangeListener { r, g, b, a ->
                            property.set(this, Vector4f(r, g, b, a))
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
                // todo operations: translate, rotate, scale
                for (i in 0 until 4) {
                    panel.add(FloatVectorInput("", visibilityKey, value.getRow(i, Vector4f()), Type.VEC4, style)
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
                        (panel.children[i + 1] as FloatVectorInput)
                            .setValue(it.getRow(i, Vector4f()), false)
                    }
                }
                return panel
            }

            // AABBf/AABBd
            "AABBf" -> {
                value as AABBf
                default as AABBf
                val typeMin = Type.VEC3.withDefault(default.getMin2())
                val pane = TitledListY(title, visibilityKey, style)
                pane.add(FloatVectorInput("", visibilityKey, value.getMin2(), typeMin, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue((it as AABBf).getMin2(), false) }
                    setChangeListener { x, y, z, _ ->
                        value.setMin(x.toFloat(), y.toFloat(), z.toFloat())
                        property.set(this, value)
                    }
                })
                val typeMax = Type.VEC3D.withDefault(default.getMax2())
                pane.add(FloatVectorInput("", visibilityKey, value.getMax2(), typeMax, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue((it as AABBf).getMax2(), false) }
                    setChangeListener { x, y, z, _ ->
                        value.setMax(x.toFloat(), y.toFloat(), z.toFloat())
                        property.set(this, value)
                    }
                })
                return pane
            }
            "AABBd" -> {
                value as AABBd
                default as AABBd
                val typeMin = Type.VEC3D.withDefault(default.getMin2())
                val pane = TitledListY(title, visibilityKey, style)
                pane.add(FloatVectorInput("", visibilityKey, value.getMin2(), typeMin, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue((it as AABBd).getMin2(), false) }
                    setChangeListener { x, y, z, _ ->
                        value.setMin(x, y, z)
                        property.set(this, value)
                    }
                })
                val typeMax = Type.VEC3D.withDefault(default.getMax2())
                pane.add(FloatVectorInput("", visibilityKey, value.getMax2(), typeMax, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue((it as AABBd).getMax2(), false) }
                    setChangeListener { x, y, z, _ ->
                        value.setMax(x, y, z)
                        property.set(this, value)
                    }
                })
                return pane
            }

            // todo smaller matrices, and for double
            // todo when editing a matrix, maybe add a second mode for translation x rotation x scale

            // todo edit native arrays (byte/short/int/float/...) as images

            // native arrays
            // todo char array?
            "ByteArray", "Byte[]", "byte[]" -> {
                return object : AnyArrayPanel(title, visibilityKey, "Byte", style) {
                    override fun onChange() {
                        property.set(this, ByteArray(values.size) { values[it] as Byte })
                    }
                }.apply {
                    property.init(this)
                    setValues((value as ByteArray).toList())
                }
            }
            "ShortArray", "Short[]", "short[]" -> {
                return object : AnyArrayPanel(title, visibilityKey, "Short", style) {
                    override fun onChange() {
                        property.set(this, ShortArray(values.size) { values[it] as Short })
                    }
                }.apply {
                    property.init(this)
                    setValues((value as ShortArray).toList())
                }
            }
            "IntArray", "IntegerArray", "Integer[]", "integer[]", "Int[]", "int[]" -> {
                return object : AnyArrayPanel(title, visibilityKey, "Int", style) {
                    override fun onChange() {
                        property.set(this, IntArray(values.size) { values[it] as Int })
                    }
                }.apply {
                    property.init(this)
                    setValues((value as IntArray).toList())
                }
            }
            "LongArray", "Long[]", "long[]" -> {
                return object : AnyArrayPanel(title, visibilityKey, "Long", style) {
                    override fun onChange() {
                        property.set(this, LongArray(values.size) { values[it] as Long })
                    }
                }.apply {
                    property.init(this)
                    setValues((value as LongArray).toList())
                }
            }
            "FloatArray", "Float[]", "float[]" -> {
                return object : AnyArrayPanel(title, visibilityKey, "Float", style) {
                    override fun onChange() {
                        property.set(this, FloatArray(values.size) { values[it] as Float })
                    }
                }.apply {
                    property.init(this)
                    setValues((value as FloatArray).toList())
                }
            }
            "DoubleArray", "Double[]", "double[]" -> {
                return object : AnyArrayPanel(title, visibilityKey, "Double", style) {
                    override fun onChange() {
                        property.set(this, DoubleArray(values.size) { values[it] as Double })
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
                        LOGGER.debug("Generic type $type0 for $name")
                        when (mainType) {
                            "Array" -> {
                                value as Array<*>
                                return object : AnyArrayPanel(title, visibilityKey, generics, style) {
                                    override fun onChange() {
                                        property.set(this, values.toTypedArray2(value))
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
                                        property.set(this, values)
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
                                        property.set(this, values.toHashSet())
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
                                        property.set(this, values.associate { it.first to it.second }.toMutableMap())
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
                        // val type1 = type0.substring(0, type0.lastIndexOf('/'))
                        // todo filter the types
                        // todo index all files of this type in the current project (plus customizable extra directories)
                        // todo and show them here, with their nice icons
                        val value0 = value as? FileReference ?: InvalidRef
                        val fi = FileInput(title, style, value0, fileInputRightClickOptions).apply {
                            property.init(this)
                            setResetListener {
                                property.reset(this) as? FileReference
                                    ?: InvalidRef
                            }
                            setChangeListener {
                                // todo check if this file is ok
                                // todo if not, undo this change
                                property.set(this, it)
                            }
                        }

                        val clazzName = type0.substring(0, type0.lastIndexOf('/'))
                        val project = (StudioBase.instance as? RemsEngine)?.currentProject
                        val options = ArrayList<Prefab?>()
                        options.add(null)
                        // dangerous & slow at the moment :/
                        /*project?.forAllPrefabs { _, prefab ->
                            val sample = ISaveable.getSample(prefab.clazzName)
                            if (sample != null && instanceOf(sample, clazzName)){
                                options.add(prefab)
                            }
                        }*/
                        val prefab0 = PrefabCache[value as? FileReference]
                        if (prefab0 != null) options.add(prefab0)
                        if (options.size > 1) {
                            // todo show fileExplorer-like previews
                            val ei = EnumInput(
                                title, false, prefab0?.source?.absolutePath ?: "null",
                                options.map { NameDesc(it?.source?.absolutePath ?: "null") }, style
                            ).apply {
                                property.init(this)
                                // todo reset listener for EnumInput
                                // setResetListener {}
                                setChangeListener { _, index, _ ->
                                    property.set(this, options[index]?.source ?: InvalidRef)
                                }
                            }
                            val list = PanelListY(style)
                            list.add(fi)
                            list.add(ei)
                            return list
                        } else return fi
                    }
                    // actual instance, needs to be local, linked via path
                    // e.g. for physics constraints, events, or things like that
                    type0.endsWith("/PrefabSaveable", true) -> {
                        val type1 = type0.substring(0, type0.lastIndexOf('/'))
                        value as PrefabSaveable?
                        // todo find the class somehow...
                        val clazz = ISaveable.getClass(type1)!!
                        return PrefabSaveableInput(title, clazz, value, style)
                            .apply {
                                property.init(this)
                                setResetListener {
                                    property.reset(this) as? PrefabSaveable
                                }
                                setChangeListener {
                                    property.set(this, it)
                                }
                            }
                    }
                    type0.endsWith("/Code", true) -> {
                        // todo debugging info & such
                        val type1 = type0.substring(0, type0.lastIndexOf('/'))
                        if (!type1.equals("lua", true)) LOGGER.warn("Currently only Lua is supported")
                        return TitledListY(title, visibilityKey, style).apply {
                            add(CodeEditor(style).apply {
                                setText(value.toString(), false)
                                setOnChangeListener { ce, seq ->
                                    val code = seq.toString()
                                    val func = ScriptComponent.getRawFunction(code)
                                    ce.tooltip = if (func is LuaError) {
                                        func.toString()
                                    } else null
                                    property.set(this, code)
                                }
                            })
                        }
                    }
                    value is ISaveable && ISaveable.getClass(type0) != null -> {
                        return createISaveableInput(title, value, style, property)
                    }
                }

                LOGGER.warn("Missing knowledge to edit $type0, '$title'")

                return TextPanel("?? $title : ${value?.javaClass?.simpleName}, type $type0", style)
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
        var arrayType = property.annotations.firstInstanceOrNull<ListType>()?.valueType
        while (arrayType == null && value.hasNext()) arrayType = getTypeFromSample(value.next())
        if (warnName != null && arrayType == null) warnDetectionIssue(warnName)
        return arrayType
    }

    fun getTypeFromSample(value: Any?): String? {
        value ?: return null
        return when (value) {
            is ISaveable -> value.className
            is Serializable -> value::class.simpleName
            else -> value::class.simpleName
        }
    }

    fun getDefault(type: String): Any? {
        if (type.endsWith("?")) return null
        return when (type) {
            "Byte" -> 0.toByte()
            "Short" -> 0.toShort()
            "Char" -> ' '
            "Int", "Integer" -> 0
            "Long" -> 0L
            "Float" -> 0f
            "Double" -> 0.0
            "String" -> ""
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
            "File", "FileReference", "Reference" -> InvalidRef
            else -> {
                try {// just try it, maybe it works :)
                    ISaveable.create(type)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    private fun Panel.askForReset(property: IProperty<Any?>, callback: (Any?) -> Unit): Panel {
        addOnClickListener { _, _, button, _ ->
            if (button.isRight) {
                // todo option to edit the parent... how will that work?
                Menu.openMenu(windowStack, listOf(MenuOption(NameDesc("Reset")) {
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

    fun instanceOf(clazz: KClass<*>, clazzName: String): Boolean {
        if (clazz.simpleName == clazzName || clazz.qualifiedName == clazzName) return true
        if (clazz.superclasses.any { instanceOf(it, clazzName) }) return true
        return false
    }

    fun instanceOf(clazz: KClass<*>, parent: KClass<*>): Boolean {
        if (clazz == parent) return true
        if (clazz.superclasses.any { instanceOf(it, parent) }) return true
        return false
    }

    fun instanceOf(sample: Any, clazzName: String): Boolean {
        return instanceOf(sample::class, clazzName)
    }

}