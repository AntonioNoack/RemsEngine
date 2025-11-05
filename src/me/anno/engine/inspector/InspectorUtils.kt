package me.anno.engine.inspector

import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Group
import me.anno.ecs.prefab.PrefabInspector.Companion.formatWarning
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.prefab.PropertyTracking.createTrackingButton
import me.anno.engine.inspector.CachedProperty.Companion.DEFAULT_GROUP
import me.anno.engine.ui.input.ComponentUI
import me.anno.language.translation.NameDesc
import me.anno.ui.Style
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PropertyTablePanel
import me.anno.ui.base.groups.SizeLimitingContainer
import me.anno.ui.base.groups.TablePanel
import me.anno.ui.base.text.TextPanel
import me.anno.ui.base.text.UpdatingSimpleTextPanel
import me.anno.ui.base.text.UpdatingTextPanel
import me.anno.ui.editor.PropertyInspector.Companion.invalidateUI
import me.anno.ui.editor.SettingCategory
import me.anno.ui.input.InputPanel
import me.anno.utils.Color.black
import me.anno.utils.structures.Compare.ifSame
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull2
import me.anno.utils.types.Strings.camelCaseToTitle
import me.anno.utils.types.Strings.ifBlank2
import me.anno.utils.types.Strings.shorten2Way
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.AABBf

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
        val group = SettingCategory(NameDesc("DebugActions"), style).showByDefault()
        val debugActionWrapper = group.content
        for (debugAction in reflections.debugActions) {
            val action = debugAction.method
            val clazz = action.declaringClass ?: continue
            val isSupported = action.parameterCount == 0
            // todo if there are extra arguments, we would need to create a list inputs for them
            val nameDesc = NameDesc(debugAction.title, if (isSupported) "" else "Not supported yet", "")
            val button = TextButton(nameDesc, style)
                .apply { isInputAllowed = isSupported }
                .addLeftClickListener {
                    // could become a little heavy....
                    for (instance in instances) {
                        if (clazz.isInstance(instance)) {
                            action.invoke(instance)
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
        val groupPanel = SettingCategory(NameDesc("Debug Properties"), style).showByDefault()
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
        val xList = PanelListX(style)
        xList.add(TextPanel("$title:", style))
        val relevantInstances = instances.filter { it::class == instances.first()::class }
        // todo bug: why is text floating at the top???
        val limiter = SizeLimitingContainer(UpdatingTextPanel(100L, style) {
            relevantInstances.joinToString { relevantInstance ->
                when (val i = getter(relevantInstance)) {
                    is AABBf -> "${i.minX} - ${i.maxX}\n" +
                            "${i.minY} - ${i.maxY}\n" +
                            "${i.minZ} - ${i.maxZ}"
                    is AABBd -> "${i.minX} - ${i.maxX}\n" +
                            "${i.minY} - ${i.maxY}\n" +
                            "${i.minZ} - ${i.maxZ}"
                    else -> i.toString().shorten2Way(200)
                }
            }
        }, 350, 250, style)
        xList.add(limiter)
        list.add(xList)
        createTrackingButton(list, xList, relevantInstances, property, style)
    }

    fun showPropertyI(
        property: CachedProperty,
        property2: IProperty<Any?>, reflections: CachedReflections,
        table: TablePanel, isWritable: Boolean, style: Style,
    ) {
        val name = property.name
        try {

            val panel = ComponentUI.createUI2(
                "", "",
                property2, property.range, style
            ) ?: return
            panel.tooltip = property.description
            panel.forAllPanels { panel2 ->
                if (panel2 is InputPanel<*>) {
                    panel2.isInputAllowed = isWritable
                }
            }

            val tableY = table.sizeY++
            table[0, tableY] = TextPanel(name.camelCaseToTitle(), style)
            table[1, tableY] = panel

            val ttt = property.annotations
                .firstInstanceOrNull2(Docs::class)
                ?.description

            if (ttt != null) {
                table[0, tableY].tooltip = ttt
                table[1, tableY].tooltip = ttt
            }
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
        isWritable: Boolean, createIProperty: (CachedProperty, List<PrefabSaveable>) -> IProperty<Any?>,
    ) {
        if (reflections.editorFields.isEmpty()) return
        val table = PropertyTablePanel(2, 0, style)
        for (field in reflections.editorFields) {
            val property = reflections.allProperties[field.name]
            if (property != null) {
                val iProperty = createIProperty(property, instances)
                showPropertyI(property, iProperty, reflections, table, isWritable, style)
            } else {
                LOGGER.warn("Missing property ${field.name}")
            }
        }

        val group = SettingCategory(NameDesc("Editor Fields"), style)
            .showByDefault()
        group.content.add(table)
        list.add(group)
    }

    fun <V : Inspectable> showProperties(
        list: PanelList, reflections: CachedReflections,
        instances: List<V>, style: Style,
        isWritable: Boolean, createProperty: (CachedProperty, List<V>) -> IProperty<Any?>,
        hideNameDesc: Boolean
    ) {

        val propertiesByClass = reflections.propertiesByClass
        val panelByGroup = HashMap<String, TablePanel>()
        val table0 = PropertyTablePanel(2, 0, style)
        panelByGroup[DEFAULT_GROUP] = table0
        list.add(table0)

        for (i in propertiesByClass.indices) {
            val (clazz, propertiesI) = propertiesByClass[i]
            val relevantInstances = instances.filter { clazz.isInstance(it) }
            val shownProperties = filterShownProperties(propertiesI, relevantInstances, hideNameDesc)
            showProperties(
                reflections, shownProperties, clazz, relevantInstances,
                panelByGroup, list, style, isWritable, createProperty
            )
        }
    }

    private fun <V> filterShownProperties(
        properties: List<CachedProperty>, instances: List<V>,
        hideNameDesc: Boolean
    ): List<CachedProperty> {
        val firstInstance = instances.firstOrNull() ?: return emptyList()
        return properties.filter {
            it.serialize && !it.hideInInspector(firstInstance) &&
                    (!hideNameDesc || (it.name != "name" && it.name != "description"))
        }
    }

    private object PropertyComparator : Comparator<CachedProperty> {
        override fun compare(pa: CachedProperty, pb: CachedProperty): Int {
            return pa.group.compareTo(pb.group)
                .ifSame(pa.order.compareTo(pb.order))
        }
    }

    private fun getOrPutPanelList(
        tablesByGroup: HashMap<String, TablePanel>,
        groupPanel: PanelList, style: Style, groupName: String
    ): TablePanel {
        return tablesByGroup.getOrPut(groupName) {
            val category = SettingCategory(NameDesc(groupName), style).showByDefault()
            groupPanel.add(category)
            val table = PropertyTablePanel(2, 0, style)
            category.content.add(table)
            table
        }
    }

    fun <V : Inspectable> showProperties(
        reflections: CachedReflections, properties: List<CachedProperty>, clazz: Class<*>, relevantInstances: List<V>,
        tablesByGroup: HashMap<String, TablePanel>, groupPanel: PanelList, style: Style,
        isWritable: Boolean, createIProperty: (CachedProperty, List<V>) -> IProperty<Any?>,
    ) {
        for (property in properties.sortedWith(PropertyComparator)) {
            val group = property.group.ifBlank2(clazz.simpleName ?: DEFAULT_GROUP)
            val panelList = getOrPutPanelList(tablesByGroup, groupPanel, style, group)
            val iProperty = createIProperty(property, relevantInstances)
            showPropertyI(property, iProperty, reflections, panelList, isWritable, style)
        }
    }
}