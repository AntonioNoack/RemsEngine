package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAField
import me.anno.mesh.blender.DNAStruct
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import java.nio.ByteBuffer

@Suppress("unused")
open class BlendData(
    val file: BlenderFile,
    val dnaStruct: DNAStruct,
    val buffer: ByteBuffer,
    var position: Int
) {

    val address get() = file.blockTable.getAddressAt(position)

    fun byte(offset: Int) = buffer.get(position + offset)
    fun uByte(offset: Int) = buffer.get(position + offset).toUByte()

    fun short(offset: Int) = buffer.getShort(position + offset)
    fun int(offset: Int) = buffer.getInt(position + offset)
    fun long(offset: Int) = buffer.getLong(position + offset)

    fun float(offset: Int) = buffer.getFloat(position + offset)
    fun double(offset: Int) = buffer.getDouble(position + offset)

    fun floats(offset: Int, size: Int): FloatArray {
        return FloatArray(size) { index ->
            float(offset + index.shl(2))
        }
    }

    // fields starting with **
    fun getPointerArray(name: String) = getPointerArray(getField(name))
    fun getPointerArray(field: DNAField?): Array<BlendData?>? {
        field ?: return null
        val pointer = pointer(field.offset)
        if (pointer == 0L) return null
        val block = file.blockTable.findBlock(file, pointer) ?: return null
        // all elements will be pointers to material
        val remainingSize = block.header.size - (pointer - block.header.address)
        val length = (remainingSize / file.pointerSize).toInt()
        if (length == 0) return null
        val positionInFile = block.positionInFile
        val data = file.file.data
        return Array(length) {
            val posInFile = positionInFile + it * file.pointerSize
            val ptr = if (file.file.is64Bit) data.getLong(posInFile)
            else data.getInt(posInFile).toLong()
            file.getOrCreate(file.structByName[field.type.name]!!, field.type.name, block, ptr)
        }
    }

    fun floats(name: String, size: Int): FloatArray =
        floats(getOffset(name), size)

    fun mat4x4(offset: Int): Matrix4f {
        // +x, +z, -y
        return Matrix4f(
            float(offset + 0), float(offset + 4), float(offset + 8), float(offset + 12),
            float(offset + 16), float(offset + 20), float(offset + 24), float(offset + 28),
            float(offset + 32), float(offset + 36), float(offset + 40), float(offset + 44),
            float(offset + 48), float(offset + 52), float(offset + 56), float(offset + 60)
        )
    }

    fun mat4x4(name: String): Matrix4f = mat4x4(getOffset(name))

    fun getField(name: String) = dnaStruct.byName[name]
    fun getOffset(name: String): Int {
        val byName = getField(name)
        if (byName != null) return byName.offset
        val bracketIndex = name.indexOf('[')
        if (bracketIndex >= 0) {
            val byName1 = dnaStruct.byName[name.substring(0, bracketIndex)]
            if (byName1 != null) return byName1.offset
        }
        if (name != "no[3]") {
            LOGGER.warn("field $name is unknown, available: ${dnaStruct.byName}")
        }// else no[3] is expected to be missing from newer Blender files
        return -1
    }

    fun getOffsetOrNull(name: String) = dnaStruct.byName[name]?.offset

    fun byte(name: String): Byte = byte(getOffset(name))
    fun short(name: String): Short = short(getOffset(name))
    fun int(name: String): Int = int(getOffset(name))
    fun float(name: String): Float = float(getOffset(name))
    fun float(name: String, defaultValue: Float): Float {
        val field = getField(name)
        return if (field != null) {
            float(field.offset)
        } else defaultValue
    }

    fun int(name: String, defaultValue: Int): Int {
        val field = getField(name)
        return if (field != null) {
            int(field.offset)
        } else defaultValue
    }

    fun string(name: String, limit: Int): String = string(getOffset(name), limit)
    fun string(offset: Int, limit: Int): String {
        val position = position + offset
        for (len in 0 until limit) {
            val char = buffer.get(position + len)
            if (char.toInt() == 0) {
                return getRawString(position, len)
            }
        }
        return getRawString(position, limit)
    }

    private fun getRawString(position: Int, length: Int): String {
        return String(raw(position, length))
    }

    fun raw(position: Int, length: Int): ByteArray {
        val bytes = ByteArray(length)
        val pos = buffer.position()
        // read bytes
        buffer.position(position)
        buffer.get(bytes)
        // reset position
        buffer.position(pos)
        return bytes
    }

    fun pointer(offset: Int) = if (file.pointerSize == 4) int(offset).toLong() else long(offset)

    fun inside(name: String) = inside(dnaStruct.byName[name])
    fun inside(field: DNAField?): BlendData? {
        field ?: return null
        // in-side object struct, e.g. ID
        val block = file.blockTable.getBlockAt(position)
        val address = block.header.address + (position - block.positionInFile) + field.offset
        val sameBlock = file.blockTable.findBlock(file, address)
        if (sameBlock != block) throw IllegalStateException("$position -> $address -> other, $sameBlock != $block")
        var className = field.type.name
        val type = file.dnaTypeByName[className]!!
        val struct: DNAStruct
        if (type.size == 0 || type.name == "void") {
            struct = file.structs[block.header.sdnaIndex]
            className = struct.type.name
        } else {
            struct = file.structByName[className]!!
        }
        // don't get, because the ptr may be defined, and that would be ourselves, if offset = 0
        return file.create(struct, className, block, address)
    }

    fun getStructArray(name: String): Array<BlendData?>? = getStructArray(dnaStruct.byName[name])
    fun getStructArray(field: DNAField?): Array<BlendData?>? {
        field ?: return null
        if (field.decoratedName.startsWith("*")) {
            var address = pointer(field.offset)
            if (address == 0L) return null
            val block = file.blockTable.findBlock(file, address) ?: return null
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
            if (length > 1000) LOGGER.warn("Instantiating $length ${struct.type.name} instances, use the BInstantList, if possible")
            // if(length > 1000000) throw RuntimeException()
            file.getOrCreate(struct, className, block, address) ?: return null
            return Array(length.toInt()) {
                val obj = file.getOrCreate(struct, className, block, address)
                address += typeSize
                obj
            }
        } else {
            return inside(field)
                .run { arrayOf(this) }
        }
    }


    fun <V : BlendData> getQuickStructArray(name: String): BInstantList<V>? =
        getQuickStructArray(dnaStruct.byName[name])

    fun <V : BlendData> getQuickStructArray(field: DNAField?): BInstantList<V>? {
        field ?: return null
        if (field.decoratedName.startsWith("*")) {
            val address = pointer(field.offset)
            if (address == 0L) return null
            val block = file.blockTable.findBlock(file, address) ?: return null
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
            val length = (remainingSize / typeSize).toInt()

            @Suppress("unchecked_cast")
            val instance = file.getOrCreate(struct, className, block, address) as? V ?: return null
            return BInstantList(length, instance)
        } else throw RuntimeException()
    }

    fun getPointer(name: String): BlendData? = getPointer(dnaStruct.byName[name])
    fun getPointer(field: DNAField?): BlendData? {
        field ?: return null
        return if (field.decoratedName.startsWith("*")) {
            val address = pointer(field.offset)
            if (address == 0L) return null
            val block = file.blockTable.findBlock(file, address) ?: return null
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
            remainingSize / typeSize
            file.getOrCreate(struct, className, block, address)
        } else {
            inside(field)
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(BlendData::class)
    }
}