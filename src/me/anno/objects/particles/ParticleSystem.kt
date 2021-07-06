package me.anno.objects.particles

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX.gameTime
import me.anno.gpu.GFX.isFinalRendering
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.text.TextWriter
import me.anno.language.translation.Dict
import me.anno.language.translation.NameDesc
import me.anno.objects.Transform
import me.anno.animation.AnimatedProperty
import me.anno.animation.Integral.findIntegralX
import me.anno.animation.Integral.getIntegral
import me.anno.animation.Type
import me.anno.objects.distributions.*
import me.anno.objects.forces.ForceField
import me.anno.objects.forces.impl.BetweenParticleGravity
import me.anno.studio.rems.RemsStudio
import me.anno.ui.base.SpyPanel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.files.FileExplorerEntry.Companion.drawLoadingCircle
import me.anno.ui.editor.stacked.Option
import me.anno.ui.input.BooleanInput
import me.anno.ui.style.Style
import me.anno.utils.bugs.SumOf
import me.anno.utils.hpc.HeavyProcessing.processBalanced
import me.anno.utils.structures.ValueWithDefault
import me.anno.utils.structures.ValueWithDefault.Companion.writeMaybe
import me.anno.video.MissingFrameException
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.joml.Vector4f
import org.joml.Vector4fc
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

open class ParticleSystem(parent: Transform? = null) : Transform(parent) {

    override fun getDocumentationURL(): URL? = URL("https://remsstudio.phychi.com/?s=learn/particle-systems")

    val spawnColor = AnimatedDistribution(Type.COLOR3, listOf(Vector3f(1f), Vector3f(0f), Vector3f(0f)))
    val spawnPosition = AnimatedDistribution(Type.POSITION, listOf(Vector3f(0f), Vector3f(1f), Vector3f(0f)))
    val spawnVelocity =
        AnimatedDistribution(GaussianDistribution(), Type.POSITION, listOf(Vector3f(), Vector3f(1f), Vector3f()))
    val spawnSize = AnimatedDistribution(Type.SCALE, listOf(Vector3f(1f), Vector3f(0f), Vector3f(0f)))
    var spawnSize1D = true

    val spawnOpacity = AnimatedDistribution(Type.FLOAT, listOf(1f, 0f))

    val spawnMass = AnimatedDistribution(Type.FLOAT, listOf(1f, 0f))

    val spawnRotation = AnimatedDistribution(Type.ROT_YXZ, Vector3f())
    val spawnRotationVelocity = AnimatedDistribution(Type.ROT_YXZ, Vector3f())

    val spawnRate = AnimatedProperty.floatPlus(10f)
    val lifeTime = AnimatedDistribution(Type.FLOAT, 10f)

    var showChildren = false
    var simulationStepI = ValueWithDefault(0.5)
    var simulationStep: Double
        get() = simulationStepI.value
        set(value) {
            simulationStepI.value = value
        }

    val aliveParticles = ArrayList<Particle>(1024)
    val particles = ArrayList<Particle>(1024)

    var seed = 0L
    var random = Random(seed)

    var sumWeight = 0f

    override fun usesFadingDifferently(): Boolean = true
    override fun getStartTime(): Double = Double.NEGATIVE_INFINITY
    override fun getEndTime(): Double = Double.POSITIVE_INFINITY

    private fun spawnIfRequired(time: Double, onlyFirst: Boolean) {

        val lastTime = particles.lastOrNull()?.birthTime ?: 0.0
        val c0 = spawnRate
        val sinceThenIntegral = c0.getIntegral(lastTime, time, false)

        var missingChildren = sinceThenIntegral.toInt()
        if (missingChildren > 0) {

            if (onlyFirst) missingChildren = max(1, missingChildren)

            val ps = particles.size

            // generate new particles
            val newParticles = ArrayList<Particle>()
            var timeI = lastTime
            for (i in 0 until missingChildren) {
                // more accurate calculation for changing spawn rates
                // - calculate, when the integral since lastTime surpassed 1.0 until we have reached time
                val nextTime = c0.findIntegralX(timeI, time, 1.0, 1e-9)
                val newParticle = createParticle(ps + i, nextTime)
                timeI = nextTime
                newParticles += newParticle ?: continue
            }

            particles += newParticles
            aliveParticles += newParticles

        }

    }

