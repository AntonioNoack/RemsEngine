package me.anno.engine

import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.ScriptComponent
import me.anno.ecs.components.anim.Skeleton
import me.anno.ecs.components.camera.CameraComponent
import me.anno.ecs.components.collider.*
import me.anno.ecs.components.mesh.*
import me.anno.ecs.components.physics.Rigidbody
import me.anno.ecs.components.test.TypeTestComponent
import me.anno.ecs.prefab.PrefabComponent1
import me.anno.ecs.prefab.PrefabEntity1
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.mesh.assimp.Bone
import me.anno.studio.Build

object ECSRegistry {

    fun init() {

        registerCustomClass(Entity())
        registerCustomClass(Transform())

        registerCustomClass(CameraComponent())

        registerCustomClass(Mesh())
        registerCustomClass(MeshComponent())
        registerCustomClass(MeshRenderer())
        registerCustomClass(AnimRenderer())

        registerCustomClass(Rigidbody())

        registerCustomClass(ScriptComponent())

        registerCustomClass(BoxCollider())
        registerCustomClass(CapsuleCollider())
        registerCustomClass(ConeCollider())
        registerCustomClass(ConvexCollider())
        registerCustomClass(CylinderCollider())
        registerCustomClass(MeshCollider())
        registerCustomClass(SphereCollider())

        registerCustomClass(MorphTarget())
        registerCustomClass(Skeleton())
        registerCustomClass(Bone())

        if (Build.isDebug) {
            // test classes
            registerCustomClass(TypeTestComponent())
        }
    }

}