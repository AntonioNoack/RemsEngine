package me.anno.box2d

import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.components.collider.CollisionFilters.ANY_DYNAMIC_MASK
import me.anno.ecs.components.collider.CollisionFilters.GHOST_GROUP_ID
import me.anno.utils.structures.lists.SimpleList

class GhostBody2d : PhysicsBody2d() {

    init {
        collisionGroup = GHOST_GROUP_ID
        collisionMask = ANY_DYNAMIC_MASK
    }

    @DebugProperty
    val numOverlaps: Int
        get() = overlappingBodies.size

    // todo is this correct??? we might have multiple contacts per other body, and idk if all are registered there...
    @Docs("Overlapping Dynamic/KinematicBodies; only safe to access onPhysicsUpdate")
    val overlappingBodies: List<PhysicalBody2d> = OverlappingBodiesList(this)

    private class OverlappingBodiesList(private val self: GhostBody2d) :
        SimpleList<PhysicalBody2d>() {

        override fun get(index: Int): PhysicalBody2d {
            var next = self.nativeInstance!!.contactList!!
            repeat(index) {
                next = next.next!!
            }
            return next.other.userData as PhysicalBody2d
        }

        override val size: Int
            get() {
                var next = self.nativeInstance?.contactList
                var size = 0
                while (next != null) {
                    size++
                    next = next.next
                }
                return size
            }

        override fun iterator(): Iterator<PhysicalBody2d> {
            return object : Iterator<PhysicalBody2d> {

                var nextContact = self.nativeInstance?.contactList

                override fun hasNext(): Boolean = nextContact != null
                override fun next(): PhysicalBody2d {
                    val thisContact = nextContact!!
                    val body = thisContact.other.userData as PhysicalBody2d
                    nextContact = thisContact.next
                    return body
                }
            }
        }
    }
}