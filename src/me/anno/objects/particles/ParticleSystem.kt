package me.anno.objects.particles

import me.anno.gpu.GFX
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.BooleanInput
import me.anno.ui.style.Style
import me.anno.utils.plus
import me.anno.utils.times
import me.anno.video.MissingFrameException
import org.joml.Matrix4fStack
import org.joml.Random
import org.joml.Vector3f
import org.joml.Vector4f
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

// todo spawn from object, so the spawn point can be animated without influence on the particles;
// while still keeping the animation local
class ParticleSystem(parent: Transform?): Transform(parent){

    enum class SpawnType(val hasRotation: Boolean){
        CUBE(true), PLANE(true), CYLINDER(true),
        SPHERE(false)
    }

    // todo ranges...
    // todo special spawning spaces like cylinders, spheres, ...?
    // todo -> calculate the volume of a mesh, sample from that distribution
    // todo use 3D mesh as spawning space
    var gdPosition = AnimatedProperty.pos()
    var gddPosition = AnimatedProperty.pos().set(Vector3f(0f, -9.81f, 0f))

    var gdRotation = AnimatedProperty.rotYXZ()
    var gddRotation = AnimatedProperty.rotYXZ()

    var spawnRate = AnimatedProperty.float().set(10f) // per second
    var lifeTime = AnimatedProperty.float().set(10f)

    // todo manipulate colors, position, forces, and more...
    // todo this is a complex object type
    // todo dock force fields?

    var childrenScale = 0.1f

    var showChildren = false
    var nextIndex = 0
    var missingChildren = 0f
    var timeStep = 0.1f

    val particles = ArrayList<ParticleInfo>()
    var seed = 0L
    var random = Random(seed)
    var sumWeight = 0f

    var fadingIn = 0.5f
    var fadingOut = 0.5f

    fun step(particles: ArrayList<ParticleInfo>, time: Float, timeIndex: Int, dt: Float){
        missingChildren += spawnRate[time] * dt
        while(missingChildren > 1f){
            particles += createParticle(timeIndex)
            missingChildren -= 1f
        }
        // todo collect global forces
        val globalForce = gddPosition[time]
        particles.forEach {
            it.apply {
                // todo collect local forces
                val oldState = it.states.last()
                val localForce = Vector3f()
                val force = globalForce + localForce
                val ddPosition = force / mass
                val dPosition = oldState.dPosition + ddPosition * dt
                val position = oldState.position + dPosition * dt
                val newState = ParticleState()
                newState.position = position
                newState.dPosition = dPosition
                it.states.add(newState)
            }
        }

    }

    override fun drawChildrenAutomatically() = !GFX.isFinalRendering && showChildren

    // todo visualize forces

    fun createParticle(timeIndex: Int): ParticleInfo {
        var randomIndex = random.nextFloat() * sumWeight
        var type = children.first()
        for(child in children){
            val cWeight = child.weight
            randomIndex -= cWeight
            if(randomIndex <= 0f){
                type = child
                break
            }
        }
        val particle = ParticleInfo(type, timeIndex, (lifeTime[timeIndex*timeStep]/timeStep).roundToInt(), 1f)
        particle.states.add(ParticleState())
        return particle
    }

    fun calculateMissingSteps(time: Float): Boolean {
        // cancel after 30ms = 30fps
        val maxTime = 30_000_000L
        val time0 = GFX.lastTime
        synchronized(this){
            while((nextIndex - 2) * timeStep < time){
                step(particles, nextIndex*timeStep, nextIndex, timeStep)
                nextIndex++
                val time1 = System.nanoTime()
                if(abs(time1-time0) > maxTime){
                    return false
                }
            }
        }
        return true
    }

    fun clearCache(){
        synchronized(this){
            nextIndex = 0
            particles.clear()
            random = Random(seed)
        }
    }

    override fun onDraw(stack: Matrix4fStack, time: Float, color: Vector4f) {

        sumWeight = children.sumByDouble { it.weight.toDouble() }.toFloat()
        if(time < 0f || children.isEmpty() || sumWeight <= 0.0) return

        // calculate the missing timesteps, if required
        if(!calculateMissingSteps(time)){
            if(GFX.isFinalRendering) throw MissingFrameException(File("particles.automatic"))
            return
        } // we need more time for the calculation


        // todo draw all particles at this point in time
        particles.forEach {
            it.apply {

                val opacity = it.getLifeOpacity(time, timeStep, fadingIn, fadingOut)
                if(opacity > 0f){// else not visible
                    stack.pushMatrix()
                    val index = time / timeStep - it.birthIndex
                    val index0 = index.toInt()
                    val indexF = index-index0

                    val position = getPosition(index0, indexF)
                    val rotation = getRotation(index0, indexF)

                    stack.translate(position)
                    stack.rotate(rotation)
                    stack.scale(childrenScale)

                    // todo interpolate position, rotation, and scale...
                    // todo is scale animated? should probably not be directly animated...
                    // todo normalize time?
                    val particleColor = Vector4f(color.x, color.y, color.z, color.w * opacity)
                    type.draw(stack, time - it.birthIndex, particleColor)

                    stack.popMatrix()
                }

            }
        }

    }

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += BooleanInput("Show Children", showChildren, style)
            .setChangeListener { showChildren = it }
            .setIsSelectedListener { show(null) }
    }

    override fun acceptsWeight() = true
    override fun getClassName() = "ParticleSystem"

}