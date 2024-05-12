package me.anno.graph.visual.vector

val vectorTypes = "Vector2f,Vector3f,Vector4f,Vector2d,Vector3d,Vector4d,Vector2i,Vector3i,Vector4i".split(',')

fun getVectorTypeF(type: String): String {
    return when {
        type.endsWith('f') -> "Float"
        else -> "Double"
    }
}

fun getVectorType(type: String): String {
    return when {
        type.endsWith('f') -> "Float"
        type.endsWith('i') -> "Int"
        else -> "Double"
    }
}
