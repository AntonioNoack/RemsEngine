package me.anno.engine

import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.anim.*
import me.anno.ecs.components.anim.graph.AnimController
import me.anno.ecs.components.audio.AudioComponent
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.camera.control.FirstPersonController
import me.anno.ecs.components.camera.control.OrbitControls
import me.anno.ecs.components.camera.control.ThirdPersonController
import me.anno.ecs.components.collider.*
import me.anno.ecs.components.light.*
import me.anno.ecs.components.mesh.*
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
import me.anno.io.ISaveable
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.io.SaveableArray
import me.anno.io.files.FileReference
import me.anno.io.files.Reference
import me.anno.io.utils.StringMap
import me.anno.ui.UIRegistry
import org.apache.logging.log4j.LogManager

object ECSRegistry {

    private val LOGGER = LogManager.getLogger(ECSRegistry::class)

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

        registerCustomClass(StringMap())
        registerCustomClass(SaveableArray())

        if ("Entity" !in ISaveable.objectTypeRegistry) initMeshes()

        registerCustomClass(Transform())
        registerCustomClass(LocalPlayer())
        registerCustomClass(RemotePlayer())

        // camera and effects
        registerCustomClass(Camera())
        registerCustomClass(OrbitControls())
        registerCustomClass(FirstPersonController())
        registerCustomClass(ThirdPersonController())

        // scripting
        try {
            val clazz = this::class.java.classLoader
                .loadClass("me.anno.lua.LuaRegistry")
            clazz.getMethod("init").invoke(null)
        } catch (e: ClassNotFoundException) {
            LOGGER.warn("Lua was not found", e)
        } catch (e: NoClassDefFoundError) {
            LOGGER.warn("Lua was not found", e)
        }

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

        // physics
        initIfAvailable("me.anno.bullet.PhysicsRegistry", "BulletPhysics")

        // box2d
        registerIfAvailable("me.anno.box2d.Box2dPhysics", "Box2d")
        registerIfAvailable("me.anno.box2d.Rigidbody2d", null)

        // utils
        // currently a small thing, hopefully will become important and huge <3
        registerCustomClass(TriTerrain())
        registerIfAvailable("me.anno.recast.NavMesh", "Recast")

        initIfAvailable("me.anno.sdf.SDFRegistry", "SDF")

        NodeLibrary.registerClasses()
    }

    fun initIfAvailable(clazzName: String, moduleName: String?) {
        try {
            val clazz = this::class.java.classLoader
                .loadClass(clazzName)
            clazz.getMethod("init").invoke(null)
        } catch (e: ClassNotFoundException) {
            warnIfUnavailable(moduleName)
        } catch (e: NoClassDefFoundError) {
            warnIfUnavailable(moduleName)
        }
    }

    fun registerIfAvailable(clazzName: String, moduleName: String?) {
        try {
            val clazz = this::class.java.classLoader.loadClass(clazzName)
            registerCustomClass(clazz.getConstructor().newInstance() as ISaveable)
        } catch (e: ClassNotFoundException) {
            warnIfUnavailable(moduleName)
        } catch (e: NoClassDefFoundError) {
            warnIfUnavailable(moduleName)
        }
    }

    private fun warnIfUnavailable(moduleName: String?) {
        if (moduleName != null) LOGGER.warn("$moduleName module was not found")
    }

    @JvmStatic
    fun initWithGFX(w: Int = 512, h: Int = w) {
        HiddenOpenGLContext.createOpenGL(w, h)
        ShaderLib.init()
        init()
    }
}