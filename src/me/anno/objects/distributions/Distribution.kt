package me.anno.objects.distributions

import me.anno.io.Saveable
import me.anno.language.translation.Dict
import me.anno.objects.Transform
import me.anno.objects.inspectable.InspectableAttribute
import me.anno.objects.inspectable.InspectableVector
import me.anno.objects.models.SphereAxesModel.sphereAxesModels
import me.anno.ui.base.groups.PanelList
import me.anno.ui.editor.sceneView.Grid
import me.anno.ui.style.Style
import org.joml.Matrix4fArrayList
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.*

abstract class Distribution(val displayName: String, val description: String) : Saveable(),
    InspectableAttribute {

    constructor(displayName: String, description: String, dictPath: String) :
            this(Dict[displayName, dictPath], Dict[description, "$dictPath.desc"])

    /**
     * used by nearly all distributions anyways
     * */
    val random = Random()

    open fun nextV1(): Float {
        throw RuntimeException("Single component is not supported in ${javaClass.simpleName}")
    }

    open fun nextV2(): Vector2f {
        throw RuntimeException("Two components are not supported in ${javaClass.simpleName}")
    }

    open fun nextV3(): Vector3f {
        throw RuntimeException("Three components are not supported in ${javaClass.simpleName}")
    }

    open fun nextV4(): Vector4f {
        throw RuntimeException("Four components are not supported in ${javaClass.simpleName}")
    }

    open fun listProperties(): List<InspectableVector> = emptyList()

    override fun createInspector(list: PanelList, actor: Transform, style: Style) {
        val properties = listProperties()
        properties.forEach { property ->
            list += actor.vi(
                property.title,
                property.description,
                property.pType.type,
                property.value,
                style
            ) { property.value.set(it) }
        }
    }

    override fun isDefaultValue() = false
    override fun getApproxSize() = 20

    fun Vector2f.mul(size: Vector4f) = mul(size.x, size.y)
    fun Vector2f.add(delta: Vector4f) = add(delta.x, delta.y)

    fun Vector3f.mul(size: Vector4f) = mul(size.x, size.y, size.z)
    fun Vector3f.add(delta: Vector4f) = add(delta.x, delta.y, delta.z)

    open fun draw(stack: Matrix4fArrayList, color: Vector4f) {
        onDraw(stack, color)
    }

    abstract fun onDraw(stack: Matrix4fArrayList, color: Vector4f)

    fun drawSphere(stack: Matrix4fArrayList, color: Vector4f, alpha: Float = 1f) {
        Grid.drawBuffer(
            stack,
            if (alpha == 1f) color
            else Vector4f(color.x, color.y, color.z, color.w * alpha),
            sphereAxesModels[sphereSubDivision].value
        )
    }

    override fun equals(other: Any?): Boolean {
        return other?.javaClass === javaClass && other.toString() == toString()
    }

    companion object {
        const val sphereSubDivision = 4
    }

}