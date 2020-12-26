package me.anno.io.binary

import me.anno.io.ISaveable
import me.anno.io.base.BaseReader
import me.anno.io.binary.BinaryTypes.*
import me.anno.utils.readNBytes2
import java.io.DataInputStream
import java.lang.RuntimeException

class BinaryReader(val input: DataInputStream) : BaseReader() {

    private val knownNames = ArrayList<String>()

    private val knownNameTypes = HashMap<String, ArrayList<NameType>>()

    private var currentClass = ""
    private var currentNameTypes = knownNameTypes.getOrPut(currentClass) { ArrayList() }

    private fun readEfficientString(): String? {
        val id = input.readInt()
        return when {
            id == -1 -> null
            id >= +0 -> knownNames[id]
            else -> {
                val length = -id-2
                val bytes = input.readNBytes2(length)
                val value = String(bytes)
                knownNames += value
                value
            }
        }
    }

    private fun readTypeString(): String {
        return readEfficientString()!!
    }

    fun readTypeName(id: Int): NameType {
        return if (id < 0) {
            currentNameTypes[id]
        } else {
            val name = readTypeString()
            val type = input.read().toChar()
            val value = NameType(name, type)
            currentNameTypes.add(value)
            value
        }
    }

    fun readTypeName(): NameType {
        return readTypeName(input.readInt())
    }

    override fun readObject(): ISaveable {
        val clazz = readTypeString()
        val obj = getNewClassInstance(clazz)
        // todo read all properties...
        while (true){
            val typeId = input.readInt()
            if(typeId < -1) break
            val typeName = readTypeName(typeId)
            val name = typeName.name
            when(typeName.type){
                BOOL -> obj.readBool(name, input.readByte() != 0.toByte())
                BYTE -> obj.readByte(name, input.readByte())
                SHORT -> obj.readShort(name, input.readShort())
                INT -> obj.readInt(name, input.readInt())
                LONG -> obj.readLong(name, input.readLong())
                FLOAT -> obj.readFloat(name, input.readFloat())
                DOUBLE -> obj.readDouble(name, input.readDouble())
                STRING -> obj.readString(name, readEfficientString() ?: throw RuntimeException("String must not be null"))
                OBJECT -> {
                    val ptr = input.readInt()
                    when {
                        ptr == -1 -> {
                            obj.readObject(name, null)
                        }
                        ptr >= 0 -> {
                            val child = content[ptr]
                            if (child == null) {
                                addMissingReference(obj, name, ptr)
                            } else {
                                obj.readObject(name, child)
                            }
                        }
                        else -> {
                            // todo it doesn't already have a pointer

                        }
                    }
                }
                else -> throw RuntimeException("Unknown type ${typeName.type}")
            }
        }
        return obj
    }

    override fun readAllInList() {
        TODO("Not yet implemented")
    }

}