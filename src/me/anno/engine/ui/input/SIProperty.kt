package me.anno.engine.ui.input

import me.anno.engine.inspector.IProperty
import me.anno.io.Saveable
import me.anno.io.files.FileReference
import me.anno.io.find.DetectiveWriter
import me.anno.ui.Panel
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Matrix2d
import org.joml.Matrix2f
import org.joml.Matrix3d
import org.joml.Matrix3f
import org.joml.Matrix3x2d
import org.joml.Matrix3x2f
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import org.joml.Planed
import org.joml.Planef
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

/**
 * IProperty for Saveable
 * */
class SIProperty<V>(
    val name: String,
    val type: String,
    val saveable: Saveable,
    val startValue: V,
    val property: IProperty<Any?>,
    val detective: DetectiveWriter
) : IProperty<Any?> {

    val reflections = saveable.getReflections()

    // get matching annotations, if they are available
    override val annotations: List<Annotation>
        get() = reflections.allProperties[name]?.annotations ?: emptyList()

    override fun set(panel: Panel?, value: Any?, mask: Int) {
        saveable.setProperty(name, value)
        property.set(panel, saveable)
    }

    override fun get(): Any? {
        saveable.save(detective)
        return detective.dst[name]?.second
    }

    override fun getDefault(): Any? {
        // this will cause issues, if an Saveable saves ISaveables inside, and we assume we can just use this value without copying
        val sample = Saveable.getInstanceOf(saveable::class)
        return if (sample is Saveable) {
            sample.save(detective)
            detective.dst[name]?.second
        } else startValue
    }

    override fun reset(panel: Panel?): Any? {
        return getDefault()
    }

    override fun init(panel: Panel?) {
        // todo set boldness, if this is somehow changed
    }
}