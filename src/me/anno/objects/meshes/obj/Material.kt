package me.anno.objects.meshes.obj

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import org.joml.Vector3f
import java.io.File

// https://en.wikipedia.org/wiki/Wavefront_.obj_file
class Material: Saveable(){
    var ambientColor: Vector3f? = null // Ka
    var ambientTexture: File? = null
    var diffuseColor: Vector3f? = null // Kd
    var diffuseTexture: File? = null
    var specularColor: Vector3f? = null // Ks
    var specularTexture: File? = null
    var specularExponent = 0f // [0, 1000], Ns
    var opacity = 1f // d, or 1-Tr
    var opacityTexture: File? = null
    var refractionIndex = 1f // Ni [0.001, 10]
    var model = IlluminationModel.COLOR
    override fun getClassName() = "Material"
    override fun getApproxSize(): Int = 10
    override fun isDefaultValue() = false
    override fun save(writer: BaseWriter) {
        super.save(writer)
        ambientColor?.apply { writer.writeVector3("ambient", this) }
        ambientTexture?.apply { writer.writeFile("ambient", this) }
        diffuseColor?.apply { writer.writeVector3("diffuse", this) }
        diffuseTexture?.apply { writer.writeFile("diffuse", this) }
        specularColor?.apply { writer.writeVector3("specular", this) }
        specularTexture?.apply { writer.writeFile("specular", this) }
        writer.writeFloat("specularExponent", specularExponent)
        writer.writeFloat("opacity", opacity)
        opacityTexture?.apply { writer.writeFile("opacity", this) }
        writer.writeFloat("refrationIndex", refractionIndex)
        writer.writeInt("model", model.id)
    }
}