package me.anno.bullet.bodies

/**
 * Physics object, that should be controlled exclusively by scripts.
 * Any implementation should implement OnPhysicsUpdate to do these changes.
 *
 * A KinematicBody that doesn't move should just be replaced with a Rigidbody.
 * */
class KinematicBody : PhysicalBody()