package me.anno.ecs.prefab

import me.anno.config.DefaultStyle.black
import me.anno.ecs.Entity
import me.anno.ecs.interfaces.ControlReceiver
import me.anno.ecs.interfaces.CustomEditMode
import me.anno.ecs.prefab.PrefabCache.loadPrefab
import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ui.ComponentUI
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.io.ISaveable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextWriter
import me.anno.objects.inspectable.Inspectable
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.base.text.UpdatingTextPanel
import me.anno.ui.editor.PropertyInspector.Companion.invalidateUI
import me.anno.ui.editor.stacked.Option
import me.anno.ui.editor.stacked.StackPanel
import me.anno.ui.input.TextInput
import me.anno.ui.style.Style
import me.anno.utils.process.DelayedTask
import me.anno.utils.strings.StringHelper.camelCaseToTitle
import me.anno.utils.strings.StringHelper.shorten2Way
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager

// todo bug: right click to reset is not working on text inputs, why?
// todo bug: instance and inspector can get out of sync: the color slider for materials stops working :/

// done if there is a physics components, start it
// todo only execute it, if the scene is visible/selected
// todo later: only execute it, if in game mode
// todo later: control it's speed, and step size

// this can be like a scene (/scene tab)
// show changed values in bold

class PrefabInspector(val prefab: Prefab) {

    val reference get() = prefab.source

