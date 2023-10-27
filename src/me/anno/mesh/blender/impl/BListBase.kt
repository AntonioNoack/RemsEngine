package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

/**
 * doubly linked list
 * https://github.com/Blender/blender/blob/main/source/blender/makesdna/DNA_listBase.h
 * */
@Suppress("UNCHECKED_CAST")
class BListBase<Type>(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position), Iterable<Type> {

    val first get() = getPointer("*first") as? Type
    val last get() = getPointer("*last") as? Type

    val size: Int
        get() {
            var element = first
            var size = 0
            while (element != null) {
                element = (element as BLink<Type>).next
                size++
            }
            return size
        }

    fun toList(): List<Type> {
        var element = first
        val result = ArrayList<Type>(size)
        while (element != null) {
            result.add(element as Type)
            element = (element as BLink<Type>).next
        }
        return result
    }

    override fun iterator(): Iterator<Type> {
        var element = first
        return object : Iterator<Type> {
            override fun hasNext(): Boolean {
                return element != null
            }

            override fun next(): Type {
                val value = element!!
                element = (value as BLink<Type>).next
                return value
            }
        }
    }

    override fun toString(): String {
        return toList().toString()
    }
}