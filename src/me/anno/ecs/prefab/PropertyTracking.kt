package me.anno.ecs.prefab

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
import kotlin.reflect.KProperty

object PropertyTracking {
    private val LOGGER = LogManager.getLogger(PropertyTracking::class)
    fun createTrackingButton(
        list: PanelList,
        insertAfter: PanelListX,
        relevantInstances: List<Any?>,
        property: KProperty<*>,
        style: Style
    ) {
        val getter = property.getter
        // when clicked, a tracking graph/plot is displayed (real time)
        val channels: List<Pair<(Any?) -> Double, Int>> = when (property.returnType.classifier) {
            Boolean::class,
            UByte::class, Byte::class, UShort::class, Short::class, UInt::class, Int::class,
            ULong::class, Long::class, Float::class, Double::class -> listOf(
                { it: Any? -> getDouble(getter.call(it), 0.0) } to Color.white
            )
            Vector2f::class, Vector2d::class, Vector2i::class -> listOf(
                { it: Any? -> getDouble(getter.call(it), 0, 0.0) } to axisXColor,
                { it: Any? -> getDouble(getter.call(it), 1, 0.0) } to axisYColor,
            )
            Vector3f::class, Vector3d::class, Vector3i::class -> listOf(
                { it: Any? -> getDouble(getter.call(it), 0, 0.0) } to axisXColor,
                { it: Any? -> getDouble(getter.call(it), 1, 0.0) } to axisYColor,
                { it: Any? -> getDouble(getter.call(it), 2, 0.0) } to axisZColor,
            )
            // todo AxisAngle?
            Vector4f::class, Vector4d::class, Vector4i::class, Quaternionf::class, Quaterniond::class -> listOf(
                { it: Any? -> getDouble(getter.call(it), 0, 0.0) } to axisXColor,
                { it: Any? -> getDouble(getter.call(it), 1, 0.0) } to axisYColor,
                { it: Any? -> getDouble(getter.call(it), 2, 0.0) } to axisZColor,
                { it: Any? -> getDouble(getter.call(it), 3, 0.0) } to axisWColor,
            )
            AABBf::class, AABBd::class -> listOf(
                { it: Any? -> getDouble(getter.call(it), 0, 0.0) } to axisXColor,
                { it: Any? -> getDouble(getter.call(it), 1, 0.0) } to axisYColor,
                { it: Any? -> getDouble(getter.call(it), 2, 0.0) } to axisZColor,
                { it: Any? -> getDouble(getter.call(it), 3, 0.0) } to axisXColor,
                { it: Any? -> getDouble(getter.call(it), 4, 0.0) } to axisYColor,
                { it: Any? -> getDouble(getter.call(it), 5, 0.0) } to axisZColor,
            )
            // do we need other types?
            else -> {
                LOGGER.warn("Type ${property.returnType} isn't handled")
                return
            }
        }
        val samples = relevantInstances.cross(channels, ArrayList(relevantInstances.size * channels.size))
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