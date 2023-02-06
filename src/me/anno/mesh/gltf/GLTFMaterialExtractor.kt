package me.anno.mesh.gltf

import me.anno.io.files.FileReference
import me.anno.io.json.JsonArray
import me.anno.io.json.JsonObject
import me.anno.io.json.JsonReader
import me.anno.utils.types.InputStreams.skipN

object GLTFMaterialExtractor {

    data class PBRMaterialData(val metallic: Float, val roughness: Float)

    fun extract(file: FileReference): Map<String, PBRMaterialData>? {
        val materialList = file.inputStreamSync().use {
            // first check whether it is binary glTF;
            // binary glTF has a 20 byte header, and then follows the structure data as JSON, and then the (unused) binary data
            val first = it.read()
            if (first == 'g'.code && it.read() == 'l'.code && it.read() == 'T'.code && it.read() == 'F'.code)
                it.skipN(16) // version, lengths, content-type (json)
            val readOpeningBracket = first != '{'.code
            JsonReader(it).readObject(readOpeningBracket) { name ->
                when (name) {
                    "materials", "name", "pbrMetallicRoughness",
                    "baseColorTexture", "index", "metallicFactor",
                    "roughnessFactor" -> true
                    else -> false
                }
            }
        }["materials"] as? JsonArray ?: return null
        // sample: [{name=fox_material, pbrMetallicRoughness={baseColorTexture={index=0}, metallicFactor=0.0, roughnessFactor=0.6036471918720245}}]
        val result = HashMap<String, PBRMaterialData>(materialList.size)
        for (index in materialList.indices) {
            val material = materialList[index]
            if (material is JsonObject) {
                val name = material.getString("name") ?: continue
                val pbrData = material["pbrMetallicRoughness"] as? JsonObject ?: continue
                val metallic = pbrData.getFloat("metallicFactor", 1f)
                val roughness = pbrData.getFloat("roughnessFactor", 1f)
                result[name] = PBRMaterialData(metallic, roughness)
            }
        }
        return result
    }

}