package me.anno.export.reflect.clazz

import me.anno.utils.structures.lists.Lists.createArrayList
import java.io.DataInputStream
import java.io.DataOutputStream

open class Member(clazz: Clazz, input: DataInputStream) {
    val accessFlags = input.readUnsignedShort()
    val name = clazz.constantPool[input.readUnsignedShort()] as String
    val descriptor = clazz.constantPool[input.readUnsignedShort()] as String
    val attributes = createArrayList(input.readUnsignedShort()) {
        Attribute(clazz, input)
    }

    override fun toString(): String {
        return "{ $name, $descriptor, 0x${accessFlags.toString(16)}, ${attributes.toList()} }"
    }

    fun write(clazz: Clazz, dst: DataOutputStream) {
        dst.writeShort(accessFlags)
        dst.writeShort(clazz.getConstant(name))
        dst.writeShort(clazz.getConstant(descriptor))
        dst.writeShort(attributes.size)
        for (attrib in attributes) {
            attrib.write(clazz, dst)
        }
    }
}