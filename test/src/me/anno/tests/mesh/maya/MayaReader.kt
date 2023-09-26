package me.anno.tests.mesh.maya

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.utils.LOGGER
import me.anno.utils.OS.desktop
import me.anno.utils.structures.arrays.ExpandingIntArray
import me.anno.utils.types.Arrays.resize
import me.anno.utils.types.Floats.toRadians
import me.anno.utils.types.Strings.toDouble
import me.anno.utils.types.Strings.toFloat
import me.anno.utils.types.Strings.toInt
import org.joml.Quaterniond
import org.joml.Vector3d
import kotlin.math.max
import kotlin.test.assertEquals

/**
 * todo reading some Maya files from Synty Store
 * not working yet
 *
 * Maya Ascii 2015
 * */

fun split(data: String, list: ArrayList<CharSequence>) {
    var i = 0
    while (i < data.length) {
        val di = data[i]
        when {
            isSpace(di) -> i++
            di == '"' -> {
                val i0 = ++i
                while (i < data.length && data[i] != '"') i++
                list.add(data.subSequence(i0, i))
                i++
            }
            else -> {
                // read until space
                val i0 = i
                while (i < data.length && !isSpace(data[i])) i++
                list.add(data.subSequence(i0, i))
            }
        }
    }
}

fun isSpace(char: Char): Boolean {
    return when (char) {
        ' ', '\t', '\r', '\n' -> true
        else -> false
    }
}

fun named(arguments: ArrayList<CharSequence>, named: HashMap<String, CharSequence>) {
    var i = 0
    while (i < arguments.size) {
        val arg = arguments[i]
        if (arg.length >= 2 &&
            arg[0] == '-' && arg[1] !in '0'..'9'
        ) {
            named[arg.toString()] = if (i + 1 < arguments.size) {
                arguments.removeAt(i + 1)
            } else ""
            arguments.removeAt(i)
            i--
        } else i++
    }
}

