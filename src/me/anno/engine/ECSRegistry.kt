package me.anno.engine

import me.anno.config.DefaultConfig
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.ScriptComponent
import me.anno.ecs.components.anim.BoneByBoneAnimation
import me.anno.ecs.components.anim.ImportedAnimation
import me.anno.ecs.components.anim.Skeleton
import me.anno.ecs.components.camera.CameraComponent
import me.anno.ecs.components.collider.*
import me.anno.ecs.components.light.*
import me.anno.ecs.components.mesh.*
import me.anno.ecs.components.mesh.spline.PathProfile
import me.anno.ecs.components.mesh.spline.SplineControlPoint
import me.anno.ecs.components.mesh.spline.SplineMesh
import me.anno.ecs.components.mesh.terrain.TriTerrain
import me.anno.ecs.components.physics.BulletPhysics
import me.anno.ecs.components.physics.Rigidbody
import me.anno.ecs.components.physics.Vehicle
import me.anno.ecs.components.physics.VehicleWheel
import me.anno.ecs.components.physics.constraints.*
import me.anno.ecs.components.test.RaycastTestComponent
import me.anno.ecs.components.test.TestVehicleController
import me.anno.ecs.components.test.TypeTestComponent
import me.anno.ecs.components.ui.CanvasComponent
import me.anno.ecs.prefab.ChangeHistory
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Path
import me.anno.engine.scene.ScenePrefab
import me.anno.engine.ui.render.ECSShaderLib
import me.anno.gpu.ShaderLib
import me.anno.gpu.TextureLib
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.shader.BaseShader
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.io.SaveableArray
import me.anno.io.files.FileReference
import me.anno.io.utils.StringMap
import me.anno.mesh.assimp.Bone
import me.anno.Build
import me.anno.ui.base.Font
import me.anno.ui.base.IconPanel
import me.anno.ui.base.Panel
import me.anno.ui.base.SpacerPanel
import me.anno.ui.base.buttons.ImageButton
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.*
import me.anno.ui.base.scrolling.ScrollPanelX
import me.anno.ui.base.scrolling.ScrollPanelXY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.text.LinkPanel
import me.anno.ui.base.text.SimpleTextPanel
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.color.ColorChooser
import me.anno.ui.input.*

object ECSRegistry {

    private var hasBeenInited = false
    fun init() {

        if (hasBeenInited) return
        hasBeenInited = true

        FileReference.registerStatic(ScenePrefab)

        registerCustomClass(StringMap())
        registerCustomClass(SaveableArray())

        registerCustomClass(Entity())
        registerCustomClass(Transform())

        registerCustomClass(CameraComponent())

        // ui base
        registerCustomClass(CanvasComponent())

        // ui containers
        val style = DefaultConfig.style
        registerCustomClass { Font() }
        registerCustomClass { Padding() }
        registerCustomClass { Panel(style) }
        registerCustomClass { PanelListX(style) }
        registerCustomClass { PanelListY(style) }
        registerCustomClass { PanelStack(style) }
        registerCustomClass { ScrollPanelX(style) }
        registerCustomClass { ScrollPanelY(style) }
        registerCustomClass { ScrollPanelXY(style) }
        registerCustomClass { NineTilePanel(style) }
        registerCustomClass { TitledListY(style) }

        // ui content
        registerCustomClass { TextPanel(style) }
        registerCustomClass { LinkPanel(style) }
        registerCustomClass { SimpleTextPanel(style) }
        registerCustomClass { IconPanel(style) }
        registerCustomClass { TextButton(style) }
        registerCustomClass { ImageButton(style) }
        registerCustomClass { SpacerPanel(style) }
        registerCustomClass { ColorChooser(style) }
        registerCustomClass { BooleanInput(style) }
        registerCustomClass { ColorInput(style) }
        registerCustomClass { FloatInput(style) }
        registerCustomClass { IntInput(style) }
        registerCustomClass { TextInput(style) }
        registerCustomClass { TextInputML(style) }
        registerCustomClass { FloatVectorInput(style) }
        registerCustomClass { IntVectorInput(style) }
        // not finished:
        // registerCustomClass { ConsoleInput(style) }

        // meshes and rendering
        registerCustomClass(Mesh())
        registerCustomClass(Material())
        registerCustomClass(MeshComponent())
        registerCustomClass(AnimRenderer())
        registerCustomClass(SplineMesh())
        registerCustomClass(SplineControlPoint())
        registerCustomClass(PathProfile())

        // lights
        registerCustomClass(SpotLight())
        registerCustomClass(PointLight())
        registerCustomClass(DirectionalLight())
        registerCustomClass(AmbientLight())
        registerCustomClass(EnvironmentMap())
        registerCustomClass(PlanarReflection())

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
        registerCustomClass(Path())
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

        // todo test scene for all these constraints
        // todo drag on physics to add forces/impulses
        // physics constraints
        registerCustomClass(PointConstraint())
        registerCustomClass(GenericConstraint())
        registerCustomClass(ConeTwistConstraint())
        registerCustomClass(HingeConstraint())
        registerCustomClass(SliderConstraint())

        // utils
        // currently a small thing, hopefully will become important and huge <3
        registerCustomClass(TriTerrain())
        registerCustomClass(ManualProceduralMesh())

        if (Build.isDebug) {
            // test classes
            registerCustomClass(TypeTestComponent())
            registerCustomClass(RaycastTestComponent())
            registerCustomClass(TestVehicleController())
        }
    }

    fun initWithGFX(w: Int, h: Int = w) {
        HiddenOpenGLContext.createOpenGL(w, h)
        ShaderLib.init()
        TextureLib.init()
        ECSShaderLib.init()
        init()
    }

    fun initNoGFX() {
        ECSShaderLib.pbrModelShader = BaseShader()
        init()
    }

}