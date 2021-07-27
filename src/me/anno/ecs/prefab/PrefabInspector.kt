package me.anno.ecs.prefab

import me.anno.animation.Type
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.prefab.EntityPrefab.Companion.loadPrefab
import me.anno.engine.IProperty
import me.anno.engine.ui.ComponentUI
import me.anno.io.NamedSaveable
import me.anno.io.files.FileReference
import me.anno.io.text.TextWriter
import me.anno.objects.inspectable.Inspectable
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.ui.base.Panel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.base.text.TextStyleable
import me.anno.ui.editor.stacked.Option
import me.anno.ui.editor.stacked.StackPanel
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.TextInput
import me.anno.ui.input.VectorInput
import me.anno.ui.style.Style
import me.anno.utils.process.DelayedTask
import me.anno.utils.structures.StartsWith.startsWith
import me.anno.utils.types.Quaternions.toQuaternionDegrees
import org.apache.logging.log4j.LogManager
import org.joml.Quaterniond
import org.joml.Vector3d

// this can be like a scene (/scene tab)
// show changed values in bold

class PrefabInspector(val reference: FileReference, val prefab: EntityPrefab) {

    constructor(prefab: EntityPrefab) : this(prefab.ownFile, prefab)

    constructor(reference: FileReference) : this(reference, loadPrefab(reference) ?: EntityPrefab())

    val history: ChangeHistory = prefab.history ?: ChangeHistory()
    val changes: MutableList<Change> = ArrayList(prefab.changes ?: emptyList())

    init {

        if (history.isEmpty()) {
            history.put("[]")
        }

        prefab.history = history
        prefab.changes = changes

    }

    // val changes = ArrayList()
    var root = prefab.createInstance()

    // todo if there is a physics components, start it
    // todo only execute it, if the scene is visible/selected
    // todo later: only execute it, if in game mode
    // todo later: control it's speed, and step size

    private val savingTask = DelayedTask {
        addEvent {
            history.put(TextWriter.toText(changes, false))
        }
    }

    fun onChange() {
        savingTask.update()
    }

    fun resetEntity(path: Path) {
        if (changes.removeIf { it is ChangeSetEntityAttribute && it.path == path }) {
            onChange()
        }
    }

    fun resetComponent(path: Path) {
        if (changes.removeIf { it is ChangeSetComponentAttribute && it.path == path }) {
            onChange()
        }
    }

    fun isEntityChanged(path: Path): Boolean {
        val oldChange = changes.firstOrNull { it is ChangeSetEntityAttribute && it.path == path }
        return oldChange != null
    }

    fun changeEntity(path: Path, value: Any?) {
        val oldChange = changes.firstOrNull { it is ChangeSetEntityAttribute && it.path == path }
        if (oldChange == null) {
            changes.add(ChangeSetEntityAttribute(path, value))
        } else {
            oldChange as ChangeSetAttribute
            oldChange.value = value
        }
        onChange()
    }

    fun isComponentChanged(path: Path): Boolean {
        val oldChange = changes.firstOrNull { it is ChangeSetComponentAttribute && it.path == path }
        return oldChange != null
    }

    fun changeComponent(path: Path, value: Any?) {
        val oldChange = changes.firstOrNull { it is ChangeSetComponentAttribute && it.path == path }
        if (oldChange == null) {
            changes.add(ChangeSetComponentAttribute(path, value))
        } else {
            oldChange as ChangeSetAttribute
            oldChange.value = value
        }
        onChange()
    }

