package me.anno.engine.inspector

import me.anno.ecs.prefab.PrefabInspector
import me.anno.engine.ui.input.ComponentUI
import me.anno.io.saveable.Saveable
import me.anno.ui.Style
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.base.text.UpdatingTextPanel
import me.anno.ui.editor.PropertyInspector
import me.anno.ui.input.InputPanel
import me.anno.utils.Color
import me.anno.utils.structures.Collections.filterIsInstance2
import me.anno.utils.structures.Compare.ifSame
import me.anno.utils.types.Strings.camelCaseToTitle
import me.anno.utils.types.Strings.shorten2Way

object AutoInspector {
    fun inspect(instances: List<Inspectable>, list: PanelListY, style: Style) {

        val isWritable = true
        val reflections = Saveable.getReflections(instances.first())

        // debug warnings
        for (warn in reflections.debugWarnings) {
            val title = warn.name.camelCaseToTitle()
            list.add(UpdatingTextPanel(500L, style) {
                PrefabInspector.formatWarning(title, instances.firstNotNullOfOrNull { warn.getter(it) })
            }.apply { textColor = Color.black or 0xffff33 })
        }

        // debug actions: buttons for them
        val debugActionWrapper = PanelListY(style)
        for (action in reflections.debugActions) {
            // todo if there are extra arguments, we would need to create a list inputs for them
            /* for (param in action.parameters) {
                     param.kind
            } */
            val title = action.name.camelCaseToTitle()
            val clazz = instances.first()::class // todo find class of debug action
            val button = TextButton(title, style)
                .addLeftClickListener {
                    // could become a little heavy....
                    for (instance in instances) {
                        if (clazz.isInstance(instance)) {
                            action.call(instance)
                        }
                    }
                    PropertyInspector.invalidateUI(true) // typically sth would have changed -> show that automatically
                }
            debugActionWrapper.add(button)
        }
        debugActionWrapper.alignmentX = AxisAlignment.MIN
        list.add(debugActionWrapper)

        // debug properties: text showing the value, constantly updating
        for (property in reflections.debugProperties) {
            val title = property.name.camelCaseToTitle()
            val list1 = PanelListX(style)
            val clazz = instances.first()::class // todo find class of debug property
            list1.add(TextPanel("$title:", style))
            list1.add(UpdatingTextPanel(100L, style) {
                instances
                    .filterIsInstance2(clazz)
                    .joinToString { property.getter(it).toString() }
                    .shorten2Way(200)
            })
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
        val cleanInstance = try {
            instances.first()::class
                .constructors
                .first { it.parameters.isEmpty() }
                .call()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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

                try {
                    val property2 = InspectableProperty(relevantInstances, property, cleanInstance)
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
}