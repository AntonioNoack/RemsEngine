package me.anno.ecs.prefab

import me.anno.engine.inspector.CachedProperty
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.UIColors.axisWColor
import me.anno.ui.UIColors.axisXColor
import me.anno.ui.UIColors.axisYColor
import me.anno.ui.UIColors.axisZColor
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.custom.CustomSizeContainer
import me.anno.ui.debug.TrackingPanel
import me.anno.utils.Color
import me.anno.utils.Color.withAlpha
import me.anno.utils.structures.Collections.cross
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.AnyToDouble.getDouble
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4d
import org.joml.Vector4f
import org.joml.Vector4i

object PropertyTracking {
    private val LOGGER = LogManager.getLogger(PropertyTracking::class)
    private fun createGetter(index: Int, getter: (Any) -> Any?): (Any?) -> Double {
        return { it: Any? -> if (it == null) 0.0 else getDouble(getter(it), index, 0.0) }
    }

    private val colors = intArrayOf(axisXColor, axisYColor, axisZColor, axisWColor)

    fun createTrackingButton(
        list: PanelList,
        insertAfter: PanelListX,
        instances: List<Any>,
        property: CachedProperty,
        style: Style
    ) {
        val getter = property.getter
        val sample = getter(instances.first())
        // when clicked, a tracking graph/plot is displayed (real time)
        val channels: List<Pair<(Any?) -> Double, Int>> = when (sample) {
            is Boolean,
            is Number -> listOf(createGetter(0, getter) to Color.white)
            is Vector -> {
                val num = sample.numComponents
                val limit = if (num == 6) 3 else 4
                createArrayList(num) { createGetter(it, getter) to colors[it % limit] }
            }
            // do we need other types?
            null -> return
            else -> {
                LOGGER.warn("Type ${sample::class} isn't handled")
                return
            }
        }
        val samples = instances.cross(channels, ArrayList(instances.size * channels.size))
        var dynPanel: Panel? = null
        val button = TextButton(NameDesc("\uD83D\uDC41\uFE0F"), true, style)
        button.setTooltip("Start Tracking")
        button.addLeftClickListener {
            button.setTooltip(if (dynPanel == null) "Stop Tracking" else "Track")
            if (dynPanel == null) {
                val tracking = TrackingPanel(
                    samples.map { (inst, funcI) -> // functions
                        val func = funcI.first
                        { func(inst) }
                    },
                    IntArray(samples.size) { idx -> // colors
                        samples[idx].second.second.withAlpha(255)
                    },
                    style
                )
                // todo layout is very weird: we're given enough, but also too few space
                val wrapper = CustomSizeContainer(isX = false, isY = true, tracking, style)
                wrapper.alignmentY = AxisAlignment.MIN
                list.add(list.children.indexOf(insertAfter) + 1, wrapper)
                dynPanel = wrapper
            } else {
                dynPanel?.removeFromParent()
                dynPanel = null
            }
        }
        for (it in insertAfter.children) {
            it.alignmentY = AxisAlignment.CENTER
        }
        insertAfter.add(0, button)
    }
}