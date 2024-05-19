package me.anno.tests.mesh.blender

import me.anno.mesh.blender.BinaryFile
import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.impl.BMesh
import me.anno.mesh.blender.impl.BlendData
import me.anno.tests.LOGGER
import me.anno.utils.OS.documents
import me.anno.utils.types.Strings.spaces

fun main() {
    // inspect all properties
    val folder = documents.getChild("Blender")
    val src = folder.getChild("GlassMaterialTest.blend")
    val file = BlenderFile(BinaryFile(src.readByteBufferSync(false)), folder)
    println("Version ${file.version}")
    val mesh = file.instances["Mesh"]!!.first() as BMesh
    inspect(mesh, 0, 4)
}

fun inspect(structure: BlendData, indent: Int, depth: Int) {
    // todo I can't find the mesh indices :(, and 12 vertices is too little for a cube without them
    println("${spaces(2 * indent)}${structure.javaClass.simpleName}:")
    val pre = spaces(2 * (indent + 1))
    for (field in structure.dnaStruct.fields
        .filter { !it.decoratedName.startsWith("_pad") }
        .sortedBy { it.decoratedName }.sortedBy { it.type.name }) {
        val name = field.decoratedName
        val size = field.arraySizeOr1
        val idx = (0 until size)
        when (field.type.name) {
            "int" -> println("$pre$name: [i32] ${idx.map { structure.int(field.offset + 4 * it) }}")
            "float" -> println("$pre$name: [f32] ${idx.map { structure.float(field.offset + 4 * it) }}")
            "char" -> {
                if (name.endsWith(']')) println("$pre$name: '${structure.string(field.offset, 256)}'")
                else println("$pre$name: [i8] ${structure.byte(field.offset)}")
            }
            "short" -> println("$pre$name: [i16] ${idx.map { structure.short(field.offset + it * 2) }}")
            "ushort" -> println("$pre$name: [u16] ${idx.map { structure.short(field.offset + it * 2) }}")
            else -> {
                if (depth > 0) {
                    if (name.startsWith("*")) {
                        var err: String? = null
                        val values = try {
                            structure.getStructArray(field)
                        } catch (e: IllegalStateException) {
                            LOGGER.warn(e.message)
                            err = e.message
                            err = "$err (not constructable)"
                            null
                        }
                        if (values != null) {
                            println("$pre$name: ${values.size}x")
                            for (value in values) {
                                inspect(value!!, indent + 2, depth - 1)
                            }
                        } else println("$pre$name: $err")
                    } else {
                        var err: String? = null
                        val value = try {
                            structure.inside(field)
                        } catch (e: IllegalStateException) {
                            LOGGER.warn(e.message)
                            err = e.message
                            err = "$err (not constructable)"
                            null
                        }
                        if (value != null) {
                            println("$pre$name:")
                            inspect(value, indent + 2, depth - 1)
                        } else {
                            println("$pre$name: $err")
                        }
                    }
                } else println("$pre$name: ${field.type} @${field.offset}")
            }
        }
    }
}