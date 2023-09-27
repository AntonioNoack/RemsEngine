package me.anno.engine.ui.control

import me.anno.ecs.annotations.Range
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.studio.Inspectable
import org.joml.Vector3d

class SnappingSettings : Inspectable {

    @Range(0.0, Double.POSITIVE_INFINITY)
    var snapSize = 1.0

    @SerializedProperty
    var snapX = false

    @SerializedProperty
    var snapY = false

    @SerializedProperty
    var snapZ = false

    @SerializedProperty
    var snapCenter = false

    @NotSerializedProperty
    val snapRemainder = Vector3d()
}