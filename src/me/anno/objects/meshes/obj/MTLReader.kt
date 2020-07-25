package me.anno.objects.meshes.obj

import org.apache.logging.log4j.LogManager
import java.io.EOFException
import java.io.File

class MTLReader(val file: File): OBJMTLReader(file.inputStream().buffered()){

    companion object {
        private val LOGGER = LogManager.getLogger(MTLReader::class)
    }

    val materials = HashMap<String, Material>()
    init {
        // todo load all the materials
        // the full spec ofc is again very complex...
        try {
            lateinit var material: Material
            lateinit var materialName: String
            while(true){
                skipSpaces()
                val char0 = next()
                if(char0 == '#'.toInt()){
                    // just a comment
                    skipLine()
                } else {
                    putBack(char0)
                    when(val name = readUntilSpace()){
                        "newmtl" -> {
                            skipSpaces()
                            materialName = readUntilSpace()
                            material = Material()
                            materials[materialName] = material
                        }
                        "Ka" -> material.ambientColor = readVector3f()
                        "map_Ka" -> material.ambientTexture = readFile(file)
                        "Ks" -> material.specularColor = readVector3f()
                        "map_Ks" -> material.specularTexture = readFile(file)
                        "Ns" -> material.specularExponent = readValue()
                        "map_Ns" -> material.specularTexture = readFile(file)
                        "Ni" -> material.refractionIndex = readValue()
                        "d" -> material.opacity = readValue()
                        "map_d" -> material.opacityTexture = readFile(file)
                        "Tr" -> material.opacity = 1f - readValue()
                        "illum" -> {
                            skipSpaces()
                            val modelIndex = readUntilSpace().toInt()
                            skipLine()
                            material.model = IlluminationModel.values().firstOrNull { it.id == modelIndex } ?: material.model
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
        } catch (e: EOFException){}
        reader.close()
    }
}