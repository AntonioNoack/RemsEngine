package me.anno.mesh.blender.impl.attr

import com.sun.org.slf4j.internal.LoggerFactory
import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.DNAField
import me.anno.mesh.blender.DNAStruct
import me.anno.mesh.blender.DNAType
import me.anno.mesh.blender.impl.BInstantList
import me.anno.mesh.blender.impl.BlendData
import me.anno.mesh.blender.impl.primitives.BVector1i
import me.anno.mesh.blender.impl.primitives.BVector2f
import me.anno.mesh.blender.impl.primitives.BVector3f

class AttributeStorage(ptr: ConstructorData) : BlendData(ptr) {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AttributeStorage::class.java)

        private val vec1iType = lazy {
            val type0 = DNAType("vec1i", 4)
            val floatType = DNAType("int", 4)
            val fields = arrayOf(DNAField(0, "i", floatType))
            DNAStruct(-1, type0, fields, -1)
        }

        private val vec2fType = lazy {
            val type0 = DNAType("vec2f", 12)
            val floatType = DNAType("float", 4)
            DNAStruct(
                -1, type0, arrayOf(
                    DNAField(0, "x", floatType),
                    DNAField(1, "y", floatType),
                ), -1
            )
        }

        private val vec3fType = lazy {
            val type0 = DNAType("vec3f", 12)
            val floatType = DNAType("float", 4)
            DNAStruct(
                -1, type0, arrayOf(
                    DNAField(0, "x", floatType),
                    DNAField(1, "y", floatType),
                    DNAField(2, "z", floatType),
                ), -1
            )
        }

    }

    @Suppress("UNCHECKED_CAST")
    val attributes: List<Attribute>
        get() = getStructArray("*dna_attributes")?.toList() as? List<Attribute> ?: emptyList()

    override fun toString(): String = attributes.toString()

    fun findAttribute(
        name: String,
        dataType: AttributeType
    ): AttributeArray? {
        val attribute = attributes.firstOrNull { it.name == name }
            ?: return null

        if (!attribute.isArray) {
            LOGGER.warn("Expected positions to be array")
            return null
        }

        if (attribute.dataTypeEnum != dataType) {
            LOGGER.warn("Expected positions to be $dataType array")
            return null
        }

        val data = attribute.data as? AttributeArray
        if (data == null) {
            LOGGER.warn("Expected positions.data to be AttributeArray")
            return null
        }

        return data
    }

    fun loadVector1iArray(name: String): BInstantList<BVector1i>? {
        val data = findAttribute(name, AttributeType.Int32)
            ?: return null

        val type = data.file.structByName["vec1i"] ?: vec1iType.value
        return createArray(data, type, ::BVector1i)
    }

    fun loadVector2fArray(name: String): BInstantList<BVector2f>? {
        val data = findAttribute(name, AttributeType.Float2)
            ?: return null

        val type = data.file.structByName["vec2f"] ?: vec2fType.value
        return createArray(data, type, ::BVector2f)
    }

    fun loadVector3fArray(name: String): BInstantList<BVector3f>? {
        val data = findAttribute(name, AttributeType.Float3)
            ?: return null

        val type = data.file.structByName["vec3f"] ?: vec3fType.value
        return createArray(data, type, ::BVector3f)
    }

    private fun <V : BlendData> createArray(
        data: AttributeArray, type: DNAStruct,
        createInstance: (ConstructorData) -> V
    ): BInstantList<V>? {
        val address = data.dataPtr
        val block = file.blockTable.findBlock(file, address) ?: return null
        val position = (address + block.dataOffset).toInt()
        val instance = createInstance(ConstructorData(file, type, position))
        return BInstantList(data.size.toInt(), instance)
    }

}