package me.anno.mesh.obj

import me.anno.ecs.prefab.Prefab
import me.anno.io.files.FileReference
import me.anno.io.zip.InnerFolder
import me.anno.maths.Maths.clamp
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector4f
import java.io.EOFException
import kotlin.math.sqrt

class MTLReader(val file: FileReference) : OBJMTLReader(file.inputStream()) {

    val materials = HashMap<String, Prefab>()

    init {
        var material: Prefab? = null
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
                            val materialName = readUntilSpace()
                            material = Prefab("Material")
                            materials[materialName] = material
                            color = Vector4f(1f)
                            hadOpacity = false
                            skipLine()
                        }
                        /*"Ka" -> material.ambientColor = readVector3f()*/ // ???
                        "Kd" -> color.set(readVector3f(), color.w)
                        "Ke" -> material?.setProperty("emissiveBase", readVector3f())
                        // "Ks" -> material.specularColor = readVector3f()
                        // metallic
                        "Ns" -> {
                            // in Blender, this is the roughness, expressed as (1-roughness)² * 1000
                            val exponent = readValue()
                            val smoothness = sqrt(exponent / 1000f)
                            val roughness = clamp(1f - smoothness)
                            material?.setProperty("roughnessMinMax", Vector2f(0f, roughness))
                        }
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
                            // to do roughness or metallic?
                            // not used by Blender in my small experiment
                            // material!!.setProperty("roughnessMap", readFile(file))
                            // material.specularTexture = readFile(file)
                            skipLine()
                        }
                        // "map_Ns" -> material.specularTexture = readFile(file)
                        // "map_d" -> material.opacityTexture = readFile(file)
                        /*"Ni" -> material.refractionIndex = readValue()*/
                        "Tr" -> {
                            if (!hadOpacity) {
                                // todo mark Tr as reversed, if the value is 1.0f? maybe :)
                                val v = readValue()
                                color.w = if (v == 1f) 1f else 1f - v
                            } else skipLine()
                        }
                        @Suppress("SpellCheckingInspection")
                        "illum" -> {
                            val v = readValue().toInt()
                            if (v == 3) {
                                // metallic, at least as Blender output
                                // 2 = just roughness
                                material?.setProperty("metallicMinMax", Vector2f(0f, 1f))
                            }
                            /**
                             * TR = transparency,
                             * FR = fresnel, RT = ray tracing (not supported)
                             * RF = refraction, GS = glass

                            0. Color on and Ambient off
                            1. Color on and Ambient on
                            2. Highlight on
                            3. Reflection on and Ray trace on
                            4. Transparency: Glass on, Reflection: Raytrace on
                            5. Reflection: Fresnel on and Ray trace on
                            6. Transparency: Refraction on, Reflection: Fresnel off and Ray trace on
                            7. Transparency: Refraction on, Reflection: Fresnel on and Ray trace on
                            8. Reflection on and Ray trace off
                            9. Transparency: Glass on, Reflection: Raytrace off
                            10. It casts shadows onto invisible surfaces

                             * */
                        }
                        // ignored for now:
                        // if you need any of these properties, just write me (Antonio)
                        "map_Ns", "map_d", "map_Ka", "Ks", "Ka", "map_Bump", "map_bump", "Tf", "Ni", "bump" -> skipLine()
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
        } catch (_: EOFException) {
        }
        material?.setProperty("diffuseBase", color)
        reader.close()
    }

    // todo normal map, occlusion
    // todo extra opacity texture? how could we integrate that?

    companion object {

        fun readAsFolder(file: FileReference, dstFolder: InnerFolder = InnerFolder(file)): InnerFolder {
            val materials = MTLReader(file).materials
            for ((name, material) in materials)
                dstFolder.createPrefabChild("$name.json", material)
            return dstFolder
        }

        private val LOGGER = LogManager.getLogger(MTLReader::class)
    }
}