package me.anno.remsstudio.objects.forces

import me.anno.config.DefaultConfig
import me.anno.language.translation.Dict
import me.anno.remsstudio.objects.Transform
import me.anno.remsstudio.objects.forces.impl.*
import me.anno.remsstudio.objects.inspectable.InspectableAnimProperty
import me.anno.remsstudio.objects.models.ArrowModel
import me.anno.remsstudio.objects.particles.Particle
import me.anno.remsstudio.objects.particles.ParticleState
import me.anno.remsstudio.objects.particles.ParticleSystem
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.sceneView.Grid
import me.anno.ui.editor.stacked.Option
import me.anno.ui.style.Style
import me.anno.maths.Maths
import me.anno.utils.types.Floats.toRadians
import org.joml.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor

abstract class ForceField(val displayName: String, val descriptionI: String) : Transform() {

    constructor(displayName: String, description: String, dictSubPath: String) :
            this(Dict[displayName, "obj.force.$dictSubPath"], Dict[description, "obj.force.$dictSubPath.desc"])

    val strength = scale

    override val symbol: String
        get() = DefaultConfig["ui.symbol.forceField", "⇶"]
    override val defaultDisplayName: String
        get() = displayName
    override val description: String
        get() = descriptionI

    override fun isDefaultValue() = false
    override val approxSize = 25

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
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)
        createInspector(getGroup("Force Field", "", "forces").content, style)
    }

    fun createInspector(list: PanelList, style: Style) {
        for (property in listProperties()) {
            list += vi(property.title, property.description, property.value, style)
        }
    }

    fun drawPerParticle(
        stack: Matrix4fArrayList, time: Double, color: Vector4fc,
        applyTransform: (Particle, index0: Int, indexF: Float) -> Unit
    ) {
        super.onDraw(stack, time, color)
        val thisTransform = Matrix4f(stack)
        stack.popMatrix() // use the parent transform
        val system = parent as? ParticleSystem ?: return
        val stepSize = system.simulationStep
        val t0 = System.nanoTime()
        for (particle in system.particles) {
            val opacity = particle.opacity * particle.getLifeOpacity(time, stepSize, 0.5, 0.5).toFloat()
            if (opacity > 0f) {
                val index = (time - particle.birthTime) / stepSize
                val index0 = floor(index).toInt()
                val indexF = (index - index0).toFloat()
                val position = particle.getPosition(index0, indexF)
                stack.next {
                    stack.translate(position)
                    applyTransform(particle, index0, indexF)
                    Grid.drawBuffer(
                        stack, Vector4f(color.x(), color.y(), color.z(), color.w() * opacity),
                        ArrowModel.arrowLineModel
                    )
                }
                val t1 = System.nanoTime()
                if (abs(t1 - t0) > 10_000_000) break // spend at max 10ms here
            }
        }
        stack.pushMatrix()
        stack.set(thisTransform)
    }

    fun drawForcePerParticle(
        stack: Matrix4fArrayList, time: Double, color: Vector4fc
    ) {
        val particles = (parent as? ParticleSystem)?.particles?.filter { it.isAlive(time) } ?: return
        drawPerParticle(stack, time, color) { p, index0, indexF ->
            val state0 = p.states.getOrElse(index0) { p.states.last() }
            val state1 = p.states.getOrElse(index0 + 1) { p.states.last() }
            val otherParticles = particles.filter { it !== p }
            val force0 = getForce(state0, time, otherParticles)
            val force = if (state0 === state1) {
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

    fun getDirection(time: Double): Vector3f {
        val rot = rotationYXZ[time]
        val quat = Quaternionf()
        quat.rotateY(rot.y().toRadians())
        quat.rotateX(rot.x().toRadians())
        quat.rotateZ(rot.z().toRadians())
        return quat.transform(Vector3f(0f, 1f, 0f))
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