package me.anno.ecs.prefab

import me.anno.ecs.annotations.DebugTitle
import me.anno.ecs.interfaces.ControlReceiver
import me.anno.ecs.interfaces.CustomEditMode
import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Path
import me.anno.engine.RemsEngine.Companion.collectSelected
import me.anno.engine.RemsEngine.Companion.restoreSelected
import me.anno.engine.ui.ComponentUI
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.gpu.drawing.DrawRectangles
import me.anno.io.ISaveable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.studio.Inspectable
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.studio.StudioBase.Companion.workspace
import me.anno.ui.Panel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.base.text.UpdatingTextPanel
import me.anno.ui.editor.PropertyInspector.Companion.invalidateUI
import me.anno.ui.editor.stacked.Option
import me.anno.ui.editor.stacked.StackPanel
import me.anno.ui.input.InputPanel
import me.anno.ui.input.TextInput
import me.anno.ui.Style
import me.anno.utils.Color.black
import me.anno.utils.Color.hex32
import me.anno.utils.Color.mulARGB
import me.anno.utils.process.DelayedTask
import me.anno.utils.strings.StringHelper.camelCaseToTitle
import me.anno.utils.strings.StringHelper.shorten2Way
import me.anno.utils.structures.Compare.ifSame
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager

// todo bug: instance and inspector can get out of sync: the color slider for materials stops working :/

// this can be like a scene (/scene tab)
// show changed values in bold

class PrefabInspector(val reference: FileReference) {

    val prefab: Prefab
        get() {
            val prefab = PrefabCache[reference] ?: throw NullPointerException("Missing prefab of $reference")
            prefab.ensureMutableLists()
            val history = prefab.history ?: ChangeHistory().apply {
                put(serialize(prefab))
            }
            history.prefab = prefab
            prefab.history = history
            return prefab
        }

    val history get() = prefab.history!!

    fun serialize(prefab: Prefab) =
        TextWriter.toText(prefab.adds + prefab.sets.map { k1, k2, v -> CSet(k1, k2, v) }, workspace)

