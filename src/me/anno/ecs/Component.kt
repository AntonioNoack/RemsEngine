package me.anno.ecs

import me.anno.engine.ui.ComponentUI
import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.io.text.TextWriter
import me.anno.objects.inspectable.Inspectable
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.TextInput
import me.anno.ui.style.Style
import me.anno.utils.LOGGER

abstract class Component : NamedSaveable(), Inspectable {

    @NotSerializedProperty
    open var entity: Entity? = null

    @SerializedProperty
    private var superComponent: FileReference = InvalidRef

    @SerializedProperty
    open var isEnabled = true

    open fun onCreate() {}

    open fun onDestroy() {}

    open fun onBeginPlay() {}

    open fun onUpdate() {}

    open fun onPhysicsUpdate() {}

    override val approxSize get() = 1000
    override fun isDefaultValue(): Boolean = false

    open fun onDrawGUI() {}

    open fun onClick() {}

    open fun onChangeProperty(name: String, value: Any?) {}

    // todo automatic property inspector by reflection
    // todo property inspector annotations, e.g. Range, ExecuteInEditMode, HideInInspector, GraphicalValueTracker...

    @SerializedProperty
    val changedPropertiesInInstance = HashSet<String>()

    override fun toString(): String {
        return "$className('$name')"
    }

    override val className: String = javaClass.name

    override fun save(writer: BaseWriter) {
        super.save(writer)
        saveSerializableProperties(writer)
    }

    fun toString(depth: Int): StringBuilder {
        val builder = StringBuilder()
        for (i in 0 until depth) builder.append('\t')
        builder.append(toString())
        builder.append('\n')
        return builder
    }

    // todo stack-panel with enable/disable buttons

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {

        // todo create title bar, where you can change the script
        // todo save values to history
        list.add(BooleanInput(
            "Is Enabled", "When a component is disabled, its functions won't be called.",
            isEnabled, true, style
        ).setChangeListener { isEnabled = it })
        list.add(TextInput("Name", "name", style, name).setChangeListener { name = it })
        list.add(TextInput("Description", "desc", style, name).setChangeListener { description = it })

        list.add(TextButton("Copy", false, style).setSimpleClickListener {
            LOGGER.info("Copy: ${TextWriter.toText(this, true)}")
        })

        val reflections = getReflections()
        for ((name, property) in reflections.properties) {
            val component = ComponentUI.createUI(name, property, this, style) ?: continue
            list.add(component)
        }

    }

    private fun getSuperParent(): Component {
        return ComponentCache.get(superComponent) ?: this::class.java.getConstructor().newInstance()
    }

    fun getDefaultValue(name: String): Any? {
        // how do we find the default value, if the root is null? -> create an empty copy
        val reflections = getReflections()
        return reflections.get(getSuperParent(), name)
    }

    fun resetProperty(name: String): Any? {
        // how do we find the default value, if the root is null? -> create an empty copy
        val parent = ComponentCache.get(superComponent) ?: this::class.java.getConstructor().newInstance()
        val reflections = getReflections()
        val defaultValue = reflections.get(parent, name)
        reflections.set(this, name, defaultValue)
        changedPropertiesInInstance.remove(name)
        LOGGER.info("Reset $className/$name to $defaultValue")
        return defaultValue
    }

    // todo instead of using reflection on all properties, we just need to save the prefab and all changed properties

    // todo system to quickly load the scene from multiple files:
    //  - use zipping for a shipped game -> faster file load speed and only a single file access
    //  - just do it serially, it's not that much data

}