package me.anno.objects.meshes.fbx.model

import me.anno.objects.meshes.fbx.structure.FBXNode
import org.joml.Vector3f
import org.joml.Vector4f

class FBXMaterial(data: FBXNode) : FBXObject(data) {

    // todo how are textures connected to the properties?

    val shadingModel = data["ShadingModel"].firstOrNull()?.properties?.get(0) as? String
    val multiLayer = data["Multilayer"].firstOrNull()?.properties?.get(0) as? Int ?: 0 // 1 = clear coat???

    val ambient = Vector4f(0f, 0f, 0f, 1f)
    val transparency = Vector4f(0f, 0f, 0f, 0f)
    val emissive = Vector4f(0f, 0f, 0f, 1f)
    val diffuse = Vector4f(1f, 1f, 1f, 1f)
    val specular = Vector4f(1f, 1f, 1f, 0f)
    var shininess = 0f
    var shininessExponent = 20f
    val reflection = Vector4f(0f, 0f, 0f, 1f)

    override fun onReadProperty70(name: String, value: Any) {
        when (name) {
            "Ambient", "AmbientColor" -> ambient.set(value as Vector3f)
            "AmbientFactor" -> ambient.w = value as Float
            "TransparencyColor" -> transparency.set(value as Vector3f)
            "TransparencyFactor" -> transparency.w = value as Float
            "Emissive" -> emissive.set(value as Vector3f)
            "EmissiveFactor" -> emissive.w = value as Float
            "Diffuse", "DiffuseColor" -> diffuse.set(value as Vector3f)
            "DiffuseFactor" -> diffuse.w = value as Float
            "Specular", "SpecularColor" -> specular.set(value as Vector3f)
            "SpecularFactor" -> specular.w = value as Float
            "Shininess" -> shininess = value as Float
            "ShininessExponent" -> shininessExponent = value as Float
            "Opacity" -> transparency.w = 1f - value as Float
            "ReflectionColor" -> reflection.set(value as Vector3f)
            "Reflectivity", "ReflectionFactor" -> reflection.w = value as Float
            "Displacement", "DisplacementColor", "DisplacementFactor",
            "VectorDisplacement", "VectorDisplacementColor", "VectorDisplacementFactor",
            "BumpColor", "BumpFactor" -> Unit // not (yet?) supported / implemented
            "MultiTake" -> {}
            else -> super.onReadProperty70(name, value)
        }
    }

    fun Vector4f.set(col: Vector3f) {
        x = col.x
        y = col.y
        z = col.z
    }

}