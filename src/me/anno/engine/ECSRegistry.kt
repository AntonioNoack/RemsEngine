package me.anno.engine

import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.ScriptComponent
import me.anno.ecs.components.anim.BoneByBoneAnimation
import me.anno.ecs.components.anim.ImportedAnimation
import me.anno.ecs.components.anim.Skeleton
import me.anno.ecs.components.camera.CameraComponent
import me.anno.ecs.components.collider.*
import me.anno.ecs.components.light.AmbientLight
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.light.SpotLight
import me.anno.ecs.components.mesh.*
import me.anno.ecs.components.physics.Rigidbody
import me.anno.ecs.components.physics.Vehicle
import me.anno.ecs.components.physics.VehicleWheel
import me.anno.ecs.components.test.RaycastTestComponent
import me.anno.ecs.components.test.TypeTestComponent
import me.anno.ecs.prefab.CAdd
import me.anno.ecs.prefab.CSet
import me.anno.ecs.prefab.ChangeHistory
import me.anno.ecs.prefab.Prefab
import me.anno.engine.physics.BulletPhysics
import me.anno.engine.scene.ScenePrefab
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.io.files.FileReference
import me.anno.io.utils.StringMap
import me.anno.mesh.assimp.Bone
import me.anno.studio.Build

object ECSRegistry {

    fun init() {

        FileReference.register(ScenePrefab)

        registerCustomClass(StringMap())

        registerCustomClass(Entity())
        registerCustomClass(Transform())

        registerCustomClass(CameraComponent())

        // meshes and rendering
        registerCustomClass(Mesh())
        registerCustomClass(Material())
        registerCustomClass(MeshComponent())
        registerCustomClass(MeshRenderer())
        registerCustomClass(AnimRenderer())

        // lights
        registerCustomClass(SpotLight())
        registerCustomClass(PointLight())
        registerCustomClass(DirectionalLight())
        registerCustomClass(AmbientLight())

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
        registerCustomClass(ImportedAnimation())
        registerCustomClass(BoneByBoneAnimation())

        // prefab system
        registerCustomClass(ChangeHistory())
        registerCustomClass(CAdd())
        registerCustomClass(CSet())
        registerCustomClass(Prefab())

        // project
        registerCustomClass(GameEngineProject())

        // physics
        registerCustomClass(BulletPhysics())
        registerCustomClass(Rigidbody())
        registerCustomClass(Vehicle())
        registerCustomClass(VehicleWheel())

        if (Build.isDebug) {
            // test classes
            registerCustomClass(TypeTestComponent())
            registerCustomClass(RaycastTestComponent())
        }
    }

}