package me.anno.mesh.blender.impl.attr

import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.BlendData

class Attribute(ptr: ConstructorData) : BlendData(ptr) {

    val name get() = charPointer("*name")

    val dataType get() = u16("data_type")
    val dataTypeEnum get() = AttributeType.entries.getOrNull(dataType)

    val domain get() = i8("domain")
    val domainEnum get() = AttributeDomain.entries.getOrNull(domain.toInt())

    /**
     * /** Some storage types are only relevant for certain attribute types. */
     * enum class AttrStorageType : int8_t {
     *   Array, /** #AttributeDataArray. */
     *   Single, /** A single value for the whole attribute. */
     * };
     * */
    val storageType get() = i8("storage_type")
    val isArray get() = storageType == 0.toByte()

    // AttributeArray or single (however that works -> directly RawData?)
    val data get() = getPointer("*data")

    override fun toString(): String = "($name[$dataTypeEnum,$domainEnum,${if (isArray) "Array" else "Single"}]=$data)"
}