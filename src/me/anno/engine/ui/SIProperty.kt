package me.anno.engine.ui

import me.anno.engine.IProperty
import me.anno.io.ISaveable
import me.anno.io.files.FileReference
import me.anno.ui.Panel
import org.joml.*

class SIProperty<V>(
    val name: String,
    val type: String,
    val saveable: ISaveable,
    val startValue: V,
    val property: IProperty<Any?>,
    val detective: DetectiveWriter
) : IProperty<Any?> {

    val reflections = saveable.getReflections()

    // get matching annotations, if they are available
    override val annotations: List<Annotation>
        get() = reflections.allProperties[name]?.annotations ?: emptyList()

    override fun set(panel: Panel?, value: Any?) {
        // todo depending on the type, call a different "setter"
        // better depending on actual type first...
        // (except idk whether that works completely reliably in JavaScript
        // todo all types, and crash previously, if a type is not supported, so we have a fallback...
        when (value) {
            is Boolean -> saveable.readBoolean(name, value)
            is BooleanArray -> saveable.readBooleanArray(name, value)
            is Byte -> saveable.readByte(name, value)
            is ByteArray -> saveable.readByteArray(name, value)
            is Char -> saveable.readChar(name, value)
            is CharArray -> saveable.readCharArray(name, value)
            is Short -> saveable.readShort(name, value)
            is ShortArray -> saveable.readShortArray(name, value)
            is Int -> saveable.readInt(name, value)
            is IntArray -> saveable.readIntArray(name, value)
            is Long -> saveable.readLong(name, value)
            is LongArray -> saveable.readLongArray(name, value)
            is Float -> saveable.readFloat(name, value)
            is FloatArray -> saveable.readFloatArray(name, value)
            is Double -> saveable.readDouble(name, value)
            is DoubleArray -> saveable.readDoubleArray(name, value)
            is String -> saveable.readString(name, value)
            is Vector2f -> saveable.readVector2f(name, value)
            is Vector3f -> saveable.readVector3f(name, value)
            is Vector4f -> saveable.readVector4f(name, value)
            is Vector2d -> saveable.readVector2d(name, value)
            is Vector3d -> saveable.readVector3d(name, value)
            is Vector4d -> saveable.readVector4d(name, value)
            is Vector2i -> saveable.readVector2i(name, value)
            is Vector3i -> saveable.readVector3i(name, value)
            is Vector4i -> saveable.readVector4i(name, value)
            is Matrix2f -> saveable.readMatrix2x2f(name, value)
            is Matrix3f -> saveable.readMatrix3x3f(name, value)
            is Matrix4f -> saveable.readMatrix4x4f(name, value)
            is Matrix3x2f -> saveable.readMatrix3x2f(name, value)
            is Matrix4x3f -> saveable.readMatrix4x3f(name, value)
            is Matrix2d -> saveable.readMatrix2x2d(name, value)
            is Matrix3d -> saveable.readMatrix3x3d(name, value)
            is Matrix4d -> saveable.readMatrix4x4d(name, value)
            is Matrix3x2d -> saveable.readMatrix3x2d(name, value)
            is Matrix4x3d -> saveable.readMatrix4x3d(name, value)
            is FileReference -> saveable.readFile(name, value)
            is Quaternionf -> saveable.readQuaternionf(name, value)
            is Quaterniond -> saveable.readQuaterniond(name, value)
            is Planef -> saveable.readPlanef(name, value)
            is Planed -> saveable.readPlaned(name, value)
            is AABBf -> saveable.readAABBf(name, value)
            is AABBd -> saveable.readAABBd(name, value)
            is Map<*, *> -> saveable.readMap(name, value as Map<Any?, Any?>)
            else -> TODO("implement type $type for class ${saveable.className}")
        }
        property.set(panel, saveable)
    }

    override fun get(): Any? {
        saveable.save(detective)
        return detective.dst[name]?.second
    }

    override fun getDefault(): Any? {
        // this will cause issues, if an ISaveable saves ISaveables inside, and we assume we can just use this value without copying
        val sample = ISaveable.getInstanceOf(saveable::class)
        return if (sample is ISaveable) {
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