    fun inspectComponent(component: Component, list: PanelListY, style: Style) {

        val entity = component.entity!!

        val path0 = entity.pathInRoot(root); path0.add(-1)
        val path = path0.toIntArray()

        // the index may not be set in the beginning
        fun getPath(name: String): Path {
            if (path[path.lastIndex] < 0) {
                path[path.lastIndex] = entity.components.indexOf(component)
            }
            return Path(path, name)
        }

        val prefab = component.prefab

        // create the ui for the component, and also keep track of the changes :)
        /*list.add(BooleanInput(
            "Is Enabled", "When a component is disabled, its functions won't be called.",
            component.isEnabled, true, style
        ).apply {
            setBold(isComponentChanged(getPath("isEnabled")))
            setChangeListener { setBold(); changeComponent(getPath("isEnabled"), it); component.isEnabled = it }
            setResetListener {
                unsetBold(); resetComponent(getPath("isEnabled"))
                component.isEnabled = prefab?.isEnabled ?: true; component.isEnabled
            }
        })*/
        list.add(TextInput("Name", "", component.name, style).apply {
            setBold(isComponentChanged(getPath("name")))
            setChangeListener { setBold(); changeComponent(getPath("name"), it); component.name = it }
            setResetListener {
                unsetBold(); resetComponent(getPath("name"))
                component.name = prefab?.name ?: ""; component.name
            }
        })
        list.add(TextInput("Description", "", component.description, style).apply {
            setChangeListener { setBold(); changeComponent(getPath("description"), it); component.description = it }
            setResetListener {
                unsetBold(); resetComponent(getPath("description"))
                component.description = prefab?.description ?: ""; component.description
            }
        })

        // for debugging
        list.add(TextButton("Copy", false, style).setSimpleClickListener {
            LOGGER.info("Copy: ${TextWriter.toText(component, false)}")
        })

        // bold/non bold for other properties

        val reflections = component.getReflections()
        for ((clazz, propertyNames) in reflections.propertiesByClass.value.reversed()) {

            if (clazz == NamedSaveable::class) continue

            val className = clazz.simpleName
            list.add(TextPanel(className ?: "Anonymous", style).apply {
                textColor = textColor and 0x7fffffff
                focusTextColor = textColor
                setItalic()
            })

            for (name in propertyNames) {

                val property = reflections.allProperties[name]!!
                if (property.hideInInspector) continue

                // todo mesh input, skeleton selection, animation selection, ...

                // todo more indentation?

                try {

                    val panel = ComponentUI.createUI2(name, name, object : IProperty<Any?> {

                        override fun init(panel: Panel?) {
                            (panel as? TextStyleable)?.setBold(isComponentChanged(getPath(name)))
                        }

                        override fun getDefault(): Any? {
                            // info("default of $name: ${component.getDefaultValue(name)}")
                            return component.getDefaultValue(name)
                        }

                        override fun set(panel: Panel?, value: Any?) {
                            (panel as? TextStyleable)?.setBold()
                            // info("setting value of $name, ${panel is TextStyleable}")
                            property[component] = value
                            changeComponent(getPath(name), value)
                        }

                        override fun get(): Any? {
                            return property[component]
                        }

                        override fun reset(panel: Panel?): Any? {
                            (panel as? TextStyleable)?.unsetBold()
                            // info("reset $name")
                            resetComponent(getPath(name))
                            val value = getDefault()
                            property[component] = value
                            return value
                        }

                        override val annotations: List<Annotation>
                            get() = property.annotations

                    }, property.range, style) ?: continue
                    list.add(panel)

                } catch (e: Exception) {
                    LOGGER.error("Error $e from ${reflections.clazz}/$name")
                    e.printStackTrace()
                }

            }

        }
    }

    fun inspectEntity(entity: Entity, list: PanelListY, style: Style) {

        val path = entity.pathInRoot(root).toIntArray()
        val prefab = entity.prefab

        // LOGGER.info("inspecting entity ${entity.name}, hierarchy: ${entity.listOfAll.joinToString { it.name }} -> path [${path.joinToString()}]")

        fun getPath(name: String) = Path(path, name)

        list.add(BooleanInput("Is Enabled", entity.isEnabled, prefab?.isEnabled ?: true, style).apply {
            setBold(isEntityChanged(getPath("isEnabled")))
            setChangeListener { setBold(); changeEntity(getPath("isEnabled"), it); entity.isEnabled = it }
            setResetListener {
                unsetBold(); resetEntity(getPath("isEnabled"))
                entity.isEnabled = prefab?.isEnabled ?: true; entity.isEnabled
            }
        })

        list.add(TextInput("Name", "", true, entity.name, style).apply {
            setBold(isEntityChanged(getPath("name")))
            setChangeListener { setBold(); changeEntity(getPath("name"), it); entity.name = it }
            setResetListener {
                unsetBold(); resetEntity(getPath("name"))
                entity.name = prefab?.name ?: ""; entity.name
            }
        })

        list.add(TextInput("Description", "", true, entity.description, style).apply {
            setBold(isEntityChanged(getPath("description")))
            setChangeListener { setBold(); changeEntity(getPath("description"), it); entity.description = it }
            setResetListener {
                unsetBold(); resetEntity(getPath("description"))
                entity.description = prefab?.description ?: ""; entity.description
            }
        })

        val transform = entity.transform
        list.add(VectorInput(
            "Position", "Where it's located relative to its parent",
            transform.localPosition, Type.POSITION, style
        ).apply {
            setBold(isEntityChanged(getPath("position")))
            setChangeListener { x, y, z, _ ->
                setBold(); changeEntity(getPath("position"), Vector3d(x, y, z))
                transform.localPosition = Vector3d(x, y, z)
            }
            setResetListener {
                unsetBold(); resetEntity(getPath("position"));
                transform.localPosition = prefab?.transform?.localPosition ?: Vector3d()
                transform.localPosition
            }
        })

        list.add(VectorInput(
            "Rotation", "How its rotated relative to its parent",
            transform.localRotation, Type.ROT_YXZ, style
        ).apply {
            setBold(isEntityChanged(getPath("rotation")))
            setChangeListener { x, y, z, _ ->
                setBold(); changeEntity(getPath("rotation"), Vector3d(x, y, z).toQuaternionDegrees())
                transform.localRotation = Vector3d(x, y, z).toQuaternionDegrees()
            }
            setResetListener {
                unsetBold(); resetEntity(getPath("rotation"))
                transform.localRotation = prefab?.transform?.localRotation ?: Quaterniond()
                transform.localRotation
            }
        })

        list.add(VectorInput(
            "Scale", "How large the entity is relative to its parent",
            entity.transform.localScale, Type.SCALE, style
        ).apply {
            setBold(isEntityChanged(getPath("scale")))
            setChangeListener { x, y, z, _ ->
                setBold(); changeEntity(getPath("scale"), Vector3d(x, y, z))
                transform.localScale = Vector3d(x, y, z)
            }
            setResetListener {
                unsetBold(); resetEntity(getPath("scale"))
                transform.localScale = prefab?.transform?.localScale ?: Vector3d(1.0)
                transform.localScale
            }
        })

        list.add(TextButton("Copy", false, style).apply {
            setSimpleClickListener {
                for ((index, change) in changes.withIndex()) {
                    println("[$index] ${TextWriter.toText(change, false)}")
                }
            }
        })

        list.add(TextButton("Save", false, style).setSimpleClickListener {
            save()
        })

        // ("creating stack panel from ${entity.components.size} components inside ${entity.name}")
        list.add(object : StackPanel(
            "Components", "Behaviours and properties",
            Component.getComponentOptions(entity), entity.components, style
        ) {

            override fun onAddComponent(component: Inspectable, index: Int) {
                component as Component
                addComponent(entity, component, index)
            }

            override fun onRemoveComponent(component: Inspectable) {
                component as Component
                removeComponent(entity, component)
            }

            override fun getOptionFromInspectable(inspectable: Inspectable): Option {
                inspectable as Component
                return Option(inspectable.className, "") { inspectable }
            }

        })

    }