    val adds get() = prefab.adds as MutableList
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
        onChange(false)
    }

    fun inspect(instance: PrefabSaveable, list: PanelListY, style: Style) {

        if (instance.prefab !== prefab && instance.prefab != null)
            LOGGER.warn(
                "Component ${instance.name}:${instance.className} " +
                        "is not part of tree ${root.name}:${root.className}, " +
                        "its root is ${instance.root.name}:${instance.root.className}; " +
                        "${instance.prefab?.source} vs ${prefab.source}"
            )

        val path = instance.prefabPath
        instance.ensurePrefab()
        list += TextPanel(
            "$path@${instance.prefab?.source}, " +
                    "${instance.className}@${hex32(System.identityHashCode(instance))}", style
        )

        // the index may not be set in the beginning
        fun getPath(): Path {
            if (path.lastIndex() < 0) {
                path.index = instance.parent!!.getIndexOf(instance)
            }
            return path
        }

        val isWritable = prefab.isWritable
        val original = instance.getOriginal()

        list.add(TextButton("Select Parent", false, style).addLeftClickListener {
            EditorState.select(instance.parent)
        })

        val warningPanel = UpdatingTextPanel(500, style) { instance.lastWarning }
        warningPanel.textColor = warningPanel.textColor.mulARGB(0xffff3333.toInt())
        warningPanel.tooltip = "Click to hide this warning until the issue reappears."
        warningPanel.addLeftClickListener { instance.lastWarning = null } // "marks" the warning as "read"
        list += warningPanel

        if (instance.className !in ISaveable.objectTypeRegistry) {
            val warningPanel1 = TextPanel("Class '${instance.className}' wasn't registered as a custom class", style)
            warningPanel1.tooltip =
                "This class cannot be saved properly to disk, and might not be copyable.\n" +
                        "Use registerCustomClass { customConstructor() } or registerCustomClass(YourClass::class)."
            warningPanel1.textColor = warningPanel.textColor
            list += warningPanel1
        }

        list.add(TextInput("Name", "", instance.name, style).apply {
            isBold = isChanged(getPath(), "name")
            addChangeListener { isBold = true; change(getPath(), instance, "name", it) }
            setResetListener {
                isBold = false; reset(getPath(), "name")
                instance.name = original?.name ?: ""; instance.name
            }
        }.apply { this.isInputAllowed = isWritable })
        list.add(TextInput("Description", "", instance.description, style).apply {
            addChangeListener { isBold = true; change(getPath(), instance, "description", it) }
            setResetListener {
                isBold = false; reset(getPath(), "description")
                instance.description = original?.description ?: ""; instance.description
            }
        }.apply { this.isInputAllowed = isWritable })

        // for debugging
        /*list.add(TextButton("Copy Internal Data", false, style).addLeftClickListener {
            val text = TextWriter.toText(instance)
            setClipboardContent(text)
            LOGGER.info("Copy: $text")
        })*/

        if (instance is ControlReceiver) {
            list.add(TextButton("Test Controls", false, style)
                .addLeftClickListener { EditorState.control = instance })
        }

        if (instance is CustomEditMode) {
            list.add(object : TextButton("Toggle Edit Mode", false, style) {

                var borderColor = 0

                override fun onUpdate() {
                    super.onUpdate()
                    val newBorderColor = if (EditorState.editMode === instance) {
                        instance.getEditModeBorderColor()
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
            val title = action.annotations.firstInstanceOrNull<DebugTitle>()?.title ?: action.name.camelCaseToTitle()
            list.add(TextButton(title, false, style)
                .addLeftClickListener {
                    action.call(instance)
                    invalidateUI(true) // typically sth would have changed -> show that automatically
                })
        }

        // debug properties: text showing the value, constantly updating
        for (property in reflections.debugProperties) {
            val title = property.name.camelCaseToTitle()
            val list1 = PanelListX(style)
            list1.add(TextPanel("$title:", style))
            list1.add(UpdatingTextPanel(100L, style) {
                property.getter.call(instance)
                    .toString().shorten2Way(200)
            })
            // todo when clicked, a tracking graph/plot is displayed (real time)
            /*list1.addLeftClickListener {

            }*/
            list.add(list1)
        }

        val allProperties = reflections.allProperties

        fun applyGroupStyle(tp: TextPanel): TextPanel {
            tp.textColor = tp.textColor and 0x7fffffff
            tp.focusTextColor = tp.textColor
            tp.isItalic = true
            return tp
        }

        // todo place actions into these groups

        // bold/plain for other properties
        val properties = reflections.propertiesByClass
        for (i in properties.size - 1 downTo 0) {
            val (clazz, propertyNames) = properties[i]

            var hadIntro = false
            val defaultGroup = ""
            var lastGroup = ""

            for (name in propertyNames
                .sortedWith { a, b ->
                    val pa = allProperties[a]!!
                    val pb = allProperties[b]!!
                    val ga = pa.group ?: defaultGroup
                    val gb = pb.group ?: defaultGroup
                    ga.compareTo(gb)
                        .ifSame { pa.order.compareTo(pb.order) }
                }) {

                val property = allProperties[name]!!
                if (!property.serialize) continue
                if (property.hideInInspector.any { it(instance) }) continue

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
                    val property2 = PIProperty(this, instance, name, property)
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

        // todo either
        //  - only create UI when it is visible,
        //  - show a preview
        //  - hide this

        // todo disable this if !prefab.isWritable

        val types = instance.listChildTypes()
        for (i in types.indices) {
            val type = types[i]

            val options = instance.getOptionsByType(type) ?: continue
            val niceName = instance.getChildListNiceName(type)
            val children = instance.getChildListByType(type)

            if (niceName.equals("children", true)) continue

            val nicerName = niceName.camelCaseToTitle()
            list.add(object : StackPanel(
                nicerName, "",
                options, children, {
                    it as ISaveable
                    Option(it.className.camelCaseToTitle(), "") { it }
                }, style
            ) {

                override val value: List<Inspectable>
                    get() = instance.getChildListByType(type)

                override fun setValue(newValue: List<Inspectable>, notify: Boolean): Panel {
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

    fun save() {
        if (reference == InvalidRef) throw IllegalStateException("Prefab doesn't have source!!")
        if (reference.exists) {
            // check, that we actually can save this file;
            //  we must not override resources like .obj files
            val testRead = try {
                TextReader.read(reference, workspace, false).firstOrNull()
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
        TextWriter.save(prefab, reference, workspace)
        DelayedTask { addEvent { restoreSelected(selected) } }.update()
    }

    override fun toString(): String = TextWriter.toText(prefab, workspace)

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