fun main() {
    val file = desktop.getChild("Simple_Racer_StaticMeshes.ma")
    val debug = desktop.getChild("ma-debug")
    debug.tryMkdirs()

    /* println(1.0419103e-33)
     println(1.0419103e-33.toFloat())
     println("1.0419103e-033".toFloat())
     return*/

    // not supported by Assimp
    // println(AnimatedMeshesLoader.loadFile(file, defaultFlags))

    file.readText { text, e ->
        if (text != null) {
            var meshIndex = 0
            var node: PrefabSaveable? = null
            val nodes = ArrayList<PrefabSaveable>()
            val namedNodes = HashMap<String, PrefabSaveable>()
            val namedArguments = HashMap<String, CharSequence>()
            val arguments = ArrayList<CharSequence>()
            for (line in text.split(';')) {
                // todo remove comments
                val depth = line.indexOfFirst { it != '\t' }
                if (depth < 0) continue
                // todo split, but also respect spaces in comments
                arguments.clear()
                namedArguments.clear()
                split(line, arguments)
                named(arguments, namedArguments)
                println("$arguments, $namedArguments")
                fun finishNode() {
                    when (val m = node) {
                        is Mesh -> {
                            val dst = debug.getChild("${meshIndex++}.json")
                            dst.writeText(m.toString())
                            // GLTFWriter().write(m, dst)
                        }
                    }
                }
                when (arguments.first()) {
                    "requires" -> {}
                    "currentUnit" -> {}
                    "fileInfo" -> {}
                    "createNode" -> {
                        finishNode()
                        val name = namedArguments["-n"].toString()
                        when (val type = arguments[1]) {
                            "camera" -> {
                                node = Camera().apply { this.name = name }
                            }
                            "transform" -> {
                                node = Entity().apply { this.name = name }
                            }
                            "mesh" -> {
                                node = Mesh().apply { this.name = name }
                            }
                            "groupId" -> {
                                // idk...
                                node = null
                            }
                            else -> {
                                LOGGER.warn("Unknown node type $type")
                                node = null
                            }
                        }
                        if (node != null) {
                            namedNodes[name] = node
                            val parent = namedArguments["-p"]
                            if (parent != null) {
                                if (parent !in namedNodes) {
                                    LOGGER.warn("Missing parent $parent")
                                } else node.parent = namedNodes[parent]
                            }
                        }
                    }
                    "addAttr" -> {
                    }
                    "setAttr" -> {
                        when (val key = arguments[1]) {
                            ".t" -> {
                                node as Entity
                                node.transform.localPosition = Vector3d(
                                    arguments[2].toDouble(),
                                    arguments[3].toDouble(),
                                    arguments[4].toDouble()
                                )
                            }
                            ".r" -> {
                                node as Entity
                                node.transform.localRotation = Quaterniond()
                                    .rotateX(arguments[2].toDouble().toRadians())
                                    .rotateY(arguments[3].toDouble().toRadians())
                                    .rotateZ(arguments[4].toDouble().toRadians())
                            }
                            ".vt" -> {
                                node as Mesh
                                val size = namedArguments["-s"]!!.toInt() * 3
                                node.positions = node.positions.resize(size)
                            }
                            ".n" -> {
                                node as Mesh
                                val size = namedArguments["-s"]!!.toInt() * 3
                                node.normals = node.normals.resize(size)
                            }
                            else -> {

                                // setAttr -s 2 ".iog"; -> there are 2 iog-s (whatever that means)
                                // setAttr ".iog[0].og[0].gcl" -type "componentList" 1 "f[0:99]"; ->
                                //  iog [0] uses f[0:99] for og[0]

                                // setAttr -s 2 ".uvst"; -> there are 2 uv maps
                                // .uvst[0].uvsn -> uv map [0]

                                // todo decode start and end,
                                //  and then save data
                                if (key.startsWith(".vt[")) {
                                    // vertices
                                    node as Mesh

                                    if ("-s" in namedArguments) {
                                        val size = namedArguments["-s"]!!.toInt() * 3
                                        node.positions = node.positions.resize(size)
                                    }

                                    val colon = key.indexOf(':')
                                    val start = key.substring(4, colon).toInt()
                                    val end = key.substring(colon + 1, key.length - 1).toInt() + 1
                                    val dst = node.positions!!
                                    assertEquals((end - start) * 3, arguments.size - 2, key.toString())
                                    val offset = start * 3 - 2
                                    for (i in 2 until arguments.size) {
                                        dst[i + offset] = arguments[i].toFloat()
                                    }
                                } else if (key.startsWith(".n[")) {
                                    // normals
                                    node as Mesh

                                    if ("-s" in namedArguments) {
                                        val size = namedArguments["-s"]!!.toInt() * 3
                                        node.normals = node.normals.resize(size)
                                    }

                                    val colon = key.indexOf(':')
                                    val start = key.substring(3, colon).toInt()
                                    val end = key.substring(colon + 1, key.length - 1).toInt() + 1
                                    val dst = node.normals!!
                                    assertEquals((end - start) * 3, arguments.size - 2)
                                    val offset = start * 3 - 2
                                    for (i in 2 until arguments.size) {
                                        dst[i + offset] = arguments[i].toFloat()
                                    }
                                } else if (key.startsWith(".fc[")) {
                                    // face count
                                    node as Mesh
                                    // -s is the number of "f", -ch of "f" and "mu"
                                    val indices = ExpandingIntArray(64)
                                    // val colon = key.indexOf(':')
                                    // val start = key.substring(3, colon).toInt()
                                    // else we have a problem, and need to join multiple indices sections...
                                    var i = 2
                                    while (i < arguments.size) {
                                        when (arguments[i++]) {
                                            "f" -> {
                                                val numVertices = node.positions!!.size / 3
                                                val count = arguments[i++].toInt()
                                                val indices1 = IntArray(count) {
                                                    val idx = arguments[i++].toInt()
                                                    val idx1 = if (idx < 0) numVertices + idx else idx
                                                    if(idx1<0) println("Illegal index? $idx, #verts: $numVertices")
                                                    max(idx1, 0)
                                                }
                                                for (j in 2 until count) {
                                                    indices.add(indices1[0])
                                                    indices.add(indices1[j - 1])
                                                    indices.add(indices1[j])
                                                }
                                            }
                                            "mu" -> {
                                                i++ // can also be 1, idk what it means...
                                                // assertEquals("0", arguments[i++].toString())
                                                val count = arguments[i++].toInt()
                                                i += count
                                                // idk what data this is
                                            }
                                            "mc" -> {
                                                assertEquals("0", arguments[i++].toString())
                                                val count = arguments[i++].toInt()
                                                i += count
                                                // idk what data this is
                                            }
                                            else -> throw NotImplementedError(arguments[i].toString())
                                        }
                                    }
                                    // not ideal...
                                    node.indices = (node.indices ?: IntArray(0)) + indices.toIntArray()
                                } else if (key.startsWith(".ed[")) {
                                    // ???
                                }
                            }
                        }
                    }
                    "connectAttr" -> {
                        val k0 = arguments[1]
                        val k1 = arguments[2]
                    }
                }
            }
        } else e!!.printStackTrace()
    }
    Engine.requestShutdown()
}