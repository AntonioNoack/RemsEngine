package me.anno.engine.inspector

import me.anno.engine.inspector.InspectorUtils.showDebugActions
import me.anno.engine.inspector.InspectorUtils.showDebugProperties
import me.anno.engine.inspector.InspectorUtils.showDebugWarnings
import me.anno.engine.inspector.InspectorUtils.showProperties
import me.anno.io.saveable.Saveable
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelListY

object AutoInspector {
    fun inspect(instances: List<Inspectable>, list: PanelListY, style: Style) {

        val isWritable = true
        val reflections = Saveable.getReflections(instances.first())

        showDebugWarnings(list, reflections, instances, style)
        showDebugActions(list, reflections, instances, style)
        showDebugProperties(list, reflections, instances, style)

        // bold/plain for other properties
        val cleanInstance = try {
            instances.first()::class
                .constructors
                .first { it.parameters.isEmpty() }
                .call()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        showProperties(list, reflections, instances, style, isWritable) { property, relevantInstances ->
            InspectableProperty(relevantInstances, property, cleanInstance)
        }
    }
}