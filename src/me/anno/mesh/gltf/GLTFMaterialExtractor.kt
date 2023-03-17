package me.anno.mesh.gltf

import me.anno.io.files.FileReference
import me.anno.io.json.JsonReader
import me.anno.utils.types.AnyToFloat.getFloat
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
        }["materials"] as? List<*> ?: return null
        // sample: [{name=fox_material, pbrMetallicRoughness={baseColorTexture={index=0}, metallicFactor=0.0, roughnessFactor=0.6036471918720245}}]
        val result = HashMap<String, PBRMaterialData>(materialList.size)
        for (index in materialList.indices) {
            val material = materialList[index]
            if (material is HashMap<*, *>) {
                val name = material["name"]?.toString() ?: continue
                val pbrData = material["pbrMetallicRoughness"] as? HashMap<*, *> ?: continue
                val metallic = getFloat(pbrData["metallicFactor"], 1f)
                val roughness = getFloat(pbrData["roughnessFactor"], 1f)
                result[name] = PBRMaterialData(metallic, roughness)
            }
        }
        return result
    }

}