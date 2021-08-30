package me.anno.ecs.prefab

import me.anno.ecs.Component
import me.anno.ecs.prefab.PrefabCache.loadPrefab
import me.anno.engine.IProperty
import me.anno.engine.ui.ComponentUI
import me.anno.engine.ui.DefaultLayout
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
import me.anno.ui.input.TextInput
import me.anno.ui.style.Style
import me.anno.utils.process.DelayedTask
import me.anno.utils.strings.StringHelper
import me.anno.utils.strings.StringHelper.titlecase
import me.anno.utils.structures.StartsWith.startsWith
import org.apache.logging.log4j.LogManager

// this can be like a scene (/scene tab)
// show changed values in bold

class PrefabInspector(val reference: FileReference, val prefab: Prefab) {

    constructor(prefab: Prefab) : this(prefab.src, prefab)

    constructor(reference: FileReference, classNameIfNull: String) : this(
        reference, loadPrefab(reference) ?: Prefab(classNameIfNull)
    )

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

    fun reset(path: Path) {
        if (changes.removeIf { it is CSet && it.path == path }) {
            onChange()
        }
    }

    fun reset(path: Path, name: String) {
        if (changes.removeIf { it is CSet && it.path == path && it.name == name }) {
            onChange()
        }
    }

    fun isChanged(path: Path): Boolean {
        val oldChange = changes.firstOrNull { it is CSet && it.path == path }
        return oldChange != null
    }

    fun isChanged(path: Path, name: String): Boolean {
        val oldChange = changes.firstOrNull { it is CSet && it.path == path && it.name == name }
        return oldChange != null
    }

    fun change(path: Path, name: String, value: Any?) {
        val oldChange = changes.firstOrNull { it is CSet && it.path == path && it.name == name }
        if (oldChange == null) {
            changes.add(CSet(path, name, value))
        } else {
            oldChange as CSet
            oldChange.value = value
        }
        onChange()
    }

    fun inspect(instance: PrefabSaveable, list: PanelListY, style: Style) {

        val path = instance.pathInRoot2(root, withExtra = false)
        val pathIndices = path.indices


        // the index may not be set in the beginning
        fun getPath(): Path {
            val li = pathIndices.lastIndex
            if (li >= 0 && pathIndices[li] < 0) {
                pathIndices[li] = instance.parent!!.getIndexOf(instance)
            }
            return path
        }

        val prefab = instance.prefab

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

        list.add(TextButton("Select Parent", false, style).setSimpleClickListener {
            DefaultLayout.library.select(instance.parent)
        })

        list.add(TextInput("Name", "", instance.name, style).apply {
            setBold(isChanged(getPath(), "name"))
            setChangeListener { setBold(); change(getPath(), "name", it); instance.name = it }
            setResetListener {
                unsetBold(); reset(getPath(), "name")
                instance.name = prefab?.name ?: ""; instance.name
            }
        })
        list.add(TextInput("Description", "", instance.description, style).apply {
            setChangeListener { setBold(); change(getPath(), "description", it); instance.description = it }
            setResetListener {
                unsetBold(); reset(getPath(), "description")
                instance.description = prefab?.description ?: ""; instance.description
            }
        })

        // for debugging
        list.add(TextButton("Copy", false, style).setSimpleClickListener {
            LOGGER.info("Copy: ${TextWriter.toText(instance, false)}")
        })

        // bold/non bold for other properties

        val reflections = instance.getReflections()
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
                            (panel as? TextStyleable)?.setBold(isChanged(getPath(), name))
                        }

                        override fun getDefault(): Any? {
                            // info("default of $name: ${component.getDefaultValue(name)}")
                            return instance.getDefaultValue(name)
                        }

                        override fun set(panel: Panel?, value: Any?) {
                            (panel as? TextStyleable)?.setBold()
                            // info("setting value of $name, ${panel is TextStyleable}")
                            property[instance] = value
                            change(getPath(), name, value)
                        }

                        override fun get(): Any? {
                            return property[instance]
                        }

