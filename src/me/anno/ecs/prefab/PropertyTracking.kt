package me.anno.ecs.prefab

import me.anno.engine.inspector.CachedProperty
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
import me.anno.utils.types.AnyToDouble.getDouble
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Quaterniond
import org.joml.Quaternionf
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
            is UByte, is Byte, is UShort, is Short, is UInt, is Int,
            is ULong, is Long, is Float, is Double -> listOf(
                createGetter(0, getter) to Color.white
            )
            Vector2f, is Vector2d, is Vector2i -> listOf(
                createGetter(0, getter) to axisXColor,
                createGetter(1, getter) to axisYColor,
            )
            Vector3f, is Vector3d, is Vector3i -> listOf(
                createGetter(0, getter) to axisXColor,
                createGetter(1, getter) to axisYColor,
                createGetter(2, getter) to axisZColor,
            )
            // todo AxisAngle?
            is Vector4f, is Vector4d, is Vector4i, is Quaternionf, is Quaterniond -> listOf(
                createGetter(0, getter) to axisXColor,
                createGetter(1, getter) to axisYColor,
                createGetter(2, getter) to axisZColor,
                createGetter(3, getter) to axisWColor,
            )
            is AABBf, is AABBd -> listOf(
                createGetter(0, getter) to axisXColor,
                createGetter(1, getter) to axisYColor,
                createGetter(2, getter) to axisZColor,
                createGetter(3, getter) to axisXColor,
                createGetter(4, getter) to axisYColor,
                createGetter(5, getter) to axisZColor,
            )
            // do we need other types?
            null -> return
            else -> {
                LOGGER.warn("Type ${sample::class} isn't handled")
                return
            }
        }
        val samples = instances.cross(channels, ArrayList(instances.size * channels.size))
        var dynPanel: Panel? = null
        val button = TextButton("\uD83D\uDC41\uFE0F", true, style)
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