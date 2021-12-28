package me.anno.engine.ui.scenetabs

import me.anno.config.DefaultConfig
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.light.LightComponentBase
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshBaseComponent
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.RenderView
import me.anno.input.MouseButton
import me.anno.io.files.FileReference
import me.anno.ui.base.text.TextPanel
import me.anno.utils.hpc.SyncMaster
import me.anno.utils.maths.Maths.length
import me.anno.utils.types.AABBs.avgX
import me.anno.utils.types.AABBs.avgY
import me.anno.utils.types.AABBs.avgZ
import me.anno.utils.types.AABBs.deltaX
import me.anno.utils.types.AABBs.deltaY
import me.anno.utils.types.AABBs.deltaZ
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Matrix4x3d
import org.joml.Vector3d

class ECSSceneTab(
    val syncMaster: SyncMaster,
    val inspector: PrefabInspector,
    val file: FileReference
) : TextPanel(inspector.reference.nameWithoutExtension, DefaultConfig.style) {

    constructor(
        syncMaster: SyncMaster,
        fileReference: FileReference,
        classNameIfNull: String
    ) : this(
        syncMaster,
        PrefabInspector(fileReference, classNameIfNull)
    )

    constructor(syncMaster: SyncMaster, prefab: Prefab) :
            this(syncMaster, PrefabInspector(prefab))

    constructor(syncMaster: SyncMaster, inspector: PrefabInspector) :
            this(syncMaster, inspector, inspector.reference)

    init {
        LOGGER.info("Created tab with ${inspector.prefab.countTotalChanges(true)}+ changes")
    }

    // different tabs have different "cameras"
    var radius = 50.0
    var position = Vector3d()
    var rotation = Vector3d(-20.0, 0.0, 0.0)

    var isFirstTime = true

    fun resetCamera(root: PrefabSaveable) {
        rotation.set(-20.0, 0.0, 0.0)
        when (root) {
            is MeshBaseComponent -> {
                val mesh = root.getMesh() ?: return
                resetCamera(mesh)
            }
            is Mesh -> resetCamera(root)
            is Material, is LightComponentBase -> {
                radius = 2.0
            }
            is Entity -> {
                root.validateTransform()
                root.validateAABBs()
                resetCamera(root.aabb, true)
            }
            is Collider -> {
                val aabb = AABBd()
                root.union(Matrix4x3d(), aabb, Vector3d(), false)
                resetCamera(aabb, false)
            }
            else -> {
                LOGGER.warn("Please implement bounds for ${root.className}")
            }
        }
    }

    private fun resetCamera(mesh: Mesh) {
        resetCamera(mesh.aabb, true)
    }

    private fun resetCamera(aabb: AABBf, translate: Boolean) {
        if (aabb.avgX().isFinite() && aabb.avgY().isFinite() && aabb.avgZ().isFinite()) {
            position.set(aabb.avgX().toDouble(), aabb.avgY().toDouble(), aabb.avgZ().toDouble())
            radius = length(aabb.deltaX(), aabb.deltaY(), aabb.deltaZ()).toDouble()
        }
    }

    private fun resetCamera(aabb: AABBd, translate: Boolean) {
        if (aabb.avgX().isFinite() && aabb.avgY().isFinite() && aabb.avgZ().isFinite()) {
            if (translate) position.set(aabb.avgX(), aabb.avgY(), aabb.avgZ())
            radius = length(aabb.deltaX(), aabb.deltaY(), aabb.deltaZ())
        }
    }

    fun onStart() {
        // todo when first created, center around scene,
        // todo and adjust the radius
        syncMaster.nextSession()
        val root = inspector.root
        val rootEntity = root as? Entity
        rootEntity?.physics?.startWork()
        if (isFirstTime) {
            isFirstTime = false
            resetCamera(root)
        }
        for (window in window?.windowStack ?: emptyList()) {
            window.panel.forAll {
                if (it is RenderView) {
                    it.radius = radius
                    it.position.set(position)
                    it.rotation.set(rotation)
                }
            }
        }
    }

    fun onStop() {
        syncMaster.nextSession()
        try {
            for (window in windowStack) {
                window.panel.forAll {
                    if (it is RenderView) {
                        radius = it.radius
                        position.set(it.position)
                        rotation.set(it.rotation)
                        // early exit
                        throw RuntimeException()
                    }
                }
            }
        } catch (e: RuntimeException) {
        }
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        when {
            button.isLeft -> ECSSceneTabs.open(this)
            button.isRight -> ECSSceneTabs.close(this) // todo open menu instead
            else -> super.onMouseClicked(x, y, button, long)
        }
    }

    override fun tickUpdate() {
        super.tickUpdate()
        backgroundColor = if (ECSSceneTabs.currentTab == this) 0xff777777.toInt()
        else originalBGColor
    }

    fun save() {
        inspector.save()
    }

    companion object {
        private val LOGGER = LogManager.getLogger(ECSSceneTab::class)
    }

}