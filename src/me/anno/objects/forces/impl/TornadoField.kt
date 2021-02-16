package me.anno.objects.forces.impl

import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.inspectable.InspectableAnimProperty
import me.anno.objects.models.ArrowModel.arrowLineModel
import me.anno.objects.particles.Particle
import me.anno.objects.particles.ParticleState
import me.anno.objects.forces.ForceField
import me.anno.ui.editor.sceneView.Grid
import me.anno.utils.Maths.next
import me.anno.utils.Maths.pow
import me.anno.utils.types.Vectors.minus
import me.anno.utils.types.Vectors.times
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.joml.Vector4f
import org.joml.Vector4fc
import kotlin.math.cos
import kotlin.math.sin

class TornadoField : ForceField(
    "Tornado",
    "Circular motion around center", "tornado"
) {

    val exponent = AnimatedProperty.float(-1f)

    override fun getForce(state: ParticleState, time: Double, particles: List<Particle>): Vector3f {

        val direction = getDirection(time)
        val strength = strength[time]
        val delta = state.position - position[time]
        val l = delta.length()
        return delta.cross(direction) * pow(l, -(exponent[time] + 1f)) * strength

    }

    override fun listProperties(): List<InspectableAnimProperty> {
        return super.listProperties() + listOf(
            InspectableAnimProperty(
                exponent,
                "Exponent",
                "How quickly the force declines with distance"
            )
        )
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "exponent", exponent)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "exponent" -> exponent.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4fc) {
        super.onDraw(stack, time, color)
        // draw a tornado of arrows
        for (i in 1 until 5) {
            val distance = i / 2f
            val arrowCount = i * 5
            for (j in 0 until arrowCount) {
                val angle = j * 6.2830f / arrowCount
                val pos = Vector3f(cos(angle) * distance, 0f, sin(angle) * distance)
                val force = pow(distance, -exponent[time])
                stack.next {
                    stack.translate(pos)
                    stack.scale(visualForceScale * force)
                    stack.rotateY(-angle - 1.57f)
                    Grid.drawBuffer(stack, Vector4f(1f), arrowLineModel)
                }
            }
        }
    }

    override fun getClassName() = "TornadoField"

}