    /**
     * returns whether everything was calculated
     * */
    fun step(time: Double, timeLimit: Double = 1.0 / 120.0): Boolean {
        val startTime = System.nanoTime()

        if (aliveParticles.isNotEmpty()) {

            val simulationStep = simulationStep
            val forces = children.filterIsInstance<ForceField>()
            val hasHeavyComputeForce = forces.any { it is BetweenParticleGravity }
            var currentTime = aliveParticles.map { it.lastTime(simulationStep) }.minOrNull() ?: return true
            while (currentTime < time) {

                // 10 ms timeout
                val deltaTime = abs(System.nanoTime() - startTime)
                if (deltaTime / 1e9 > timeLimit) return false

                currentTime = min(time, currentTime + simulationStep)

                spawnIfRequired(currentTime, false)

                val needsUpdate = aliveParticles.filter { it.lastTime(simulationStep) < currentTime }

                // update all particles, which need an update
                if (hasHeavyComputeForce) {
                    // just process the first entries...
                    val limit = max(65536 / needsUpdate.size, 10)
                    if (needsUpdate.size > limit) {
                        processBalanced(0, limit, 16) { i0, i1 ->
                            for (i in i0 until i1) needsUpdate[i].step(simulationStep, forces, aliveParticles)
                        }
                        currentTime -= simulationStep // undo the advancing step...
                    } else {
                        processBalanced(0, needsUpdate.size, 16) { i0, i1 ->
                            for (i in i0 until i1) needsUpdate[i].step(simulationStep, forces, aliveParticles)
                        }
                        aliveParticles.removeIf { it.hasDied }
                    }
                } else {
                    // process all
                    processBalanced(0, needsUpdate.size, 256) { i0, i1 ->
                        for (i in i0 until i1) needsUpdate[i].step(simulationStep, forces, aliveParticles)
                    }
                    aliveParticles.removeIf { it.hasDied }
                }
            }

        } else {

            spawnIfRequired(time, true)
            return aliveParticles.isEmpty() || step(time)

        }

        return true
    }

    override fun drawChildrenAutomatically() = !isFinalRendering && showChildren

    open fun createParticle(index: Int, time: Double): Particle? {

        val random = random

        // find the particle type
        var randomIndex = random.nextFloat() * sumWeight
        var type = children.firstOrNull() ?: Transform()
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
        val color3 = spawnColor.nextV3(time, random)
        val opacity = spawnOpacity.nextV1(time, random)
        val color4 = Vector4f(color3, opacity)
        val scale = if (spawnSize1D) Vector3f(spawnSize.nextV1(time, random)) else spawnSize.nextV3(time, random)
        val particle = Particle(type, time, lifeTime, spawnMass.nextV1(time, random), color4, scale, simulationStep)

        // create the initial state
        val state = ParticleState(
            spawnPosition.nextV3(time, random),
            spawnVelocity.nextV3(time, random),
            spawnRotation.nextV3(time, random),
            spawnRotationVelocity.nextV3(time, random)
        )

        // apply the state
        particle.states.add(state)

        return particle

    }

    fun clearCache(state: Any? = getSystemState()) {
        lastState = state
        lastCheckup = gameTime
        particles.clear()
        aliveParticles.clear()
        random = Random(seed)
        RemsStudio.updateSceneViews()
    }

    var lastState: Any? = null
    var lastCheckup = 0L
    var timeoutMultiplier = 1

    private fun checkNeedsUpdate() {
        val time = gameTime
        if (abs(time - lastCheckup) > 33_000_000 * timeoutMultiplier) {// 30 fps
            // how fast is this method?
            // would be binary writing and reading faster?
            val state = getSystemState()
            lastCheckup = time
            if (lastState != state) {
                timeoutMultiplier = 1
                lastState = state
                clearCache(state)
            } else {// once every 5s, if not changed
                timeoutMultiplier = min(
                    timeoutMultiplier + 1,
                    30 * 5
                )
            }
        }
    }

