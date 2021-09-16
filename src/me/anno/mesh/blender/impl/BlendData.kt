package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAField
import me.anno.mesh.blender.DNAStruct
import org.joml.Vector2f
import org.joml.Vector3f
import java.nio.ByteBuffer
import kotlin.math.min

open class BlendData(
    val file: BlenderFile,
    val type: DNAStruct,
    val buffer: ByteBuffer,
    val position: Int
) {

    fun short(offset: Int) = buffer.getShort(position + offset)
    fun int(offset: Int) = buffer.getInt(position + offset)
    fun long(offset: Int) = buffer.getLong(position + offset)

    fun float(offset: Int) = buffer.getFloat(position + offset)
    fun double(offset: Int) = buffer.getDouble(position + offset)

    fun getOffset(name: String) = type.byName[name]!!.offset

    fun short(name: String): Short = short(getOffset(name))
    fun int(name: String): Int = int(getOffset(name))
    fun float(name: String): Float = float(getOffset(name))

    fun string(name: String, limit: Int): String = string(getOffset(name), limit)
    fun string(offset: Int, limit: Int): String {
        val str = StringBuilder(min(64, limit))
        val position = position + offset
        for (i in 0 until limit) {
            val char = buffer.get(position + i)
            if (char.toInt() == 0) break
            str.append(char.toInt().and(255).toChar())
        }
        return str.toString()
    }

    fun vec2f(name: String): Vector2f = vec2f(getOffset(name))
    fun vec2f(offset: Int): Vector2f {
        return Vector2f(
            buffer.getFloat(position + offset),
            buffer.getFloat(position + offset + 4),
        )
    }

    fun vec3f(name: String) = vec3f(getOffset(name))
    fun vec3f(offset: Int): Vector3f {
        return Vector3f(
            buffer.getFloat(position + offset),
            buffer.getFloat(position + offset + 4),
            buffer.getFloat(position + offset + 8)
        )
    }

    fun vec3sNorm(offset: Int): Vector3f {
        val x = buffer.getShort(position + offset)
        val y = buffer.getShort(position + offset + 2)
        val z = buffer.getShort(position + offset + 4)
        return Vector3f(x.toFloat(), y.toFloat(), z.toFloat()).div(32767f)
    }

    fun pointer(offset: Int) = if (file.pointerSize == 4) int(offset).toLong() else long(offset)

    fun inside(name: String) = inside(type.byName[name])
    fun inside(field: DNAField?): BlendData? {
        field ?: return null
        // in-side object struct, e.g. ID
        val block = file.blockTable.getBlockAt(position)
        val address = block.header.address + (position - block.positionInFile) + field.offset
        if (file.blockTable.getBlock(address) != block) throw IllegalStateException("$position -> $address -> other")
        var className = field.type.name
        val type = file.dnaTypeByName[className]!!
        val struct: DNAStruct
        if (type.size == 0 || type.name == "void") {
            struct = file.structs[block.header.sdnaIndex]
            className = struct.type.name
        } else {
            struct = file.structByName[className]!!
        }
        return file.create(struct, className, block, address)
    }

    fun ptr(name: String): Array<BlendData?>? = ptr(type.byName[name])
    fun ptr(field: DNAField?): Array<BlendData?>? {
        field ?: return null
        if (field.decoratedName.startsWith("*")) {
            var address = pointer(field.offset)
            if (address == 0L) return null
            val block = file.blockTable.getBlock(address) ?: return null
            var className = field.type.name
            val type = file.dnaTypeByName[className]!!
            var typeSize = type.size
            val struct: DNAStruct
            if (type.size == 0 || type.name == "void") {
                struct = file.structs[block.header.sdnaIndex]
                className = struct.type.name
                typeSize = file.pointerSize
            } else {
                struct = file.structByName[className]!!
            }
            val addressInBlock = address - block.header.address
            val remainingSize = block.header.size - addressInBlock
            val length = remainingSize / typeSize
            return Array(length.toInt()) {
                val obj = file.create(struct, className, block, address)
                address += typeSize
                obj
            }
        } else throw IllegalArgumentException("Type must be ptr")
    }

}