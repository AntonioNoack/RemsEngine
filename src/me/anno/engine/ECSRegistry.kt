package me.anno.engine

import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.ScriptComponent
import me.anno.ecs.components.anim.Skeleton
import me.anno.ecs.components.camera.CameraComponent
import me.anno.ecs.components.collider.*
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.mesh.*
import me.anno.ecs.components.physics.Rigidbody
import me.anno.ecs.components.test.TypeTestComponent
import me.anno.ecs.prefab.*
import me.anno.engine.scene.ScenePrefab
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.io.files.FileReference
import me.anno.mesh.assimp.Bone
import me.anno.studio.Build

object ECSRegistry {

    fun init() {

        FileReference.register(ScenePrefab)

        registerCustomClass(Entity())
        registerCustomClass(Transform())

        registerCustomClass(CameraComponent())

        // meshes and rendering
        registerCustomClass(Mesh())
        registerCustomClass(MeshComponent())
        registerCustomClass(MeshRenderer())
        registerCustomClass(AnimRenderer())

        // lights
        registerCustomClass(PointLight())

        // others
        registerCustomClass(ScriptComponent())

        // colliders
        registerCustomClass(BoxCollider())
        registerCustomClass(CapsuleCollider())
        registerCustomClass(ConeCollider())
        registerCustomClass(ConvexCollider())
        registerCustomClass(CylinderCollider())
        registerCustomClass(MeshCollider())
        registerCustomClass(SphereCollider())

        // skeletons and such
        registerCustomClass(MorphTarget())
        registerCustomClass(Skeleton())
        registerCustomClass(Bone())

        // prefab system
        registerCustomClass(ChangeHistory())
        registerCustomClass(ChangeSetEntityAttribute())
        registerCustomClass(ChangeSetComponentAttribute())
        registerCustomClass(ChangeAddEntity())
        registerCustomClass(ChangeAddComponent())

        registerCustomClass(EntityPrefab())

        // project
        registerCustomClass(GameEngineProject())

        // physics
        registerCustomClass(BulletPhysics())
        registerCustomClass(Rigidbody())

        if (Build.isDebug) {
            // test classes
            registerCustomClass(TypeTestComponent())
        }
    }

}