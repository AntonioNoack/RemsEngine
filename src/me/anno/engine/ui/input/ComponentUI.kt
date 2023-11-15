package me.anno.engine.ui.input

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
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.IProperty
import me.anno.engine.ui.AssetImport
import me.anno.engine.ui.DetectiveWriter
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.input.Key
import me.anno.io.ISaveable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.inner.temporary.InnerTmpFile
import me.anno.io.json.generic.JsonFormatter
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths
import me.anno.studio.Inspectable
import me.anno.studio.StudioBase
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.constraints.SizeLimitingContainer
import me.anno.ui.base.groups.PanelList2D
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.groups.TitledListY
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.code.CodeEditor
import me.anno.ui.editor.files.FileExplorerEntry
import me.anno.ui.editor.files.FileExplorerOption
import me.anno.ui.input.*
import me.anno.utils.Color
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.OS
import me.anno.utils.strings.StringHelper.camelCaseToTitle
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull
import me.anno.utils.structures.tuples.MutablePair
import me.anno.utils.types.AnyToFloat
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
        FileExplorerOption(NameDesc("Open Scene")) { _, files ->
            for (file in files) {
                ECSSceneTabs.open(
                    file, PlayMode.EDITING,
                    setActive = (file == files.first())
                )
            }
        },
        FileExplorerOption(
            NameDesc(
                "Deep-Copy-Import", "Make this file [mutable, project-indexed] by transferring it to the project",
                ""
            )
        ) { _, files -> AssetImport.deepCopyImport(StudioBase.workspace, files, null) },
        FileExplorerOption(
            NameDesc(
                "Shallow-Copy-Import", "Make this file [mutable, project-indexed] by transferring it to the project",
                ""
            )
        ) { _, files -> AssetImport.shallowCopyImport(StudioBase.workspace, files, null) },

        // todo test whether this actually works
        // (on both Windows and Linux)
        FileExplorerOption(
            NameDesc(
                "Link", "Make this file [project-indexed] by linking it to the project",
                ""
            )
        ) { panel, files ->
            val file = files.first()
            // todo ask file instead
            val ext = if (OS.isWindows) "url" else "desktop"
            Menu.askName(panel.windowStack, NameDesc(),
                if (file.isDirectory) file.name
                else "${file.nameWithoutExtension}.$ext",
                NameDesc("Link"), { -1 }, { newName ->
                    val dst = StudioBase.workspace.getChild(newName)
                    if (file != dst) {
                        AssetImport.createLink(file, dst)
                    } else LOGGER.warn("Cannot link file to itself")
                })
        }
    )

    // todo position control+x is not working (reset on right click is working)

    fun List<*>.writeTo(array: Any): Any {
        val dstSize0 = when (array) {
            is ByteArray -> array.size
            is ShortArray -> array.size
            is IntArray -> array.size
            is LongArray -> array.size
            is FloatArray -> array.size
            is DoubleArray -> array.size
            is Array<*> -> array.size
            else -> throw NotImplementedError()
        }
        val dst = if (dstSize0 != size) {
            java.lang.reflect.Array.newInstance(array.javaClass.componentType, size)
        } else array
        when (dst) {
            is ByteArray -> {
                for (i in indices) {
                    dst[i] = this[i] as Byte
                }
            }
            is ShortArray -> {
                for (i in indices) {
                    dst[i] = this[i] as Short
                }
            }
            is IntArray -> {
                for (i in indices) {
                    dst[i] = this[i] as Int
                }
            }
            is LongArray -> {
                for (i in indices) {
                    dst[i] = this[i] as Long
                }
            }
            is FloatArray -> {
                for (i in indices) {
                    dst[i] = this[i] as Float
                }
            }
            is DoubleArray -> {
                for (i in indices) {
                    dst[i] = this[i] as Double
                }
            }
            is Array<*> -> {
                @Suppress("UNCHECKED_CAST")
                dst as Array<Any?>
                for (i in indices) {
                    dst[i] = this[i]
                }
            }
            else -> throw NotImplementedError()
        }
        return dst
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
                        property.set(this, values.writeTo(value))
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
                        property.set(this, values.associate { it.first to it.second }.toMutableMap())
                    }
                }.apply {
                    setValues(value.map { MutablePair(it.key, it.value) })
                }
            }

            // todo tables for structs? (database table type)

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
                return TextPanel("?? $title, ${if (value != null) value::class else null}", style)
            }
        }

        return createUIByTypeName(name, visibilityKey, property, type1, range, style)
    }

    fun createISaveableInput(title: String, saveable: ISaveable, style: Style, property: IProperty<Any?>): Panel {
        // if saveable is Inspectable, we could use the inspector as well :)
        // runtime: O(n²) where n is number of properties of that class
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
                        JsonStringWriter.toText(saveable, InvalidRef)
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
        val value0 = JsonFormatter.format(JsonStringWriter.toText(saveable, StudioBase.workspace))
        val input = TextInputML(title, value0, style)
        val textColor = input.base.textColor
        input.addChangeListener {
            if (it == "null") {
                property.set(input, null)
            } else {
                try {
                    val value2 = JsonStringReader.read(it, StudioBase.workspace, false).firstOrNull()
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
                return IntInput(title, visibilityKey, type, style).apply {
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
                return IntInput(title, visibilityKey, type, style).apply {
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
                return IntInput(title, visibilityKey, type, style).apply {
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
                return IntInput(title, visibilityKey, type, style).apply {
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
                    return ColorInput(style, title, visibilityKey, (value as? Int ?: 0).toVecRGBA(), true).apply {
                        property.init(this)
                        askForReset(property) { it as Int; setValue(it.toVecRGBA(), -1, false) }
                        setResetListener { (property.reset(this) as Int).toVecRGBA() }
                        setChangeListener { r, g, b, a, mask ->
                            property.set(this, Color.rgba(r, g, b, a), mask)
                        }
                    }
                } else {
                    val type = Type(default as? Int ?: 0,
                        { Maths.clamp(it.toLong(), range.minInt().toLong(), range.maxInt().toLong()) }, { it })
                    return IntInput(title, visibilityKey, type, style).apply {
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
                val type = Type(default as? UInt ?: 0u,
                    { Maths.clamp(it.toLong(), range.minUInt().toLong(), range.maxUInt().toLong()).toUInt() }, { it })
                return IntInput(title, visibilityKey, type, style).apply {
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
                val type = Type(default as? Long ?: 0L,
                    { Maths.clamp(it.toLong(), range.minLong(), range.maxLong()) }, { it })
                return IntInput(title, visibilityKey, type, style).apply {
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
                val type = Type(default as? ULong ?: 0uL,
                    { Maths.clamp(it.toULong2(), range.minULong(), range.maxULong()) }, { it })
                return IntInput(title, visibilityKey, type, style).apply {
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
                val type = Type(AnyToFloat.getFloat(default, 0f),
                    { Maths.clamp(AnyToFloat.getFloat(it, 0f), range.minFloat(), range.maxFloat()).toDouble() }, { it })
                return FloatInput(title, visibilityKey, type, style).apply {
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
                val type = Type(default as? Double ?: 0.0,
                    { Maths.clamp(it as Double, range.minDouble(), range.maxDouble()) }, { it })
                return FloatInput(title, visibilityKey, type, style).apply {
                    property.init(this)
                    setValue(value as Double, -1, false)
                    setResetListener { property.reset(this).toString() }
                    askForReset(property) { setValue(it as Double, -1, false) }
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
                    addChangeListener { x, y, _, _, mask ->
                        property.set(this, Vector2f(x.toFloat(), y.toFloat()), mask)
                    }
                }
            }
            "Vector3f" -> {
                val type = Type.VEC3.withDefault(default as? Vector3f ?: Vector3f())
                return FloatVectorInput(title, visibilityKey, value as Vector3f, type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector3f, false) }
                    addChangeListener { x, y, z, _, mask ->
                        property.set(this, Vector3f(x.toFloat(), y.toFloat(), z.toFloat()), mask)
                    }
                }
            }
            "Vector4f" -> {
                val type = Type.VEC4.withDefault(default as? Vector4f ?: Vector4f())
                return FloatVectorInput(title, visibilityKey, value as Vector4f, type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector4f, false) }
                    addChangeListener { x, y, z, w, mask ->
                        property.set(this, Vector4f(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat()), mask)
                    }
                }
            }
            "Planef" -> {
                val type = Type.PLANE4.withDefault(default as? Planef ?: Planef())
                return FloatVectorInput(title, visibilityKey, value as Planef, type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Planef, false) }
                    addChangeListener { x, y, z, w, mask ->
                        property.set(this, Planef(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat()), mask)
                    }
                }
            }
            "Vector2d" -> {
                val type = Type.VEC2D.withDefault(default as? Vector2d ?: Vector2d())
                return FloatVectorInput(title, visibilityKey, value as Vector2d, type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector2d, false) }
                    addChangeListener { x, y, _, _, mask ->
                        property.set(this, Vector2d(x, y), mask)
                    }
                }
            }
            "Vector3d" -> {
                val type = Type.VEC3D.withDefault(default as? Vector3d ?: Vector3d())
                return FloatVectorInput(title, visibilityKey, value as Vector3d, type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector3d, false) }
                    addChangeListener { x, y, z, _, mask ->
                        property.set(this, Vector3d(x, y, z), mask)
                    }
                }
            }
            "Vector4d" -> {
                val type = Type.VEC4D.withDefault(default as? Vector4d ?: Vector4d())
                return FloatVectorInput(title, visibilityKey, value as Vector4d, type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector4d, -1, false) }
                    addChangeListener { x, y, z, w, mask ->
                        property.set(this, Vector4d(x, y, z, w), mask)
                    }
                }
            }
            "Planed" -> {
                val type = Type.PLANE4D.withDefault(default as? Planed ?: Planed())
                return FloatVectorInput(title, visibilityKey, value as Planed, type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Planed, false) }
                    addChangeListener { x, y, z, w, mask ->
                        property.set(this, Planed(x, y, z, w), mask)
                    }
                }
            }
            // int vectors
            "Vector2i" -> {
                val type = Type(default as? Vector2i ?: Vector2i(), 2)
                return IntVectorInput(title, visibilityKey, value as Vector2i, type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector2i, false) }
                    addChangeListener { x, y, _, _, mask ->
                        property.set(this, Vector2i(x.toInt(), y.toInt()), mask)
                    }
                }
            }
            "Vector3i" -> {
                val type = Type(default as? Vector3i ?: Vector3i(), 3)
                return IntVectorInput(title, visibilityKey, value as Vector3i, type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector3i, false) }
                    addChangeListener { x, y, z, _, mask ->
                        property.set(this, Vector3i(x.toInt(), y.toInt(), z.toInt()), mask)
                    }
                }
            }
            "Vector4i" -> {
                val type = Type(default as? Vector4i ?: Vector4i(), 4)
                return IntVectorInput(title, visibilityKey, value as Vector4i, type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector4i, -1, false) }
                    addChangeListener { x, y, z, w, mask ->
                        property.set(this, Vector4i(x.toInt(), y.toInt(), z.toInt(), w.toInt()), mask)
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
                    addChangeListener { x, y, z, _, mask ->
                        val q = Vector3f(x.toFloat(), y.toFloat(), z.toFloat()).toQuaternionDegrees()
                        property.set(this, q, mask)
                    }
                }
            }
            "Quaterniond" -> {
                value as Quaterniond
                val type = Type.ROT_YXZ64.withDefault((default as Quaterniond).toEulerAnglesDegrees())
                return FloatVectorInput(title, visibilityKey, value.toEulerAnglesDegrees(), type, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue((it as Quaterniond).toEulerAnglesDegrees(), false) }
                    addChangeListener { x, y, z, _, mask ->
                        property.set(this, Vector3d(x, y, z).toQuaternionDegrees(), mask)
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
                    val power = Maths.clamp(ln(length) / ln(maxPower) * 0.5f + 0.5f, 0f, 1f)
                    return Vector4f(b.x, b.y, b.z, power)
                }

                fun l2b(l: Vector4f): Vector3f {
                    val power = maxPower.pow(l.w * 2f - 1f)
                    return Vector3f(l.x, l.y, l.z).mul(power)
                }
                return object : ColorInput(style, title, visibilityKey, b2l(value), type0 == "Color3HDR") {
                    override fun onCopyRequested(x: Float, y: Float): String? {
                        if (type0 == "Color3") return super.onCopyRequested(x, y)
                        val v = l2b(this.value)
                        return "vec3(${v.x},${v.y},${v.z})"
                    }
                }.apply {
                    property.init(this)
                    // todo reset listener for color inputs
                    // todo brightness should have different background than alpha
                    // setResetListener { property.reset(this) }
                    askForReset(property) { setValue(b2l(it as Vector3f), -1, false) }
                    setChangeListener { r, g, b, a, mask ->
                        property.set(this, l2b(Vector4f(r, g, b, a)), mask)
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
                        askForReset(property) { setValue(it as Vector4f, -1, false) }
                        setChangeListener { r, g, b, a, mask ->
                            property.set(this, Vector4f(r, g, b, a), mask)
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
                    panel.add(
                        FloatVectorInput("", visibilityKey, value.getRow(i, Vector4f()), Type.VEC4, style)
                            .apply {
                                // todo correct change listener
                                addChangeListener { x, y, z, w, _ ->
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
                val typeMin = Type.VEC3.withDefault(default.getMin())
                val pane = TitledListY(title, visibilityKey, style)
                pane.add(FloatVectorInput("", visibilityKey, value.getMin(), typeMin, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue((it as AABBf).getMin(), false) }
                    addChangeListener { x, y, z, _, mask ->
                        value.setMin(x.toFloat(), y.toFloat(), z.toFloat())
                        property.set(this, value, mask.and(7))
                    }
                })
                val typeMax = Type.VEC3D.withDefault(default.getMax())
                pane.add(FloatVectorInput("", visibilityKey, value.getMax(), typeMax, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue((it as AABBf).getMax(), false) }
                    addChangeListener { x, y, z, _, mask ->
                        value.setMax(x.toFloat(), y.toFloat(), z.toFloat())
                        property.set(this, value, mask.and(7).shl(3))
                    }
                })
                return pane
            }
            "AABBd" -> {
                value as AABBd
                default as AABBd
                val typeMin = Type.VEC3D.withDefault(default.getMin())
                val pane = TitledListY(title, visibilityKey, style)
                pane.add(FloatVectorInput("", visibilityKey, value.getMin(), typeMin, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue((it as AABBd).getMin(), false) }
                    addChangeListener { x, y, z, _, mask ->
                        value.setMin(x, y, z)
                        property.set(this, value, mask.and(3))
                    }
                })
                val typeMax = Type.VEC3D.withDefault(default.getMax())
                pane.add(FloatVectorInput("", visibilityKey, value.getMax(), typeMax, style).apply {
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue((it as AABBd).getMax(), false) }
                    addChangeListener { x, y, z, _, mask ->
                        value.setMax(x, y, z)
                        property.set(this, value, mask.and(7).shl(3))
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
                    setValues((value as? ByteArray)?.asList() ?: emptyList())
                }
            }
            "ShortArray", "Short[]", "short[]" -> {
                return object : AnyArrayPanel(title, visibilityKey, "Short", style) {
                    override fun onChange() {
                        property.set(this, ShortArray(values.size) { values[it] as Short })
                    }
                }.apply {
                    property.init(this)
                    setValues((value as? ShortArray)?.asList() ?: emptyList())
                }
            }
            "IntArray", "IntegerArray", "Integer[]", "integer[]", "Int[]", "int[]" -> {
                return object : AnyArrayPanel(title, visibilityKey, "Int", style) {
                    override fun onChange() {
                        property.set(this, IntArray(values.size) { values[it] as Int })
                    }
                }.apply {
                    property.init(this)
                    setValues((value as? IntArray)?.asList() ?: emptyList())
                }
            }
            "LongArray", "Long[]", "long[]" -> {
                return object : AnyArrayPanel(title, visibilityKey, "Long", style) {
                    override fun onChange() {
                        property.set(this, LongArray(values.size) { values[it] as Long })
                    }
                }.apply {
                    property.init(this)
                    setValues((value as? LongArray)?.asList() ?: emptyList())
                }
            }
            "FloatArray", "Float[]", "float[]" -> {
                return object : AnyArrayPanel(title, visibilityKey, "Float", style) {
                    override fun onChange() {
                        property.set(this, FloatArray(values.size) { values[it] as Float })
                    }
                }.apply {
                    property.init(this)
                    setValues((value as? FloatArray)?.asList() ?: emptyList())
                }
            }
            "DoubleArray", "Double[]", "double[]" -> {
                return object : AnyArrayPanel(title, visibilityKey, "Double", style) {
                    override fun onChange() {
                        property.set(this, DoubleArray(values.size) { values[it] as Double })
                    }
                }.apply {
                    property.init(this)
                    setValues((value as? DoubleArray)?.asList() ?: emptyList())
                }
            }

            "File", "FileReference", "InvalidReference" -> {
                value as FileReference
                // todo if resource is located here, and we support the type, allow editing here directly
                //  (Materials), #fileInputRightClickOptions
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

            // todo tables for structs?
            // todo show changed values with bold font or a different text color

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
                                        property.set(this, values.writeTo(value))
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
                                        property.set(this, HashSet(values))
                                    }
                                }.apply {
                                    property.init(this)
                                    setValues(value.toList())
                                }
                            }
                            "Map" -> {
                                val (genericKey, genericValue) = generics.split(',').map { it.trim() }
                                value as Map<*, *>
                                return object : AnyMapPanel(title, visibilityKey, genericKey, genericValue, style) {
                                    override fun onChange() {
                                        property.set(this, HashMap(values.associate { it.first to it.second }))
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
                        val value0 = value as? FileReference ?: InvalidRef
                        val fi = FileInput(title, style, value0, fileInputRightClickOptions).apply {
                            property.init(this)
                            setResetListener {
                                property.reset(this) as? FileReference ?: InvalidRef
                            }
                            setChangeListener { property.set(this, it) }
                        }

                        val ci = TextButton("\uD83D\uDCDA", true, style)
                        ci.setTooltip("Open File from Project")
                        ci.addLeftClickListener { button ->

                            val value1 = property.get() as? FileReference ?: InvalidRef

                            // only just now search the results, so the list is always up-to-date

                            // also use all, which are currently in Material-cache? weird cache abuse
                            // todo also use all, which can be found in currently open FileExplorers (?)

                            val panelList = PanelListY(style)
                            val options = ArrayList<FileReference>()

                            fun createCategory(title: String, options: List<FileReference>) {
                                if (options.isNotEmpty()) {
                                    val optionList = PanelList2D(style)
                                    // title needs to be bold, or sth like that
                                    panelList.add(TextPanel(title, style).apply {
                                        isBold = true
                                        textSize *= 1.5f
                                    })
                                    panelList.add(optionList)
                                    for (option in options) {
                                        optionList.add(object : FileExplorerEntry(option, style) {
                                            override fun updateTooltip() {
                                                // as tooltip texts, show their whole path
                                                super.updateTooltip()
                                                tooltip = "${option.toLocalPath()}\n$tooltip"
                                            }

                                            override fun onDoubleClick(x: Float, y: Float, button: Key) {
                                                super.onDoubleClick(x, y, button)
                                                window?.close()
                                            }
                                        }.addLeftClickListener {
                                            fi.setValue(option, true)
                                        })
                                    }
                                }
                            }

                            val indexedAssets = ECSSceneTabs.project?.assetIndex?.get(type1)
                            if (indexedAssets != null) {
                                createCategory("In Project", indexedAssets.toList())
                            }

                            createCategory("Temporary (Debug Only)", InnerTmpFile.findPrefabs(type1))

                            if (value1 != InvalidRef && value1 !in options) {
                                createCategory("Old Value", listOf(value1))
                            }

                            // todo button to create temporary instance?
                            // todo button to create instance in project
                            // todo search panel
                            val buttons = TextButton("Cancel", style)
                                .addLeftClickListener {
                                    fi.setValue(value1, true)
                                    it.window?.close()
                                }
                            buttons.alignmentX = AxisAlignment.FILL // todo this isn't working
                            buttons.weight = 1f
                            val mainList = SizeLimitingContainer(
                                panelList,
                                Maths.max(button.window!!.width / 3, 200),
                                -1, style
                            )
                            Menu.openMenuByPanels(
                                button.windowStack, NameDesc("Choose $type1"),
                                listOf(ScrollPanelY(mainList, style), buttons)
                            )
                        }

                        val list = PanelListY(style)
                        if (!name.isNullOrBlank()) {
                            list.add(TextPanel(name, style))
                        }
                        list.add(fi)

                        val qi = TextButton("\uD83C\uDFA8", style)
                        qi.setTooltip("Quick-Edit")
                        qi.addLeftClickListener {
                            val source = (property.get() as? FileReference)?.nullIfUndefined()
                            val prefab = PrefabCache[source]
                            if (prefab != null) {
                                // todo pressing save shouldn't necessarily close the window
                                EditorState.selectForeignPrefab(prefab)
                            } else LOGGER.warn("Prefab couldn't be loaded from $source")
                        }

                        fi.addButton(ci)
                        fi.addButton(qi)
                        return list
                    }

                    // actual instance, needs to be local, linked via path
                    // e.g., for physics constraints, events or things like that
                    type0.endsWith("/SameSceneRef", true) -> {
                        val type1 = type0.substring(0, type0.lastIndexOf('/'))
                        value as PrefabSaveable?
                        // todo find the class somehow...
                        val clazz = ISaveable.getClass(type1) ?: throw IllegalStateException("Missing class $type1")
                        return SameSceneRefInput(title, visibilityKey, clazz, value, style)
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
                                    try {
                                        val clazz = javaClass.classLoader.loadClass("me.anno.sdf.ScriptComponent")
                                        val method = clazz.getMethod("getRawFunction", String::class.java)
                                        val func = method.invoke(code)
                                        ce.tooltip = if (func is LuaError) {
                                            func.toString()
                                        } else null
                                    } catch (e: Exception) {
                                        LOGGER.warn("Lua not available?", e)
                                        ce.tooltip = "$e"
                                    }
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

                return TextPanel(
                    "?? $title : ${if (value != null) value::class.simpleName else null}, type $type0",
                    style
                )
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
            "Vector3f", "Color3", "Color3HDR" -> Vector3f()
            "Vector4f", "Color4", "Color4HDR" -> Vector4f()
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
                if (type.endsWith("/FileReference") || type.endsWith("/Reference")) {
                    return InvalidRef
                }
                try {// just try it, maybe it works :)
                    ISaveable.create(type)
                } catch (e: Exception) {
                    LOGGER.warn("Unknown type $type for getDefault()")
                    null
                }
            }
        }
    }

    private fun Panel.askForReset(property: IProperty<Any?>, callback: (Any?) -> Unit): Panel {
        addOnClickListener { _, _, _, button, _ ->
            if (button == Key.BUTTON_RIGHT) {
                // todo option to edit the parent... how will that work?
                val enabled = this !is InputPanel<*> || this.isInputAllowed
                Menu.openMenu(windowStack, listOf(MenuOption(NameDesc("Reset")) {
                    callback(property.reset(this))
                }.setEnabled(enabled, "Instance is immutable, inherit from it!")))
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
            is String -> toLong(10)
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
            is String -> toULong(10)
            else -> throw RuntimeException()
        }
    }

    fun instanceOf(clazz: KClass<*>, parent: KClass<*>): Boolean {
        if (clazz == parent) return true
        return clazz.superclasses.any { instanceOf(it, parent) }
    }
}