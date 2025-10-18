package me.anno.tests.mesh.blender

import me.anno.cache.Promise.Companion.loadSync
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.io.files.inner.InnerFolderCache
import me.anno.mesh.blender.BinaryFile
import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.BlenderReader
import me.anno.mesh.blender.impl.BMesh
import me.anno.mesh.blender.impl.BlendData
import me.anno.mesh.gltf.GLTFWriter
import me.anno.tests.LOGGER
import me.anno.utils.OS.desktop
import me.anno.utils.OS.documents
import me.anno.utils.types.Strings.spaces

fun main() {
    // inspect all properties
    val folder = documents.getChild("Blender")
    val src = folder.getChild("GlassMaterialTest.blend")
    InnerFolderCache.registerSignatures("blend", BlenderReader::readAsFolder)
    val sample = MeshCache.getEntry(src).waitFor() as? Mesh
    println(sample)
    if (sample != null) {
        loadSync { GLTFWriter().write(sample, desktop.getChild(src.nameWithoutExtension + ".glb"), it) }
    }
    val file = BlenderFile(BinaryFile(src.readByteBufferSync(false)), folder)
    println("Version ${file.version}")
    val mesh = file.instances["Mesh"]!!.first() as BMesh
    inspect(mesh, 0, 4)
}

fun inspect(structure: BlendData, indent: Int, depth: Int) {
    println("${spaces(2 * indent)}${structure.javaClass.simpleName}[${structure.dnaStruct.type.size}]:")
    if (structure is BMesh) {
        println("  *polyOffsetIndices: ${structure.polyOffsetIndices?.toList()}")
    }
    val pre = spaces(2 * (indent + 1))
    for (field in structure.dnaStruct.fields
        .filter { !it.decoratedName.startsWith("_pad") }
        .sortedBy { it.decoratedName }.sortedBy { it.type.name }) {
        val name = field.decoratedName
        val size = field.arraySizeOr1
        val idx = (0 until size)
        when (field.type.name) {
            "int" -> println("$pre$name: [i32] ${idx.map { structure.i32(field.offset + 4 * it) }}")
            "float" -> println("$pre$name: [f32] ${idx.map { structure.f32(field.offset + 4 * it) }}")
            "char" -> {
                if (name.endsWith(']')) println("$pre$name: '${structure.string(field.offset, 256)}'")
                else println("$pre$name: [i8] ${structure.i8(field.offset)}")
            }
            "short" -> println("$pre$name: [i16] ${idx.map { structure.i16(field.offset + it * 2) }}")
            "ushort" -> println("$pre$name: [u16] ${idx.map { structure.i16(field.offset + it * 2) }}")
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