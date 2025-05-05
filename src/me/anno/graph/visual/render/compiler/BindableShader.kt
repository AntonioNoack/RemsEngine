package me.anno.graph.visual.render.compiler

import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.gpu.shader.Shader

typealias GraphShader = Pair<Shader, Map<String, TypeValue>>