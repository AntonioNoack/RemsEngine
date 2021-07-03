package me.anno.mesh.obj

import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import org.apache.logging.log4j.LogManager
import java.io.EOFException
import java.io.File

class MTLReader(val file: FileReference) : OBJMTLReader(file.inputStream().buffered()) {

    constructor(file: File) : this(getReference(file))

    companion object {
        private val LOGGER = LogManager.getLogger(MTLReader::class)
    }

    val materials = HashMap<String, Material>()

    init {
        // load all materials
        // the full spec ofc is again very complex...
        try {
            lateinit var material: Material
            lateinit var materialName: String
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
                        "d" -> material.opacity = readValue()
                        "map_Ka" -> material.ambientTexture = readFile(file)
                        "map_Kd" -> material.diffuseTexture = readFile(file)
                        "map_Ke" -> material.emissiveTexture = readFile(file)
                        "map_Ks" -> material.specularTexture = readFile(file)
                        "map_Ns" -> material.specularTexture = readFile(file)
                        "map_d" -> material.opacityTexture = readFile(file)
                        "Ni" -> material.refractionIndex = readValue()
                        "Tr" -> material.opacity = 1f - readValue()
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
}