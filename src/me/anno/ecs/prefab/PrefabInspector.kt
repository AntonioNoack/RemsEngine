package me.anno.ecs.prefab

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.prefab.PrefabCache.loadPrefab
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Path
import me.anno.engine.IProperty
import me.anno.engine.ui.ComponentUI
import me.anno.engine.ui.ECSTypeLibrary
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
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
import me.anno.utils.strings.StringHelper.camelCaseToTitle
import org.apache.logging.log4j.LogManager

// todo right click - reset is not available on all elements
//  - float input
//  - vector input: only control + x, not right click
//  - text input: only control + x, not right click

// this can be like a scene (/scene tab)
// show changed values in bold

class PrefabInspector(
    val reference: FileReference,
    val prefab: Prefab
) {

    constructor(prefab: Prefab) : this(prefab.source, prefab)

    constructor(reference: FileReference, classNameIfNull: String) :
            this(reference, loadPrefab(reference) ?: Prefab(classNameIfNull))

    init {
        prefab.ensureMutableLists()
    }

    val history: ChangeHistory = prefab.history ?: ChangeHistory()
    val adds get() = prefab.adds as MutableList
    val sets get() = prefab.sets // as MutableList

    init {

        if (history.isEmpty()) {
            history.put("[]")
        }

        prefab.history = history

    }

    // val changes = ArrayList()
    val root get() = prefab.getSampleInstance()

    // done if there is a physics components, start it
    // todo only execute it, if the scene is visible/selected
    // todo later: only execute it, if in game mode
    // todo later: control it's speed, and step size

    private val savingTask = DelayedTask {
        addEvent { history.put(TextWriter.toText(adds + sets.map { k1, k2, v -> CSet(k1, k2, v) })) }
    }

    fun onChange() {
        savingTask.update()
    }

    fun reset(path: Path) {
        if (!prefab.isWritable) throw ImmutablePrefabException(prefab.source)
        // if (sets.removeIf { it.path == path }) {
        if (sets.removeMajorIf { it == path }) {
            prefab.invalidate()
            onChange()
            ECSSceneTabs.updatePrefab(prefab)
        }
    }

    fun reset(path: Path, name: String) {
        if (!prefab.isWritable) throw ImmutablePrefabException(prefab.source)
        // if (sets.removeIf { it.path == path && it.name == name }) {
        if (sets.contains(path, name)) {
            sets.remove(path, name)
            prefab.invalidate()
            onChange()
            ECSSceneTabs.updatePrefab(prefab)
        }
    }

    /*fun isChanged(path: Path): Boolean {
        val oldChange = sets.firstOrNull { it.path == path }
        return oldChange != null
    }*/

    fun isChanged(path: Path, name: String): Boolean {
        val oldChange = sets[path, name] // .firstOrNull { it.path == path && it.name == name }
        return oldChange != null
    }

    fun change(path: Path, name: String, value: Any?) {
        prefab.set(path, name, value)
        onChange()
    }

    fun inspect(instance: PrefabSaveable, list: PanelListY, style: Style) {

        if (instance.prefabRoot !== root)
            LOGGER.warn(
                "Component ${instance.name}:${instance.className} " +
                        "is not part of tree ${root.name}:${root.className}, " +
                        "its root is ${instance.root.name}:${instance.root.className}"
            )
        val path = instance.pathInRoot2(instance.prefabRoot, withExtra = false)
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

        list.add(TextButton("Select Parent", false, style).addLeftClickListener {
            ECSTypeLibrary.select(instance.parent)
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
        list.add(TextButton("Copy", false, style).addLeftClickListener {
            LOGGER.info("Copy: ${TextWriter.toText(instance)}")
        })

        // bold/non bold for other properties

        val reflections = instance.getReflections()
        for ((clazz, propertyNames) in reflections.propertiesByClass.value.reversed()) {

            var hadIntro = false
            for (name in propertyNames) {

                val property = reflections.allProperties[name]!!
                if (property.hideInInspector || !property.serialize) continue

                if (!hadIntro) {
                    hadIntro = true
                    val className = clazz.simpleName
                    list.add(TextPanel(className ?: "Anonymous", style).apply {
                        textColor = textColor and 0x7fffffff
                        focusTextColor = textColor
                        setItalic()
                    })
                }

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
                    if (panel.tooltip != null) panel.setTooltip(property.description)
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

            val nicerName = niceName.camelCaseToTitle()
            list.add(object : StackPanel(
                nicerName, "",
                options, children, style
            ) {

                override fun onAddComponent(component: Inspectable, index: Int) {
                    component as PrefabSaveable
                    val newPath = instance.pathInRoot2(root, true)
                    newPath.setLast(component.className, index, type)
                    Hierarchy.add(this@PrefabInspector.prefab, newPath, instance, component)
                }

                override fun onRemoveComponent(component: Inspectable) {
                    component as PrefabSaveable
                    ECSTypeLibrary.unselect(component)
                    Hierarchy.removePathFromPrefab(this@PrefabInspector.prefab, component)
                }

                override fun getOptionFromInspectable(inspectable: Inspectable): Option {
                    inspectable as Component
                    return Option(inspectable.className, "") { inspectable }
                }
            })
        }
    }

    fun checkDependencies(parent: PrefabSaveable, src: FileReference): Boolean {
        if (src == InvalidRef) return true
        return if (parent.anyInHierarchy { it.prefab2?.source == src }) {
            LOGGER.warn("Cannot add $src to ${parent.name} because of dependency loop!")
            false
        } else true
    }

    fun addEntityChild(parent: Entity, prefab: Prefab) {
        if (!checkDependencies(parent, prefab.source)) return
        if (prefab.clazzName != "Entity") throw IllegalArgumentException("Type must be Entity!")
        val path = parent.pathInRoot2(root, false)
        prefab.add(CAdd(path, 'e', prefab.clazzName, prefab.clazzName, prefab.source))
    }

    fun save() {
        TextWriter.save(prefab, reference)
    }

    override fun toString(): String = TextWriter.toText(prefab)

    companion object {

        private val LOGGER = LogManager.getLogger(PrefabInspector::class)

        init {
            LogManager.disableLogger("FBStack")
        }

        var currentInspector: PrefabInspector? = null

    }

}
