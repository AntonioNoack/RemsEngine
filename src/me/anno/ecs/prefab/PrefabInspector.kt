package me.anno.ecs.prefab

import me.anno.ecs.interfaces.CustomEditMode
import me.anno.ecs.interfaces.InputListener
import me.anno.ecs.prefab.PropertyTracking.createTrackingButton
import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Path
import me.anno.engine.EngineBase.Companion.workspace
import me.anno.engine.Events.addEvent
import me.anno.engine.RemsEngine.Companion.collectSelected
import me.anno.engine.RemsEngine.Companion.restoreSelected
import me.anno.engine.inspector.CachedProperty
import me.anno.engine.inspector.CachedReflections
import me.anno.engine.inspector.Inspectable
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.input.ComponentUI
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.gpu.drawing.DrawRectangles
import me.anno.io.Saveable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.generic.JsonFormatter
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.base.text.UpdatingTextPanel
import me.anno.ui.editor.PropertyInspector.Companion.invalidateUI
import me.anno.ui.editor.stacked.Option
import me.anno.ui.editor.stacked.StackPanel
import me.anno.ui.input.InputPanel
import me.anno.ui.input.TextInput
import me.anno.utils.Color.black
import me.anno.utils.Color.hex32
import me.anno.utils.Color.mulARGB
import me.anno.utils.Logging.hash32
import me.anno.utils.process.DelayedTask
import me.anno.utils.structures.Compare.ifSame
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull
import me.anno.utils.types.Strings.camelCaseToTitle
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.shorten2Way
import org.apache.logging.log4j.LogManager
import kotlin.reflect.KProperty

// todo bug: instance and inspector can get out of sync: the color slider for materials stops working :/

/**
 * creates UI to inspect and edit PrefabSaveables
 * */
class PrefabInspector(var reference: FileReference) {

    val prefab: Prefab
        get() {
            val prefab = PrefabCache[reference]
                ?: throw NullPointerException("Missing prefab of $reference, ${reference::class.simpleName}")
            val history = prefab.history ?: ChangeHistory().apply {
                put(serialize(prefab))
            }
            history.prefab = prefab
            prefab.history = history
            return prefab
        }

    val history get() = prefab.history!!

    fun serialize(prefab: Prefab) =
        JsonStringWriter.toText(
            prefab.adds.values.flatten() +
                    prefab.sets.map { k1, k2, v -> CSet(k1, k2, v) },
            workspace
        )

    fun update() {
        reference = reference.validate()
    }

    val adds get() = prefab.adds
    val sets get() = prefab.sets

    val root get() = prefab.getSampleInstance()

    private val savingTask = DelayedTask {
        addEvent {
            history.put(serialize(prefab))
            LOGGER.debug("Pushed new version to history")
        }
    }

    fun onChange(major: Boolean) {
        savingTask.update()
        invalidateUI(major)
    }

    fun reset(path: Path?) {
        path ?: return
        if (!prefab.isWritable) throw ImmutablePrefabException(prefab.source)
        if (sets.removeMajorIf { it == path }) {
            prefab.invalidateInstance()
            onChange(true)
            ECSSceneTabs.updatePrefab(prefab)
        }
    }

    fun reset(path: Path?, name: String) {
        path ?: return
        if (!prefab.isWritable) throw ImmutablePrefabException(prefab.source)
        // if (sets.removeIf { it.path == path && it.name == name }) {
        if (sets.contains(path, name)) {
            sets.remove(path, name)
            prefab.invalidateInstance()
            onChange(true)
            ECSSceneTabs.updatePrefab(prefab)
        }
    }

    fun isChanged(path: Path?, name: String): Boolean {
        path ?: return false
        return sets.contains(path, name)
    }

    fun change(path: Path?, instance: PrefabSaveable, name: String, value: Any?) {
        instance[name] = value
        path ?: return
        prefab[path, name] = value
    }

    // the index may not be set in the beginning
    private fun getPath(instance: PrefabSaveable): Path {
        val path = instance.prefabPath
        if (path.lastIndex() < 0) {
            path.index = instance.parent!!.getIndexOf(instance)
        }
        return path
    }

