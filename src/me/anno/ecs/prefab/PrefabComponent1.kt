package me.anno.ecs.prefab

import me.anno.ecs.Component
import me.anno.engine.ui.ComponentUI
import me.anno.io.ISaveable
import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter
import me.anno.io.text.TextWriter
import me.anno.objects.inspectable.Inspectable
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.TextInput
import me.anno.ui.style.Style
import me.anno.utils.LOGGER

class PrefabComponent1() : NamedSaveable(), Inspectable {

    constructor(component: Component) : this() {
        type = component.className
        // todo copy over all properties?
    }

    var type = ""

    var entity: PrefabEntity1? = null

    // todo inspectable

    // todo import values from real instance

    fun apply(path: String, change: Change0) {
        changes[path] = change
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("type", type)
        for ((path, change) in changes) {
            // self=null, because order is important
            writer.writeObject(null, "c:$path", change)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        if (name.startsWith("c:") && value is Change0) {
            changes[name.substring(2)] = value
        } else super.readObject(name, value)
    }

    /*fun createInstance(): Component {
        val instance: Component = prefab?.createInstance() ?: createEmptyInstance()
        for ((path, change) in changes) {
            change.applyChange(instance, path)
        }
        return instance
    }*/

    fun createEmptyInstance(): Component {
        return ISaveable.objectTypeRegistry[type]!!.generator() as Component
    }

    fun setProperty(path: String, value: Any?) {
        val change = changes[path]
        if (change == null) {
            changes[path] = Change0(ChangeType0.SET_VALUE, value, 0)
        } else change.value = value
    }

    fun reset(path: String) {
        changes.remove(path)
    }

    val changes = HashMap<String, Change0>()

    // todo make this use the changes
    var isEnabled = false

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        // todo create title bar, where you can change the script
        // todo save values to history
        list.add(
            BooleanInput(
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
        val sample = createEmptyInstance()
        for ((name, property) in reflections.properties) {
            val component = ComponentUI.createUI(name, property, this, sample, style) ?: continue
            list.add(component)
        }
    }

    override val className: String = "PrefabComponent"

}


