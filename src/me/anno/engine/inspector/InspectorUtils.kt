package me.anno.engine.inspector

import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Group
import me.anno.ecs.components.anim.AnimationCache
import me.anno.ecs.components.anim.SkeletonCache
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.material.MaterialCache
import me.anno.ecs.prefab.PrefabByFileCache
import me.anno.ecs.prefab.PrefabInspector.Companion.formatWarning
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.prefab.PropertyTracking.createTrackingButton
import me.anno.engine.inspector.CachedProperty.Companion.DEFAULT_GROUP
import me.anno.engine.ui.input.ComponentUI
import me.anno.engine.ui.input.ComponentUI.createUIByTypeName
import me.anno.io.files.FileReference
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PropertyTablePanel
import me.anno.ui.base.groups.SizeLimitingContainer
import me.anno.ui.base.groups.TablePanel
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.text.TextPanel
import me.anno.ui.base.text.UpdatingTextPanel
import me.anno.ui.editor.PropertyInspector.Companion.invalidateUI
import me.anno.ui.editor.SettingCategory
import me.anno.ui.input.InputPanel
import me.anno.utils.Color.black
import me.anno.utils.structures.Compare.ifSame
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull2
import me.anno.utils.types.Defaults
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
            debugActionWrapper += showDebugAction(debugAction, instances, style) ?: continue
        }
        group.alignmentX = AxisAlignment.MIN
        list.add(group)
    }

    fun showDebugAction(
        debugAction: DebugActionInstance,
        instances: List<Inspectable>, style: Style,
    ): TextButton? {
        val action = debugAction.method
        val clazz = action.declaringClass ?: return null
        val parameters = action.parameters
        val parameterNames = parameters.mapIndexed { index, parameter ->
            debugAction.parameterNames.getOrNull(index) ?: parameter.name
        }
        val isSimpleAction = parameters.isEmpty()
        val nameDesc = NameDesc(
            debugAction.title,
            parameters.withIndex().joinToString(", ", "(", ")") { (index, it) ->
                val type = it.type?.simpleName?.capitalize() ?: "??"
                val name = parameterNames[index]
                "$name: $type"
            }, ""
        )
        val button = TextButton(nameDesc, style)
        if (isSimpleAction) {
            button.addLeftClickListener {
                // could become a little heavy....
                for (instance in instances) {
                    if (clazz.isInstance(instance)) {
                        action.invoke(instance)
                    }
                }
                invalidateUI(true) // typically sth would have changed -> show that automatically
            }
        } else {
            // there are extra arguments, create inputs for them
            button.addLeftClickListener {
                val values = arrayOfNulls<Any>(parameters.size)
                val table = PropertyTablePanel(2, parameters.size, style)
                val isReferenceTo = arrayOfNulls<PrefabByFileCache<*>>(parameters.size)
                for ((index, parameter) in parameters.withIndex()) {

                    val type = parameter.type
                    var typeName = type?.simpleName?.capitalize() ?: "?"
                    when (typeName) {
                        "Mesh" -> {
                            typeName = "Mesh/Reference"
                            isReferenceTo[index] = MeshCache
                        }
                        "Material" -> {
                            typeName = "Material/Reference"
                            isReferenceTo[index] = MaterialCache
                        }
                        "Skeleton" -> {
                            typeName = "Skeleton/Reference"
                            isReferenceTo[index] = SkeletonCache
                        }
                        "Animation" -> {
                            typeName = "Animation/Reference"
                            isReferenceTo[index] = AnimationCache
                        }
                    }

                    val property = object : IProperty<Any?> {

                        override val annotations: List<Annotation> get() = emptyList()

                        override fun set(panel: Panel?, value: Any?, mask: Int) {
                            values[index] = value
                        }

                        override fun init(panel: Panel?) {}
                        override fun get(): Any? = values[index]
                        override fun getDefault(): Any? = Defaults.getDefaultValue(typeName)

                        override fun reset(panel: Panel?): Any? {
                            val value = getDefault()
                            values[index] = value
                            return value
                        }
                    }

                    values[index] = property.getDefault()
                    table[0, index] = TextPanel(parameterNames[index], style)
                    table[1, index] = createUIByTypeName(null, "", property, typeName, null, style)
                }
                val okButton = TextButton(nameDesc, style)
                okButton.addLeftClickListener {
                    val values = Array(values.size) { index ->
                        val value = values[index]
                        val cache = isReferenceTo[index]
                        if (cache != null) cache.getEntry(value as? FileReference).waitFor()
                        else value
                    }
                    for (index in values.indices) {
                        val param = parameters[index]
                        val value = values[index]
                        if (param.type.isPrimitive) {
                            check(value != null) {
                                "Parameter '${parameterNames[index]}' must not be null"
                            }
                        } else {
                            check(value == null || param.type.isInstance(value)) {
                                "Parameter '${parameterNames[index]}' has incorrect type, $value (${value?.javaClass}) !is ${param.type}"
                            }
                        }
                    }
                    // could become a little heavy....
                    for (instance in instances) {
                        if (clazz.isInstance(instance)) {
                            action.invoke(instance, *values)
                        }
                    }
                    invalidateUI(true) // typically sth would have changed -> show that automatically
                }
                Menu.openMenuByPanels(button.windowStack, nameDesc, listOf(table, okButton))
            }
        }
        return button
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
            )

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