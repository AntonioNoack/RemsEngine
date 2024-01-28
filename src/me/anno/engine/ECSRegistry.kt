package me.anno.engine

import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.anim.AnimMeshComponent
import me.anno.ecs.components.anim.AnimationState
import me.anno.ecs.components.anim.Bone
import me.anno.ecs.components.anim.BoneAttachmentComponent
import me.anno.ecs.components.anim.BoneByBoneAnimation
import me.anno.ecs.components.anim.ImportedAnimation
import me.anno.ecs.components.anim.Retargeting
import me.anno.ecs.components.anim.Skeleton
import me.anno.ecs.components.anim.graph.AnimController
import me.anno.ecs.components.audio.AudioComponent
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.camera.control.FirstPersonController
import me.anno.ecs.components.camera.control.OrbitControls
import me.anno.ecs.components.camera.control.ThirdPersonController
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.collider.CapsuleCollider
import me.anno.ecs.components.collider.ConeCollider
import me.anno.ecs.components.collider.ConvexCollider
import me.anno.ecs.components.collider.CylinderCollider
import me.anno.ecs.components.collider.MeshCollider
import me.anno.ecs.components.collider.SphereCollider
import me.anno.ecs.components.light.CircleLight
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.EnvironmentMap
import me.anno.ecs.components.light.PlanarReflection
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.light.RectangleLight
import me.anno.ecs.components.light.SpotLight
import me.anno.ecs.components.mesh.BillboardTransformer
import me.anno.ecs.components.mesh.ImagePlane
import me.anno.ecs.components.mesh.LODMeshComponent
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MorphTarget
import me.anno.ecs.components.mesh.decal.DecalMaterial
import me.anno.ecs.components.mesh.decal.DecalMeshComponent
import me.anno.ecs.components.mesh.spline.PathProfile
import me.anno.ecs.components.mesh.spline.SplineControlPoint
import me.anno.ecs.components.mesh.spline.SplineCrossing
import me.anno.ecs.components.mesh.spline.SplineMesh
import me.anno.ecs.components.mesh.terrain.TriTerrain
import me.anno.ecs.components.player.LocalPlayer
import me.anno.ecs.components.player.RemotePlayer
import me.anno.ecs.components.shaders.AutoTileableMaterial
import me.anno.ecs.components.shaders.CuboidMesh
import me.anno.ecs.components.shaders.Skybox
import me.anno.ecs.components.shaders.TriplanarMaterial
import me.anno.ecs.components.text.SDFTextComponent
import me.anno.ecs.components.text.TextMeshComponent
import me.anno.ecs.components.text.TextTextureComponent
import me.anno.ecs.prefab.ChangeHistory
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Path
import me.anno.engine.projects.GameEngineProject
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.shader.ShaderLib
import me.anno.graph.types.NodeLibrary
import me.anno.io.Saveable
import me.anno.io.Saveable.Companion.registerCustomClass
import me.anno.io.SaveableArray
import me.anno.io.files.Reference
import me.anno.io.utils.StringMap
import me.anno.ui.UIRegistry

object ECSRegistry {

    @JvmField
    var hasBeenInited = false

    @JvmStatic
    fun initMeshes() {
        registerCustomClass(Entity())
        registerCustomClass(Mesh())
        registerCustomClass(Material())
        registerCustomClass(MeshComponent())
        registerCustomClass(AnimMeshComponent())
        registerCustomClass(AnimationState())
        registerCustomClass(ImportedAnimation())
        registerCustomClass(BoneByBoneAnimation())
        registerCustomClass(Skeleton())
        registerCustomClass(Bone())
        registerCustomClass(Retargeting())
        registerCustomClass(MorphTarget())
        registerCustomClass(BoneAttachmentComponent())
    }

    @JvmStatic
    fun initPrefabs() {
        registerCustomClass(Path.ROOT_PATH)
        registerCustomClass(ChangeHistory())
        registerCustomClass(CAdd())
        registerCustomClass(CSet())
        registerCustomClass(Prefab())
    }

    @JvmStatic
    fun initLights() {
        registerCustomClass(SpotLight())
        registerCustomClass(PointLight())
        registerCustomClass(DirectionalLight())
        registerCustomClass(EnvironmentMap())
        registerCustomClass(PlanarReflection())
        registerCustomClass(CircleLight())
        registerCustomClass(RectangleLight())
        registerCustomClass(Skybox())
    }

    @JvmStatic
    fun init3dColliders() {
        registerCustomClass(BoxCollider())
        registerCustomClass(CapsuleCollider())
        registerCustomClass(ConeCollider())
        registerCustomClass(ConvexCollider())
        registerCustomClass(CylinderCollider())
        registerCustomClass(MeshCollider())
        registerCustomClass(SphereCollider())
    }

    @JvmStatic
    fun initTextRenderers() {
        registerCustomClass(TextMeshComponent())
        registerCustomClass(TextTextureComponent())
        registerCustomClass(SDFTextComponent())
    }

    @JvmStatic
    fun init() {

        if (hasBeenInited) return
        hasBeenInited = true

        Reference.registerStatic(ScenePrefab)
        DefaultAssets.init()

        registerCustomClass(StringMap())
        registerCustomClass(SaveableArray())

        if ("Entity" !in Saveable.objectTypeRegistry) initMeshes()

        registerCustomClass(Transform())
        registerCustomClass(LocalPlayer())
        registerCustomClass(RemotePlayer())

        // camera and effects
        registerCustomClass(Camera())
        registerCustomClass(OrbitControls())
        registerCustomClass(FirstPersonController())
        registerCustomClass(ThirdPersonController())

        // ui, could be skipped for headless servers
        UIRegistry.init()

        // meshes and rendering
        registerCustomClass(LODMeshComponent())
        registerCustomClass(SplineMesh())
        registerCustomClass(SplineCrossing())
        registerCustomClass(SplineControlPoint())
        registerCustomClass(PathProfile())
        registerCustomClass(CuboidMesh())
        registerCustomClass(TriplanarMaterial())
        registerCustomClass(AutoTileableMaterial())
        registerCustomClass(BillboardTransformer())
        registerCustomClass(ImagePlane())
        registerCustomClass(DecalMeshComponent())
        registerCustomClass(DecalMaterial())
        initTextRenderers()

        // animated meshes
        registerCustomClass(AnimController())

        initLights()

        // audio
        registerCustomClass(AudioComponent())

        // colliders
        init3dColliders()

        // prefab system
        initPrefabs()

        // project
        registerCustomClass(GameEngineProject())

        // utils
        // currently a small thing, hopefully will become important and huge <3
        registerCustomClass(TriTerrain())

        NodeLibrary.registerClasses()
    }

    @JvmStatic
    fun initWithGFX(w: Int = 512, h: Int = w) {
        HiddenOpenGLContext.createOpenGL(w, h)
        ShaderLib.init()
        init()
    }
}