    fun inspect(instances: List<PrefabSaveable>, list: PanelListY, style: Style) {

        for (instance in instances) {
            if (instance.prefab !== prefab && instance.prefab != null)
                LOGGER.warn(
                    "Component ${instance.name}:${instance.className} " +
                            "is not part of tree ${root.name}:${root.className}, " +
                            "its root is ${instance.root.name}:${instance.root.className}; " +
                            "${instance.prefab?.source} vs ${prefab.source}"
                )
        }

        for (instance in instances) {
            instance.ensurePrefab()
        }

        val pathInformation = instances.joinToString("\n\n") {
            "" +
                    "${it.className}@${hash32(it)}\n" +
                    "Path: ${it.prefabPath.toString(", ")}\n" +
                    "PathHash: ${hex32(it.prefabPath.hashCode())}"
        }
        list += TextPanel(pathInformation, style)

        // todo find where an object is coming from within a prefab, and open that file
        if (false) {
            list += TextButton("Open Prefab", style)
                .addLeftClickListener {
                    val src = instances.first().prefab?.source
                    if (src?.exists == true) {
                        ECSSceneTabs.open(src, PlayMode.EDITING, true)
                    }
                }
        }

        val isWritable = prefab.isWritable

        list.add(TextButton("Select Parent", style).addLeftClickListener {
            ECSSceneTabs.refocus()
            EditorState.select(instances.map { it.parent }.toHashSet().filterIsInstance<Inspectable>().toList())
        })

        val warningPanel = UpdatingTextPanel(500, style) {
            instances.mapNotNull { it.lastWarning }.joinToString().ifBlank { null }
        }
        warningPanel.textColor = warningPanel.textColor.mulARGB(0xffff3333.toInt())
        warningPanel.tooltip = "Click to hide this warning until the issue reappears."
        warningPanel.addLeftClickListener {
            // "marks" the warning as "read"
            for (instance in instances) {
                if (instance.lastWarning != null) {
                    instance.lastWarning = null
                    break
                }
            }
        }
        list += warningPanel

        showMissingClasses(list, instances, style, warningPanel)
        showTextProperty("Name", "name",
            list, instances, style, isWritable,
            { it.name }) { it, v -> it.name = v }
        showTextProperty("Description", "description",
            list, instances, style, isWritable,
            { it.description }) { it, v -> it.description = v }

        val inputListener = instances.firstInstanceOrNull<InputListener>()
        if (inputListener != null) {
            list.add(TextButton("Test Controls", style)
                .addLeftClickListener { EditorState.control = inputListener })
        }

        val customEditModes = instances.filterIsInstance<CustomEditMode>()
        if (customEditModes.isNotEmpty()) {
            showCustomEditModeButton(list, customEditModes, style)
        }

        val reflections = instances.first().getReflections()
        showDebugWarnings(list, reflections, instances, style)
        showDebugActions(list, reflections, instances, style)
        showDebugProperties(list, reflections, instances, style)

        val allProperties = reflections.allProperties

        // todo place actions into these groups
        showProperties(list, reflections, instances, style, allProperties, isWritable)

        val instance = instances.first()
        val types = instance.listChildTypes()
        for (i in types.indices) {
            showChildType(list, types[i], instance, style, isWritable)
        }
    }

    fun applyGroupStyle(tp: TextPanel): TextPanel {
        tp.textColor = tp.textColor and 0x7fffffff
        tp.focusTextColor = tp.textColor
        tp.isItalic = true
        return tp
    }

    private fun showProperties(
        list: PanelList, reflections: CachedReflections, instances: List<PrefabSaveable>, style: Style,
        allProperties: Map<String, CachedProperty>, isWritable: Boolean,
    ) {
        val properties = reflections.propertiesByClass
        for (i in properties.size - 1 downTo 0) {
            val (clazz, propertyNames) = properties[i]
            val relevantInstances = instances.filter { clazz.isInstance(it) }

            var hadIntro = false
            val defaultGroup = ""
            var lastGroup = ""

            for (name in propertyNames
                .sortedWith { a, b ->
                    val pa = allProperties[a]!!
                    val pb = allProperties[b]!!
                    val ga = pa.group ?: defaultGroup
                    val gb = pb.group ?: defaultGroup
                    ga.compareTo(gb).ifSame(pa.order.compareTo(pb.order))
                }) {

                val property = allProperties[name]!!
                if (!property.serialize) continue
                val firstInstance = relevantInstances.first()
                if (property.hideInInspector.any { it(firstInstance) }) continue

                val group = property.group ?: ""
                if (group != lastGroup) {
                    lastGroup = group
                    // add title for group
                    list.add(applyGroupStyle(TextPanel(group.camelCaseToTitle(), style)))
                }

                if (!hadIntro) {
                    hadIntro = true
                    val className = clazz.simpleName
                    list.add(applyGroupStyle(TextPanel(className ?: "Anonymous", style)))
                }

                // todo mesh selection, skeleton selection, animation selection, ...
                // to do more indentation?

                try {
                    val property2 = PrefabSaveableProperty(this, relevantInstances, name, property)
                    val panel = ComponentUI.createUI2(name, name, property2, property.range, style) ?: continue
                    panel.tooltip = property.description
                    panel.forAllPanels { panel2 ->
                        if (panel2 is InputPanel<*>) {
                            panel2.isInputAllowed = isWritable
                        }
                    }
                    list.add(panel)
                } catch (e: Error) {
                    RuntimeException("Error from ${reflections.clazz}, property $name", e)
                        .printStackTrace()
                } catch (e: ClassCastException) { // why is this not covered by the catch above?
                    RuntimeException("Error from ${reflections.clazz}, property $name", e)
                        .printStackTrace()
                } catch (e: Exception) {
                    RuntimeException("Error from ${reflections.clazz}, property $name", e)
                        .printStackTrace()
                }
            }
        }
    }

