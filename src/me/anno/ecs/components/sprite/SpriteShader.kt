package me.anno.ecs.components.sprite

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable

object SpriteShader : ECSMeshShader("SpriteECS") {

    val replacements = "diffuseMap,emissiveMap,normalMap,occlusionMap,roughnessMap,metallicMap".split(',')
        .map { name -> name to "${name}Array" }

    override fun createFragmentVariables(key: ShaderKey): ArrayList<Variable> {
        val list = super.createFragmentVariables(key)
        for ((src, dst) in replacements) {
            list.remove(Variable(GLSLType.S2D, src))
            list.add(Variable(GLSLType.S2DA, dst))
        }
        list.add(Variable(GLSLType.V1I, "spriteIndex"))
        return list
    }

    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        val stages = super.createFragmentStages(key)
        val lastStage = stages.lastOrNull() ?: return emptyList()
        lastStage.body = replaceTextureReading(lastStage.body)
        return stages.subList(0, stages.lastIndex) + lastStage
    }

    fun StringBuilder.replace(src: String, dst: String) {
        for (i in 0 until length - src.length) {
            if (startsWith(src, startIndex = i)) {
                replace(i, i + src.length, dst)
            }
        }
    }

    fun replaceTextureReading(shader: String): String {
        val builder = StringBuilder(shader)
        for ((src, dst) in replacements) {
            builder.replace(
                "texture($src, uv,",
                "texture($dst, vec3(uv, spriteIndex),"
            )
            builder.replace(
                "textureSize($src,",
                "textureSize($dst,"
            )
        }
        return builder.toString()
    }
}