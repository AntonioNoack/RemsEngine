package me.anno.ecs.prefab

import me.anno.ecs.interfaces.CustomEditMode
import me.anno.ecs.interfaces.InputListener
import me.anno.ecs.prefab.change.Path
import me.anno.ecs.prefab.change.PrefabChanges
import me.anno.engine.EngineBase.Companion.workspace
import me.anno.engine.Events.addEvent
import me.anno.engine.RemsEngine.Companion.collectSelected
import me.anno.engine.RemsEngine.Companion.restoreSelected
import me.anno.engine.inspector.Inspectable
import me.anno.engine.inspector.InspectorUtils.showDebugActions
import me.anno.engine.inspector.InspectorUtils.showDebugProperties
import me.anno.engine.inspector.InspectorUtils.showDebugWarnings
import me.anno.engine.inspector.InspectorUtils.showEditorFields
import me.anno.engine.inspector.InspectorUtils.showProperties
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.gpu.drawing.DrawRectangles
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.generic.JsonFormatter
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.base.text.UpdatingTextPanel
import me.anno.ui.editor.PropertyInspector.Companion.invalidateUI
import me.anno.ui.editor.stacked.Option
import me.anno.ui.editor.stacked.StackPanel
import me.anno.ui.input.TextInput
import me.anno.utils.Color.black
import me.anno.utils.Color.hex32
import me.anno.utils.Color.mulARGB
import me.anno.utils.Logging.hash32
import me.anno.utils.process.DelayedTask
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull
import me.anno.utils.types.Strings.camelCaseToTitle
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.shorten2Way
import org.apache.logging.log4j.LogManager

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

    fun serialize(prefab: Prefab): String {
        return JsonStringWriter.toText(PrefabChanges(prefab), workspace)
    }

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
        LOGGER.info("Setting ${prefab.source}.$path.$name = $value")
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

        if (instances.isEmpty()) {
            LOGGER.warn("No instances found")
            return
        }

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
        val warningPanel = UpdatingTextPanel(500, style) {
            instances.mapNotNull { it.lastWarning }.joinToString().ifBlank { null }
        }
        warningPanel.textColor = warningPanel.textColor.mulARGB(0xff3333 or black)
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
        showEditorFields(list, reflections, instances, style, isWritable) { property, relevantInstances ->
            PrefabSaveableProperty(this, relevantInstances as List<PrefabSaveable>, property.name, property)
        }

        // todo place actions into these groups
        showProperties(list, reflections, instances, style, isWritable) { property, relevantInstances ->
            PrefabSaveableProperty(this, relevantInstances as List<PrefabSaveable>, property.name, property)
        }

        val instance = instances.first()
        val types = instance.listChildTypes()
        for (i in types.indices) {
            showChildType(list, types[i], instance, style, isWritable)
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
        val first = instances.firstOrNull() ?: return
        val original = first.getOriginal()
        val text = instances
            .map(getter).filter { it.isNotBlank() }
            .joinToString(", ")
        val input = TextInput(title, "", text, style).apply {
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
        }
        input.isInputAllowed = isWritable
        list.add(input)
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
