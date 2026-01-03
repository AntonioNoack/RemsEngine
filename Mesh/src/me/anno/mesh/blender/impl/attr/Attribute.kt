package me.anno.mesh.blender.impl.attr

import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.BlendData

class Attribute(ptr: ConstructorData) : BlendData(ptr) {

    val name get() = charPointer("*name")

    val dataType = u16("data_type")
    val dataTypeEnum = AttributeType.entries.getOrNull(dataType)

    val domain = i8("domain")
    val domainEnum = AttributeDomain.entries.getOrNull(domain.toInt())

    /**
     * /** Some storage types are only relevant for certain attribute types. */
     * enum class AttrStorageType : int8_t {
     *   Array, /** #AttributeDataArray. */
     *   Single, /** A single value for the whole attribute. */
     * };
     * */
    val storageType = i8("storage_type")
    val isArray get() = storageType == 0.toByte()

    // AttributeArray or single (however that works -> directly RawData?)
    val data: BlendData? get() = getPointer("*data")

    override fun toString(): String = "($name[$dataTypeEnum,$domainEnum,${if (isArray) "Array" else "Single"}]=$data)"

    fun BlendData.next(): BlendData? {
        val address = positionInFile - block.dataOffset
        val self = file.getOrCreate(dnaStruct, dnaStruct.type.name, block, address)
        check(self === this)

        val stride = dnaStruct.type.sizeInBytes
        if (address + stride > block.address + block.sizeInBytes) return null // OOB

        return file.getOrCreate(dnaStruct, dnaStruct.type.name, block, address + stride)
    }
}