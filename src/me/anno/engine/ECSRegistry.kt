package me.anno.engine

import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.ScriptComponent
import me.anno.ecs.components.camera.CameraComponent
import me.anno.ecs.components.collider.*
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshRenderer
import me.anno.ecs.components.physics.Rigidbody
import me.anno.ecs.components.test.TypeTestComponent
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.studio.Build

object ECSRegistry {

    fun init() {
        registerCustomClass(Entity())
        registerCustomClass(CameraComponent())
        registerCustomClass(MeshComponent())
        registerCustomClass(MeshRenderer())
        registerCustomClass(Rigidbody())
        registerCustomClass(ScriptComponent())
        registerCustomClass(BoxCollider())
        registerCustomClass(CapsuleCollider())
        registerCustomClass(ConeCollider())
        registerCustomClass(ConvexCollider())
        registerCustomClass(CylinderCollider())
        registerCustomClass(MeshCollider())
        registerCustomClass(SphereCollider())
        registerCustomClass(ECSWorld())
        registerCustomClass(Transform())
        if (Build.isDebug) {
            // test classes
            registerCustomClass(TypeTestComponent())
        }
    }

}