                        override fun reset(panel: Panel?): Any? {
                            (panel as? TextStyleable)?.unsetBold()
                            // info("reset $name")
                            reset(getPath(), name)
                            val value = getDefault()
                            property[instance] = value
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

        val types = instance.listChildTypes()
        for (i in types.indices) {
            val type = types[i]

            val options = instance.getOptionsByType(type) ?: continue
            val niceName = instance.getChildListNiceName(type)
            val children = instance.getChildListByType(type)

            // todo all list properties, e.g. children, properties and such
            // todo ofc, children should be hidden, but other material may be important

            // todo get options of any type...

            val nicerName = StringHelper.splitCamelCase(niceName.titlecase())
            list.add(object : StackPanel(
                nicerName, "",
                options, children, style
            ) {

                override fun onAddComponent(component: Inspectable, index: Int) {
                    component as PrefabSaveable
                    addComponent(instance, component, index, type)
                }

                override fun onRemoveComponent(component: Inspectable) {
                    component as PrefabSaveable
                    removeComponent(instance, component, type)
                }

                override fun getOptionFromInspectable(inspectable: Inspectable): Option {
                    inspectable as Component
                    return Option(inspectable.className, "") { inspectable }
                }

            })

        }


    }

    /**
     * renumber all changes, which are relevant to the components
     * */
    private fun renumber(from: Int, delta: Int, path: Path) {
        val targetSize = path.indices.size
        val changedArrays = HashSet<IntArray>()
        for (change in changes) {
            val path2 = change.path
            val indices = path2.indices
            val types = path2.types
            if (change is CSet &&
                indices.size == targetSize &&
                indices[targetSize - 1] >= from &&
                indices !in changedArrays &&
                indices.startsWith(path.indices) &&
                types.startsWith(path.types)
            ) {
                indices[targetSize - 1] += delta
                changedArrays.add(indices)
            }
        }
    }

    fun addEntityChild(parent: PrefabSaveable, prefab: Prefab) {
        if (prefab.clazzName != "Entity") throw IllegalArgumentException("Type must be Entity!")
        val path = parent.pathInRoot2(root, false)
        changes.add(CAdd(path, 'e', "Entity", prefab.name, prefab.src))
    }

    fun addComponent(parent: PrefabSaveable, component: PrefabSaveable, index: Int, type: Char) {

        val path = parent.pathInRoot2(root, false)

        val prefab = parent.prefab

        component.parent = parent

        val prefabComponents = prefab?.getChildListByType(type)
        if (prefab != null && index < prefabComponents!!.size) {
            // if index < prefab.size, then disallow
            throw RuntimeException("Cannot insert between prefab components!")
        }

        val parentComponents = parent.getChildListByType(type)
        if (index < parentComponents.size) {
            renumber(index, +1, path)
        }

        parent.addChildByType(index, type, component)

        // just append it :)
        changes.add(CAdd(path, 'c', component.className, component.name))
        // if it contains any changes, we need to apply them
        val base = Component.create(component.className)
        val compPath = path.added(component.name, index, type)

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
                changes.add(CSet(compPath, name, value))
            }
        }

    }

    fun removeComponent(parent: PrefabSaveable, component: PrefabSaveable, type: Char) {

        val path = parent.pathInRoot2(root, false)

        val prefab = parent.prefab

        val components = parent.getChildListByType(type)
        if (component !in components) return
        // done :)

        val index = components.indexOf(component)
        val prefabComponents = prefab?.getChildListByType(type)
        if (prefab != null && index < prefabComponents!!.size) {

            // original component, cannot be removed
            component.isEnabled = false

        } else {

            // when a component is deleted, its changes need to be deleted as well
            val compPath = path.added(component.name, index, type)
            changes.removeIf { it is CSet && it.path == compPath }

            if (index + 1 < components.size) {
                // not the last one
                renumber(index + 1, -1, path)
            }

            // it's ok, and fine
            // remove the respective change
            parent.deleteChild(component)
            // not very elegant, but should work...
            // correct?

            changes.removeIf { it.path == path && it is CAdd }
            val prefabList = prefab?.getChildListByType(type)
            val i0 = (prefabList?.size ?: 0)
            for (i in i0 until components.size) {
                val componentI = components[i]
                changes.add(i - i0, CAdd(path, type, componentI.className, componentI.name))
            }

        }

    }

    fun save() {
        TextWriter.save(prefab, false, reference)
    }

    override fun toString(): String = TextWriter.toText(prefab, false)

    companion object {

        private val LOGGER = LogManager.getLogger(PrefabInspector::class)

        init {
            LogManager.disableLogger("FBStack")
        }

        var currentInspector: PrefabInspector? = null

    }

}