    private fun showChildType(
        list: PanelList, type: Char, instance: PrefabSaveable, style: Style,
        isWritable: Boolean
    ) {

        val options = instance.getOptionsByType(type) ?: return
        val niceName = instance.getChildListNiceName(type)
        val children = instance.getChildListByType(type)

        if (niceName.equals("children", true)) return

        val nicerName = niceName.camelCaseToTitle()
        list.add(object : StackPanel(
            nicerName, "",
            options, children, {
                it as Saveable
                Option(NameDesc(it.className.camelCaseToTitle())) { it }
            }, style
        ) {

            override val value: List<Inspectable>
                get() = instance.getChildListByType(type)

            override fun setValue(newValue: List<Inspectable>, mask: Int, notify: Boolean): Panel {
                if (newValue != value) {
                    throw IllegalStateException("Cannot directly set the value of components[]!")
                } // else done
                return this
            }

            override fun onAddComponent(component: Inspectable, index: Int) {
                component as PrefabSaveable
                if (component.prefabPath == Path.ROOT_PATH) {
                    val newPath = instance.prefabPath.added(Path.generateRandomId(), index, type)
                    Hierarchy.add(this@PrefabInspector.prefab, newPath, instance, component)
                } else LOGGER.warn("Component had prefab path already")
            }

            override fun onRemoveComponent(component: Inspectable) {
                component as PrefabSaveable
                EditorState.unselect(component)
                Hierarchy.removePathFromPrefab(this@PrefabInspector.prefab, component)
            }
        }.apply { isInputAllowed = isWritable })
    }

    private fun showMissingClasses(
        list: PanelList, instances: List<PrefabSaveable>, style: Style,
        warningPanel: TextPanel
    ) {
        for (className in instances
            .map { it.className }
            .filter { it !in Saveable.objectTypeRegistry }
            .toHashSet()
            .sorted()
        ) {
            val warningPanel1 = TextPanel("Class '$className' wasn't registered as a custom class", style)
            warningPanel1.tooltip =
                "This class cannot be saved properly to disk, and might not be copyable.\n" +
                        "Use registerCustomClass { customConstructor() } or registerCustomClass(YourClass::class)."
            warningPanel1.textColor = warningPanel.textColor
            list += warningPanel1
        }
    }

    private fun showTextProperty(
        title: String, name: String, list: PanelList, instances: List<PrefabSaveable>,
        style: Style, isWritable: Boolean,
        getter: (PrefabSaveable) -> String,
        setter: (PrefabSaveable, String) -> Unit
    ) {
        val first = instances.first()
        val original = first.getOriginal()
        list.add(
            TextInput(
                title,
                "",
                instances
                    .map(getter)
                    .filter { it.isNotBlank() }
                    .joinToString(", "),
                style
            ).apply {
                val firstValue = getter(first)
                isBold = instances.any { isChanged(getPath(it), name) }
                isInputAllowed = isWritable && instances.all { getter(it) == firstValue }
                addChangeListener {
                    isBold = true
                    for (instance in instances) {
                        change(getPath(instance), instance, name, it)
                    }
                    onChange(false)
                }
                setResetListener {
                    isBold = false
                    val defaultValue = if (original != null) getter(original) else ""
                    for (instance in instances) {
                        reset(getPath(instance), name)
                        setter(instance, defaultValue)
                    }
                    defaultValue
                }
            }.apply { this.isInputAllowed = isWritable })
    }

