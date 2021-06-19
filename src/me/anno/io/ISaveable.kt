package me.anno.io

import me.anno.io.base.BaseWriter
import me.anno.io.serialization.NotSerializableProperty
import me.anno.io.serialization.SerializableProperty
import org.joml.*
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.isAccessible

interface ISaveable {

    fun getClassName(): String

    /**
    a guess, small objects shouldn't contain large ones
    (e.g. human containing building vs building containing human)
     **/
    fun getApproxSize(): Int

    fun save(writer: BaseWriter)

    fun onReadingStarted() {}
    fun onReadingEnded() {}

    fun readBoolean(name: String, value: Boolean)
    fun readBooleanArray(name: String, values: BooleanArray)

    fun readByte(name: String, value: Byte)
    fun readByteArray(name: String, values: ByteArray)

    fun readShort(name: String, value: Short)
    fun readShortArray(name: String, values: ShortArray)

    fun readInt(name: String, value: Int)
    fun readIntArray(name: String, values: IntArray)

    fun readLong(name: String, value: Long)
    fun readLongArray(name: String, values: LongArray)

    fun readFloat(name: String, value: Float)
    fun readFloatArray(name: String, values: FloatArray)
    fun readFloatArray2D(name: String, values: Array<FloatArray>)

    fun readDouble(name: String, value: Double)
    fun readDoubleArray(name: String, values: DoubleArray)
    fun readDoubleArray2D(name: String, values: Array<DoubleArray>)

    fun readString(name: String, value: String)
    fun readStringArray(name: String, values: Array<String>)

    fun readObject(name: String, value: ISaveable?)
    fun readObjectArray(name: String, values: Array<ISaveable?>)

    fun readVector2f(name: String, value: Vector2f)
    fun readVector2fArray(name: String, values: Array<Vector2f>)

    fun readVector3f(name: String, value: Vector3f)
    fun readVector3fArray(name: String, values: Array<Vector3f>)

    fun readVector4f(name: String, value: Vector4f)
    fun readVector4fArray(name: String, values: Array<Vector4f>)

    fun readVector2d(name: String, value: Vector2d)
    fun readVector2dArray(name: String, values: Array<Vector2d>)

    fun readVector3d(name: String, value: Vector3d)
    fun readVector3dArray(name: String, values: Array<Vector3d>)

    fun readVector4d(name: String, value: Vector4d)
    fun readVector4dArray(name: String, values: Array<Vector4d>)

    // read matrices
    // array versions? idk...
    fun readMatrix3x3f(name: String, value: Matrix3f)
    fun readMatrix4x3f(name: String, value: Matrix4x3f)
    fun readMatrix4x4f(name: String, value: Matrix4f)

    fun readMatrix3x3d(name: String, value: Matrix3d)
    fun readMatrix4x3d(name: String, value: Matrix4x3d)
    fun readMatrix4x4d(name: String, value: Matrix4d)

    /**
     * can saving be ignored?, because this is default anyways?
     * */
    fun isDefaultValue(): Boolean

    fun saveSerializableProperties(writer: BaseWriter) {
        val clazz = this::class
        for (field in clazz.declaredMemberProperties) {
            val getter = field.getter
            val isPublic = field.visibility == KVisibility.PUBLIC
            val serial = field.findAnnotation<SerializableProperty>()
            val needsSerialization =
                serial != null || (isPublic && field.findAnnotation<NotSerializableProperty>() == null)
            if (needsSerialization) {
                try {
                    // save the field
                    var name = serial?.name
                    if (name == null || name.isEmpty()) name = field.name
                    // make sure we can access it
                    getter.isAccessible = true
                    val value = getter.call(this)
                    val forceSaving = serial?.forceSaving ?: value is Boolean
                    writer.writeSomething(this, name, value, forceSaving)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun isMethodPublic(field: Field, modifiers: Int): Boolean {
        if (Modifier.isPublic(modifiers)) return true
        val getter = try {
            javaClass.getDeclaredMethod("get${field.name.capitalize()}")
        } catch (e: NoSuchMethodException) {
            null
        }
        return getter != null && Modifier.isPublic(getter.modifiers)
    }

    companion object {

        val objectTypeRegistry = HashMap<String, () -> ISaveable>()

        @JvmStatic
        fun registerCustomClass(className: String, constructor: () -> ISaveable) {
            objectTypeRegistry[className] = constructor
        }

    }

}