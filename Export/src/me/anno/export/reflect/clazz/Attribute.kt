package me.anno.export.reflect.clazz

import me.anno.io.Streams.readNBytes2
import java.io.DataInputStream
import java.io.DataOutputStream

class Attribute(clazz: Clazz, input: DataInputStream) {
    val name = clazz.constantPool[input.readUnsignedShort()] as String
    val data = input.readNBytes2(input.readInt(), true)!!

    override fun toString(): String {
        return "'$name'+=${data.size}"
    }

    fun write(clazz: Clazz, dst: DataOutputStream) {
        dst.writeShort(clazz.getConstant(name))
        dst.writeInt(data.size)
        dst.write(data)
    }
}