    private fun showCustomEditModeButton(list: PanelList, customEditModes: List<CustomEditMode>, style: Style) {
        list.add(object : TextButton("Toggle Edit Mode", style) {

            var borderColor = 0

            override fun onUpdate() {
                super.onUpdate()
                val editMode = EditorState.editMode
                val newBorderColor = if (editMode in customEditModes) {
                    editMode!!.getEditModeBorderColor()
                } else 0
                if (newBorderColor != borderColor) {
                    borderColor = newBorderColor
                    invalidateDrawing()
                }
            }

            override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
                super.onDraw(x0, y0, x1, y1)
                DrawRectangles.drawBorder(x, y, width, height, borderColor, 2)
            }
        }.addLeftClickListener {
            val editMode = EditorState.editMode
            val index = customEditModes.indexOf(editMode) + 1
            EditorState.editMode = customEditModes.getOrNull(index)
        })
    }

    private fun showDebugWarnings(
        list: PanelList,
        reflections: CachedReflections,
        instances: List<PrefabSaveable>,
        style: Style
    ) {
        for (warn in reflections.debugWarnings) {
            val title = warn.name.camelCaseToTitle()
            list.add(UpdatingTextPanel(500L, style) {
                formatWarning(title, instances.firstNotNullOfOrNull { warn.getter.call(it) })
            }.apply { textColor = black or 0xffff33 })
        }
    }

    private fun showDebugActions(
        list: PanelList,
        reflections: CachedReflections,
        instances: List<PrefabSaveable>,
        style: Style
    ) {
        for (action in reflections.debugActions) {
            // todo if there are extra arguments, we would need to create a list inputs for them
            /* for (param in action.parameters) {
                     param.kind
            } */
            val title = action.name.camelCaseToTitle()
            val button = TextButton(title, style)
                .addLeftClickListener {
                    // could become a little heavy....
                    for (instance in instances) {
                        // todo check class using inheritance / whether it exists...
                        if (instance::class == instances.first()::class) {
                            action.call(instance)
                        }
                    }
                    invalidateUI(true) // typically sth would have changed -> show that automatically
                }
            list.add(button)
        }
    }

    /**
     * debug properties: text showing the value, constantly updating
     * */
    private fun showDebugProperties(
        list: PanelList, reflections: CachedReflections,
        instances: List<PrefabSaveable>, style: Style
    ) {
        for (property in reflections.debugProperties) {
            showDebugProperty(list, property, style, instances)
        }
    }

    private fun showDebugProperty(
        list: PanelList, property: KProperty<*>, style: Style,
        instances: List<PrefabSaveable>
    ) {
        // todo group them by their @Group-value
        val title = property.name.camelCaseToTitle()
        val getter = property.getter
        val list1 = PanelListX(style)
        list1.add(TextPanel("$title:", style))
        val relevantInstances = instances.filter { it::class == instances.first()::class }
        list1.add(UpdatingTextPanel(100L, style) {
            relevantInstances
                .joinToString { getter.call(it).toString() }
                .shorten2Way(200)
        })
        list.add(list1)
        createTrackingButton(list, list1, relevantInstances, property, style)
    }

    fun checkDependencies(parent: PrefabSaveable, src: FileReference): Boolean {
        if (src == InvalidRef) return true
        return if (parent.anyInHierarchy { it.prefab?.source == src }) {
            LOGGER.warn("Cannot add $src to ${parent.name} because of dependency loop!")
            false
        } else true
    }

    fun addNewChild(parent: PrefabSaveable, type: Char, prefab: Prefab): Path? {
        if (!checkDependencies(parent, prefab.source)) return null
        return this.prefab.add(parent.prefabPath, type, prefab.clazzName, Path.generateRandomId(), prefab.source)
    }

    fun addNewChild(parent: PrefabSaveable, type: Char, clazz: String): Path {
        return this.prefab.add(parent.prefabPath, type, clazz, Path.generateRandomId(), InvalidRef)
    }

    fun addNewChild(parent: Path, type: Char, clazz: String, source: FileReference): Path {
        return this.prefab.add(parent, type, clazz, Path.generateRandomId(), source)
    }

    fun save() {
        if (reference == InvalidRef) throw IllegalStateException("Prefab doesn't have source!!")
        if (reference.exists) {
            // check, that we actually can save this file;
            //  we must not override resources like .obj files
            val testRead = try {
                JsonStringReader.read(reference, workspace, false).firstOrNull()
            } catch (e: Exception) {
                null
            }
            if (testRead !is Prefab) {
                throw IllegalArgumentException("Must not override assets! $reference is not a prefab")
            }
        }
        val selected = collectSelected()
        // save -> changes last modified -> selection becomes invalid
        // remember selection, and apply it later (in maybe 500-1000ms)
        reference.writeText(JsonFormatter.format(JsonStringWriter.toText(prefab, workspace)))
        addEvent(500) { restoreSelected(selected) }
    }

    override fun toString(): String = JsonStringWriter.toText(prefab, workspace)

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
