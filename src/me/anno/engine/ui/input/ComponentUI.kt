package me.anno.engine.ui.input

import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.ExtendableEnum
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Range.Companion.maxLong
import me.anno.ecs.annotations.Range.Companion.minLong
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.DefaultAssets
import me.anno.engine.EngineBase
import me.anno.engine.inspector.IProperty
import me.anno.engine.inspector.Inspectable
import me.anno.engine.ui.AssetImport
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.input.ComponentUIImpl.createAABBdInput
import me.anno.engine.ui.input.ComponentUIImpl.createAABBfInput
import me.anno.engine.ui.input.ComponentUIImpl.createBooleanInput
import me.anno.engine.ui.input.ComponentUIImpl.createByteArrayInput
import me.anno.engine.ui.input.ComponentUIImpl.createByteInput
import me.anno.engine.ui.input.ComponentUIImpl.createDoubleArrayInput
import me.anno.engine.ui.input.ComponentUIImpl.createDoubleInput
import me.anno.engine.ui.input.ComponentUIImpl.createFloatArrayInput
import me.anno.engine.ui.input.ComponentUIImpl.createFloatInput
import me.anno.engine.ui.input.ComponentUIImpl.createIntArrayInput
import me.anno.engine.ui.input.ComponentUIImpl.createIntInput
import me.anno.engine.ui.input.ComponentUIImpl.createLongArrayInput
import me.anno.engine.ui.input.ComponentUIImpl.createShortArrayInput
import me.anno.engine.ui.input.ComponentUIImpl.createShortInput
import me.anno.engine.ui.input.ComponentUIImpl.createVector2dInput
import me.anno.engine.ui.input.ComponentUIImpl.createVector2fInput
import me.anno.engine.ui.input.ComponentUIImpl.createVector3dInput
import me.anno.engine.ui.input.ComponentUIImpl.createVector3fInput
import me.anno.engine.ui.input.ComponentUIImpl.createVector4dInput
import me.anno.engine.ui.input.ComponentUIImpl.createVector4fInput
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.input.Input
import me.anno.input.Key
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.inner.temporary.InnerTmpFile
import me.anno.io.find.DetectiveWriter
import me.anno.io.json.generic.JsonFormatter
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable
import me.anno.io.saveable.UnknownSaveable
import me.anno.language.translation.DefaultNames
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.max
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelList2D
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.groups.SizeLimitingContainer
import me.anno.ui.base.groups.TitledListY
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.code.CodeEditor
import me.anno.ui.editor.files.FileExplorerEntry
import me.anno.ui.editor.files.FileExplorerOption
import me.anno.ui.input.ColorInput
import me.anno.ui.input.EnumInput
import me.anno.ui.input.EnumInput.Companion.enumToNameDesc
import me.anno.ui.input.FileInput
import me.anno.ui.input.FloatVectorInput
import me.anno.ui.input.InputPanel
import me.anno.ui.input.IntInput
import me.anno.ui.input.IntVectorInput
import me.anno.ui.input.NullableInput
import me.anno.ui.input.NumberType
import me.anno.ui.input.TextInput
import me.anno.ui.input.TextInputML
import me.anno.utils.Color
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.OS
import me.anno.utils.Reflections.getParentClasses
import me.anno.utils.algorithms.Recursion
import me.anno.utils.files.LinkCreator
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull2
import me.anno.utils.structures.tuples.MutablePair
import me.anno.utils.types.AnyToLong
import me.anno.utils.types.AnyToVector
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Strings.camelCaseToTitle
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Matrix2d
import org.joml.Matrix2f
import org.joml.Matrix3d
import org.joml.Matrix3f
import org.joml.Matrix3x2d
import org.joml.Matrix3x2f
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
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
import java.io.Serializable
import kotlin.math.ln
import kotlin.math.pow
import kotlin.reflect.KClass

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
        ) { _, files -> AssetImport.deepCopyImport(EngineBase.workspace, files, null) },
        FileExplorerOption(
            NameDesc(
                "Shallow-Copy-Import", "Make this file [mutable, project-indexed] by transferring it to the project",
                ""
            )
        ) { _, files -> AssetImport.shallowCopyImport(EngineBase.workspace, files, null) },

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
            Menu.askName(
                panel.windowStack, NameDesc.EMPTY,
                if (file.isDirectory) file.name
                else "${file.nameWithoutExtension}.$ext",
                NameDesc("Link"), { -1 }, { newName ->
                    val dst = EngineBase.workspace.getChild(newName)
                    if (file != dst) {
                        LinkCreator.createLink(file, dst)
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
            is List<*> -> {
                @Suppress("UNCHECKED_CAST")
                dst as MutableList<Any?>
                for (i in indices) {
                    dst[i] = this[i]
                }
            }
            else -> throw NotImplementedError()
        }
        return dst
    }

    fun createUI2(
        name: String?, visibilityKey: String,
        property: IProperty<Any?>, range: Range?, style: Style
    ): Panel? {

        val title = if (name != null) NameDesc(name.camelCaseToTitle()) else NameDesc.EMPTY

        val type0 = property.annotations.firstInstanceOrNull(me.anno.ecs.annotations.Type::class)?.type
        val type1 = type0 ?: when (val value = property.get()) {

            // native types
            is Boolean,
            is Char,
            is Byte, is Short, is Int, is Long,
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

            // collections and maps
            is List<*> -> {
                val arrayType = getArrayType(value.iterator(), name) ?: return null
                return object : AnyArrayPanel(title, visibilityKey, arrayType, style) {
                    override fun onChange() {
                        property.set(this, values)
                    }
                }.apply { setValues(value.toList()) }
            }
            is Set<*> -> {
                val arrayType = getArrayType(value.iterator(), name) ?: return null
                return object : AnyArrayPanel(title, visibilityKey, arrayType, style) {
                    override fun onChange() {
                        property.set(this, values.toSet())
                    }
                }.apply { setValues(value.toList()) }
            }
            is Map<*, *> -> {
                val keyType = getType(value.keys.iterator(), name) ?: return null
                val valueType = getType(value.values.iterator(), name) ?: return null
                return object : AnyMapPanel(title, visibilityKey, keyType, valueType, style) {
                    override fun onChange() {
                        property.set(this, values.associate { it.first to it.second }.toMutableMap())
                    }
                }.apply {
                    setValues(value.map { MutablePair(it.key, it.value) })
                }
            }
            is Enum<*> -> {
                val values = EnumInput.getEnumConstants(value.javaClass)
                val input = EnumInput.createInput(title.name, value, style)
                input.setChangeListener { _, index, _ -> property.set(input, values[index]) }
                input.askForReset(property) { resetValue ->
                    val nameDesc = enumToNameDesc(resetValue as Enum<*>)
                    input.setValue(nameDesc, false)
                }
                return input
            }
            is Saveable -> return createISaveableInput(title, value, style, property)
            is ExtendableEnum -> return createExtendableEnumInput(title, property, value, style)
            else -> return TextPanel("?? ${title.name}, ${if (value != null) value::class else null}", style)
        }
        return createUIByTypeName(name, visibilityKey, property, type1, range, style)
    }

    private fun createExtendableEnumInput(
        nameDesc: NameDesc, property: IProperty<Any?>,
        value: ExtendableEnum, style: Style
    ): Panel {
        val values = value.values
        val input = EnumInput(nameDesc, value.nameDesc, values.map { it.nameDesc }, style)
        input.setChangeListener { _, index, _ -> property.set(input, values[index]) }
        input.askForReset(property) { resetValue ->
            val resetNameDesc = (resetValue as ExtendableEnum).nameDesc
            input.setValue(resetNameDesc, false)
        }
        return input
    }

    fun createISaveableInput(nameDesc: NameDesc, saveable: Saveable, style: Style, property: IProperty<Any?>): Panel {

        if (saveable is Inspectable) {
            val panel = PanelListY(style.getChild("deep"))
            saveable.createInspector(panel, style)
            return panel
        }

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
                val title2 = nameDesc.name.ifBlank { saveable.className }
                // todo list/array-views should have their content visibility be toggleable
                panel2.add(object : TextPanel(title2, style) {
                    override fun onCopyRequested(x: Float, y: Float): String =
                        JsonStringWriter.toText(saveable, InvalidRef)
                }.apply {
                    // make it look like a title
                    isItalic = true
                    disableFocusColors()
                })
                for ((name, typeValue) in typeMap) {
                    val (type, startValue) = typeValue
                    panel2.add(
                        createUIByTypeName(
                            name, "${saveable.className}/$name",
                            SIProperty(name, type, saveable, startValue, property, detective),
                            type, null, style
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
        val value0 = JsonFormatter.format(JsonStringWriter.toText(saveable, EngineBase.workspace))
        val input = TextInputML(nameDesc, value0, style)
        val textColor = input.base.textColor
        input.addChangeListener {
            if (it == "null") {
                property.set(input, null)
            } else {
                try {
                    val value2 = JsonStringReader.read(it, EngineBase.workspace, false).firstOrNull()
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
        name: String?, visibilityKey: String, property: IProperty<Any?>,
        type0: String, range: Range?, style: Style
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

        val ttt = property.annotations
            .firstInstanceOrNull2(Docs::class)
            ?.description ?: ""
        val title = if (name != null || !ttt.isBlank2()) {
            NameDesc(name?.camelCaseToTitle() ?: "", ttt, "")
        } else NameDesc.EMPTY
        val value = property.get()
        val default = property.getDefault()

        when (type0) {
            // native types
            "Bool", "Boolean" -> return createBooleanInput(title, value, default, property, style)
            "Byte" -> return createByteInput(title, visibilityKey, value, default, property, range, style)
            "Short" -> return createShortInput(title, visibilityKey, value, default, property, range, style)
            "Int", "Integer" -> {
                return if (title.englishName.endsWith("color", true)) {
                    ColorInput(title, visibilityKey, (value as? Int ?: 0).toVecRGBA(), true, style).apply {
                        alignmentX = AxisAlignment.FILL
                        property.init(this)
                        askForReset(property) { it as Int; setValue(it.toVecRGBA(), -1, false) }
                        setResetListener { (property.reset(this) as Int).toVecRGBA() }
                        setChangeListener { r, g, b, a, mask ->
                            property.set(this, Color.rgba(r, g, b, a), mask)
                        }
                    }
                } else createIntInput(title, visibilityKey, value, default, property, range, style)
            }
            "Long" -> {
                val type = NumberType(
                    default as? Long ?: 0L,
                    { clamp(AnyToLong.getLong(it, 0), range.minLong(), range.maxLong()) }, { it })
                return IntInput(title, visibilityKey, type, style).apply {
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
            // todo slider-style? maybe in background for everything with a reasonable range?
            "Float" -> return createFloatInput(title, visibilityKey, value, default, property, range, style)
            "Double" -> return createDoubleInput(title, visibilityKey, value, default, property, range, style)
            "Char" -> {
                return TitledListY(title, visibilityKey, style).add(
                    TextInput(title, visibilityKey, value.toString(), style.getChild("deep")).apply {
                        base.lengthLimit = 1
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
            "Vector2f" -> return createVector2fInput(title, visibilityKey, value, default, property, style)
            "Vector3f" -> return createVector3fInput(title, visibilityKey, value, default, property, style)
            "Vector4f" -> return createVector4fInput(title, visibilityKey, value, default, property, style)
            "Planef" -> {
                val type = NumberType.PLANE4.withDefault(default as? Planef ?: Planef())
                return FloatVectorInput(title, visibilityKey, value as Planef, type, style).apply {
                    alignmentX = AxisAlignment.FILL
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Planef, false) }
                    addChangeListener { x, y, z, w, mask ->
                        property.set(this, Planef(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat()), mask)
                    }
                }
            }
            "Vector2d" -> return createVector2dInput(title, visibilityKey, value, default, property, style)
            "Vector3d" -> return createVector3dInput(title, visibilityKey, value, default, property, style)
            "Vector4d" -> return createVector4dInput(title, visibilityKey, value, default, property, style)
            "Planed" -> {
                val type = NumberType.PLANE4D.withDefault(default as? Planed ?: Planed())
                return FloatVectorInput(title, visibilityKey, value as Planed, type, style).apply {
                    alignmentX = AxisAlignment.FILL
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
                val type = NumberType(default as? Vector2i ?: Vector2i(), 2)
                return IntVectorInput(title, visibilityKey, value as Vector2i, type, style).apply {
                    alignmentX = AxisAlignment.FILL
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector2i, false) }
                    addChangeListener { x, y, _, _, mask ->
                        property.set(this, Vector2i(x.toInt(), y.toInt()), mask)
                    }
                }
            }
            "Vector3i" -> {
                val type = NumberType(default as? Vector3i ?: Vector3i(), 3)
                return IntVectorInput(title, visibilityKey, value as Vector3i, type, style).apply {
                    alignmentX = AxisAlignment.FILL
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue(it as Vector3i, false) }
                    addChangeListener { x, y, z, _, mask ->
                        property.set(this, Vector3i(x.toInt(), y.toInt(), z.toInt()), mask)
                    }
                }
            }
            "Vector4i" -> {
                val type = NumberType(default as? Vector4i ?: Vector4i(), 4)
                return IntVectorInput(title, visibilityKey, value as Vector4i, type, style).apply {
                    alignmentX = AxisAlignment.FILL
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
                val type = NumberType.ROT_YXZ.withDefault((default as Quaternionf).toEulerAnglesDegrees())
                return FloatVectorInput(title, visibilityKey, value.toEulerAnglesDegrees(), type, style).apply {
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
            "Quaterniond" -> {
                value as Quaterniond
                val type = NumberType.ROT_YXZ64.withDefault((default as Quaterniond).toEulerAnglesDegrees())
                return FloatVectorInput(title, visibilityKey, value.toEulerAnglesDegrees(), type, style).apply {
                    alignmentX = AxisAlignment.FILL
                    property.init(this)
                    setResetListener { property.reset(this) }
                    askForReset(property) { setValue((it as Quaterniond).toEulerAnglesDegrees(), false) }
                    addChangeListener { x, y, z, _, mask ->
                        property.set(this, Vector3d(x, y, z).toQuaternionDegrees(), mask)
                    }
                }
            }

            // colors, e.g. for materials
            "Color3" -> {
                val value0 = AnyToVector.getVector4f(value)
                return ColorInput(title, visibilityKey, value0, false, style).apply {
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
            "Color3HDR" -> {
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
                return object : ColorInput(title, visibilityKey, b2l(value), true, style) {
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
            "Color4", "Color4HDR" -> {
                value as Vector4f
                // todo hdr colors per color amplitude
                return ColorInput(title, visibilityKey, value, true, style)
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
                return panel
            }

            // AABBf/AABBd
            "AABBf" -> return createAABBfInput(title, visibilityKey, value, default, property, style)
            "AABBd" -> return createAABBdInput(title, visibilityKey, value, default, property, style)

            // todo smaller matrices, and for double
            // todo when editing a matrix, maybe add a second mode for translation x rotation x scale

            // todo edit native arrays (byte/short/int/float/...) as images

            // native arrays
            // todo char array?
            "ByteArray" -> return createByteArrayInput(title, visibilityKey, value, property, style)
            "ShortArray" -> return createShortArrayInput(title, visibilityKey, value, property, style)
            "IntArray" -> return createIntArrayInput(title, visibilityKey, value, property, style)
            "LongArray" -> return createLongArrayInput(title, visibilityKey, value, property, style)
            "FloatArray" -> return createFloatArrayInput(title, visibilityKey, value, property, style)
            "DoubleArray" -> return createDoubleArrayInput(title, visibilityKey, value, property, style)
            "FileReference" -> {
                value as FileReference
                // todo if resource is located here, and we support the type, allow editing here directly
                //  (Materials), #fileInputRightClickOptions
                return FileInput(title, style, value, fileInputRightClickOptions).apply {
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
            "Inspectable" -> {
                value as Inspectable
                val list = PanelListY(style)
                value.createInspector(list, style)
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
                            "List" -> {
                                value as List<*>
                                return object : AnyArrayPanel(title, visibilityKey, generics, style) {
                                    override fun onChange() {
                                        // todo why is the changing sdf materials not visible until refresh??
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
                            addChangeListener { property.set(this, it) }
                        }

                        val openFromProjectButton = TextButton(NameDesc("\uD83D\uDCDA"), true, style)
                        openFromProjectButton.setTooltip("Open File from Project")
                        openFromProjectButton.addLeftClickListener { button ->
                            val value1 = property.get() as? FileReference ?: InvalidRef
                            chooseFileFromProject(type1, value1, button, style) { option, _ ->
                                fi.setValue(option, true)
                            }
                        }

                        val list = PanelListY(style)
                        if (!name.isNullOrBlank()) {
                            list.add(TextPanel(title, style))
                        }
                        list.add(fi)

                        val quickEditButton = object : TextButton(NameDesc("\uD83C\uDFA8"), style) {
                            override fun onUpdate() {
                                super.onUpdate()
                                isInputAllowed = true // hack, to make this always available
                            }
                        }
                        quickEditButton.setTooltip("Quick-Edit")
                        quickEditButton.addLeftClickListener {
                            val source = (property.get() as? FileReference)?.nullIfUndefined()
                            val prefab = PrefabCache[source].waitFor()?.prefab
                            if (prefab != null) {
                                // todo pressing save shouldn't necessarily close the window
                                EditorState.selectForeignPrefab(prefab)
                            } else LOGGER.warn("Prefab couldn't be loaded from $source")
                        }

                        fi.addButton(openFromProjectButton)
                        fi.addButton(quickEditButton)
                        return list
                    }

                    // actual instance, needs to be local, linked via path
                    // e.g., for physics constraints, events or things like that
                    type0.endsWith("/SameSceneRef", true) -> {
                        val type1 = type0.substring(0, type0.lastIndexOf('/'))
                        value as PrefabSaveable?
                        val clazz = Saveable.getClass(type1)
                        if (clazz != UnknownSaveable::class) {
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
                                        val clazz = javaClass.classLoader.loadClass("me.anno.lua.ScriptComponent")
                                        val method = clazz.getMethod("getRawFunction", String::class.java)
                                        val func = method.invoke(null, code)
                                        val luaErrorClass = javaClass.classLoader.loadClass("org.luaj.vm2.LuaError")
                                        ce.tooltip = if (luaErrorClass.isInstance(func)) {
                                            LOGGER.warn(func.toString())
                                            func.toString()
                                        } else ""
                                    } catch (e: Exception) {
                                        LOGGER.warn("Lua not available?", e)
                                        ce.tooltip = "$e"
                                    }
                                    property.set(this, code)
                                }
                            })
                        }
                    }
                    value is Saveable && Saveable.getClass(type0) != UnknownSaveable::class -> {
                        // todo we're currently wasting quite a lot of space... fix that somehow...
                        //  example: SnapSettings
                        return createISaveableInput(title, value, style, property)
                    }
                }
                LOGGER.warn("Missing knowledge to edit $type0, '${title.name}'")
                return TextPanel(
                    "?? ${title.name} : ${if (value != null) value::class.simpleName else null}, type $type0",
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
    fun getArrayType(value: Iterator<Any?>, warnName: String? = null): String? {
        var arrayType: String? = null
        while (arrayType == null && value.hasNext()) arrayType = getTypeFromSample(value.next())
        if (warnName != null && arrayType == null) warnDetectionIssue(warnName)
        return arrayType
    }

    fun getTypeFromSample(value: Any?): String? {
        value ?: return null
        return when (value) {
            is Saveable -> value.className
            is Serializable -> value::class.simpleName
            else -> value::class.simpleName
        }
    }

    fun Panel.askForReset(property: IProperty<Any?>, callback: (Any?) -> Unit): Panel {
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

    fun chooseFileFromProject(
        type1: String,
        value1: FileReference,
        sampleUI: Panel,
        style: Style,
        callback: (file: FileReference, cancelled: Boolean) -> Unit
    ) {

        // only just now search the results, so the list is always up-to-date

        // also use all, which are currently in Material-cache? weird cache abuse
        // todo also use all, which can be found in currently open FileExplorers (?)

        val panelList = PanelListY(style)
        val options = ArrayList<FileReference>()

        var entrySize = 64f
        val minEntrySize = 16f

        fun createCategory(title: String, options: List<FileReference>) {
            if (options.isNotEmpty()) {
                val optionList = object : PanelList2D(style) {
                    override fun onUpdate() {
                        super.onUpdate()
                        childWidth = entrySize.toInt()
                        childHeight = childWidth
                    }

                    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
                        if (Input.isControlDown) {
                            val newEntrySize = entrySize * (1.05f).pow(dy - dx)
                            entrySize = clamp(
                                newEntrySize,
                                minEntrySize,
                                max(width - spacing * 2f - 1f, minEntrySize)
                            )
                        } else super.onMouseWheel(x, y, dx, dy, byMouse)
                    }
                }
                optionList.childWidth = entrySize.toInt()
                optionList.childHeight = optionList.childWidth
                // title needs to be bold, or sth like that
                panelList.add(TextPanel(title, style).apply {
                    isBold = true
                    textSize *= 1.5f
                })
                panelList.add(optionList)
                for (i in options.indices) {
                    val option = options[i]
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
                        callback(option, false)
                    })
                }
            }
        }

        val indexedAssets = ECSSceneTabs.project?.assetIndex?.get(type1)
        if (indexedAssets != null) {
            createCategory("In Project", indexedAssets.toList())
        }

        val staticAssets = DefaultAssets.assets[type1]
        if (staticAssets != null) {
            createCategory("Default", staticAssets.toList())
        }

        createCategory("Temporary (Debug Only)", InnerTmpFile.findPrefabs(type1))

        if (value1 != InvalidRef && value1 !in options) {
            createCategory("Old Value", listOf(value1))
        }

        // todo button to create temporary instance?
        // todo button to create instance in project
        // todo search panel
        val cancelButton = TextButton(DefaultNames.cancel, style)
            .addLeftClickListener {
                callback(value1, true)
                it.window?.close()
            }
        cancelButton.weight = 1f
        val mainList = SizeLimitingContainer(
            panelList,
            max(sampleUI.window!!.width / 3, 200),
            -1, style
        )
        Menu.openMenuByPanels(
            sampleUI.windowStack, NameDesc("Choose $type1"),
            listOf(ScrollPanelY(mainList, style), cancelButton)
        )
    }

    fun instanceOf(clazz: KClass<*>, tested: KClass<*>): Boolean {
        return Recursion.anyRecursive(clazz) { c, remaining ->
            if (c == tested) true
            else {
                remaining.addAll(getParentClasses(c))
                false
            }
        }
    }
}