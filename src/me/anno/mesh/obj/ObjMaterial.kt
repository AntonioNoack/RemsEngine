package me.anno.mesh.obj

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.files.LocalFile.toGlobalFile
import org.joml.Vector3f

// https://en.wikipedia.org/wiki/Wavefront_.obj_file
class ObjMaterial : Saveable() {
    var ambientColor: Vector3f? = null // Ka
    var ambientTexture: FileReference = InvalidRef
    var diffuseColor: Vector3f? = null // Kd
    var diffuseTexture: FileReference = InvalidRef
    var emissiveColor: Vector3f? = null
    var emissiveTexture: FileReference = InvalidRef
    var specularColor: Vector3f? = null // Ks
    var specularTexture: FileReference = InvalidRef
    var specularExponent = 0f // [0, 1000], Ns
    var opacity = 1f // d, or 1-Tr
    var opacityTexture: FileReference = InvalidRef
    var refractionIndex = 1f // Ni [0.001, 10]
    var model = IlluminationModel.COLOR
    override val className get() = "Material"
    override val approxSize get() = 10
    override fun isDefaultValue() = false
    override fun save(writer: BaseWriter) {
        super.save(writer)
        ambientColor?.apply { writer.writeVector3f("ambient", this) }
        ambientTexture.apply { writer.writeFile("ambient", this) }
        emissiveColor?.apply { writer.writeVector3f("emissive", this) }
        emissiveTexture.apply { writer.writeFile("emissive", this) }
        diffuseColor?.apply { writer.writeVector3f("diffuse", this) }
        diffuseTexture.apply { writer.writeFile("diffuse", this) }
        specularColor?.apply { writer.writeVector3f("specular", this) }
        specularTexture.apply { writer.writeFile("specular", this) }
        writer.writeFloat("specularExponent", specularExponent)
        writer.writeFloat("opacity", opacity)
        opacityTexture.apply { writer.writeFile("opacity", this) }
        writer.writeFloat("refractionIndex", refractionIndex)
        writer.writeInt("model", model.id)
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "model" -> model = IlluminationModel.values().firstOrNull { it.id == value } ?: return
            else -> super.readInt(name, value)
        }
    }

    override fun readFloat(name: String, value: Float) {
        when (name) {
            "opacity" -> opacity = value
            "specularExponent" -> specularExponent = value
            "refractionIndex" -> refractionIndex = value
            else -> super.readFloat(name, value)
        }
    }

    override fun readString(name: String, value: String?) {
        when (name) {
            "ambient" -> ambientTexture = value?.toGlobalFile() ?: InvalidRef
            "emissive" -> emissiveTexture = value?.toGlobalFile() ?: InvalidRef
            "diffuse" -> diffuseTexture = value?.toGlobalFile() ?: InvalidRef
            "specular" -> specularTexture = value?.toGlobalFile() ?: InvalidRef
            "opacity" -> opacityTexture = value?.toGlobalFile() ?: InvalidRef
            else -> super.readString(name, value)
        }
    }

    override fun readFile(name: String, value: FileReference) {
        when (name) {
            "ambient" -> ambientTexture = value
            "emissive" -> emissiveTexture = value
            "diffuse" -> diffuseTexture = value
            "specular" -> specularTexture = value
            "opacity" -> opacityTexture = value
            else -> super.readFile(name, value)
        }
    }
}