package me.anno.ecs.components.mesh.material

import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.components.mesh.material.shaders.FurShader
import me.anno.gpu.shader.GLSLType
import org.joml.Vector3f

@Docs("Implements shell rendering for fur-like visuals")
class FurMaterial : Material() {

    @Range(1.0, 1024.0)
    var numShells = 64

    @Range(0.0, 1e38)
    var hairLength = 0.01f

    @Range(0.0, 1e38)
    var hairDensity = 3000f

    @Range(0.0, 100.0)
    var hairSharpness = 1.5f

    @Range(-1.0, 1.0)
    var hairGravity = Vector3f(0f, -0.5f, 0f)
        set(value) {
            field.set(value)
        }

    init {
        shaderOverrides["relativeHairLength"] = TypeValue(GLSLType.V1F) { 1f / numShells }
        shaderOverrides["hairLength"] = TypeValue(GLSLType.V1F) { hairLength / numShells }
        shaderOverrides["hairGravity"] = TypeValue(GLSLType.V3F, hairGravity)
        shaderOverrides["hairDensity"] = TypeValue(GLSLType.V1F) { hairDensity }
        shaderOverrides["hairSharpness"] = TypeValue(GLSLType.V1F) { hairSharpness }
        shader = FurShader
    }
}