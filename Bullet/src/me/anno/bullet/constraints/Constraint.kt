package me.anno.bullet.constraints

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.Transform
import me.anno.bullet.BulletPhysics.Companion.castB
import me.anno.bullet.Rigidbody
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityPhysics.invalidateRigidbody
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.DebugWarning
import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnDrawGUI
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.ui.LineShapes
import me.anno.gpu.pipeline.Pipeline
import me.anno.ui.UIColors
import org.joml.Quaterniond
import org.joml.Vector3d

// constraints: https://download.autodesk.com/global/docs/maya2014/en_us/index.html?url=files/GUID-CDB3638D-23AF-49EF-8EF6-53081EE4D39D.htm,topicNumber=d30e571077
abstract class Constraint<TypedConstraint : com.bulletphysics.dynamics.constraintsolver.TypedConstraint> :
    Component(), OnDrawGUI {

    override var isEnabled: Boolean
        get() = super.isEnabled
        set(value) {
            // todo is this working???
            if (super.isEnabled != value) {
                super.isEnabled = value
                invalidateConstraint()
            }
        }

    @Type("Rigidbody/SameSceneRef")
    var other: Rigidbody? = null
        set(value) {
            if (field != value) {
                value?.linkedConstraints?.add(this)
                field?.linkedConstraints?.remove(this)
                field = value
            }
        }

    @DebugProperty
    @NotSerializedProperty
    var bulletInstance: TypedConstraint? = null

    @DebugWarning
    @NotSerializedProperty
    val missingOther
        get() = if (other == null) "True" else null

    @DebugWarning
    @NotSerializedProperty
    val isMissingRigidbody
        get() = if (entity?.getComponent(Rigidbody::class) == null) "" else null

    @DebugWarning
    @NotSerializedProperty
    val otherIsRigidbody
        get() = if (entity?.getComponent(Rigidbody::class)?.run { this == other } == true) "" else null

    var disableCollisionsBetweenLinked = true

    var selfPosition = Vector3d()
        set(value) {
            field.set(value)
            invalidateConstraint()
        }

    var selfRotation = Quaterniond()
        set(value) {
            field.set(value)
            invalidateConstraint()
        }

    var otherPosition = Vector3d()
        set(value) {
            field.set(value)
            invalidateConstraint()
        }

    var otherRotation = Quaterniond()
        set(value) {
            field.set(value)
            invalidateConstraint()
        }

    @DebugAction
    fun invalidateConstraint() {
        invalidateRigidbody()
        other?.invalidatePhysics()
    }

    fun getTA(): Transform {
        val t = Transform()
        t.origin.set(castB(selfPosition))
        t.basis.set(castB(selfRotation))
        return t
    }

    fun getTB(): Transform {
        val t = Transform()
        t.origin.set(castB(otherPosition))
        t.basis.set(castB(otherRotation))
        return t
    }

    override fun onChangeStructure(entity: Entity) {
        super.onChangeStructure(entity)
        entity.invalidateRigidbody()
    }

    abstract fun createConstraint(a: RigidBody, b: RigidBody, ta: Transform, tb: Transform): TypedConstraint

    override fun onDrawGUI(pipeline: Pipeline, all: Boolean) {
        LineShapes.drawPoint(entity, selfPosition, 1.0, constraintColor)
        LineShapes.drawLine(entity, zero, selfPosition, constraintColor)
        val other = other?.entity ?: entity
        LineShapes.drawPoint(other, otherPosition, 1.0, constraintColor)
        LineShapes.drawLine(other, zero, otherPosition, constraintColor)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as Constraint<*>
        dst.other = getInClone(other, dst)
        dst.selfPosition.set(selfPosition)
        dst.selfRotation.set(selfRotation)
        dst.otherPosition.set(otherPosition)
        dst.otherRotation.set(otherRotation)
        dst.disableCollisionsBetweenLinked = disableCollisionsBetweenLinked
    }

    companion object {
        private val zero = Vector3d()
        private val constraintColor = UIColors.dodgerBlue
    }
}