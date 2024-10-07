package me.anno.engine.inspector

import me.anno.ecs.annotations.Group
import me.anno.ecs.prefab.PrefabInspector.Companion.formatWarning
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.prefab.PropertyTracking.createTrackingButton
import me.anno.engine.ui.input.ComponentUI
import me.anno.language.translation.NameDesc
import me.anno.ui.Style
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.base.text.UpdatingTextPanel
import me.anno.ui.editor.PropertyInspector.Companion.invalidateUI
import me.anno.ui.editor.SettingCategory
import me.anno.ui.input.InputPanel
import me.anno.utils.Color.black
import me.anno.utils.structures.Compare.ifSame
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull
import me.anno.utils.types.Strings.camelCaseToTitle
import me.anno.utils.types.Strings.shorten2Way
import org.apache.logging.log4j.LogManager
import kotlin.reflect.jvm.javaMethod

object InspectorUtils {

    private val LOGGER = LogManager.getLogger(InspectorUtils::class)

    fun showDebugWarnings(
        list: PanelList,
        reflections: CachedReflections,
        instances: List<Inspectable>,
        style: Style
    ) {
        for (warn in reflections.debugWarnings) {
            val title = warn.name.camelCaseToTitle()
            list.add(UpdatingTextPanel(500L, style) {
                formatWarning(title, instances.firstNotNullOfOrNull { warn.getter(it) })
            }.apply { textColor = black or 0xffff33 })
        }
    }

    fun showDebugActions(
        list: PanelList, reflections: CachedReflections,
        instances: List<Inspectable>, style: Style
    ) {
        if (reflections.debugActions.isEmpty()) return
        val group = SettingCategory(NameDesc("DebugActions"), style).showContent()
        val debugActionWrapper = group.content
        for (action in reflections.debugActions) {
            val clazz = action.javaMethod?.declaringClass ?: continue
            // todo if there are extra arguments, we would need to create a list inputs for them
            /* for (param in action.parameters) {
                     param.kind
            } */
            val title = action.name.camelCaseToTitle()
            val button = TextButton(NameDesc(title), style)
                .addLeftClickListener {
                    // could become a little heavy....
                    for (instance in instances) {
                        if (clazz.isInstance(instance)) {
                            action.call(instance)
                        }
                    }
                    invalidateUI(true) // typically sth would have changed -> show that automatically
                }
            debugActionWrapper.add(button)
        }
        group.alignmentX = AxisAlignment.MIN
        list.add(group)
    }


    /**
     * debug properties: text showing the value, constantly updating
     * */
    fun showDebugProperties(
        list: PanelList, reflections: CachedReflections,
        instances: List<Inspectable>, style: Style
    ) {
        if (reflections.debugProperties.isEmpty()) return
        val groupPanel = SettingCategory(NameDesc("Debug Properties"), style).showContent()
        val listI = groupPanel.content
        // group them by their @Group-value
        for ((group, properties) in reflections.debugProperties
            .groupBy { it.annotations.firstInstanceOrNull(Group::class)?.name ?: "" }
            .toSortedMap()) {
            val helper = if (group.isNotEmpty()) { // show title for group
                val sc = SettingCategory(NameDesc(group), style)
                listI.add(sc)
                sc.content
            } else listI
            for (property in properties) {
                showDebugProperty(helper, property, style, instances)
            }
        }
        list.add(groupPanel)
    }

    private fun showDebugProperty(
        list: PanelList, property: CachedProperty, style: Style,
        instances: List<Inspectable>
    ) {
        val title = property.name.camelCaseToTitle()
        val getter = property.getter
        val list1 = PanelListX(style)
        list1.add(TextPanel("$title:", style))
        val relevantInstances = instances.filter { it::class == instances.first()::class }
        list1.add(UpdatingTextPanel(100L, style) {
            relevantInstances
                .joinToString { getter(it).toString() }
                .shorten2Way(200)
        })
        list.add(list1)
        createTrackingButton(list, list1, relevantInstances, property, style)
    }

