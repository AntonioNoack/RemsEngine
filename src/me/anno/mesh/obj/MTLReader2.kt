package me.anno.mesh.obj

import me.anno.ecs.prefab.Prefab
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.zip.InnerFile
import me.anno.io.zip.InnerFolder
import org.apache.logging.log4j.LogManager
import org.joml.Vector4f
import java.io.EOFException
import java.io.File

class MTLReader2(val file: FileReference) : OBJMTLReader(file.inputStream()) {

    constructor(file: File) : this(getReference(file))

    val materials = HashMap<String, Prefab>()

    init {
        var material: Prefab? = null
        var materialName = ""
        var hadOpacity = false
        var color = Vector4f()
        // load all materials
        // the full spec ofc is again very complex...
        try {
            while (true) {
                skipSpaces()
                val char0 = next()
                if (char0 == '#'.code) {
                    // just a comment
                    skipLine()
                } else {
                    putBack(char0)
                    // we could make this as efficient as OBJReader2, however we probably don't need to,
                    // because material files will always be much shorter than the actual geometry
                    when (val name = readUntilSpace()) {
                        "newmtl" -> {
                            material?.setProperty("diffuseBase", color)
                            skipSpaces()
                            materialName = readUntilSpace()
                            material = Prefab("Material")
                            materials[materialName] = material
                            color = Vector4f(1f)
                            hadOpacity = false
                            skipLine()
                        }
                        /*"Ka" -> material.ambientColor = readVector3f()*/ // ???
                        "Kd" -> color.set(readVector3f(), color.w)
                        "Ke" -> material!!.setProperty("emissiveBase", readVector3f())
                        // "Ks" -> material.specularColor = readVector3f()
                        // "Ns" -> material.specularExponent = readValue() // todo metallic
                        "d" -> {
                            color.w = readValue()
                            hadOpacity = true
                        }
                        /*"map_Ka" -> // ???
                            material!!.setProperty("diffuseMap", readFile(file))
                            material.ambientTexture = readFile(file)*/
                        "map_Kd" -> material!!.setProperty("diffuseMap", readFile(file))
                        "map_Ke" -> material!!.setProperty("emissiveMap", readFile(file))
                        "map_Ks" -> {
                            // todo roughness or metallic?
                            // material!!.setProperty("roughnessMap", readFile(file))
                            // material.specularTexture = readFile(file)
                            skipLine()
                        }
                        // "map_Ns" -> material.specularTexture = readFile(file)
                        // "map_d" -> material.opacityTexture = readFile(file)
                        /*"Ni" -> material.refractionIndex = readValue()*/
                        "Tr" -> {
                            if (!hadOpacity) {
                                color.w = 1f - readValue()
                            } else skipLine()
                        }
                        "illum" -> {
                            skipSpaces()
                            /*when (readUntilSpace().toInt()) {
                                0 -> {
                                } // color on, ambient off,
                                1 -> {
                                } // color on, ambient on
                                2 -> {
                                } // highlight on
                                3 -> {
                                } // reflection & raytrace on
                                4 -> {
                                } // transparency on
                                // ... not really any value...
                            }*/
                            skipLine()
                        }
                        "map_Ns", "map_d", "map_Ka", "Ns", "Ks", "Ka", "map_Bump", "map_bump", "Tf", "Ni", "bump" -> {
                            skipLine()
                        }
                        // bump maps, displacement maps, decal maps exists;
                        // also there is additional parameters for texture blending, scale,
                        // offset, clamped textures, bump multiplier, channel selection for textures, ...
                        else -> {
                            if (name.isNotEmpty()) LOGGER.info("Unknown tag in mtl: $name")
                            skipLine()
                        }
                    }
                }
            }
        } catch (e: EOFException) {
        }
        material?.setProperty("diffuseBase", color)
        reader.close()
    }

    // todo roughness, metallic, normal map, occlusion
    // todo extra opacity texture? how could we integrate that?

    companion object {

        fun readAsFolder(file: FileReference, dstFolder: InnerFolder = InnerFolder(file)): InnerFile {
            val materials = MTLReader2(file).materials
            for ((name, material) in materials) {
                dstFolder.createPrefabChild("$name.json", material)
            }
            return dstFolder
        }

        private val LOGGER = LogManager.getLogger(MTLReader2::class)
    }
}