package me.anno.objects.distributions

import me.anno.objects.InspectableVector
import org.joml.Vector4f

abstract class CenterSizeDistribution(
    displayName: String, description: String,
    val center: Vector4f, val size: Vector4f
) :
    Distribution(displayName, description) {

    override fun nextV1(): Float {
        return random.nextFloat() * size.x + center.x
    }

    override fun listProperties(): List<InspectableVector> {
        return listOf(
            InspectableVector(center, "Center"),
            InspectableVector(size, "Radius / Size")
        )
    }

}