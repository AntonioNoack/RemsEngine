package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.DNAField
import me.anno.mesh.blender.DNAStruct
import me.anno.mesh.blender.blocks.Block
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import java.nio.ByteBuffer
import kotlin.math.min

@Suppress("unused")
open class BlendData(val ptr: ConstructorData) {

    val file: BlenderFile get() = ptr.file
    val dnaStruct: DNAStruct get() = ptr.type
    val buffer: ByteBuffer = ptr.file.file.data // sooo many accesses -> let's be direct in this one case
    var positionInFile: Int = ptr.positionInFile
    val block: Block get() = file.blockTable.getBlockAt(positionInFile)

    fun i8(offset: Int) = if (offset < 0) 0 else buffer.get(positionInFile + offset)
    fun i16(offset: Int) = if (offset < 0) 0 else buffer.getShort(positionInFile + offset)
    fun u16(offset: Int) = if (offset < 0) 0 else buffer.getShort(positionInFile + offset).toInt().and(0xffff)
    fun i32(offset: Int) = if (offset < 0) 0 else buffer.getInt(positionInFile + offset)
    fun i64(offset: Int) = if (offset < 0) 0 else buffer.getLong(positionInFile + offset)
    fun f32(offset: Int) = if (offset < 0) 0f else f32Unsafe(offset)
    fun f32Unsafe(offset: Int) = buffer.getFloat(positionInFile + offset)

    fun f32s(name: String, size: Int, default: Float = 0f): FloatArray = f32s(getOffset(name), size, default)
    fun f32s(offset: Int, size: Int, default: Float = 0f): FloatArray {
        if (offset < 0) return FloatArray(size) { default }
        return FloatArray(size) { index ->
            f32Unsafe(offset + index.shl(2))
        }
    }

    // fields starting with **
    fun getPointerArray(name: String) = getPointerArray(getField(name))
    fun getPointerArray(field: DNAField?): List<BlendData?>? {
        field ?: return null
        val pointer = pointer(field.offset)
        if (pointer == 0L) return null

        val block = file.blockTable.findBlock(file, pointer) ?: return null
        // all elements will be pointers to material
        val remainingSize = block.sizeInBytes - (pointer - block.address)
        val length = min(remainingSize / file.pointerSize, Int.MAX_VALUE.toLong()).toInt()
        if (length <= 0) return null

        val positionInFile = block.positionInFile
        val data = file.file.data
        return List(length) {
            val posInFile = positionInFile + it * file.pointerSize
            val ptr = if (file.file.is64Bit) data.getLong(posInFile)
            else data.getInt(posInFile).toLong()
            val struct = file.structByName[field.type.name]
                ?: throw IllegalStateException("Missing struct array of type ${field.type}")
            // todo I don't get it, how is ptr valid with that given block???
            file.getOrCreate(struct, field.type.name, block, ptr)
        }
    }

    fun mat4x4(offset: Int): Matrix4f? {
        if (offset < 0) return null
        // +x, +z, -y
        return Matrix4f(
            f32(offset + 0), f32(offset + 4), f32(offset + 8), f32(offset + 12),
            f32(offset + 16), f32(offset + 20), f32(offset + 24), f32(offset + 28),
            f32(offset + 32), f32(offset + 36), f32(offset + 40), f32(offset + 44),
            f32(offset + 48), f32(offset + 52), f32(offset + 56), f32(offset + 60)
        )
    }

    fun mat4x4(name: String): Matrix4f? = mat4x4(getOffset(name))

    /**
     * Fields get duplicated into variants with array size and without.
     * Where the array size is important, use them, where not, discard them.
     * */
    fun getField(name: String): DNAField? {
        return dnaStruct.byName[name]
    }

    fun getOffset(name: String): Int {
        val field = getField(name)
        if (field != null) return field.offset

        if (name != "no[3]" && name != "obmat[4][4]") {
            LOGGER.warn("Field ${dnaStruct.type}.'$name' is unknown")
        }// else known to be missing from newer Blender files
        return -1
    }

    fun getOffsetOrNull(name: String) = dnaStruct.byName[name]?.offset

    fun i8(name: String): Byte = i8(getOffset(name))
    fun i16(name: String): Short = i16(getOffset(name))
    fun u16(name: String): Int = u16(getOffset(name))
    fun i32(name: String): Int = i32(getOffset(name))
    fun i64(name: String): Long = i64(getOffset(name))
    fun f32(name: String): Float = f32(getOffset(name))
    fun f32(name: String, defaultValue: Float): Float {
        val field = getField(name)
        return if (field != null) {
            f32(field.offset)
        } else defaultValue
    }

    fun i32(name: String, defaultValue: Int): Int {
        val field = getField(name)
        return if (field != null) {
            i32(field.offset)
        } else defaultValue
    }

    fun string(name: String, limit: Int): String? = string(getOffset(name), limit)
    fun string(offset: Int, limit: Int): String? {
        if (offset < 0) return null
        val position = positionInFile + offset
        for (len in 0 until limit) {
            val char = buffer.get(position + len)
            if (char.toInt() == 0) {
                return getRawString(position, len)
            }
        }
        return getRawString(position, limit)
    }