    constructor(reference: FileReference, classNameIfNull: String) :
            this(loadPrefab(reference) ?: Prefab(classNameIfNull))

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
            prefab.invalidateInstance()
            onChange()
            ECSSceneTabs.updatePrefab(prefab)
        }
    }

    fun reset(path: Path, name: String) {
        if (!prefab.isWritable) throw ImmutablePrefabException(prefab.source)
        // if (sets.removeIf { it.path == path && it.name == name }) {
        if (sets.contains(path, name)) {
            sets.remove(path, name)
            prefab.invalidateInstance()
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

        if (instance.prefab !== prefab)
            LOGGER.warn(
                "Component ${instance.name}:${instance.className} " +
                        "is not part of tree ${root.name}:${root.className}, " +
                        "its root is ${instance.root.name}:${instance.root.className}; " +
                        "${instance.prefab?.source} vs ${prefab.source}"
            )

        val path = instance.prefabPath
        if (path == null) {
            LOGGER.error(
                "Missing path for " +
                        "[${instance.listOfHierarchy.joinToString { "${it.className}:${it.name}" }}], " +
                        "prefab: '${instance.prefab?.source}', root prefab: '${instance.root.prefab?.source}'"
            )
            return
        }

        val pathIndices = path.indices

        // the index may not be set in the beginning
        fun getPath(): Path {
            val li = pathIndices.lastIndex
            if (li >= 0 && pathIndices[li] < 0) {
                pathIndices[li] = instance.parent!!.getIndexOf(instance)
            }
            return path
        }

        val prefab = instance.getOriginal()

        list.add(TextButton("Select Parent", false, style).addLeftClickListener {
            EditorState.select(instance.parent)
        })

        list.add(TextInput("Name", "", instance.name, style).apply {
            setBold(isChanged(getPath(), "name"))
            addChangeListener { setBold(); change(getPath(), "name", it); instance.name = it }
            setResetListener {
                unsetBold(); reset(getPath(), "name")
                instance.name = prefab?.name ?: ""; instance.name
            }
        })
        list.add(TextInput("Description", "", instance.description, style).apply {
            addChangeListener { setBold(); change(getPath(), "description", it); instance.description = it }
            setResetListener {
                unsetBold(); reset(getPath(), "description")
                instance.description = prefab?.description ?: ""; instance.description
            }
        })

        // for debugging
        list.add(TextButton("Copy", false, style).addLeftClickListener {
            LOGGER.info("Copy: ${TextWriter.toText(instance)}")
        })

        if (instance is ControlReceiver) {
            list.add(TextButton("Test Controls", false, style)
                .addLeftClickListener { EditorState.control = instance })
        }

        if (instance is CustomEditMode) {
            list.add(TextButton("Toggle Edit Mode", false, style)
                .addLeftClickListener {
                    EditorState.editMode =
                        if (EditorState.editMode === instance) null
                        else instance
                })
        }

        val reflections = instance.getReflections()

        // debug warnings
        for (warn in reflections.debugWarnings) {
            val title = warn.name.camelCaseToTitle()
            list.add(UpdatingTextPanel(500L, style) {
                formatWarning(title, warn.getter.call(instance))
            }.apply { textColor = black or 0xffff33 })
        }

        // debug actions: buttons for them
        for (action in reflections.debugActions) {
            // todo if there are extra arguments, we would need to create a list inputs for them
            /* for (param in action.parameters) {
                     param.kind
            } */
            list.add(TextButton(action.name.camelCaseToTitle(), false, style)
                .addLeftClickListener {
                    action.call(instance)
                    invalidateUI() // typically sth would have changed -> show that automatically
                })
        }

        // debug properties: text showing the value, constantly updating
        for (property in reflections.debugProperties) {
            val title = property.name.camelCaseToTitle()
            val list1 = PanelListX(style)
            list1.add(TextPanel("$title:", style))
            list1.add(UpdatingTextPanel(100L, style) { property.getter.call(instance).toString().shorten2Way(50) })
            // todo when clicked, a tracking graph/plot is displayed (real time)
            /*list1.addLeftClickListener {

            }*/
            list.add(list1)
        }

        // todo form groups
        // todo groups on a global or by-class level?

        val allProperties = reflections.allProperties

        // bold/non bold for other properties
        for ((clazz, propertyNames) in reflections.propertiesByClass.value.reversed()) {

            var hadIntro = false
            for (name in propertyNames
                .sortedBy { allProperties[it]!!.order }) {

                val property = allProperties[name]!!
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
                    val property2 = PIProperty(this, instance, name, property)
                    val panel = ComponentUI.createUI2(name, name, property2, property.range, style) ?: continue
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

            val nicerName = niceName.camelCaseToTitle()
            list.add(object : StackPanel(
                nicerName, "",
                options, children, style
            ) {

                override fun onAddComponent(component: Inspectable, index: Int) {
                    component as PrefabSaveable
                    val newPath = instance.prefabPath!!.added(component.className, index, type)
                    Hierarchy.add(this@PrefabInspector.prefab, newPath, instance, component)
                }

                override fun onRemoveComponent(component: Inspectable) {
                    component as PrefabSaveable
                    EditorState.unselect(component)
                    Hierarchy.removePathFromPrefab(this@PrefabInspector.prefab, component)
                }

                override fun getOptionFromInspectable(inspectable: Inspectable): Option {
                    inspectable as ISaveable
                    return Option(inspectable.className.camelCaseToTitle(), "") { inspectable }
                }
            })
        }
    }

    fun checkDependencies(parent: PrefabSaveable, src: FileReference): Boolean {
        if (src == InvalidRef) return true
        return if (parent.anyInHierarchy { it.prefab?.source == src }) {
            LOGGER.warn("Cannot add $src to ${parent.name} because of dependency loop!")
            false
        } else true
    }

    fun addEntityChild(parent: Entity, prefab: Prefab) {
        if (!checkDependencies(parent, prefab.source)) return
        if (prefab.clazzName != "Entity") throw IllegalArgumentException("Type must be Entity!")
        val path = parent.prefabPath!!
        prefab.add(path, 'e', prefab.clazzName, prefab.clazzName, prefab.source)
    }

    fun save() {
        if (reference == InvalidRef) LOGGER.warn("Prefab doesn't have source!!")
        TextWriter.save(prefab, reference)
    }

    override fun toString(): String = TextWriter.toText(prefab)

    companion object {

        fun formatWarning(title: String, warn: Any?): String? {
            if (warn == null) return null
            val tos = warn.toString()
            val title2 = if (' ' in title) title else title.camelCaseToTitle()
            if (tos.isBlank2()) return title2
            return "$title2: ${tos.shorten2Way(50)}"
        }

        private val LOGGER = LogManager.getLogger(PrefabInspector::class)

        init {
            LogManager.disableLogger("FBStack")
        }

        var currentInspector: PrefabInspector? = null

    }

}
