package me.anno.objects.particles

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX.gameTime
import me.anno.gpu.GFX.isFinalRendering
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.language.translation.Dict
import me.anno.language.translation.NameDesc
import me.anno.objects.Transform
import me.anno.objects.animation.Type
import me.anno.objects.distributions.*
import me.anno.objects.forces.ForceField
import me.anno.objects.forces.impl.BetweenParticleGravity
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.studio.rems.RemsStudio
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.files.FileEntry.Companion.drawLoadingCircle
import me.anno.ui.editor.stacked.Option
import me.anno.ui.input.BooleanInput
import me.anno.ui.style.Style
import me.anno.utils.Lists.sumByFloat
import me.anno.utils.Maths.clamp
import me.anno.utils.Maths.fract
import me.anno.utils.Maths.mix
import me.anno.utils.Vectors.plus
import me.anno.utils.Vectors.times
import me.anno.utils.processBalanced
import me.anno.utils.structures.UnsafeArrayList
import me.anno.utils.structures.UnsafeSkippingArrayList
import me.anno.video.MissingFrameException
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// todo translations for everything...
// todo limit the history to entries with 5x the same name? how exactly?...

class ParticleSystem(parent: Transform? = null) : Transform(parent) {

    // todo if notCalculating && property was changed, invalidate cache

    val spawnColor = AnimatedDistribution(Type.COLOR3, listOf(Vector3f(1f), Vector3f(0f), Vector3f(0f)))
    val spawnPosition = AnimatedDistribution(Type.POSITION, listOf(Vector3f(0f), Vector3f(1f), Vector3f(0f)))
    val spawnVelocity =
        AnimatedDistribution(GaussianDistribution(), Type.POSITION, listOf(Vector3f(0f), Vector3f(1f), Vector3f(0f)))
    val spawnSize = AnimatedDistribution(Type.SCALE, listOf(Vector3f(1f), Vector3f(0f), Vector3f(0f)))
    var spawnSize1D = true

    val spawnOpacity = AnimatedDistribution(Type.FLOAT, listOf(1f, 0f))

    val spawnMass = AnimatedDistribution(Type.FLOAT, listOf(1f, 0f))

    val spawnRotation = AnimatedDistribution(Type.ROT_YXZ, Vector3f())
    val spawnRotationVelocity = AnimatedDistribution(Type.ROT_YXZ, Vector3f())

    val spawnRate = AnimatedDistribution(Type.FLOAT, 10f)
    val lifeTime = AnimatedDistribution(Type.FLOAT, 10f)

    var showChildren = false
    var simulationStep = 0.5

    val aliveParticles = UnsafeSkippingArrayList<Particle>()
    val particles = UnsafeArrayList<Particle>()

    var seed = 0L
    var random = Random(seed)

    var sumWeight = 0f

    /**
     * the calculation depends somewhat on it;
     * they could be animated, but idk, why you would do that...
     * */
    var fadeIn = 0.5
    var fadeOut = 0.5

