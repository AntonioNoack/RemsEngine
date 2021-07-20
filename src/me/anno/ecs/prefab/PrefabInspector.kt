package me.anno.ecs.prefab

import me.anno.animation.Type
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.engine.IProperty
import me.anno.engine.ui.ComponentUI
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.objects.inspectable.Inspectable
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.ui.base.Panel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.TextStyleable
import me.anno.ui.editor.stacked.Option
import me.anno.ui.editor.stacked.StackPanel
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.TextInput
import me.anno.ui.input.VectorInput
import me.anno.ui.style.Style
import me.anno.utils.LOGGER
import me.anno.utils.process.DelayedTask
import me.anno.utils.structures.StartsWith.startsWith
import me.anno.utils.types.Quaternions.toQuaternionDegrees
import org.apache.logging.log4j.LogManager
import org.joml.Vector3d

// todo this can be like a scene (/scene tab)

// todo show changed values in bold

class PrefabInspector(val reference: FileReference) {

    val changes: MutableList<Change>
    val history: ChangeHistory

    init {
        val (c, h) = loadChanges(reference)
        changes = c.toMutableList()
        history = h
        if (history.isEmpty()) {
            history.put("[]")
        }
    }

    // val changes = ArrayList()
    var root = createInstance(changes)

    private val savingTask = DelayedTask {
        addEvent {
            history.put(TextWriter.toText(changes, false))
        }
    }

    // keeps track of an entity
    // todo can save and load entities from prefabs

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

        // todo create the ui for the component, and also keep track of the changes :)
        list.add(BooleanInput(
            "Is Enabled", "When a component is disabled, its functions won't be called.",
            component.isEnabled, true, style
        ).apply {
            setBold(isComponentChanged(getPath("isEnabled")))
            setChangeListener { setBold(); changeComponent(getPath("isEnabled"), it); component.isEnabled = it }
            setResetListener { unsetBold(); resetComponent(getPath("isEnabled")); prefab?.isEnabled ?: true }
        })
        list.add(TextInput("Name", "", component.name, style).apply {
            setBold(isComponentChanged(getPath("name")))
            setChangeListener { setBold(); changeComponent(getPath("name"), it); component.name = it }
            setResetListener { unsetBold(); resetComponent(getPath("name")); prefab?.name ?: "" }
        })
        list.add(TextInput("Description", "", component.description, style).apply {
            setChangeListener { setBold(); changeComponent(getPath("description"), it); component.description = it }
            setResetListener { unsetBold(); resetComponent(getPath("description")); prefab?.description ?: "" }
        })

        // for debugging
        list.add(TextButton("Copy", false, style).setSimpleClickListener {
            LOGGER.info("Copy: ${TextWriter.toText(component, false)}")
        })

        // todo bold/non bold for other properties

