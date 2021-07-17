package me.anno.ecs

import me.anno.ecs.prefab.PrefabComponent1
import me.anno.engine.ui.ComponentUI
import me.anno.io.ISaveable
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
import me.anno.ui.editor.stacked.Option
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.TextInput
import me.anno.ui.style.Style
import me.anno.utils.LOGGER
import me.anno.utils.strings.StringHelper
import me.anno.utils.structures.lists.UpdatingList

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

    override fun isDefaultValue(): Boolean = false

    open fun onDrawGUI() {}

    open fun onClick() {}

    open fun onChangeProperty(name: String, value: Any?) {}

    // automatic property inspector by reflection
    // property inspector annotations, e.g. Range, ExecuteInEditMode, HideInInspector,
    // todo GraphicalValueTracker

    @SerializedProperty
    val changedPropertiesInInstance = HashSet<String>()

    @NotSerializedProperty
    val components
        get() = entity!!.components

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

    // todo stack-panel class with enable/disable buttons

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
        list.add(TextInput("Name", "", style, name).setChangeListener { name = it })
        list.add(TextInput("Description", "", style, name).setChangeListener { description = it })

        // for debugging
        list.add(TextButton("Copy", false, style).setSimpleClickListener {
            LOGGER.info("Copy: ${TextWriter.toText(this, false)}")
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
        return getSuperParent()[name]
    }

    fun resetProperty(name: String): Any? {
        // how do we find the default value, if the root is null? -> create an empty copy
        val parent = ComponentCache.get(superComponent) ?: this::class.java.getConstructor().newInstance()
        val reflections = getReflections()
        val defaultValue = reflections[parent, name]
        reflections[this, name] = defaultValue
        changedPropertiesInInstance.remove(name)
        LOGGER.info("Reset $className/$name to $defaultValue")
        return defaultValue
    }

    // todo instead of using reflection on all properties, we just need to save the prefab and all changed properties

    // todo system to quickly load the scene from multiple files:
    //  - use zipping for a shipped game -> faster file load speed and only a single file access
    //  - just do it serially, it's not that much data

    companion object {

        fun create(type: String) = ISaveable.objectTypeRegistry[type]!!.generator() as Component

        fun getComponentOptions(): List<Option> {
            // registry over all options... / todo search the raw files + search all scripts
            val knownComponents = ISaveable.objectTypeRegistry.filterValues { it.sampleInstance is Component }
            return UpdatingList {
                knownComponents.map {
                    Option(StringHelper.splitCamelCase(it.key), "") {
                        it.value.generator() as Component
                    }
                }.sortedBy { it.title }
            }
        }

        fun getPrefabComponentOptions(): List<Option> {
            // registry over all options... / todo search the raw files + search all scripts
            val knownComponents = ISaveable.objectTypeRegistry.filterValues { it.sampleInstance is Component }
            return UpdatingList {
                knownComponents.map {
                    Option(StringHelper.splitCamelCase(it.key), "") {
                        PrefabComponent1(it.value.generator() as Component)
                    }
                }.sortedBy { it.title }
            }
        }

    }

}