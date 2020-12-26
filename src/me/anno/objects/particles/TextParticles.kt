package me.anno.objects.particles

import me.anno.gpu.GFX
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.Transform
import me.anno.objects.animation.Type
import me.anno.objects.distributions.*
import me.anno.objects.particles.forces.ForceField
import me.anno.objects.particles.forces.impl.*
import me.anno.studio.rems.RemsStudio
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.stacked.Option
import me.anno.ui.input.BooleanInput
import me.anno.ui.style.Style
import me.anno.utils.Lists.sumByFloat
import me.anno.utils.Maths
import me.anno.utils.Vectors.plus
import me.anno.utils.Vectors.times
import me.anno.utils.processBalanced
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max
import kotlin.math.roundToInt

// todo text particle system
// todo linear + randomness for spawn time
// todo select base font individually? just select the most common properties? or animatable base properties? idk...

class TextParticles: Transform() {

    // todo if notCalculating && property was changed, invalidate cache

    // todo what about negative colors?... need to be generatable
    val spawnColor = AnimatedDistribution(Type.COLOR3, listOf(Vector3f(1f), Vector3f(0f)))
    val spawnPosition = AnimatedDistribution(Type.POSITION, listOf(Vector3f(0f), Vector3f(1f)))
    val spawnVelocity = AnimatedDistribution(GaussianDistribution(), Type.POSITION, listOf(Vector3f(0f), Vector3f(1f)))
    val spawnSize = AnimatedDistribution(Type.SCALE, listOf(Vector3f(1f), Vector3f(0f)))
    var spawnSize1D = true

    val spawnOpacity = AnimatedDistribution(Type.FLOAT, listOf(1f, 0f))

    val spawnMass = AnimatedDistribution(Type.FLOAT, listOf(1f, 0f))

    val spawnRotation = AnimatedDistribution(Type.ROT_YXZ, Vector3f())
    val spawnRotationVelocity = AnimatedDistribution(Type.ROT_YXZ, Vector3f())

    val spawnRate = AnimatedDistribution(Type.FLOAT, 10f)
    val lifeTime = AnimatedDistribution(Type.FLOAT, 10f)

    var showChildren = false
    var simulationStep = 0.5

    val aliveParticles = ArrayList<Particle>()
    val particles = ArrayList<Particle>()
    var seed = 0L

    var random = Random(seed)

    var sumWeight = 0f

    var fadingIn = 0.5
    var fadingOut = 0.5

    fun step(particle: Particle, forces: List<ForceField>) {
        particle.apply {
            val oldState = states.last()
            val force = Vector3f()
            val time = particle.states.size * simulationStep + particle.birthTime
            forces.forEach { field ->
                force.add(field.getForce(oldState, time, aliveParticles))
            }
            val ddPosition = force / mass
            val dt = simulationStep.toFloat()
            val dPosition = oldState.dPosition + ddPosition * dt
            val position = oldState.position + dPosition * dt
            val newState = ParticleState()
            newState.position = position
            newState.dPosition = dPosition
            newState.rotation = oldState.rotation + oldState.dRotation * dt
            newState.dRotation = oldState.dRotation // todo rotational friction or acceleration???...
            newState.color = oldState.color
            synchronized(states) {
                states.add(newState)
            }
        }
    }

    fun step(particles: ArrayList<Particle>, time: Double) {

        val lastTime = particles.lastOrNull()?.birthTime ?: 0.0
        if (lastTime >= time) return

        spawnRate.update(time, random)

        val c0 = spawnRate.channels[0]
        val integral0 = c0.getIntegral<Float>(lastTime, false)
        val integral1 = c0.getIntegral<Float>(time, false)
        val sinceThenIntegral = integral1 - integral0

        val missingChildren = sinceThenIntegral.toInt()

        // todo more accurate calculation for changing spawn rates...
        // todo calculate, when the integral since lastTime surpassed 1.0 xD
        // todo until we have reached time
        // generate new particles
        for (i in 0 until missingChildren) {
            val newParticle = createParticle(Maths.mix(lastTime, time, (i + 1.0) / sinceThenIntegral))
            particles += newParticle
            aliveParticles += newParticle
        }

        // update all particles, which need an update
        val forces = children.filterIsInstance<ForceField>()
        processBalanced(0, aliveParticles.size, false) { i0, i1 ->
            for (i in i0 until i1) {
                val particle = aliveParticles[i]
                val particleTime = time - particle.birthTime
                val index = (particleTime / simulationStep).roundToInt()
                while (particle.states.size < index + 2) {
                    step(particle, forces)
                }
            }
        }
        aliveParticles.removeIf { time - it.birthTime >= it.lifeTime + 2 * simulationStep }

    }

