package me.anno.ecs

import me.anno.io.NamedSaveable
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.objects.inspectable.Inspectable
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.SettingCategory
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.TextInput
import me.anno.ui.style.Style

abstract class Component : NamedSaveable(), Inspectable {

    @NotSerializedProperty
    open var entity: Entity? = null

    @SerializedProperty
    open var isEnabled = true

    open fun onCreate() {}

    open fun onDestroy() {}

    open fun onBeginPlay() {}

    open fun onUpdate() {}

    open fun onPhysicsUpdate() {}

    override val approxSize get() = 1000
    override fun isDefaultValue(): Boolean = false

    open fun onClick() {}

    // todo automatic property inspector by reflection
    // todo property inspector annotations, e.g. Range, ExecuteInEditMode, HideInInspector, GraphicalValueTracker...

    // todo nice ui inputs for array types and maps

    @SerializedProperty
    val changedPropertiesInInstance = HashSet<String>()

    override fun toString(): String {
        return "$className('$name')"
    }

    fun toString(depth: Int): StringBuilder {
        val builder = StringBuilder()
        for (i in 0 until depth) builder.append('\t')
        builder.append(toString())
        builder.append('\n')
        return builder
    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        // todo create title bar, where you can change the script
        // todo save values to history
        list.add(BooleanInput(
            "Is Enabled", "When a component is disabled, its functions won't be called.",
            isEnabled, style
        ).setChangeListener { isEnabled = it })
        list.add(TextInput("Name", style, name).setChangeListener { name = it })
        list.add(TextInput("Description", style, name).setChangeListener { description = it })
    }

    // todo instead of using reflection on all properties, we just need to save the prefab and all changed properties

    // todo system to quickly load the scene from multiple files:
    //  - use zipping for a shipped game -> faster file load speed and only a single file access
    //  - just do it serially, it's not that much data

}