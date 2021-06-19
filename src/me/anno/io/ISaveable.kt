package me.anno.io

import me.anno.io.base.BaseWriter
import me.anno.io.serialization.NotSerializableProperty
import me.anno.io.serialization.SerializableProperty
import me.anno.utils.LOGGER
import org.joml.*
import java.io.Serializable
import java.lang.reflect.Field
import java.lang.reflect.Modifier

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
    fun readMatrix3f(name: String, value: Matrix3f)
    fun readMatrix4x3f(name: String, value: Matrix4x3f)
    fun readMatrix4f(name: String, value: Matrix4f)

    fun readMatrix3d(name: String, value: Matrix3d)
    fun readMatrix4x3d(name: String, value: Matrix4x3d)
    fun readMatrix4d(name: String, value: Matrix4d)

    /**
     * can saving be ignored?, because this is default anyways?
     * */
    fun isDefaultValue(): Boolean

    fun saveSerializableProperties(writer: BaseWriter) {
        println("ssp")
        val clazz = javaClass
        for (field in clazz.declaredFields) {
            val modifiers = field.modifiers
            val serial = field.getAnnotation(SerializableProperty::class.java)
            val needsSerialization =
                serial != null ||
                        (isMethodPublic(
                            field,
                            modifiers
                        ) && field.getAnnotation(NotSerializableProperty::class.java) == null)
            // println("${field.name}: $needsSerialization")
            if (needsSerialization) {
                // todo save the field...
                var name = serial?.name
                if (name == null || name.isEmpty()) name = field.name
                name!!
                // todo when it is a boolean, the default value needs to be true
                val forceSaving = serial?.forceSaving ?: false
                field.isAccessible = true
                when (field.type) {

                    Boolean::class.java -> writer.writeBoolean(name, field.getBoolean(this), forceSaving)
                    // Char::class.java -> writer.writeChar(name, field.getChar(this), forceSaving)

                    Byte::class.java -> writer.writeByte(name, field.getByte(this), forceSaving)
                    Short::class.java -> writer.writeShort(name, field.getShort(this), forceSaving)
                    Int::class.java -> writer.writeInt(name, field.getInt(this), forceSaving)
                    Long::class.java -> writer.writeLong(name, field.getLong(this), forceSaving)

                    Float::class.java -> writer.writeFloat(name, field.getFloat(this), forceSaving)
                    Double::class.java -> writer.writeDouble(name, field.getDouble(this), forceSaving)

                    else -> writeSomething(writer, name, field.get(this), forceSaving)

                }
                println(field.type)
            }
        }
    }

    fun writeSomething(writer: BaseWriter, name: String, value: Any?, forceSaving: Boolean) {
        when (value) {
            is String -> writer.writeString(name, value, forceSaving)
            is ISaveable -> writer.writeObject(this, name, value)
            is List<*> -> {
                // try to save the list
                if (value.isNotEmpty()) {
                    when (val sample = value[0]) {

                        else -> {
                            // todo implement saving a list of $sample
                            LOGGER.warn("Not yet implemented: saving a list of $sample")
                        }
                    }
                } // else if is force saving, then this won't work, because of the weak generics in Java :/
            }
            is Array<*> -> {
                if (value.isNotEmpty()) {
                    when (val sample = value[0]) {
                        is String -> writer.writeStringArray(name, value as Array<String>, forceSaving)
                        is FloatArray -> writer.writeFloatArray2D(name, value as Array<FloatArray>, forceSaving)
                        is DoubleArray -> writer.writeDoubleArray2D(name, value as Array<DoubleArray>, forceSaving)
                        is ISaveable -> writer.writeObjectArray(this, name, value as Array<ISaveable>, forceSaving)
                        else -> {
                            // todo implement saving an array of $sample
                            LOGGER.warn("Not yet implemented: saving an array of $sample")
                        }
                    }
                } // else if is force saving, then this won't work, because of the weak generics in Java :/
            }
            is BooleanArray -> writer.writeBooleanArray(name, value, forceSaving)
            is ByteArray -> writer.writeByteArray(name, value, forceSaving)
            is ShortArray -> writer.writeShortArray(name, value, forceSaving)
            is IntArray -> writer.writeIntArray(name, value, forceSaving)
            is LongArray -> writer.writeLongArray(name, value, forceSaving)
            is FloatArray -> writer.writeFloatArray(name, value, forceSaving)
            is DoubleArray -> writer.writeDoubleArray(name, value, forceSaving)
            // all vectors and such
            is Vector2f -> writer.writeVector2f(name, value, forceSaving)
            is Vector3f -> writer.writeVector3f(name, value, forceSaving)
            is Vector4f -> writer.writeVector4f(name, value, forceSaving)
            is Vector2d -> writer.writeVector2d(name, value, forceSaving)
            is Vector3d -> writer.writeVector3d(name, value, forceSaving)
            is Vector4d -> writer.writeVector4d(name, value, forceSaving)
            is Matrix3f -> writer.writeMatrix3f(name, value, forceSaving)
            is Matrix4x3f -> writer.writeMatrix4x3f(name, value, forceSaving)
            is Matrix4f -> writer.writeMatrix4f(name, value, forceSaving)
            is Matrix3d -> writer.writeMatrix3d(name, value, forceSaving)
            is Matrix4x3d -> writer.writeMatrix4x3d(name, value, forceSaving)
            is Matrix4d -> writer.writeMatrix4d(name, value, forceSaving)
            null -> if (forceSaving) {
                writer.writeNull(name)
            }
            is Serializable -> {
                // implement it?...
                LOGGER.warn("Could not serialize field $name with value $value, Serializable")
            }
            else -> {
                LOGGER.warn("Could not serialize field $name with value $value")
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