    override fun drawChildrenAutomatically() = !GFX.isFinalRendering && showChildren

    fun createParticle(time: Double): Particle {

        // find the particle type
        var randomIndex = random.nextFloat() * sumWeight
        var type = children.first()
        for (child in children.filterNot { it is ForceField }) {
            val cWeight = child.weight
            randomIndex -= cWeight
            if (randomIndex <= 0f) {
                type = child
                break
            }
        }

        val lifeTime = lifeTime.nextV1(time, random).toDouble()

        // create the particle
        val particle = Particle(type, time, lifeTime, spawnMass.nextV1(time, random))
        if (spawnSize1D) particle.scale.set(spawnSize.nextV1(time, random))
        else particle.scale.set(spawnSize.nextV3(time, random))
        particle.opacity = spawnOpacity.nextV1(time, random)

        // create the initial state
        val state = ParticleState()
        state.position = spawnPosition.nextV3(time, random)
        state.rotation = spawnRotation.nextV3(time, random)
        state.color = spawnColor.nextV3(time, random)
        state.dPosition = spawnVelocity.nextV3(time, random)
        state.dRotation = spawnRotationVelocity.nextV3(time, random)

        // apply the state
        particle.states.add(state)

        return particle

    }

    fun clearCache() {
        synchronized(this) {
            particles.clear()
            aliveParticles.clear()
            random = Random(seed)
        }
        RemsStudio.updateSceneViews()
    }

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f) {

        super.onDraw(stack, time, color)

        sumWeight = children.filterNot { it is ForceField }.sumByFloat { it.weight }
        if (time < 0f || children.isEmpty() || sumWeight <= 0.0) return

        step(particles, time)

        // draw all particles at this point in time
        particles.forEach {
            it.apply {

                val lifeOpacity = it.getLifeOpacity(time, simulationStep, fadingIn, fadingOut).toFloat()
                val opacity = Maths.clamp(lifeOpacity * it.opacity, 0f, 1f)
                if (opacity > 1e-3f) {// else not visible
                    stack.pushMatrix()

                    val particleTime = time - it.birthTime
                    val index = particleTime / simulationStep
                    val index0 = index.toInt()
                    val indexF = Maths.fract(index).toFloat()

                    val position = getPosition(index0, indexF)
                    val rotation = getRotation(index0, indexF)

                    stack.translate(position)
                    stack.rotateY(rotation.y)
                    stack.rotateX(rotation.x)
                    stack.rotateZ(rotation.z)
                    stack.scale(scale)

                    val color0 = getColor(index0, indexF)

                    // normalize time for calculated functions?
                    // node editor? like in Blender or Unreal Engine
                    val particleColor = color * Vector4f(color0, opacity)
                    type.draw(stack, time - it.birthTime, particleColor)

                    stack.popMatrix()
                }

            }
        }

    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, id: String) -> SettingCategory
    ) {

        super.createInspector(list, style, getGroup)

        var viCtr = 0
        fun VI(name: String, description: String, property: AnimatedDistribution) {
            fun getName() = "$name: ${property.distribution.getClassName().split("Distribution").first()}"
            val group = getGroup(getName(), "$viCtr")
            group.setTooltip(description)
            group.setOnClickListener { _, _, button, long ->
                if (button.isRight || long) {
                    // show all options for different distributions
                    GFX.openMenu(
                        "Change Distribution",
                        listOf(
                            { ConstantDistribution() },
                            { GaussianDistribution() },
                            { UniformDistribution() },
                            { SphereHullDistribution() },
                            { SphereVolumeDistribution() }
                        ).map { generator ->
                            val sample = generator()
                            GFX.MenuOption(sample.displayName, sample.description) {
                                RemsStudio.largeChange("Change $name Distribution") {
                                    property.distribution = generator()
                                }
                                clearCache()
                                group.content.clear()
                                group.title.text = getName()
                                property.createInspector(group.content, this, style)
                            }
                        }
                    )
                }
            }
            property.createInspector(group.content, this, style)
            viCtr++
        }

        // todo visualize the distributions and their parameters somehow...

        VI("Spawn Rate", "How many particles are spawned per second", spawnRate)
        VI("Life Time", "How many seconds a particle is visible", lifeTime)
        VI("Initial Position", "Where the particles spawn", spawnPosition)
        VI("Initial Velocity", "How fast the particles are, when they are spawned", spawnVelocity)
        VI("Initial Rotation", "How the particles are rotated initially", spawnRotation)
        VI("Rotation Velocity", "How fast the particles are rotating", spawnRotationVelocity)

        VI("Color", "Initial particle color", spawnColor)
        VI("Opacity", "Initial particle opacity (1-transparency)", spawnOpacity)
        VI("Size", "Initial particle size", spawnSize)

        val general = getGroup("Particle System", "particles")

        general += vi(
            "Simulation Step",
            "Larger values are faster, while smaller values are more accurate for forces",
            Type.DOUBLE, simulationStep, style
        ) {
            if (it > 1e-9) simulationStep = it
            clearCache()
        }

        general += BooleanInput("Show Children", showChildren, style)
            .setChangeListener { showChildren = it }
            .setIsSelectedListener { show(null) }

        general += vi("Seed", "The seed for all randomness", null, seed, style) {
            seed = it
            clearCache()
        }

        general += TextButton("Reset Cache", false, style)
            .setSimpleClickListener { clearCache() }

    }

    override fun getAdditionalChildrenOptions(): List<Option> {
        return listOf(
            option { GlobalForce() },
            option { GravityField() },
            option { MultiGravityForce() },
            option { LorentzForce() },
            option { TornadoField() },
            option { VelocityFrictionForce() }
        )
    }

    fun option(generator: () -> ForceField): Option {
        val sample = generator()
        return Option(sample.displayName, sample.description) {
            generator()
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeDouble("simulationStep", simulationStep)
        writer.writeObject(this, "spawnPosition", spawnPosition)
        writer.writeObject(this, "spawnVelocity", spawnVelocity)
        writer.writeObject(this, "spawnRotation", spawnRotation)
        writer.writeObject(this, "spawnRotationVelocity", spawnRotationVelocity)
        writer.writeObject(this, "spawnRate", spawnRate)
        writer.writeObject(this, "lifeTime", lifeTime)
        writer.writeObject(this, "spawnColor", spawnColor)
        writer.writeObject(this, "spawnOpacity", spawnOpacity)
        writer.writeObject(this, "spawnSize", spawnSize)
    }

    override fun readDouble(name: String, value: Double) {
        when (name) {
            "simulationStep" -> simulationStep = max(1e-9, value)
            else -> super.readDouble(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "spawnPosition" -> spawnPosition.copyFrom(value)
            "spawnVelocity" -> spawnVelocity.copyFrom(value)
            "spawnRotation" -> spawnRotation.copyFrom(value)
            "spawnRotationVelocity" -> spawnRotationVelocity.copyFrom(value)
            "spawnRate" -> spawnRate.copyFrom(value)
            "lifeTime" -> lifeTime.copyFrom(value)
            "spawnColor" -> spawnColor.copyFrom(value)
            "spawnOpacity" -> spawnOpacity.copyFrom(value)
            "spawnSize" -> spawnSize.copyFrom(value)
            else -> {
                super.readObject(name, value)
                return
            }
        }
        clearCache()
    }

    override fun acceptsWeight() = true
    override fun getClassName() = "TextParticleSystem"

}