    private fun getRawString(position: Int, length: Int): String {
        return getRawI8s(position, length).decodeToString()
    }

    fun charPointer(name: String): String? = charPointer(getOffset(name))
    fun charPointer(offset: Int): String? {
        if (offset < 0) return null
        val address = pointer(offset)
        if (address == 0L) return null
        val block = file.blockTable.findBlock(file, address) ?: return null
        val addressInBlock = address - block.address
        val remainingSize = (block.sizeInBytes - addressInBlock).toInt()
        val position = (address + block.dataOffset).toInt()
        for (i in 0 until remainingSize) {
            if (buffer.get(position + i) == 0.toByte()) {
                return getRawString(position, i)
            }
        }
        // return max size string
        return getRawString(position, remainingSize)
    }

    fun getRawI8s(position: Int, length: Int): ByteArray {
        val bytes = ByteArray(length)
        val pos = buffer.position()
        // read bytes
        buffer.position(position)
        buffer.get(bytes)
        // reset position
        buffer.position(pos)
        return bytes
    }

    fun getRawI32s(position: Int, length: Int): IntArray {
        return IntArray(length) { buffer.getInt(position + it * 4) }
    }

    fun pointer(offset: Int): Long {
        return if (offset < 0) 0L else
            if (file.pointerSize == 4) i32(offset).toLong()
            else i64(offset)
    }

    fun inside(name: String) = inside(dnaStruct.byName[name])
    fun inside(field: DNAField?): BlendData? {
        field ?: return null

        // in-side object struct, e.g. ID
        val block = block
        val address = block.address + (positionInFile - block.positionInFile) + field.offset
        if (address >= block.address + block.sizeInBytes) {
            LOGGER.warn("Expected same block for ${block.address}+($positionInFile-${block.positionInFile})+${field.offset}, size: ${block.sizeInBytes}")
            return null
        }

        var className = field.type.name
        val type = file.dnaTypeByName[className]!!
        val struct: DNAStruct
        if (type.size == 0 || type.name == "void") {
            struct = file.structs[block.sdnaIndex]
            className = struct.type.name
        } else {
            struct = file.structByName[className]!!
        }
        // don't get, because the ptr may be defined, and that would be ourselves, if offset = 0
        return file.getOrCreate(struct, className, block, address)
    }

    fun getStructArray(name: String): List<BlendData>? = getStructArray(dnaStruct.byName[name])
    fun getStructArray(field: DNAField?): List<BlendData>? {
        field ?: return null
        return if (field.decoratedName.startsWith("*")) {
            val (address, block, className, struct, stride, length) = createStructInfo(field) ?: return null
            if (length > 1000) LOGGER.warn("Instantiating $length ${struct.type.name} instances, use the BInstantList, if possible")
            // if no instance can be created, just return null
            file.getOrCreate(struct, className, block, address) ?: return null
            List(length) { index ->
                val addressI = address + index * stride
                file.getOrCreate(struct, className, block, addressI)!!
            }
        } else {
            val instance = inside(field)
            if (instance != null) listOf(instance)
            else emptyList()
        }
    }

    fun <V : BlendData> getInstantList(name: String, maxSize: Int = Int.MAX_VALUE): List<V>? {
        // println("reading instance list for $name")
        return getInstantList(dnaStruct.byName[name], maxSize)
    }

    fun <V : BlendData> getInstantList(field: DNAField?, maxSize: Int): List<V>? {
        field ?: return null
        if (field.decoratedName.startsWith("*")) {
            val (address, block, className, struct, stride, length) = createStructInfo(field) ?: return null
            val instance = file.create(struct, className, block, address) ?: return null
            @Suppress("unchecked_cast")
            return BInstantList(length, stride, instance as? V)
        } else throw RuntimeException()
    }

    fun getPointer(name: String): BlendData? = getPointer(dnaStruct.byName[name])
    fun getPointer(field: DNAField?): BlendData? {
        field ?: return null
        return if (field.decoratedName.startsWith("*")) {
            val (address, block, className, struct) = createStructInfo(field) ?: return null
            file.getOrCreate(struct, className, block, address)
        } else {
            inside(field)
        }
    }

    private fun createStructInfo(field: DNAField): StructInfo? {
        val address = pointer(field.offset)
        if (address == 0L) return null
        val block = file.blockTable.findBlock(file, address) ?: return null
        var className = field.type.name
        val type = file.dnaTypeByName[className]!!
        var stride = type.size
        val struct: DNAStruct
        if (type.size == 0 || type.name == "void") {
            struct = file.structs[block.sdnaIndex]
            className = struct.type.name
            stride = file.pointerSize
        } else {
            struct = file.structByName[className]!!
        }
        val addressInBlock = address - block.address
        val remainingSize = block.sizeInBytes - addressInBlock
        val length = min(remainingSize / stride, Int.MAX_VALUE.toLong()).toInt()
        return StructInfo(address, block, className, struct, stride, length)
    }

    data class StructInfo(
        val address: Long, val block: Block,
        val className: String, val struct: DNAStruct,
        val stride: Int, val length: Int
    )

    companion object {
        private val LOGGER = LogManager.getLogger(BlendData::class)
    }
}