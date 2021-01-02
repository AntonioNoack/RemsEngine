package me.anno.objects.particles.forces

import me.anno.config.DefaultConfig
import me.anno.objects.inspectable.InspectableAnimProperty
import me.anno.objects.Transform
import me.anno.objects.models.ArrowModel
import me.anno.objects.particles.Particle
import me.anno.objects.particles.ParticleState
import me.anno.objects.particles.ParticleSystem
import me.anno.objects.particles.forces.impl.*
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.sceneView.Grid
import me.anno.ui.editor.stacked.Option
import me.anno.ui.style.Style
import me.anno.utils.Floats.toRadians
import me.anno.utils.Maths
import org.joml.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor

abstract class ForceField(val displayName: String, val description: String) : Transform() {

    val strength = scale

    override fun getSymbol() = DefaultConfig["ui.symbol.forceField", "⇶"]
    override fun getDefaultDisplayName(): String = displayName

    abstract fun getForce(state: ParticleState, time: Double, particles: List<Particle>): Vector3f

    open fun listProperties() = listOf(
        // include it for convenience
        InspectableAnimProperty(
            strength,
            "Strength",
            "How much effect this force has"
        )
    )

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, id: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)
        createInspector(getGroup("Force Field", "forces").content, style)
    }

    fun createInspector(list: PanelList, style: Style) {
        for (property in listProperties()) {
            list += vi(property.title, property.description, property.value, style)
        }
    }

    fun drawPerParticle(
        stack: Matrix4fArrayList, time: Double, color: Vector4f,
        applyTransform: (Particle, index0: Int, indexF: Float) -> Unit
    ) {
        super.onDraw(stack, time, color)
        val thisTransform = Matrix4f(stack)
        stack.popMatrix() // use the parent transform
        val system = parent as? ParticleSystem ?: return
        val stepSize = system.simulationStep
        val t0 = System.nanoTime()
        for(particle in system.particles){
            val opacity = particle.opacity * particle.getLifeOpacity(time, stepSize, 0.5, 0.5).toFloat()
            if (opacity > 0f) {
                val index = (time - particle.birthTime) / stepSize
                val index0 = floor(index).toInt()
                val indexF = (index - index0).toFloat()
                val position = particle.getPosition(index0, indexF)
                stack.pushMatrix()
                stack.translate(position)
                applyTransform(particle, index0, indexF)
                Grid.drawBuffer(
                    stack, Vector4f(color.x, color.y, color.z, color.w * opacity),
                    ArrowModel.arrowLineModel
                )
                stack.popMatrix()
                val t1 = System.nanoTime()
                if(abs(t1-t0) > 10_000_000) break // spend at max 10ms here
            }
        }
        stack.pushMatrix()
        stack.set(thisTransform)
    }

    fun drawForcePerParticle(
        stack: Matrix4fArrayList, time: Double, color: Vector4f
    ) {
        val particles = (parent as? ParticleSystem)?.particles?.filter { it.isAlive(time) } ?: return
        drawPerParticle(stack, time, color) { p , index0, indexF ->
            val state0 = p.states.getOrElse(index0) { p.states.last() }
            val state1 = p.states.getOrElse(index0+1) { p.states.last() }
            val otherParticles = particles.filter { it !== p }
            val force0 = getForce(state0, time, otherParticles)
            val force = if(state0 === state1){
                force0
            } else {
                val force1 = getForce(state1, time, otherParticles)
                force0.lerp(force1, indexF, Vector3f())
            }
            stack.rotateY(-atan2(force.z, force.x))
            stack.rotateZ(+atan2(force.y, Maths.length(force.x, force.z)))
            stack.scale(force.length() * visualForceScale)
        }
    }

    override fun isDefaultValue() = false
    override fun getApproxSize() = 25

    fun getDirection(time: Double): Vector3f {
        val rot = rotationYXZ[time]
        val quat = Quaternionf()
        quat.rotateY(rot.y.toRadians())
        quat.rotateX(rot.x.toRadians())
        quat.rotateZ(rot.z.toRadians())
        return quat.transform(Vector3f(0f,1f,0f))
    }

    companion object {

        const val visualForceScale = 0.1f

        fun option(generator: () -> ForceField): Option {
            val sample = generator()
            return Option(sample.displayName, sample.description) {
                generator()
            }
        }

        fun getForceFields() = listOf(
            option { GlobalForce() },
            option { GravityField() },
            option { BetweenParticleGravity() },
            option { LorentzForce() },
            option { NoisyLorentzForce() },
            option { TornadoField() },
            option { VelocityFrictionForce() }
        )

    }

}