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
import me.anno.objects.animation.Type
import me.anno.objects.distributions.*
import me.anno.objects.forces.ForceField
import me.anno.objects.forces.impl.BetweenParticleGravity
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.studio.rems.RemsStudio
import me.anno.ui.base.SpyPanel
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
import me.anno.utils.Maths.mix
import me.anno.utils.processBalanced
import me.anno.utils.structures.UnsafeArrayList
import me.anno.utils.structures.UnsafeSkippingArrayList
import me.anno.utils.structures.ValueWithDefault
import me.anno.utils.structures.ValueWithDefault.Companion.writeMaybe
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

open class ParticleSystem(parent: Transform? = null) : Transform(parent) {

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

    val spawnRate = AnimatedDistribution(Type.FLOAT, 10f)
    val lifeTime = AnimatedDistribution(Type.FLOAT, 10f)

    var showChildren = false
    var simulationStepI = ValueWithDefault(0.5)
    var simulationStep: Double
        get() = simulationStepI.value
        set(value) = simulationStepI.set(value)

    val aliveParticles = UnsafeSkippingArrayList<Particle>()
    val particles = UnsafeArrayList<Particle>()

    var seed = 0L
    var random = Random(seed)

    var sumWeight = 0f

    /**
     * the calculation depends somewhat on it;
     * they could be animated, but idk, why you would do that...
     * */
    var fadeInI = ValueWithDefault(0.5)
    var fadeOutI = ValueWithDefault(0.5)
    var fadeIn: Double
        get() = fadeInI.value
        set(value) = fadeInI.set(value)
    var fadeOut: Double
        get() = fadeOutI.value
        set(value) = fadeOutI.set(value)

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

            val ps = particles.size

            // todo more accurate calculation for changing spawn rates...
            // todo calculate, when the integral since lastTime surpassed 1.0 xD
            // todo until we have reached time
            // generate new particles
            val newParticles = ArrayList<Particle>()
            for (i in 0 until missingChildren) {
                val newParticle = createParticle(
                    ps + i,
                    mix(lastTime, time, (i + 1.0) / sinceThenIntegral)
                )
                newParticles += newParticle ?: continue
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
                val simulationStep = simulationStep

                // update all particles, which need an update
                if (hasON2Force && !isAsync) {
                    // just process the first entries...
                    val limit = max(65536 / needsUpdate.size, 10)
                    if (needsUpdate.size > limit) {
                        processBalanced(0, limit, 16) { i0, i1 ->
                            val aliveParticles = ArrayList(aliveParticles)
                            for (i in i0 until i1) needsUpdate[i].step(simulationStep, forces, aliveParticles)
                        }
                        currentTime -= simulationStep // undo the advancing step...
                    } else {
                        processBalanced(0, needsUpdate.size, 16) { i0, i1 ->
                            val aliveParticles = ArrayList(aliveParticles)
                            for (i in i0 until i1) needsUpdate[i].step(simulationStep, forces, aliveParticles)
                        }
                        aliveParticles.removeIf { (it.states.size - 2) * simulationStep >= it.lifeTime }
                    }
                } else {
                    // process all
                    processBalanced(0, needsUpdate.size, 16) { i0, i1 ->
                        val aliveParticles = ArrayList(aliveParticles)
                        for (i in i0 until i1) needsUpdate[i].step(simulationStep, forces, aliveParticles)
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

    open fun createParticle(index: Int, time: Double): Particle? {

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

    fun clearCache(state: Any? = getSystemState()) {
        lastState = state
        lastCheckup = gameTime
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

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f) {

        super.onDraw(stack, time, color)

        checkNeedsUpdate()

        // draw all forces
        if (!isFinalRendering) {
            children.filterIsInstance<ForceField>().forEach {
                stack.pushMatrix()
                it.draw(stack, time, color)
                stack.popMatrix()
            }
            val dist = selectedDistribution
            // todo this doesn't seem to work completely :/
            synchronized(dist) {
                dist.update(time, Random())
                dist.distribution.draw(stack, color)
            }
        }

        sumWeight = children.filterNot { it is ForceField }.sumByFloat { it.weight }
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
    private fun drawParticles(stack: Matrix4fArrayList, time: Double, color: Vector4f) {
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
            fun getName() = "$name: ${property.distribution.getClassName().split("Distribution").first()}"
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
        writer.writeMaybe(this, "simulationStep", simulationStepI)
        writer.writeMaybe(this, "fadeIn", fadeInI)
        writer.writeMaybe(this, "fadeOut", fadeOutI)
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
                builder.append(it.uuid)
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

    // var timeSum = 0f
    // var timeCtr = 0

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