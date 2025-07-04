package me.anno.bullet.constraints

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.Transform
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
import me.anno.ecs.systems.OnDrawGUI
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.ui.LineShapes
import me.anno.engine.ui.render.RenderState.cameraPosition
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

    var breakingImpulseThreshold: Double = 1e308
        set(value) {
            field = value
            bulletInstance?.breakingImpulseThreshold = value
        }

    @DebugAction
    fun invalidateConstraint() {
        invalidateRigidbody()
        other?.invalidatePhysics()
    }

    fun getTA(): Transform {
        val t = Transform()
        t.origin.set(selfPosition)
        t.basis.set(selfRotation)
        return t
    }

    fun getTB(): Transform {
        val t = Transform()
        t.origin.set(otherPosition)
        t.basis.set(otherRotation)
        return t
    }

    override fun onChangeStructure(entity: Entity) {
        super.onChangeStructure(entity)
        entity.invalidatePhysics()
    }

    abstract fun createConstraint(a: RigidBody, b: RigidBody, ta: Transform, tb: Transform): TypedConstraint

    override fun onDrawGUI(pipeline: Pipeline, all: Boolean) {
        val sideLength = 0.02 * (selfPosition.distance(cameraPosition) + otherPosition.distance(cameraPosition))
        LineShapes.drawPoint(entity, selfPosition, sideLength, constraintColor)
        LineShapes.drawLine(entity, zero, selfPosition, constraintColor)
        val other = other?.entity ?: entity
        LineShapes.drawPoint(other, otherPosition, sideLength, constraintColor)
        LineShapes.drawLine(other, zero, otherPosition, constraintColor)
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
        dst.breakingImpulseThreshold = breakingImpulseThreshold
    }

    companion object {
        private val zero = Vector3d()
        private val constraintColor = UIColors.dodgerBlue
    }
}