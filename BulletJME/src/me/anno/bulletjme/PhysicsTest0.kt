package me.anno.bulletjme

import com.jme3.bullet.PhysicsSpace
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Vector3f
import me.anno.bulletjme.Rigidbody.Companion.v

fun main() {
    BulletJMEMod.Companion // load library
    val physics = PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT)
    val collider = BoxCollisionShape(1f)
    val body = PhysicsRigidBody(collider, 1f)
    physics.addCollisionObject(body)
    for (i in 0 until 10) {
        physics.update(1f)
        println(body.getPhysicsLocation(Vector3f()))
    }
}