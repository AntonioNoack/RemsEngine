package me.anno.bullet.constraints

import com.bulletphysics.dynamics.RigidBody
import me.anno.bullet.bodies.PhysicalBody
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityPhysics.invalidatePhysics
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.DebugWarning
import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnChangeStructure
import me.anno.ecs.systems.OnDrawGUI
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.ui.LineShapes
import me.anno.engine.ui.render.RenderState.cameraPosition
import me.anno.gpu.pipeline.Pipeline
import me.anno.ui.UIColors
import me.anno.utils.Color.withAlpha
import me.anno.utils.pooling.JomlPools
import org.joml.Matrix4x3
import org.joml.Quaternionf
import org.joml.Vector3d

// constraints: https://download.autodesk.com/global/docs/maya2014/en_us/index.html?url=files/GUID-CDB3638D-23AF-49EF-8EF6-53081EE4D39D.htm,topicNumber=d30e571077
abstract class Constraint<TypedConstraint : com.bulletphysics.dynamics.constraintsolver.TypedConstraint> :
    Component(), OnChangeStructure, OnDrawGUI {

    override var isEnabled: Boolean
        get() = super.isEnabled
        set(value) {
            // todo is this working???
            if (super.isEnabled != value) {
                super.isEnabled = value
                invalidateConstraint()
            }
        }

    @Type("PhysicalBody/SameSceneRef")
    var other: PhysicalBody? = null
        set(value) {
            field?.activeConstraints?.remove(this)
            value?.activeConstraints?.add(this)
            field = value
        }

    @DebugProperty
    @NotSerializedProperty
    var bulletInstance: TypedConstraint? = null

    @DebugWarning
    @NotSerializedProperty
    @Suppress("unused")
    val missingOther
        get() = if (other == null) "True" else null

    @DebugWarning
    @NotSerializedProperty
    @Suppress("unused")
    val isMissingRigidbody
        get() = if (entity?.getComponent(PhysicalBody::class) == null) "" else null

    @DebugWarning
    @NotSerializedProperty
    @Suppress("unused")
    val otherIsRigidbody
        get() = if (entity?.getComponent(PhysicalBody::class)?.run { this == other } == true) "" else null

    var disableCollisionsBetweenLinked = true

    var selfPosition = Vector3d()
        set(value) {
            field.set(value)
            invalidateConstraint()
        }

    var selfRotation = Quaternionf()
        set(value) {
            field.set(value)
            invalidateConstraint()
        }

    var otherPosition = Vector3d()
        set(value) {
            field.set(value)
            invalidateConstraint()
        }

    var otherRotation = Quaternionf()
        set(value) {
            field.set(value)
            invalidateConstraint()
        }

    /**
     * When the impulse is this large, the constraint shall break (be removed, be it deleted or disabled)
     * */
    var breakingImpulse: Float = 1e38f

    @DebugAction
    fun invalidateConstraint() {
        invalidateRigidbody()
        other?.invalidatePhysics()
    }

    override fun onChangeStructure(entity: Entity) {
        entity.invalidatePhysics()
    }

    abstract fun createConstraint(a: RigidBody, b: RigidBody): TypedConstraint

    override fun onDrawGUI(pipeline: Pipeline, all: Boolean) {
        if (breakingImpulse < 0f) return // broken shall be hidden

        val color0 = constraintColor.withAlpha(127)
        val sideLength = 0.02 * (selfPosition.distance(cameraPosition) + otherPosition.distance(cameraPosition))
        LineShapes.drawPoint(entity, selfPosition, sideLength, color0)
        LineShapes.drawLine(entity, zero, selfPosition, color0)

        val other = other?.entity ?: entity
        LineShapes.drawPoint(other, otherPosition, sideLength, color0)
        LineShapes.drawLine(other, zero, otherPosition, color0)

        // draw line from one to the other
        val selfGlobal = JomlPools.vec3d.create().set(selfPosition)
        val otherGlobal = JomlPools.vec3d.create().set(otherPosition)

        entity?.transform?.globalTransform?.transformPosition(selfGlobal)
        other?.transform?.globalTransform?.transformPosition(otherGlobal)

        val color = constraintColor
        LineShapes.drawLine(null as Matrix4x3?, selfGlobal, otherGlobal, color)

        JomlPools.vec3d.sub(2)

    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is Constraint<*>) return
        dst.other = getInClone(other, dst)
        dst.selfPosition.set(selfPosition)
        dst.selfRotation.set(selfRotation)
        dst.otherPosition.set(otherPosition)
        dst.otherRotation.set(otherRotation)
        dst.disableCollisionsBetweenLinked = disableCollisionsBetweenLinked
        dst.breakingImpulse = breakingImpulse
    }

    companion object {
        private val zero = Vector3d()
        private val constraintColor = UIColors.dodgerBlue
    }
}