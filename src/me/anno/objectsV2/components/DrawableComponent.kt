package me.anno.objectsV2.components

import me.anno.objectsV2.components.shaders.FragmentShaderComponent
import org.joml.Matrix4fArrayList
import org.joml.Vector4fc

interface DrawableComponent {

    fun draw(stack: Matrix4fArrayList, time: Double, color: Vector4fc, effects: List<FragmentShaderComponent>)

}