    /**
     * renumber all changes, which are relevant to the components
     * */
    private fun renumber(from: Int, delta: Int, path: IntArray) {
        val targetSize = path.size
        val changedArrays = HashSet<IntArray>()
        for (change in changes) {
            val path2 = change.path!!
            val hierarchy = path2.hierarchy
            if (change is ChangeSetComponentAttribute &&
                hierarchy.size == targetSize &&
                hierarchy[targetSize - 1] >= from &&
                hierarchy !in changedArrays &&
                hierarchy.startsWith(path)
            ) {
                hierarchy[targetSize - 1] += delta
                changedArrays.add(hierarchy)
            }
        }
    }

    fun addComponent(entity: Entity, component: Component, index: Int) {

        val path = entity.pathInRoot(root).toIntArray()
        val prefab = entity.prefab

        component.entity = entity

        if (prefab != null && index < prefab.components.size) {
            // if index < prefab.size, then disallow
            throw RuntimeException("Cannot insert between prefab components!")
        }
        if (index < entity.components.size) {
            renumber(index, +1, path)
        }
        entity.addComponent(index, component)
        // just append it :)
        changes.add(ChangeAddComponent(Path(path), component.className))
        // if it contains any changes, we need to apply them
        val base = Component.create(component.className)
        val compPath = path + index

        /*var clazz: KClass<*> = component::class
        while (true) {
            val reflections = ISaveable.getReflections(clazz)
            for (name in reflections.declaredProperties.keys) {
                val value = component[name]
                if (value != base[name]) {
                    changes.add(ChangeSetComponentAttribute(Path(compPath, name), value))
                }
            }
            clazz = clazz.superclasses.firstOrNull() ?: break
        }*/

        for (name in component.getReflections().allProperties.keys) {
            val value = component[name]
            if (value != base[name]) {
                changes.add(ChangeSetComponentAttribute(Path(compPath, name), value))
            }
        }

    }

    fun removeComponent(entity: Entity, component: Component) {

        val path = entity.pathInRoot(root).toIntArray()
        val prefab = entity.prefab

        if (component !in entity.components) return
        // done :)

        val index = component.components.indexOf(component)
        if (prefab != null && index < prefab.components.size) {

            // original component, cannot be removed
            component.isEnabled = false

        } else {

            // when a component is deleted, its changes need to be deleted as well
            val compPath = path + index
            changes.removeIf { it is ChangeSetComponentAttribute && it.path!!.hierarchy.contentEquals(compPath) }

            if (index + 1 < entity.components.size) {
                // not the last one
                renumber(index + 1, -1, path)
            }

            // it's ok, and fine
            // remove the respective change
            entity.remove(component)
            // not very elegant, but should work...
            // correct?
            changes.removeIf { it.path!!.hierarchy.size == path.size && it is ChangeAddComponent }
            val i0 = (prefab?.components?.size ?: 0)
            for (i in i0 until entity.components.size) {
                changes.add(i - i0, ChangeAddComponent(Path(path), entity.components[i].className))
            }

        }

    }

    fun save() {
        TextWriter.save(prefab, false, reference)
    }

    companion object {

        private val LOGGER = LogManager.getLogger(PrefabInspector::class)

        init {
            LogManager.disableLogger("FBStack")
        }

        var currentInspector: PrefabInspector? = null

    }

}
