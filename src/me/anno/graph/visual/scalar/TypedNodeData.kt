package me.anno.graph.visual.scalar

class TypedNodeData(
    val name: String,
    val glsl: Pair<String, String?>, // func name, define
    val inputs: List<String>,
    val outputType: String
)