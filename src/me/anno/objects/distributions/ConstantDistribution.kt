package me.anno.objects.distributions

import me.anno.objects.inspectable.InspectableVector
import me.anno.ui.editor.sceneView.Grid
import org.joml.*

class ConstantDistribution(val center: Vector4f) : Distribution(
    "Constant",
    "Always the same value", "obj.dist.constant"
) {

    constructor() : this(0f)
    constructor(center: Float) : this(Vector4f(center))
    constructor(center: Vector2f) : this(Vector4f(center.x, center.y, center.x, center.y))
    constructor(center: Vector3f) : this(Vector4f(center, 0f))

    override fun nextV1(): Float = center.x

    override fun nextV2(): Vector2f {
        return Vector2f(center.x, center.y)
    }

    override fun nextV3(): Vector3f {
        return Vector3f(center.x, center.y, center.z)
    }

    override fun nextV4(): Vector4f {
        return Vector4f(center)
    }

    override fun listProperties(): List<InspectableVector> {
        return listOf(
            InspectableVector(center, "Center", InspectableVector.PType.DEFAULT)
        )
    }

    override fun draw(stack: Matrix4fArrayList, color: Vector4fc) {
        stack.next {
            stack.translate(center.x, center.y, center.z)
            onDraw(stack, color)
        }
    }

    override fun onDraw(stack: Matrix4fArrayList, color: Vector4fc) {
        val l = displayLength
        Grid.drawLine(stack, color, Vector3f(-l, 0f, 0f), Vector3f(+l, 0f, 0f))
        Grid.drawLine(stack, color, Vector3f(0f, -l, 0f), Vector3f(0f, +l, 0f))
        Grid.drawLine(stack, color, Vector3f(0f, 0f, -l), Vector3f(0f, 0f, +l))
    }

    override val className get() = "ConstantDistribution"

    companion object {
        private val displayLength = 0.1f
    }

}