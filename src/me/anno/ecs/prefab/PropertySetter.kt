package me.anno.ecs.prefab

import me.anno.engine.inspector.CachedProperty
import me.anno.maths.Maths.hasFlag
import org.joml.Vector2f
import org.joml.Vector2d
import org.joml.Vector3f
import org.joml.Vector3d
import org.joml.Vector4f
import org.joml.Vector4d

object PropertySetter {
    fun setPropertyRespectingMask(value: Any?, mask: Int, property: CachedProperty, instance: Any){
        when (value) {
            is Vector2f -> {
                val value1 = property[instance] as Vector2f
                if (mask.hasFlag(1)) value1.x = value.x
                if (mask.hasFlag(2)) value1.y = value.y
                property[instance] = value1
            }
            is Vector2d -> {
                val value1 = property[instance] as Vector2d
                if (mask.hasFlag(1)) value1.x = value.x
                if (mask.hasFlag(2)) value1.y = value.y
                property[instance] = value1
            }
            is Vector3f -> {
                val value1 = property[instance] as Vector3f
                if (mask.hasFlag(1)) value1.x = value.x
                if (mask.hasFlag(2)) value1.y = value.y
                if (mask.hasFlag(4)) value1.z = value.z
                property[instance] = value1
            }
            is Vector3d -> {
                val value1 = property[instance] as Vector3d
                if (mask.hasFlag(1)) value1.x = value.x
                if (mask.hasFlag(2)) value1.y = value.y
                if (mask.hasFlag(4)) value1.z = value.z
                property[instance] = value1
            }
            is Vector4f -> {
                val value1 = property[instance] as Vector4f
                if (mask.hasFlag(1)) value1.x = value.x
                if (mask.hasFlag(2)) value1.y = value.y
                if (mask.hasFlag(4)) value1.z = value.z
                if (mask.hasFlag(8)) value1.w = value.w
                property[instance] = value1
            }
            is Vector4d -> {
                val value1 = property[instance] as Vector4d
                if (mask.hasFlag(1)) value1.x = value.x
                if (mask.hasFlag(2)) value1.y = value.y
                if (mask.hasFlag(4)) value1.z = value.z
                if (mask.hasFlag(8)) value1.w = value.w
                property[instance] = value1
            }
            else -> {
                property[instance] = value
            }
        }
    }
}