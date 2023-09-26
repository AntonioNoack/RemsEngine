package me.anno.tests.mesh.maya

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.mesh.gltf.GLTFWriter
import me.anno.utils.LOGGER
import me.anno.utils.OS.desktop
import me.anno.utils.structures.arrays.ExpandingIntArray
import me.anno.utils.types.Arrays.resize
import me.anno.utils.types.Booleans.toInt
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
    val dst = desktop.getChild("ma")
    dst.tryMkdirs()

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

            var edgeIndices: IntArray? = null

            class Face(val indices: IntArray) {
                var uvIndices: IntArray? = null
            }

            val faces = ArrayList<Face>()

            fun buildMeshIndices() {
                // decrypt face and edge indices into triangle faces
                val mesh = node as Mesh
                val ei = edgeIndices!!
                val triangleCount = faces.sumOf { max(it.indices.size - 2, 0) }
                val triangleIndices = IntArray(triangleCount * 3)
                val uvs = mesh.uvs!!
                val newUVs = FloatArray(triangleCount * 6)
                var ti = 0
                var ui = 0
                for (faceI in faces) {
                    val face = faceI.indices
                    if (face.size < 3) continue

                    val vertexIndices = IntArray(face.size) {
                        val fi = face[it]
                        ei[(if (fi < 0) -fi - 1 else fi) * 2 + (fi < 0).toInt()]
                    }

                    val uvIndices = faceI.uvIndices
                    fun push(i: Int) {
                        triangleIndices[ti++] = vertexIndices[i]
                        if (uvIndices != null) {
                            val j = uvIndices[i] * 2
                            newUVs[ui++] = uvs[j]
                            newUVs[ui++] = uvs[j + 1]
                        }
                    }

                    for (i in 2 until face.size) {
                        push(0)
                        push(i - 1)
                        push(i)
                    }
                }
                mesh.uvs = null
                mesh.indices = triangleIndices
                mesh.makeFlatShaded(false)
                mesh.uvs = newUVs
            }

            fun finishNode() {
                when (val m = node) {
                    is Mesh -> {
                        buildMeshIndices()
                        GLTFWriter().write(m, dst.getChild("mesh-${meshIndex++}.glb"))
                        // testSceneWithUI("Mesh", m)
                        // throw EOFException()
                    }
                }
                faces.clear()
            }

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
                                // ignored for now
                                /*node as Mesh
                                val size = namedArguments["-s"]!!.toInt() * 3
                                node.normals = node.normals.resize(size)*/
                            }
                            ".ed" -> {
                                node as Mesh
                                val size = namedArguments["-s"]!!.toInt() * 2
                                edgeIndices = edgeIndices.resize(size)
                            }
                            ".uvst[0].uvsp" -> {
                                node as Mesh
                                val size = namedArguments["-s"]!!.toInt() * 2
                                node.uvs = node.uvs.resize(size)
                            }
                            else -> {

                                // setAttr -s 2 ".iog"; -> there are 2 iog-s (whatever that means)
                                // setAttr ".iog[0].og[0].gcl" -type "componentList" 1 "f[0:99]"; ->
                                //  iog [0] uses f[0:99] for og[0]

                                // setAttr -s 2 ".uvst"; -> there are 2 uv maps
                                // .uvst[0].uvsn -> uv map [0]

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
                                    // normals, ignored for now, seem to depend on edge
                                    /*node as Mesh

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
                                    }*/
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
                                        // https://cgkit.sourceforge.net/doc2/mayaascii.html
                                        when (arguments[i++]) {
                                            "f" -> {
                                                // "edge indices", negative = reversed
                                                // https://forums.cgsociety.org/t/parsing-maya-ascii/928039
                                                val count = arguments[i++].toInt()
                                                faces += Face(IntArray(count) { arguments[i++].toInt() })
                                            }
                                            "mu" -> {
                                                // loop / outer loop or hole
                                                val uvIndex = arguments[i++].toInt()
                                                val count = arguments[i++].toInt()
                                                if (uvIndex == 0) {
                                                    faces.last().uvIndices = IntArray(count) { arguments[i++].toInt() }
                                                } else {
                                                    // multiple UVs are not yet supported
                                                    i += count // indices
                                                }
                                            }
                                            "mc" -> {
                                                // ???
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
                                    // edge indices, faces are built from them
                                    if ("-s" in namedArguments) {
                                        val size = namedArguments["-s"]!!.toInt() * 2
                                        edgeIndices = edgeIndices.resize(size)
                                    }
                                    val colon = key.indexOf(':')
                                    val start = key.substring(4, colon).toInt()
                                    var k = start * 2
                                    edgeIndices!!
                                    for (i in 2 until arguments.size step 3) {
                                        edgeIndices[k++] = arguments[i].toInt()
                                        edgeIndices[k++] = arguments[i + 1].toInt()
                                    }
                                } else if (key.startsWith(".uvst[0].uvsp[")) {
                                    node as Mesh
                                    // todo what do the UVs belong to?
                                    if ("-s" in namedArguments) {
                                        val size = namedArguments["-s"]!!.toInt() * 2
                                        node.uvs = node.uvs.resize(size)
                                    }
                                    val dst = node.uvs!!
                                    val colon = key.indexOf(':')
                                    val start = key.substring(".uvst[0].uvsp[".length, colon).toInt()
                                    val end = key.substring(colon + 1, key.length - 1).toInt() + 1
                                    assertEquals((end - start) * 2, arguments.size - 2, key.toString())
                                    val offset = start * 2 - 2
                                    for (i in 2 until arguments.size) {
                                        dst[i + offset] = arguments[i].toFloat()
                                    }
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
            finishNode()
        } else e!!.printStackTrace()
    }
    Engine.requestShutdown()
}