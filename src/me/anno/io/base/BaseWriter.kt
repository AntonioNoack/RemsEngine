package me.anno.io.base

import me.anno.io.ISaveable
import me.anno.io.Saveable
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.lang.RuntimeException
import java.util.*

abstract class BaseWriter {

    // in city project: writes all objects from the city into a file

    val listed = HashSet<ISaveable>()
    val pointers = HashMap<ISaveable, Int>()
    val todo = ArrayList<ISaveable>(256)

    // the type is important to notice incompatibilities by changes
    abstract fun writeBool(name: String, value: Boolean, force: Boolean = false)
    abstract fun writeByte(name: String, value: Byte, force: Boolean = false)
    abstract fun writeShort(name: String, value: Short, force: Boolean = false)
    abstract fun writeInt(name: String, value: Int, force: Boolean = false)
    abstract fun writeIntArray(name: String, value: IntArray, force: Boolean = false)
    abstract fun writeFloat(name: String, value: Float, force: Boolean = false)
    abstract fun writeDouble(name: String, value: Double, force: Boolean = false)
    abstract fun writeString(name: String, value: String?, force: Boolean = false)
    abstract fun writeLong(name: String, value: Long, force: Boolean = false)
    abstract fun writeVector2(name: String, value: Vector2f, force: Boolean = false)
    abstract fun writeVector3(name: String, value: Vector3f, force: Boolean = false)
    abstract fun writeVector4(name: String, value: Vector4f, force: Boolean = false)

    fun getOrCreatePtr(value: ISaveable): Int {
        var ptr = pointers[value]
        if(ptr != null) return ptr
        ptr = pointers.size + 1
        pointers[value] = ptr
        return ptr
    }

    fun writeObject(self: ISaveable?, name: String?, value: ISaveable?, force: Boolean = false){
        if(!force && (value == null || value.isDefaultValue())) return
        if(value == null){
            writeNull(name)
        } else {

            val ptr = getOrCreatePtr(value)

            if(value in listed){

                // println("child ${value.getClassName()} is on the list")
                writePointer(name, value.getClassName(), ptr)

            } else {

                listed += value
                val canInclude = self == null || self.getApproxSize() > value.getApproxSize()
                if(canInclude){
                    // println("child ${value.getClassName()} can be included")
                    writeObjectImpl(name, value)
                } else {
                    // println("child ${value.getClassName()} can not be included")
                    todo += value
                    writePointer(name, value.getClassName(), ptr)
                }

            }

        }
    }

    abstract fun writeNull(name: String?)
    abstract fun writePointer(name: String?, className: String, ptr: Int)
    abstract fun writeObjectImpl(name: String?, value: ISaveable)

    abstract fun <V: Saveable> writeList(self: ISaveable?, name: String, elements: List<V>?, force: Boolean = false)
    abstract fun writeListV2(name: String, elements: List<Vector2f>?, force: Boolean = false)
    abstract fun writeListV3(name: String, elements: List<Vector3f>?, force: Boolean = false)
    abstract fun writeListV4(name: String, elements: List<Vector4f>?, force: Boolean = false)

    fun add(obj: ISaveable){
        if(obj !in listed){
            getOrCreatePtr(obj)
            listed += obj
            todo += obj
        }
    }

    abstract fun writeListStart()
    abstract fun writeListEnd()
    abstract fun writeListSeparator()

    fun writeAllInList(){
        writeListStart()
        while(todo.isNotEmpty()){
            writeObjectImpl(null, todo.removeAt(0))
            if(todo.isNotEmpty()) writeListSeparator()
        }
        writeListEnd()
    }

    fun writeSomething(self: ISaveable, name: String, value: Any?, force: Boolean){
        when(value){
            is ISaveable -> writeObject(self, name, value, force)
            is Boolean -> writeBool(name, value, force)
            is Byte -> writeByte(name, value, force)
            is Short -> writeShort(name, value, force)
            is Int -> writeInt(name, value, force)
            is Long -> writeLong(name, value, force)
            is Float -> writeFloat(name, value, force)
            is Double -> writeDouble(name, value, force)
            is String -> writeString(name, value, force)
            is Vector2f -> writeVector2(name, value, force)
            is Vector3f -> writeVector3(name, value, force)
            null -> writeObject(self, name, value, force)
            else -> throw RuntimeException("todo implement saving an $value, maybe it needs to be me.anno.io.[I]Saveable?")
        }
    }

}