    fun step(particle: Particle, forces: List<ForceField>, aliveParticles: List<Particle>) {
        particle.apply {
            val oldState = states.last()
            val force = Vector3f()
            val time = particle.states.size * simulationStep + particle.birthTime
            forces.forEach { field ->
                val subForce = field.getForce(oldState, time, aliveParticles)
                val forceLength = subForce.length()
                if (forceLength.isFinite()) {
                    force.add(
                        if (forceLength < 1000f) {
                            subForce
                        } else {
                            subForce * (1000f / forceLength)
                        }
                    )
                }
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
            states.add(newState)
        }
    }

    private fun spawnIfRequired(time: Double, onlyFirst: Boolean) {

        spawnRate.update(time, random)

        val lastTime = particles.lastOrNull()?.birthTime ?: 0.0
        val c0 = spawnRate.channels[0]
        val integral0 = c0.getIntegral<Float>(lastTime, false)
        val integral1 = c0.getIntegral<Float>(time, false)
        val sinceThenIntegral = integral1 - integral0

        var missingChildren = sinceThenIntegral.toInt()

        if (missingChildren > 0) {

            if (onlyFirst) missingChildren = max(1, missingChildren)

            // todo more accurate calculation for changing spawn rates...
            // todo calculate, when the integral since lastTime surpassed 1.0 xD
            // todo until we have reached time
            // generate new particles
            val newParticles = ArrayList<Particle>()
            for (i in 0 until missingChildren) {
                val newParticle = createParticle(mix(lastTime, time, (i + 1.0) / sinceThenIntegral))
                newParticles += newParticle
            }

            synchronized(this) {
                particles += newParticles
                aliveParticles += newParticles
            }

        }

    }

    /**
     * returns whether everything was calculated
     * */
    fun step(time: Double): Boolean {
        return step(time, false, 1.0 / 120.0)// 120 fps ^^
    }

    var isWorkingAsync: Thread? = null

    /**
     * returns whether everything was calculated
     * */
    fun step(time: Double, isAsync: Boolean, timeLimit: Double): Boolean {
        val startTime = System.nanoTime()

        if (isWorkingAsync != null && !isAsync) {
            synchronized(this) {
                val needsUpdates = aliveParticles.any { it.lastTime(simulationStep) < time }
                return !needsUpdates
            }
        }

        if (aliveParticles.isNotEmpty()) {

            val forces = children.filterIsInstance<ForceField>()
            val hasON2Force = forces.any { it is BetweenParticleGravity }
            var currentTime = aliveParticles.map { it.lastTime(simulationStep) }.min()!!
            while (currentTime < time) {

                Thread.sleep(0)

                // 10 ms timeout
                val deltaTime = abs(System.nanoTime() - startTime)
                if (deltaTime / 1e9 > timeLimit) {

                    if (!isAsync) {
                        isWorkingAsync = thread {
                            try {
                                step(time + 5.0, true, 1.0)
                                addEvent { RemsStudio.updateSceneViews() }
                            } catch (e: InterruptedException) { /* cache was invalidated */
                            }
                            isWorkingAsync = null
                        }
                    }

                    return false
                }

                currentTime = min(time, currentTime + simulationStep)

                Thread.sleep(0)

                spawnIfRequired(currentTime, false)

                Thread.sleep(0)

                val needsUpdate = aliveParticles.filter { it.lastTime(simulationStep) < currentTime }

                // update all particles, which need an update
                if (hasON2Force && !isAsync) {
                    // just process the first entries...
                    val limit = max(65536 / needsUpdate.size, 10)
                    if (needsUpdate.size > limit) {
                        processBalanced(0, limit, 16) { i0, i1 ->
                            val aliveParticles = ArrayList(aliveParticles)
                            for (i in i0 until i1) {
                                step(needsUpdate[i], forces, aliveParticles)
                            }
                        }
                        currentTime -= simulationStep // undo the advancing step...
                    } else {
                        processBalanced(0, needsUpdate.size, 16) { i0, i1 ->
                            val aliveParticles = ArrayList(aliveParticles)
                            for (i in i0 until i1) {
                                step(needsUpdate[i], forces, aliveParticles)
                            }
                        }
                        aliveParticles.removeIf { (it.states.size - 2) * simulationStep >= it.lifeTime }
                    }
                } else {
                    // process all
                    processBalanced(0, needsUpdate.size, 16) { i0, i1 ->
                        val aliveParticles = ArrayList(aliveParticles)
                        for (i in i0 until i1) {
                            step(needsUpdate[i], forces, aliveParticles)
                        }
                    }
                    aliveParticles.removeIf { (it.states.size - 2) * simulationStep >= it.lifeTime }
                }


            }

        } else {

            spawnIfRequired(time, true)
            return aliveParticles.isEmpty() || step(time)

        }

        return true
    }

    override fun drawChildrenAutomatically() = !isFinalRendering && showChildren

    private fun createParticle(time: Double): Particle {

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
            if (isWorkingAsync != null) {
                isWorkingAsync?.interrupt()
                isWorkingAsync = null
            }
            particles.clear()
            aliveParticles.clear()
            random = Random(seed)
        }
        RemsStudio.updateSceneViews()
    }

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f) {

        super.onDraw(stack, time, color)

        // draw all forces
        if (!isFinalRendering) {
            children.filterIsInstance<ForceField>().forEach {
                stack.pushMatrix()
                it.draw(stack, time, color)
                stack.popMatrix()
            }
            synchronized(spawnPosition) {
                spawnPosition.update(time, Random())
                spawnPosition.distribution.onDraw(stack, color)
            }
        }

        sumWeight = children.filterNot { it is ForceField }.sumByFloat { it.weight }
        if (time < 0f || children.isEmpty() || sumWeight <= 0.0) return

        if (step(time)) {
            drawParticles(stack, time, color)
        } else {
            if (isFinalRendering) throw MissingFrameException(name)
            drawLoadingCircle(stack, (gameTime * 1e-9f) % 1f)
            drawParticles(stack, time, color)
        }

    }

    private fun drawParticles(stack: Matrix4fArrayList, time: Double, color: Vector4f) {

        // draw all particles at this point in time
        particles.forEach { p ->
            p.apply {


                val lifeOpacity = p.getLifeOpacity(time, simulationStep, fadeIn, fadeOut).toFloat()
                val opacity = clamp(lifeOpacity * p.opacity, 0f, 1f)
                if (opacity > 1e-3f) {// else not visible
                    stack.pushMatrix()

                    try {

                        val particleTime = time - p.birthTime
                        val index = particleTime / simulationStep
                        val index0 = index.toInt()
                        val indexF = fract(index).toFloat()

                        val state0 = states.getOrElse(index0) { states.last() }
                        val state1 = states.getOrElse(index0 + 1) { states.last() }

                        val position = state0.position.lerp(state1.position, indexF, position)
                        val rotation = state0.rotation.lerp(state1.rotation, indexF, rotation)

                        stack.translate(position)
                        stack.rotateY(rotation.y)
                        stack.rotateX(rotation.x)
                        stack.rotateZ(rotation.z)
                        stack.scale(scale)

                        val color0 = state0.color.lerp(state1.color, indexF, p.color)

                        // normalize time for calculated functions?
                        // node editor? like in Blender or Unreal Engine
                        val particleColor = color * Vector4f(color0, opacity)
                        type.draw(stack, time - p.birthTime, particleColor)

                    } catch (e: IndexOutOfBoundsException) {
                        if (isFinalRendering) throw MissingFrameException("$p")
                    }

                    stack.popMatrix()
                }


            }
        }

    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {

        super.createInspector(list, style, getGroup)

        var viCtr = 0
        fun vi(name: String, description: String, property: AnimatedDistribution) {
            fun getName() = "$name: ${property.distribution.getClassName().split("Distribution").first()}"
            val group = getGroup(getName(), "", "$viCtr")
            group.setTooltip(description)
            group.setOnClickListener { _, _, button, long ->
                if (button.isRight || long) {
                    // show all options for different distributions
                    openMenu(
                        NameDesc("Change Distribution", "", "obj.particles.changeDistribution"),
                        listDistributions().map { generator ->
                            val sample = generator()
                            MenuOption(NameDesc(sample.displayName, sample.description, "")) {
                                RemsStudio.largeChange("Change $name Distribution") {
                                    property.distribution = generator()
                                }
                                clearCache()
                                group.content.clear()
                                group.titlePanel.text = getName()
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

        fun vt(name: String, title: String, description: String, obj: AnimatedDistribution) {
            vi(Dict[title, "obj.particles.$name"], Dict[description, "obj.particles.$name.desc"], obj)
        }

        vt("spawnRate", "Spawn Rate", "How many particles are spawned per second", spawnRate)
        vt("lifeTime", "Life Time", "How many seconds a particle is visible", lifeTime)
        vt("initPosition", "Initial Position", "Where the particles spawn", spawnPosition)
        vt("initVelocity", "Initial Velocity", "How fast the particles are, when they are spawned", spawnVelocity)
        vi("Initial Rotation", "How the particles are rotated initially", spawnRotation)
        vi("Rotation Velocity", "How fast the particles are rotating", spawnRotationVelocity)

        vi("Color", "Initial particle color", spawnColor)
        vi("Opacity", "Initial particle opacity (1-transparency)", spawnOpacity)
        vi("Size", "Initial particle size", spawnSize)

        val general = getGroup("Particle System", "", "particles")

        general += vi(
            "Simulation Step",
            "Larger values are faster, while smaller values are more accurate for forces",
            Type.DOUBLE, simulationStep, style
        ) {
            if (it > 1e-9) simulationStep = it
            clearCache()
        }

        general += vi(
            "Fade In",
            "Time from spawning to the max. opacity",
            Type.DOUBLE_PLUS, fadeIn, style
        ) { fadeIn = it }
        general += vi(
            "Fade Out",
            "Time before death, from which is starts to fade away",
            Type.DOUBLE_PLUS, fadeOut, style
        ) { fadeOut = it }

        general += BooleanInput("Show Children", showChildren, style)
            .setChangeListener { showChildren = it }
            .setIsSelectedListener { show(null) }

        general += vi(
            "Seed",
            "The seed for all randomness",
            null, seed, style
        ) {
            seed = it
            clearCache()
        }

        general += TextButton("Reset Cache", false, style)
            .setSimpleClickListener { clearCache() }

    }

    override fun getAdditionalChildrenOptions(): List<Option> {
        return ForceField.getForceFields()
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeDouble("simulationStep", simulationStep)
        writer.writeDouble("fadeIn", fadeIn)
        writer.writeDouble("fadeOut", fadeOut)
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
            "fadeIn" -> fadeIn = max(value, 0.0)
            "fadeOut" -> fadeOut = max(value, 0.0)
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
    override fun getClassName() = "ParticleSystem"
    override fun getDefaultDisplayName(): String = Dict["Particle System", "obj.particles"]
    override fun getSymbol() = DefaultConfig["ui.symbol.particleSystem", "❄"]

    companion object {
        fun listDistributions(): List<() -> Distribution> {
            return listOf(
                { ConstantDistribution() },
                { GaussianDistribution() },
                { CuboidDistribution() },
                { CuboidHullDistribution() },
                { SphereVolumeDistribution() },
                { SphereHullDistribution() }
            )
        }
    }

}