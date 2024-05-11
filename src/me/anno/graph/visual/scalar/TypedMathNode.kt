package me.anno.graph.visual.scalar

import me.anno.io.base.BaseWriter
import me.anno.utils.structures.maps.LazyMap

abstract class TypedMathNode<V : Enum<V>>(
    val dataMap: LazyMap<String, MathNodeData<V>>,
    val valueTypes: List<String>
) : MathNode<V>(dataMap[valueTypes.first()]) {

    override fun listNodes(): List<TypedMathNode<V>> {
        return valueTypes.flatMap { valueType ->
            data.enumValues.map { enumType ->
                @Suppress("UNCHECKED_CAST")
                val clone = this.clone() as TypedMathNode<V>
                clone.enumType = enumType
                clone.setDataType(valueType)
                clone
            }
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("dataType", data.inputs.first())
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "dataType" -> setDataType(value.toString())
            else -> super.setProperty(name, value)
        }
    }

    fun setDataType(type: String): TypedMathNode<V> {
        data = dataMap[type]
        updateNameDesc()
        return this
    }
}