package me.anno.objectsV2

import me.anno.objects.Transform
import me.anno.objectsV2.components.DrawableComponent
import me.anno.objectsV2.components.shaders.FragmentShaderComponent
import org.joml.Matrix4fArrayList
import org.joml.Vector4fc

// entities would be an idea to make effects more modular
// it could apply new effects to both the camera and image sources

class Entity : Transform() {

    override fun getClassName(): String = "Entity"
    override fun getApproxSize(): Int = 10_000 + children.size
    override fun isDefaultValue(): Boolean = false

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4fc) {

        super.onDraw(stack, time, color)

        val drawable = children.firstOrNull { it is DrawableComponent } ?: return
        val fragmentEffects = children.filterIsInstance<FragmentShaderComponent>()
        (drawable as DrawableComponent).draw(stack, time, color, fragmentEffects)

    }

    override fun drawChildrenAutomatically(): Boolean = false

}