package me.anno.mesh.obj

import me.anno.cache.AsyncCacheData
import me.anno.ecs.prefab.Prefab
import me.anno.io.files.FileReference
import me.anno.io.files.inner.InnerFolder
import me.anno.maths.Maths.clamp
import me.anno.mesh.obj.OBJReader.Companion.readPath
import me.anno.utils.async.Callback
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.io.EOFException
import java.io.InputStream
import kotlin.math.sqrt

class MTLReader(val file: FileReference, input: InputStream) : TextFileReader(input) {

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
                            material?.set("diffuseBase", color)
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
                        "Ke" -> material?.set("emissiveBase", readVector3f())
                        // "Ks" -> material.specularColor = readVector3f()
                        // metallic
                        "Ns" -> {
                            // in Blender, this is the roughness, expressed as (1-roughness)² * 1000
                            val exponent = readScalar()
                            val smoothness = sqrt(exponent / 1000f)
                            val roughness = clamp(1f - smoothness)
                            material?.set("roughnessMinMax", Vector2f(0f, roughness))
                        }
                        "d" -> {
                            color.w = readScalar()
                            hadOpacity = true
                        }
                        "map_Ka" -> {
                            if (material == null) skipLine()
                            else material["occlusionMap"] = readPath(file)
                        }
                        "map_Kd" -> {
                            if (material == null) skipLine()
                            else material["diffuseMap"] = readPath(file)
                        }
                        "map_Ke" -> {
                            if (material == null) skipLine()
                            else material["emissiveMap"] = readPath(file)
                        }
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
                                val v = readScalar()
                                color.w = if (v == 1f) 1f else 1f - v
                            } else skipLine()
                        }
                        "illum" -> {
                            val v = readScalar().toInt()
                            if (v == 3) {
                                // metallic, at least as Blender output
                                // 2 = just roughness
                                material?.set("metallicMinMax", Vector2f(0f, 1f))
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
                        "map_Bump", "map_bump", "bump" -> {
                            if (material != null) {
                                // bump -bm 1.0 normal_map.png
                                // bump normal_map.png
                                skipSpaces()
                                var strength = 1f
                                val peek = nextChar()
                                if (peek == '-') {
                                    if (nextChar() == 'b' && nextChar() == 'm') {
                                        skipSpaces()
                                        strength = readScalar()
                                    }
                                } else putBack(peek)
                                skipSpaces()
                                val path = readPath(file)
                                material["normalStrength"] = strength
                                material["normalMap"] = path
                                skipLine()
                            } else skipLine()
                        }
                        "map_Ns", "map_d", "Ks", "Ka", "Tf", "Ni" -> skipLine()
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
        material?.set("diffuseBase", color)
        reader.close()
    }

    fun readScalar(): Float {
        skipSpaces()
        val x = readFloat()
        skipLine()
        return x
    }

    fun readVector3f(): Vector3f {
        skipSpaces()
        val x = readFloat()
        skipSpaces()
        val y = readFloat()
        skipSpaces()
        val z = readFloat()
        skipLine()
        return Vector3f(x, y, z)
    }

    companion object {

        fun readAsFolder(
            file: FileReference, callback: Callback<InnerFolder>,
            dstFolder: InnerFolder = InnerFolder(file)
        ) {
            file.inputStream { it, exc ->
                if (it != null) {
                    val materials = MTLReader(file, it).materials
                    for ((name, material) in materials)
                        dstFolder.createPrefabChild("$name.json", material)
                    callback.ok(dstFolder)
                } else callback.err(exc)
            }
        }

        fun readAsFolderSync(
            file: FileReference,
            dstFolder: InnerFolder = InnerFolder(file)
        ): InnerFolder? {
            return AsyncCacheData.loadSync { readAsFolder(file, it, dstFolder) }
        }

        private val LOGGER = LogManager.getLogger(MTLReader::class)
    }
}