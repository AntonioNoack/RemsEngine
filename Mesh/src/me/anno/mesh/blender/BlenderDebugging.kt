package me.anno.mesh.blender

import me.anno.mesh.blender.impl.BlendData
import me.anno.mesh.blender.impl.primitives.BVector1i

object BlenderDebugging {

    fun BlendData.debugPrint() {
        println("${dnaStruct.type} @${address}")
        var prevField: DNAField? = null
        for ((name, field) in dnaStruct.byName) {
            if (field === prevField) continue
            prevField = field

            val value = getDebugValue(name, field)
            if (value != Unit) {
                println("  ${field.type.name} $name = $value")
            } else {
                println("  ${field.type.name} $name")
            }
        }
    }

    fun BlendData.getDebugValue(name: String, field: DNAField): Any? {
        val data = this
        val o = field.offset
        return if (field.isPointer) {
            when (field.type.name) {
                "int" -> data.getInstantList<BVector1i>(name)
                "char" -> {
                    val str = data.charPointer(o)
                    if (str == null) "null" else "\"${str}\""
                }
                "MEdge", "MLoop", "MVert", "MDeformVert",
                "MCol", "MFace", "MPoly", "MTFace", "MSelect",
                "TFace", "Attribute" -> data.getStructArray(field)
                "Mesh",
                "MeshRuntimeHandle",
                "BLI_mempool",
                "CustomDataExternal",
                "Key", "AnimData",
                    -> data.getPointer(field)
                else -> Unit
            }
        } else {
            when (field.type.name) {
                "char", "int8_t" -> {
                    if (field.arraySizeOr1 == 1) data.i8(o)
                    else List(field.arraySizeOr1) { data.i8(o + it) }
                }
                "short" -> {
                    if (field.arraySizeOr1 == 1) data.i16(o)
                    else List(field.arraySizeOr1) { data.i16(o + it * 2) }
                }
                "ushort" -> {
                    if (field.arraySizeOr1 == 1) data.u16(o)
                    else List(field.arraySizeOr1) { data.u16(o + it * 2) }
                }
                "int" -> {
                    if (field.arraySizeOr1 == 1) data.i32(o)
                    else List(field.arraySizeOr1) { data.i32(o + it * 4) }
                }
                "float" -> {
                    if (field.arraySizeOr1 == 1) data.f32(o)
                    else List(field.arraySizeOr1) { data.f32(o + it * 4) }
                }
                "ID",
                "CustomData",
                "ListBase",
                "AttributeStorage" -> data.inside(field)
                else -> Unit
            }
        }
    }

    fun BlendData.toStringImpl(): String {
        return "${dnaStruct.type}@$address { ${
            dnaStruct.byName.map { (name, field) ->
                val value = getDebugValue(name, field)
                if (value == Unit) "${field.type} $name"
                else "${field.type} $name = $value"
            }
        } }"
    }
}