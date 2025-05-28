package me.anno.games.simslike

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.components.anim.AnimMeshComponent
import me.anno.ecs.systems.OnUpdate
import me.anno.maths.Maths.clamp
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.max

class Sim : Component(), OnUpdate {

    val actions = ArrayList<SimAction>()

    var male = false
    var age = 18

    override fun priority(): Int = 110 // >100 = shall run after AnimComponent

    override fun onUpdate() {
        val action = actions.firstOrNull() ?: return
        if (action.state == ActionState.READY) {
            val nav = getComponent(SimNavAgent::class, true) ?: return
            val target = calculateActionTarget(action) ?: return
            nav.isEnabled = true
            nav.target.set(target)
            action.state = ActionState.TRAVERSAL
            action.startTime = Time.gameTimeN
        }

        if (action.state == ActionState.TRAVERSAL) {
            // todo what it the agent is stuck?
            //  wait a certain time, then cancel, if nothing could be found
            updateMoveAnimState(action)
            if (updateMoveTransform(action)) {
                setIdleAnimState()
                stopNavigation()
                action.state = ActionState.EXECUTION
                action.startTime = Time.gameTimeN
            }
        }

        if (action.state == ActionState.EXECUTION) {
            if (action.execute(this)) {
                actions.removeFirst()
            }
        }

        if (action.state == ActionState.CANCEL) {
            setIdleAnimState()
            stopNavigation()
            actions.removeFirst()
        }

        // todo regularly check if this Sim should do important things, like eating, sleeping, dying, ...
    }

    fun calculateActionTarget(action: SimAction): Vector3f? {
        return when {
            action.isSimTarget -> {
                val transform = action.transform ?: return null
                val globalPos = transform.getGlobalPosition(Vector3d())
                Vector3f(0f, 0f, 1f)
                    .rotateY(action.angle)
                    .add(globalPos.x.toFloat(), globalPos.y.toFloat(), globalPos.z.toFloat())
            }
            action.isObjectTarget -> {
                val transform = action.transform ?: return null
                val globalPos = transform.getGlobalPosition(Vector3d())
                val angleY = transform.getGlobalRotation(Quaternionf())
                    .getEulerAngleYXZvY()
                Vector3f(action.offset)
                    .rotateY(angleY) // rotate offset by object rotation
                    .add(globalPos.x.toFloat(), globalPos.y.toFloat(), globalPos.z.toFloat())
            }
            else -> action.offset
        }
    }

    val animation get() = getComponent(AnimMeshComponent::class)
    val navigation get() = getComponent(SimNavAgent::class, true)

    fun stopNavigation() {
        navigation?.isEnabled = false
    }

    fun updateMoveTransform(action: SimAction): Boolean {

        val transform = transform ?: return false
        val nav0 = navigation ?: return false
        val nav1 = nav0.crowdAgent ?: return false

        // todo depends on terrain, too, sometimes we even have to climb or swim
        nav1.params.maxSpeed = if (action.supportRunning) 3f else 1.5f

        transform.setLocalPosition(nav1.currentPosition)

        val isFinished = nav1.getDistanceToGoal() < 0.1f
        if (!isFinished) {
            transform.localRotation = transform.localRotation
                .rotationY(nav1.actualVelocity.angleY())
        }

        invalidateAABB()

        return isFinished
    }

    fun updateMoveAnimState(action: SimAction) {
        val anim = animation ?: return
        val nav = navigation?.crowdAgent
        val maxSpeed = if (action.supportRunning) 2f else 1f
        val speed = if (nav != null) clamp(nav.actualVelocity.lengthXZ(), 0f, maxSpeed) else 0f
        // update animation state/blending
        anim.animations[0].weight = weight(speed)
        anim.animations[1].weight = weight(speed - 1f)
        anim.animations[2].weight = weight(speed - 2f)
    }

    fun setIdleAnimState() {
        val anim = animation ?: return
        // update animation state/blending
        anim.animations[0].weight = 1f
        anim.animations[1].weight = 0f
        anim.animations[2].weight = 0f
    }

    companion object {
        fun weight(v: Float): Float {
            return max(1f - abs(v), 0f)
        }
    }
}