        val reflections = component.getReflections()
        for ((name, property) in reflections.properties) {

            if (property.hideInInspector) continue

            // todo mesh input, skeleton selection, animation selection, ...

            val panel = ComponentUI.createUI2(name, name, object : IProperty<Any?> {

                override fun init(panel: Panel?) {
                    (panel as? TextStyleable)?.setBold(isComponentChanged(getPath(name)))
                }

                override fun getDefault(): Any? {
                    println("default of $name: ${component.getDefaultValue(name)}")
                    return component.getDefaultValue(name)
                }

                override fun set(panel: Panel?, value: Any?) {
                    (panel as? TextStyleable)?.setBold()
                    println("setting value of $name, ${panel is TextStyleable}")
                    property[component] = value
                    changeComponent(getPath(name), value)
                }

                override fun get(): Any? {
                    return property[component]
                }

                override fun reset(panel: Panel?): Any? {
                    (panel as? TextStyleable)?.unsetBold()
                    println("reset $name")
                    resetComponent(getPath(name))
                    val value = getDefault()
                    property[component] = value
                    return value
                }

                override val annotations: List<Annotation>
                    get() = property.annotations

            }, property.range, style) ?: continue
            list.add(panel)

        }

    }

    fun inspectEntity(entity: Entity, list: PanelListY, style: Style) {

        val path = entity.pathInRoot(root).toIntArray()
        val prefab = entity.prefab

        println("inspecting entity ${entity.name}, hierarchy: ${entity.listOfAll.joinToString { it.name }} -> path [${path.joinToString()}]")

        fun getPath(name: String) = Path(path, name)

        list.add(BooleanInput("Is Enabled", entity.isEnabled, prefab?.isEnabled ?: true, style).apply {
            setBold(isEntityChanged(getPath("isEnabled")))
            setResetListener { unsetBold(); resetEntity(getPath("isEnabled")); prefab?.isEnabled }
            setChangeListener { setBold(); changeEntity(getPath("isEnabled"), it) }
        })

        list.add(TextInput("Name", "", true, entity.name, style).apply {
            setBold(isEntityChanged(getPath("name")))
            setResetListener { unsetBold(); resetEntity(getPath("name")); prefab?.name }
            setChangeListener { setBold(); changeEntity(getPath("name"), it) }
        })

        list.add(TextInput("Description", "", true, entity.description, style).apply {
            setBold(isEntityChanged(getPath("description")))
            setResetListener { unsetBold(); resetEntity(getPath("description")); prefab?.description }
            setChangeListener { setBold(); changeEntity(getPath("description"), it) }
        })

        list.add(VectorInput(
            "Position", "Where it's located relative to its parent",
            entity.transform.localPosition, Type.POSITION, style
        ).apply {
            setBold(isEntityChanged(getPath("position")))
            setResetListener { unsetBold(); resetEntity(getPath("position")); prefab?.transform?.localPosition }
            setChangeListener { x, y, z, _ -> setBold(); changeEntity(getPath("position"), Vector3d(x, y, z)) }
        })

        list.add(VectorInput(
            "Rotation", "How its rotated relative to its parent",
            entity.transform.localRotation, Type.ROT_YXZ, style
        ).apply {
            setBold(isEntityChanged(getPath("rotation")))
            setResetListener { unsetBold(); resetEntity(getPath("rotation")); prefab?.transform?.localRotation }
            setChangeListener { x, y, z, _ ->
                setBold(); changeEntity(getPath("rotation"), Vector3d(x, y, z).toQuaternionDegrees())
            }
        })

        list.add(VectorInput(
            "Scale", "How large the entity is relative to its parent",
            entity.transform.localScale, Type.SCALE, style
        ).apply {
            setBold(isEntityChanged(getPath("scale")))
            setResetListener { unsetBold(); resetEntity(getPath("scale")); prefab?.transform?.localScale }
            setChangeListener { x, y, z, _ -> setBold(); changeEntity(getPath("scale"), Vector3d(x, y, z)) }
        })

        list.add(TextButton("Copy", false, style).apply {
            setSimpleClickListener {
                for((index, change) in changes.withIndex()){
                    println("[$index] ${TextWriter.toText(change, false)}")
                }
            }
        })

        list.add(TextButton("Save", false, style).setSimpleClickListener {
            save()
        })

        println("creating stack panel from ${entity.components.size} components inside ${entity.name}")
        list.add(object : StackPanel(
            "Components", "Behaviours and properties",
            Component.getComponentOptions(entity), entity.components, style
        ) {

            /**
             * renumber all changes, which are relevant to the components
             * */
            fun renumber(from: Int, delta: Int) {
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

            override fun onAddComponent(component: Inspectable, index: Int) {
                component as Component
                component.entity = entity
                if (prefab != null && index < prefab.components.size) {
                    // if index < prefab.size, then disallow
                    throw RuntimeException("Cannot insert between prefab components!")
                }
                if (index < entity.components.size) {
                    renumber(index, +1)
                }
                entity.components.add(index, component)
                // just append it :)
                changes.add(ChangeAddComponent(Path(path), component.className))
                // if it contains any changes, we need to apply them
                val base = Component.create(component.className)
                val compPath = path + index
                for ((name, property) in component.getReflections().properties) {
                    val value = property[component]
                    if (value != property[base]) {
                        changes.add(ChangeSetComponentAttribute(Path(compPath, name), value))
                    }
                }
            }

            override fun onRemoveComponent(component: Inspectable) {
                component as Component
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
                        renumber(index + 1, -1)
                    }

                    // it's ok, and fine
                    // remove the respective change
                    entity.components.removeAt(index)
                    // not very elegant, but should work...
                    // correct?
                    changes.removeIf { it.path!!.hierarchy.size == path.size && it is ChangeAddComponent }
                    val i0 = (prefab?.components?.size ?: 0)
                    for (i in i0 until entity.components.size) {
                        changes.add(i - i0, ChangeAddComponent(Path(path), entity.components[i].className))
                    }

                }
            }

            override fun getOptionFromInspectable(inspectable: Inspectable): Option {
                inspectable as Component
                return Option(inspectable.className, "") { inspectable }
            }

        })

    }

    fun save() {
        saveChanges(reference, changes, history)
    }

    companion object {

        init {
            LogManager.disableLogger("FBStack")
        }

        fun loadChanges(resource: FileReference): Pair<List<Change>, ChangeHistory> {
            val loaded = if (resource != InvalidRef && resource.exists && !resource.isDirectory) {
                // todo if is not a .json file, we need to read it e.g. as a mesh, and such
                try {
                    val read = TextReader.read(resource)
                    val changes = read.filterIsInstance<Change>()
                    for (change in changes) {
                        if (change.path == null) throw RuntimeException("Path of change $change is null, from $resource")
                    }
                    val history = read.filterIsInstance<ChangeHistory>().firstOrNull() ?: ChangeHistory()
                    changes to history
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            } else null
            return loaded ?: emptyList<Change>() to ChangeHistory()
        }

        fun saveChanges(resource: FileReference, changes: List<Change>, history: ChangeHistory) {
            resource.writeText(TextWriter.toText(changes + history, false))
        }

        fun createInstance(resource: FileReference): Entity {
            return createInstance(loadChanges(resource).first)
        }

        fun createInstance(changes: List<Change>): Entity {
            val entity = Entity()
            println("creating entity instance from ${changes.size} changes, ${changes.groupBy { it.className }.map { "${it.value.size}x ${it.key}" }}")
            for (change in changes) {
                change.apply(entity)
            }
            println("created instance '${entity.name}' has ${entity.children.size} children and ${entity.components.size} components")
            return entity
        }

        var currentInspector: PrefabInspector? = null
    }

}
