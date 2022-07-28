package me.anno.engine

import me.anno.Build
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.anim.*
import me.anno.ecs.components.anim.graph.AnimController
import me.anno.ecs.components.anim.graph.AnimStateNode
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.camera.control.FirstPersonController
import me.anno.ecs.components.camera.control.OrbitControls
import me.anno.ecs.components.camera.control.ThirdPersonController
import me.anno.ecs.components.camera.effects.BloomEffect
import me.anno.ecs.components.camera.effects.OutlineEffect
import me.anno.ecs.components.camera.effects.SSAOEffect
import me.anno.ecs.components.camera.effects.SSREffect
import me.anno.ecs.components.collider.*
import me.anno.ecs.components.collider.twod.CircleCollider
import me.anno.ecs.components.collider.twod.RectCollider
import me.anno.ecs.components.light.*
import me.anno.ecs.components.mesh.*
import me.anno.ecs.components.mesh.sdf.SDFRegistry
import me.anno.ecs.components.mesh.spline.PathProfile
import me.anno.ecs.components.mesh.spline.SplineControlPoint
import me.anno.ecs.components.mesh.spline.SplineCrossing
import me.anno.ecs.components.mesh.spline.SplineMesh
import me.anno.ecs.components.mesh.terrain.TriTerrain
import me.anno.ecs.components.physics.BulletPhysics
import me.anno.ecs.components.physics.Rigidbody
import me.anno.ecs.components.physics.Vehicle
import me.anno.ecs.components.physics.VehicleWheel
import me.anno.ecs.components.physics.constraints.*
import me.anno.ecs.components.physics.twod.Box2dPhysics
import me.anno.ecs.components.physics.twod.Rigidbody2d
import me.anno.ecs.components.player.LocalPlayer
import me.anno.ecs.components.player.RemotePlayer
import me.anno.ecs.components.script.QuickInputScriptComponent
import me.anno.ecs.components.script.QuickScriptComponent
import me.anno.ecs.components.script.ScriptComponent
import me.anno.ecs.components.shaders.CuboidMesh
import me.anno.ecs.components.shaders.SkyBox
import me.anno.ecs.components.shaders.TriplanarMaterial
import me.anno.ecs.components.test.RaycastTestComponent
import me.anno.ecs.components.test.TestVehicleController
import me.anno.ecs.components.test.TypeTestComponent
import me.anno.ecs.prefab.ChangeHistory
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Path
import me.anno.engine.scene.ScenePrefab
import me.anno.engine.ui.render.ECSShaderLib
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.ShaderLib
import me.anno.graph.types.NodeLibrary
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.io.SaveableArray
import me.anno.io.files.FileReference
import me.anno.io.utils.StringMap
import me.anno.mesh.assimp.Bone
import me.anno.ui.UIRegistry
import me.anno.utils.LOGGER

object ECSRegistry {

    var hasBeenInited = false
    fun init() {

        if (hasBeenInited) return
        hasBeenInited = true

        FileReference.registerStatic(ScenePrefab)

        registerCustomClass(StringMap())
        registerCustomClass(SaveableArray())

        registerCustomClass(Entity())
        registerCustomClass(Transform())
        registerCustomClass(LocalPlayer())
        registerCustomClass(RemotePlayer())

        // camera and effects
        registerCustomClass(Camera())
        registerCustomClass(OrbitControls())
        registerCustomClass(FirstPersonController())
        registerCustomClass(ThirdPersonController())
        registerCustomClass(BloomEffect())
        registerCustomClass(SSAOEffect())
        registerCustomClass(SSREffect())
        registerCustomClass(OutlineEffect())

        // scripting
        registerCustomClass(ScriptComponent())
        registerCustomClass(QuickScriptComponent())
        registerCustomClass(QuickInputScriptComponent())

        // ui, could be skipped for headless servers
        UIRegistry.init()

        // meshes and rendering
        registerCustomClass(Mesh())
        registerCustomClass(Material())
        registerCustomClass(MeshComponent())
        registerCustomClass(LODMeshComponent())
        registerCustomClass(SplineMesh())
        registerCustomClass(SplineCrossing())
        registerCustomClass(SplineControlPoint())
        registerCustomClass(PathProfile())
        registerCustomClass(CuboidMesh())
        registerCustomClass(TriplanarMaterial())

        // animated meshes
        AnimStateNode.register()
        registerCustomClass(AnimRenderer())
        registerCustomClass(AnimationState())
        registerCustomClass(AnimController())

        // lights
        registerCustomClass(SpotLight())
        registerCustomClass(PointLight())
        registerCustomClass(DirectionalLight())
        registerCustomClass(AmbientLight())
        registerCustomClass(EnvironmentMap())
        registerCustomClass(PlanarReflection())

        registerCustomClass(SkyBox())

        // 3d colliders
        registerCustomClass(BoxCollider())
        registerCustomClass(CapsuleCollider())
        registerCustomClass(ConeCollider())
        registerCustomClass(ConvexCollider())
        registerCustomClass(CylinderCollider())
        registerCustomClass(MeshCollider())
        registerCustomClass(SphereCollider())

        // 2d colliders
        registerCustomClass(RectCollider())
        registerCustomClass(CircleCollider())

        // skeletons and such
        registerCustomClass(MorphTarget())
        registerCustomClass(Skeleton())
        registerCustomClass(Bone())
        registerCustomClass(ImportedAnimation())
        registerCustomClass(BoneByBoneAnimation())

        // prefab system
        registerCustomClass(Path.ROOT_PATH)
        registerCustomClass(ChangeHistory())
        registerCustomClass(CAdd())
        registerCustomClass(CSet())
        registerCustomClass(Prefab())

        // project
        registerCustomClass(GameEngineProject())

        // physics
        try {
            // todo try to create an export without physics, and check everything still runs fine
            registerCustomClass(BulletPhysics())
            registerCustomClass(Rigidbody())
            registerCustomClass(Vehicle())
            registerCustomClass(VehicleWheel())

            // todo test scene for all these constraints
            // todo drag on physics to add forces/impulses
            // physics constraints
            registerCustomClass(PointConstraint())
            registerCustomClass(GenericConstraint())
            registerCustomClass(ConeTwistConstraint())
            registerCustomClass(HingeConstraint())
            registerCustomClass(SliderConstraint())
        } catch (e: ClassNotFoundException) {
            LOGGER.warn("Bullet was not found")
        }

        try {
            registerCustomClass(Box2dPhysics())
            registerCustomClass(Rigidbody2d())
        } catch (e: ClassNotFoundException) {
            LOGGER.warn("Box2d was not found")
        }

        // utils
        // currently a small thing, hopefully will become important and huge <3
        registerCustomClass(TriTerrain())
        registerCustomClass(ManualProceduralMesh())

        SDFRegistry.init()
        NodeLibrary.init()

        if (Build.isDebug) {
            // test classes
            registerCustomClass(TypeTestComponent())
            registerCustomClass(RaycastTestComponent())
            registerCustomClass(TestVehicleController())
        }
    }

    fun initWithGFX(w: Int = 512, h: Int = w) {
        HiddenOpenGLContext.createOpenGL(w, h)
        ShaderLib.init()
        ECSShaderLib.init()
        init()
    }

}