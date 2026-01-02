package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.DNAField
import me.anno.mesh.blender.DNAStruct
import me.anno.mesh.blender.DNAType
import me.anno.mesh.blender.blocks.Block
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import java.nio.ByteBuffer

@Suppress("unused")
open class BlendData(val ptr: ConstructorData) {

    val file: BlenderFile get() = ptr.file
    val dnaStruct: DNAStruct get() = ptr.type
    val buffer: ByteBuffer = ptr.file.file.data // sooo many accesses -> let's be direct in this one case
    var positionInFile: Int = ptr.position
    val block: Block get() = file.blockTable.getBlockAt(positionInFile)

    val address get() = file.blockTable.getAddressAt(positionInFile)

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
        val length = (remainingSize / file.pointerSize).toInt()
        println("array length: $length")
        if (length == 0) return null
        val positionInFile = block.positionInFile
        val data = file.file.data
        return List(length) {
            val posInFile = positionInFile + it * file.pointerSize
            val ptr = if (file.file.is64Bit) data.getLong(posInFile)
            else data.getInt(posInFile).toLong()
            val struct = file.structByName[field.type.name]
                ?: throw IllegalStateException("Missing struct array of type ${field.type}")
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

    fun getField(name: String): DNAField? {
        var byName = dnaStruct.byName[name]
        if (byName != null) return byName
        val bracketIndex = name.indexOf('[')
        if (bracketIndex >= 0) {
            byName = dnaStruct.byName[name.substring(0, bracketIndex)]
            if (byName != null) return byName
        }
        return null
    }

    fun getOffset(name: String): Int {
        val byName = getField(name)
        if (byName != null) {
            return byName.offset
        }
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

    data class ResolvedPointer(
        val struct: DNAStruct,
        val className: String,
        val block: Block,
        val stride: Int,
        val length: Int,
    )

    private fun isPointerType(type: DNAType): Boolean = type.size == 0 || type.name == "void"

    fun getPartStruct(name: String) = getPartStruct(dnaStruct.byName[name])
    fun getPartStruct(field: DNAField?): BlendData? {
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
        if (isPointerType(type)) {
            struct = file.structs[block.sdnaIndex]
            className = struct.type.name
        } else {
            struct = file.structByName[className]!!
        }
        // don't get, because the ptr may be defined, and that would be ourselves, if offset = 0
        return file.getOrCreate(struct, className, block, address)
    }

    private fun resolvePointer(field: DNAField, address: Long): ResolvedPointer? {
        // println("reading instance list for $field")
        if (address == 0L) return null
        val block = file.blockTable.findBlock(file, address) ?: return null
        var className = field.type.name
        val type = file.dnaTypeByName[className]!!
        var stride = type.size
        val struct: DNAStruct
        if (isPointerType(type)) {
            struct = file.structs[block.sdnaIndex]
            className = struct.type.name
            stride = file.pointerSize
            // println("typeSize by pointer, ${type.size}/${type.name} | $struct")
        } else {
            struct = file.structByName[className]!!
            // println("typeSize by $type")
        }
        val addressInBlock = address - block.address
        val remainingSize = block.sizeInBytes - addressInBlock
        // println("Length: min($remainingSize/$typeSize, $maxSize)")
        val length = (remainingSize / stride).toInt()
        return ResolvedPointer(struct, className, block, stride, length)
    }

    fun getStructArray(name: String): List<BlendData>? = getStructArray(dnaStruct.byName[name])
    fun getStructArray(field: DNAField?): List<BlendData>? {
        field ?: return null
        return if (field.decoratedName.startsWith("*")) {
            val address = pointer(field.offset)
            val (struct, className, block, stride, length) = resolvePointer(field, address) ?: return null

            if (length > 1000) LOGGER.warn("Instantiating $length ${struct.type.name} instances, use the BInstantList, if possible")
            val canCreateInstance = file.getOrCreate(struct, className, block, address)
                ?: return null// if no instance can be created, just return null

            println("Reading list of $className from $address (by ${this.address}), x$length, stride: $stride")

            List(length) { instanceIndex ->
                if (instanceIndex == 0) {
                    canCreateInstance
                } else {
                    val addressI = address + instanceIndex * stride
                    file.getOrCreate(struct, className, block, addressI)!!
                }
            }
        } else {
            val instance = getPartStruct(field)
            if (instance != null) listOf(instance)
            else emptyList()
        }
    }

    fun <V : BlendData> getInstantList(name: String, maxSize: Int = Int.MAX_VALUE): BInstantList<V>? {
        return getInstantList(dnaStruct.byName[name], maxSize)
    }

    fun <V : BlendData> getInstantList(field: DNAField?, maxSize: Int): BInstantList<V>? {
        field ?: return null
        if (field.decoratedName.startsWith("*")) {
            val (struct, className, block, _, length) = resolvePointer(field, address) ?: return null
            val instance = file.create(struct, className, block, address) ?: return null
            @Suppress("unchecked_cast")
            return BInstantList(length, instance as? V)
        } else throw IllegalArgumentException("Expected pointer field")
    }

    fun getPointer(name: String): BlendData? = getPointer(dnaStruct.byName[name])
    fun getPointer(field: DNAField?): BlendData? {
        field ?: return null
        return if (field.decoratedName.startsWith("*")) {
            val address = pointer(field.offset)
            val (struct, className, block, _, _) = resolvePointer(field, address) ?: return null
            file.getOrCreate(struct, className, block, address)
        } else {
            getPartStruct(field)
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(BlendData::class)
    }
}