    fun showPropertyI(
        property: CachedProperty,
        property2: IProperty<Any?>, reflections: CachedReflections,
        list: PanelListY, isWritable: Boolean, style: Style,
    ) {
        val name = property.name
        try {
            val panel = ComponentUI.createUI2(name, name, property2, property.range, style) ?: return
            panel.tooltip = property.description
            panel.forAllPanels { panel2 ->
                if (panel2 is InputPanel<*>) {
                    panel2.isInputAllowed = isWritable
                }
            }
            list.add(panel)
        } catch (e: Error) {
            warn(reflections, name, e)
        } catch (e: Exception) {
            warn(reflections, name, e)
        } catch (e: ClassCastException) {
            warn(reflections, name, e)
        }
    }

    private fun warn(reflections: CachedReflections, name: String, e: Throwable) {
        RuntimeException("Error from ${reflections.clazz}, property $name", e)
            .printStackTrace()
    }

    fun showEditorFields(
        list: PanelList, reflections: CachedReflections,
        instances: List<PrefabSaveable>, style: Style,
        isWritable: Boolean, createProperty: (CachedProperty, List<PrefabSaveable>) -> IProperty<Any?>,
    ) {
        if (reflections.editorFields.isEmpty()) return
        val group = SettingCategory(NameDesc("Editor Fields"), style).showContent()
        for (field in reflections.editorFields) {
            val property = reflections.allProperties[field.name]
            if (property != null) {
                showProperty(group.content, reflections, property, instances, style, isWritable, createProperty)
            } else {
                LOGGER.warn("Missing property ${field.name}")
            }
        }
        list.add(group)
    }

    private fun <V : Inspectable> showProperty(
        list: PanelListY, reflections: CachedReflections,
        property: CachedProperty, relevantInstances: List<V>,
        style: Style, isWritable: Boolean,
        createProperty: (CachedProperty, List<V>) -> IProperty<Any?>,
    ) {
        val property2 = createProperty(property, relevantInstances)
        showPropertyI(property, property2, reflections, list, isWritable, style)
    }

    fun <V : Inspectable> showProperties(
        list: PanelList, reflections: CachedReflections,
        instances: List<V>, style: Style,
        isWritable: Boolean, createProperty: (CachedProperty, List<V>) -> IProperty<Any?>,
    ) {
        val properties = reflections.propertiesByClass
        for (i in properties.indices) {
            val (clazz, propertiesI) = properties[i]
            if (propertiesI.isEmpty()) continue
            val relevantInstances = instances.filter { clazz.isInstance(it) }
            val firstInstance = relevantInstances.firstOrNull() ?: continue
            val propertiesJ = propertiesI.filter {
                it.serialize && !it.hideInInspector(firstInstance)
            }

            if (propertiesJ.isEmpty()) continue

            val defaultGroup = ""
            var lastGroup = ""

            val className = clazz.simpleName ?: "Anonymous"
            val groupPanel = SettingCategory(NameDesc(className), style).showContent()
            list.add(groupPanel)

            for (property in propertiesJ
                .sortedWith { pa, pb ->
                    val ga = pa.group ?: defaultGroup
                    val gb = pb.group ?: defaultGroup
                    ga.compareTo(gb).ifSame(pa.order.compareTo(pb.order))
                }) {

                // LOGGER.info("Showing property ${property.name} for class ${clazz.simpleName}")

                val group = property.group ?: ""
                if (group != lastGroup) {
                    lastGroup = group
                    // add title for group
                    groupPanel.content.add(applyGroupStyle(TextPanel(group.camelCaseToTitle(), style)))
                }

                showProperty(
                    groupPanel.content, reflections, property, relevantInstances, style,
                    isWritable, createProperty
                )
            }
        }
    }

    fun applyGroupStyle(tp: TextPanel): TextPanel {
        tp.textColor = tp.textColor and 0x7fffffff
        tp.focusTextColor = tp.textColor
        tp.isItalic = true
        return tp
    }
}