    open fun needsChildren() = true

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4fc) {

        super.onDraw(stack, time, color)

        checkNeedsUpdate()

        // draw all forces
        if (!isFinalRendering) {
            children.filterIsInstance<ForceField>().forEach {
                stack.next {
                    it.draw(stack, time, color)
                }
            }
            val dist = selectedDistribution
            dist.update(time, Random())
            dist.distribution.draw(stack, color)
        }

        sumWeight = SumOf.sumOf(children.filterNot { it is ForceField }){ it.weight }// .sumOf { it.weight }
        if (needsChildren() && (time < 0f || children.isEmpty() || sumWeight <= 0.0)) return

        if (step(time)) {
            drawParticles(stack, time, color)
        } else {
            if (isFinalRendering) throw MissingFrameException(name)
            drawLoadingCircle(stack, (gameTime * 1e-9f) % 1f)
            drawParticles(stack, time, color)
        }

    }

    /**
     * draw all particles at this point in time
     * */
    private fun drawParticles(stack: Matrix4fArrayList, time: Double, color: Vector4fc) {
        val fadeIn = fadeIn[time].toDouble()
        val fadeOut = fadeOut[time].toDouble()
        val simulationStep = simulationStep
        particles.forEach { p ->
            p.draw(stack, time, color, simulationStep, fadeIn, fadeOut)
        }
    }

    var selectedDistribution: AnimatedDistribution = spawnPosition

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {

        super.createInspector(list, style, getGroup)

        var viCtr = 0
        fun vi(name: String, description: String, property: AnimatedDistribution) {
            fun getName() = "$name: ${property.distribution.className.split("Distribution").first()}"
            val group = getGroup(getName(), "", "$viCtr")
            group.setTooltip(description)
            group.add(SpyPanel(style) {
                if (group.listOfVisible.any { it.isInFocus }) {
                    val needsUpdate = selectedDistribution !== property
                    selectedDistribution = property
                    if (needsUpdate) RemsStudio.updateSceneViews()
                }
            })
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

        fun vt(name: String, title: String, description: String, obj: AnimatedDistribution) {
            vi(Dict[title, "obj.particles.$name"], Dict[description, "obj.particles.$name.desc"], obj)
        }

        vt("lifeTime", "Life Time", "How many seconds a particle is visible", lifeTime)
        vt("initPosition", "Initial Position", "Where the particles spawn", spawnPosition)
        vt("initVelocity", "Initial Velocity", "How fast the particles are, when they are spawned", spawnVelocity)
        vi("Initial Rotation", "How the particles are rotated initially", spawnRotation)
        vi("Rotation Velocity", "How fast the particles are rotating", spawnRotationVelocity)

        vi("Color", "Initial particle color", spawnColor)
        vi("Opacity", "Initial particle opacity (1-transparency)", spawnOpacity)
        vi("Size", "Initial particle size", spawnSize)

        val general = getGroup("Particle System", "", "particles")

        general += vi("Spawn Rate", "How many particles are spawned per second", "", spawnRate, style)

        general += vi(
            "Simulation Step",
            "Larger values are faster, while smaller values are more accurate for forces",
            Type.DOUBLE, simulationStep, style
        ) {
            if (it > 1e-9) simulationStep = it
            clearCache()
        }

        // general += vi("Fade In", "Time from spawning to the max. opacity", fadeIn, style)
        // general += vi("Fade Out", "Time before death, from which is starts to fade away", fadeOut, style)

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
        writer.writeMaybe(this, "simulationStep", simulationStepI)
        // writer.writeMaybe(this, "fadeIn", fadeInI)
        // writer.writeMaybe(this, "fadeOut", fadeOutI)
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

    open fun getSystemState(): Any? {
        // val t0 = System.nanoTime()
        val writer = TextWriter(false)
        writer.add(spawnPosition)
        writer.add(spawnVelocity)
        writer.add(spawnRotation)
        writer.add(spawnRotationVelocity)
        writer.add(spawnRate)
        writer.add(lifeTime)
        writer.add(spawnColor)
        writer.add(spawnOpacity)
        writer.add(spawnSize)
        val builder = writer.data
        builder.append(simulationStepI.value)
        children.forEach {
            if (it is ForceField) {
                it.parent = null
                writer.add(it)
            } else {
                builder.append(it.weight)
                builder.append(it.clickId)
            }
        }
        writer.writeAllInList()
        children.forEach {
            if (it is ForceField) {
                it.parent = this
            }
        }
        /* val t1 = System.nanoTime()
        timeSum += (t1-t0)*1e-6f
        timeCtr++
        println("${timeSum/timeCtr}ms")*/
        // 0.3-0.5 ms -> could be improved
        // -> improved it to ~ 0.056 ms by avoiding a full copy
        // could be improved to 0.045 ms (~20%) by using a binary writer
        // but it's less well readable -> use the more expensive version;
        // the gain is just too small for the costs
        return builder.toString()
    }

    override fun acceptsWeight() = true
    override val className get() = "ParticleSystem"
    override val defaultDisplayName: String = Dict["Particle System", "obj.particles"]
    override val symbol = DefaultConfig["ui.symbol.particleSystem", "❄"]

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