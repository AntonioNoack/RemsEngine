package me.anno.objects.distributions

import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.objects.inspectable.InspectableVector
import me.anno.objects.Transform
import me.anno.animation.AnimatedProperty
import me.anno.animation.Type
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style
import me.anno.utils.structures.ValueWithDefault
import me.anno.utils.structures.ValueWithDefault.Companion.writeMaybe
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.*
import kotlin.collections.ArrayList

class AnimatedDistribution(
    distribution: Distribution = ConstantDistribution(),
    val types: List<Type>,
    val defaultValues: List<Any>
) : Saveable() {

    val distributionI = ValueWithDefault(distribution)
    var distribution: Distribution
        get() = distributionI.value
        set(value) {
            distributionI.value = value
        }

    constructor() : this(Type.ANY, 0f)
    constructor(type: Type, defaultValue: Any) : this(ConstantDistribution(), listOf(type), listOf(defaultValue))
    constructor(type: Type, defaultValues: List<Any>) : this(ConstantDistribution(), listOf(type), defaultValues)

    constructor(distribution: Distribution, type: Type, defaultValue: Any) : this(
        distribution,
        listOf(type),
        listOf(defaultValue)
    )

    constructor(distribution: Distribution, type: Type, defaultValues: List<Any>) : this(
        distribution,
        listOf(type),
        defaultValues
    )

    val channels = ArrayList<AnimatedProperty<*>>()
    lateinit var properties: List<InspectableVector>

    fun createInspector(
        list: PanelListY,
        transform: Transform,
        style: Style
    ) {
        if (lastDist !== distribution) update()
        properties.forEachIndexed { index, property ->
            if(property.pType == InspectableVector.PType.ROTATION) channels[index].type = Type.ROT_YXZ
            if(property.pType == InspectableVector.PType.SCALE) channels[index].type = Type.SCALE
            list += transform.vi(property.title, property.description, channels[index], style)
        }
    }

    fun copyFrom(data: Any?) {
        copyFrom(data as? AnimatedDistribution ?: return)
    }

    fun copyFrom(data: AnimatedDistribution) {
        distribution = data.distribution
        update()
        data.channels.forEachIndexed { index, channel ->
            setChannel(index, channel)
        }
    }

    private fun createChannel(index: Int): AnimatedProperty<*> {
        return AnimatedProperty<Any>(types[index % types.size]).apply {
            defaultValue = defaultValues[index % defaultValues.size]
        }
    }

    private var lastDist: Distribution? = null
    fun update() {
        if (lastDist === distribution) return
        properties = distribution.listProperties()
        while (properties.size > channels.size) channels += createChannel(channels.size)
        while (properties.size < channels.size) channels.removeAt(channels.lastIndex)
    }

    fun update(time: Double, random: Random) {
        if (lastDist !== distribution) update()
        distribution.random.setSeed(random.nextLong())
        properties.forEachIndexed { index, property ->
            when (types[index % types.size].components) {
                1 -> property.value.set(channels[index][time] as Float)
                2 -> property.value.set(channels[index][time] as Vector2f)
                3 -> property.value.set(channels[index][time] as Vector3f)
                4 -> property.value.set(channels[index][time] as Vector4f)
            }
        }
    }

    fun nextV1(time: Double, random: Random): Float {
        update(time, random)
        return distribution.nextV1()
    }

    fun nextV2(time: Double, random: Random): Vector2f {
        update(time, random)
        return distribution.nextV2()
    }

    fun nextV3(time: Double, random: Random): Vector3f {
        update(time, random)
        return distribution.nextV3()
    }

    fun nextV4(time: Double, random: Random): Vector4f {
        update(time, random)
        return distribution.nextV4()
    }

    private fun Vector4f.set(v: Vector2f) = set(v.x, v.y, 0f, 0f)
    private fun Vector4f.set(v: Vector3f) = set(v.x, v.y, v.z, 0f)

    override fun save(writer: BaseWriter) {
        super.save(writer)
        update()
        writer.writeMaybe(this, "distribution", distributionI)
        for (i in channels.indices) {
            writer.writeObject(this, "channel[$i]", channels[i])
        }
    }

    fun setChannel(index: Int, channel: AnimatedProperty<*>) {
        while (channels.size <= index) channels += createChannel(channels.size)
        channels[index].copyFrom(channel)
    }

    override fun readObject(name: String, value: ISaveable?) {
        update()
        when (name) {
            "distribution" -> distribution = value as? Distribution ?: return
            "channel[0]" -> setChannel(0, value as? AnimatedProperty<*> ?: return)
            "channel[1]" -> setChannel(1, value as? AnimatedProperty<*> ?: return)
            "channel[2]" -> setChannel(2, value as? AnimatedProperty<*> ?: return)
            "channel[3]" -> setChannel(3, value as? AnimatedProperty<*> ?: return)
            "channel[4]" -> setChannel(4, value as? AnimatedProperty<*> ?: return)
            "channel[5]" -> setChannel(5, value as? AnimatedProperty<*> ?: return)
            "channel[6]" -> setChannel(6, value as? AnimatedProperty<*> ?: return)
            "channel[7]" -> setChannel(7, value as? AnimatedProperty<*> ?: return)
            else -> super.readObject(name, value)
        }
    }

    override val approxSize get() = 35
    override fun isDefaultValue(): Boolean = !distributionI.isSet && channels.all { it.isDefaultValue() }
    override val className get() = "AnimatedDistribution"

}