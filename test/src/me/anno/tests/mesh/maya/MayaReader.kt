package me.anno.tests.mesh.maya

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.mesh.assimp.AnimatedMeshesLoader
import me.anno.mesh.assimp.StaticMeshesLoader.Companion.defaultFlags
import me.anno.utils.LOGGER
import me.anno.utils.types.Arrays.resize
import me.anno.utils.types.Floats.toRadians
import me.anno.utils.types.Strings.toDouble
import me.anno.utils.types.Strings.toInt
import org.joml.Quaterniond
import org.joml.Vector3d

// todo read Maya files
// todo first files: Maya Ascii 2015

fun split(data: String, list: ArrayList<CharSequence>) {
    var i = 0
    while (i < data.length) {
        when (data[i]) {
            ' ', '\t', '\r', '\n' -> i++
            '"' -> {
                val i0 = ++i
                while (i < data.length && data[i] != '"') i++
                list.add(data.subSequence(i0, i))
                i++
            }
            else -> {
                // read until space
                val i0 = i
                while (i < data.length && data[i] != ' ') i++
                list.add(data.subSequence(i0, i))
            }
        }
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
    val file = getReference(
        "E:/Assets/Sources/Simple_Fantasy_Interiors_SourceFiles.rar/" +
                "SourceFiles/Simple_Fantasy_Interiors_Demo.ma"
    )

    // not supported by Assimp
    // println(AnimatedMeshesLoader.loadFile(file, defaultFlags))

    file.readText { text, e ->
        if (text != null) {
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
                when (arguments.first()) {
                    "requires" -> {}
                    "currentUnit" -> {}
                    "fileInfo" -> {}
                    "createNode" -> {
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
                                } else if (key.startsWith(".n[")) {
                                    // normals
                                } else if (key.startsWith(".fc[")) {
                                    // face count
                                    // -s is the number of "f", -ch of "f" and "mu"
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