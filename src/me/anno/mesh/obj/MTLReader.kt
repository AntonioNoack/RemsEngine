package me.anno.mesh.obj

import me.anno.ecs.prefab.Prefab
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.io.zip.InnerFile
import me.anno.io.zip.InnerFolder
import org.apache.logging.log4j.LogManager
import org.joml.Vector4f
import java.io.EOFException
import java.io.File

class MTLReader(val file: FileReference) : OBJMTLReader(file.inputStream()) {

    constructor(file: File) : this(getReference(file))

    val materials = HashMap<String, Material>()

    init {
        // load all materials
        // the full spec ofc is again very complex...
        try {
            lateinit var material: Material
            lateinit var materialName: String
            var hadOpacity = false
            while (true) {
                skipSpaces()
                val char0 = next()
                if (char0 == '#'.code) {
                    // just a comment
                    skipLine()
                } else {
                    putBack(char0)
                    when (val name = readUntilSpace()) {
                        "newmtl" -> {
                            skipSpaces()
                            materialName = readUntilSpace()
                            material = Material()
                            materials[materialName] = material
                        }
                        "Ka" -> material.ambientColor = readVector3f()
                        "Kd" -> material.diffuseColor = readVector3f()
                        "Ke" -> material.emissiveColor = readVector3f()
                        "Ks" -> material.specularColor = readVector3f()
                        "Ns" -> material.specularExponent = readValue()
                        "d" -> {
                            material.opacity = readValue()
                            hadOpacity = true
                        }
                        "map_Ka" -> material.ambientTexture = readFile(file)
                        "map_Kd" -> material.diffuseTexture = readFile(file)
                        "map_Ke" -> material.emissiveTexture = readFile(file)
                        "map_Ks" -> material.specularTexture = readFile(file)
                        "map_Ns" -> material.specularTexture = readFile(file)
                        "map_d" -> material.opacityTexture = readFile(file)
                        "Ni" -> material.refractionIndex = readValue()
                        "Tr" -> {
                            if (!hadOpacity) {
                                material.opacity = 1f - readValue()
                            }
                        }
                        "illum" -> {
                            skipSpaces()
                            val modelIndex = readUntilSpace().toInt()
                            skipLine()
                            material.model =
                                IlluminationModel.values().firstOrNull { it.id == modelIndex } ?: material.model
                        }
                        // bump maps, displacement maps, decal maps exists;
                        // also there is additional parameters for texture blending, scale,
                        // offset, clamped textures, bump multiplier, channel selection for textures, ...
                        else -> {
                            LOGGER.info("Unknown tag in mtl: $name")
                            skipLine()
                        }
                    }
                }
            }
        } catch (e: EOFException) {
        }
        reader.close()
    }

    companion object {

        fun readAsFolder(file: FileReference): InnerFile {

            val materials = MTLReader(file).materials
            val folder = InnerFolder(file)

            for ((name, material) in materials) {
                val prefab = Prefab("Material")
                material.apply {
                    if (diffuseTexture != InvalidRef) prefab.setProperty("diffuseMap", diffuseTexture)
                    if (diffuseColor != null || opacity != 1f)
                        prefab.setProperty("diffuseBase", Vector4f(diffuseColor!!, opacity))
                    if (emissiveTexture != InvalidRef) prefab.setProperty("emissiveMap", emissiveTexture)
                    if (emissiveColor != null) prefab.setProperty("emissiveBase", emissiveColor)
                    // todo roughness, metallic, normal map, occlusion
                    // todo extra opacity texture? how could we integrate that?
                    // if(opacityTexture != InvalidRef) prefab.setProperty("occlusionMap", opacityTexture)
                }
                folder.createPrefabChild("$name.json", prefab)
            }

            return folder

        }

        private val LOGGER = LogManager.getLogger(MTLReader::class)
    }
}