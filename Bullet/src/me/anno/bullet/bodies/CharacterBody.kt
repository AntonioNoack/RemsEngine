package me.anno.bullet.bodies

import com.bulletphysics.dynamics.character.KinematicCharacterController
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.components.collider.Axis
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.utils.types.Floats.toRadians

/**
 * GhostBody, for implementing player controls.
 * Any interactions with dynamic physics objects must be implemented manually.
 * */
class CharacterBody : GhostBody() {

    @SerializedProperty
    var stepHeight = 0.5

    @SerializedProperty
    var upAxis = Axis.Y

    @SerializedProperty
    var maxSlopeDegrees = 30.0

    /**
     * Terminal velocity of a skydiver in m/s.
     * */
    @SerializedProperty
    var fallSpeed = 55.0

    @SerializedProperty
    var jumpSpeed = 10.0

    @SerializedProperty
    var gravity = 9.81

    @DebugProperty
    @NotSerializedProperty
    var nativeInstance2: KinematicCharacterController? = null

    @DebugProperty
    @NotSerializedProperty
    val wasOnGround get() = nativeInstance2?.wasOnGround ?: true

    @DebugProperty
    @NotSerializedProperty
    val touchingContact get() = nativeInstance2?.touchingContact ?: true

    @DebugProperty
    @NotSerializedProperty
    val touchingNormal get() = nativeInstance2?.touchingNormal

    @DebugAction
    fun jump() {
        nativeInstance2?.jump()
    }

    @DebugProperty
    val verticalVelocity get() = nativeInstance2?.verticalVelocity ?: 0.0

    @DebugProperty
    val verticalOffset get() = nativeInstance2?.verticalOffset ?: 0.0

    fun initialize(controller: KinematicCharacterController) {
        controller.maxSlopeRadians = maxSlopeDegrees.toRadians()
    }
}