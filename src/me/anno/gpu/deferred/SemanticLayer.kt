package me.anno.gpu.deferred

data class SemanticLayer(
    val type: DeferredLayerType,
    val textureName: String,
    val texIndex: Int,
    val mapping: String
) {
    fun appendMapping(
        builder: StringBuilder,
        dstSuffix: String,
        tmpSuffix: String,
        texSuffix: String,
        uv: String,
        imported: MutableSet<String>?,
        sampleVariableName: String?
    ) {
        val texName = textureName + texSuffix
        if (imported != null && imported.add(texName)) {
            builder.append("vec4 ").append(textureName).append(tmpSuffix)
            if (sampleVariableName != null) {
                builder.append(" = texelFetch(").append(texName)
                // texture will be sampler2DMS, so no lod is used as parameter
                builder.append(", ivec2(vec2(textureSize(").append(texName)
                builder.append("))*").append(uv)
                builder.append("), ").append(sampleVariableName).append(");\n")
            } else {
                builder.append(" = texture(").append(texName)
                builder.append(", ").append(uv).append(");\n")
            }
        }
        builder.append(type.glslName).append(dstSuffix).append(" = ")
            .append(type.dataToWork).append('(').append(textureName).append(tmpSuffix)
            .append('.').append(mapping).append(");\n")
    }

    fun appendLayer(output: StringBuilder, defRR: String?, useRandomness: Boolean) {
        output.append(textureName)
        output.append('.')
        output.append(mapping)
        val useRandomRounding = useRandomness && when (type) {
            DeferredLayerType.CLICK_ID, DeferredLayerType.GROUP_ID -> false
            else -> true
        }
        output.append(if (useRandomRounding) " = (" else " = ")
        if (type == DeferredLayerType.DEPTH) {
            val depthVariableName = if ("gl_FragDepth" in output) "gl_FragDepth" else "gl_FragCoord.z"
            output.append(depthVariableName)
        } else {
            val w2d = type.workToData
            when {
                '.' in w2d -> output.append(w2d)
                w2d.isNotEmpty() -> output.append(w2d).append('(').append(type.glslName).append(')')
                else -> output.append(type.glslName)
            }
        }
        // append random rounding
        if (useRandomRounding) {
            output.append(")*(1.0+").append(defRR).append("*").append(textureName).append("RR.x")
            output.append(")+").append(defRR).append("*").append(textureName).append("RR.y")
        }
        output.append(";\n")
    }
}