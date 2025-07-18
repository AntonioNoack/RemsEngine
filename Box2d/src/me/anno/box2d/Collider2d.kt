package me.anno.box2d

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import org.jbox2d.dynamics.Fixture

// todo colliders by meshes in 2d,
// todo colliders by points in 2d
// todo colliders by images?

abstract class Collider2d : Collider() {

    @NotSerializedProperty
    var nativeInstance: Fixture? = null

    @Range(0.0, 1e38)
    @SerializedProperty
    var density = 1f
        set(value) {
            // could we update this at runtime? would need to update mass & inertia
            if (field != value) {
                field = value
                nativeInstance?.density = value
                invalidateRigidbody()
            }
        }

    @Range(0.0, 1.0)
    @SerializedProperty
    var friction = 0.2f
        set(value) {
            field = value
            nativeInstance?.friction = value
        }

    @Range(0.0, 1.0)
    @SerializedProperty
    var restitution = 0f
        set(value) {
            field = value
            nativeInstance?.restitution = restitution
        }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is Collider2d) return
        dst.density = density
        dst.friction = friction
        dst.restitution = restitution
    }
}