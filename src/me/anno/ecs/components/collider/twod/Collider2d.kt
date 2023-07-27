package me.anno.ecs.components.collider.twod

import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import org.jbox2d.dynamics.Fixture

// todo remove box2d Fixture dependency... how can we move it? :/

// todo colliders by meshes in 2d,
// todo colliders by points in 2d
// todo colliders by images?

abstract class Collider2d : Collider() {

    @NotSerializedProperty
    var box2dInstance: Fixture? = null

    @DebugProperty
    val box2d get() = box2dInstance?.hashCode()

    @Range(0.0, 1e100)
    @SerializedProperty
    var density = 0f
        set(value) {
            // could we update this at runtime? would need to update mass & inertia
            if (field != value) {
                field = value
                box2dInstance?.density = value
                // to do check if this works
                // invalidateRigidbody()
            }
        }

    @Range(0.0, 1.0)
    @SerializedProperty
    var friction = 0.2f
        set(value) {
            field = value
            box2dInstance?.friction = value
        }

    @Range(0.0, 1.0)
    @SerializedProperty
    var restitution = 0f
        set(value) {
            field = value
            box2dInstance?.restitution = restitution
        }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as Collider2d
        dst.density = density
        dst.friction = friction
        dst.restitution = restitution
    }

}