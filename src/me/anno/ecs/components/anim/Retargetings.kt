package me.anno.ecs.components.anim

import me.anno.animation.LoopingState
import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.gpu.GFX
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.serialization.NotSerializedProperty
import me.anno.language.translation.NameDesc
import me.anno.studio.StudioBase
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import me.anno.utils.Color
import me.anno.utils.LOGGER
import me.anno.utils.OS

object Retargetings {

    val noBoneMapped = "?"

    @NotSerializedProperty
    var sampleModel: AnimMeshComponent? = null

    @NotSerializedProperty
    var sampleAnimation: FileReference? = null

    val srcColor = 0xa95555 or Color.black
    val dstColor = 0x00a7f2 or Color.black

    val srcColor1 = 0xa91111 or Color.black
    val dstColor1 = 0x0037f2 or Color.black

    val srcMat = Material.diffuse(srcColor)
    val dstMat = Material.diffuse(dstColor)

    private val cache = CacheSection("Retargeting")
    private const val timeout = 500_000L

    fun openUI(anim: AnimMeshComponent) {
        // find all animation states, where the skeleton needs mapping, and ask the user, which to edit
        val window = GFX.someWindow ?: return
        val states = anim.animations
        val dstSkeleton = anim.skeleton
        val baseSkeletonCheck = SkeletonCache[dstSkeleton]
        if (baseSkeletonCheck == null) {
            LOGGER.warn("Base skeleton is null")
        }
        val srcSkeletons = states
            .map { it to (AnimationCache[it.source]?.skeleton ?: InvalidRef) }
            .filter { SkeletonCache[it.second] != null }
        if (srcSkeletons.isEmpty()) {
            LOGGER.warn("No skeletons could be found in animation states")
        }
        val srcSkeletons1 = srcSkeletons.filter { it.second != dstSkeleton }
        when (srcSkeletons1.size) {
            0 -> LOGGER.warn("All skeletons were identical, so no retargeting required")
            1 -> openUI(srcSkeletons1.first(), dstSkeleton, anim)
            else -> {
                Menu.openMenu(window.windowStack, NameDesc("Choose skeleton to edit"),
                    srcSkeletons1.map { srcSkeleton ->
                        MenuOption(NameDesc(srcSkeleton.second.toLocalPath())) {
                            openUI(srcSkeleton, dstSkeleton, anim)
                        }
                    })
            }
        }
    }

    private fun openUI(
        srcSkeleton: Pair<AnimationState, FileReference>,
        dstSkeleton: FileReference,
        sampleModel: AnimMeshComponent
    ) {
        openUI(
            srcSkeleton.second, dstSkeleton,
            sampleModel, srcSkeleton.first.source
        )
    }

    /**
     * then edit that one
     *  - prefab inspector
     *  - save button
     *  - history?
     * */
    private fun openUI(
        srcSkeleton: FileReference, dstSkeleton: FileReference,
        sampleModel1: AnimMeshComponent, sampleAnimation1: FileReference
    ) {
        val prefab = getOrCreatePrefab(srcSkeleton, dstSkeleton) ?: return
        sampleModel = sampleModel1
        sampleAnimation = sampleAnimation1
        ECSSceneTabs.open(prefab.source, PlayMode.EDITING, true)
    }

    private fun getOrCreatePrefab(srcSkeleton: FileReference, dstSkeleton: FileReference): Prefab? {
        val configReference = getConfigFile(srcSkeleton, dstSkeleton)
        var prefab = PrefabCache[configReference]
        if (prefab == null) {
            // create new file
            prefab = Prefab("Retargeting")
            prefab.source = configReference
            prefab["srcSkeleton"] = srcSkeleton
            prefab["dstSkeleton"] = dstSkeleton

            // todo automatic bone-assignment, if none is found
            //  - use similar assignments, if some are found in the database
            // todo merge skeletons, if they are very similar (names, positions, structure)

            configReference.getParent()?.tryMkdirs()
            configReference.writeText(JsonStringWriter.toText(prefab, StudioBase.workspace))
        } else if (prefab.clazzName != "Retargeting") {
            LOGGER.warn("Class mismatch for $configReference!")
            return null
        }
        return prefab
    }

    fun getConfigFile(srcSkeleton: FileReference, dstSkeleton: FileReference): FileReference {
        // todo since we hide the names, we could also use our hierarchical database...
        val hash1 = srcSkeleton.toLocalPath().hashCode()
        val hash2 = dstSkeleton.toLocalPath().hashCode()
        // database, which stores bone assignments for a project
        val config = "retargeting/$hash1-$hash2.json"
        return StudioBase.workspace.getChild(config)
    }

    fun getRetargeting(srcSkeleton: FileReference, dstSkeleton: FileReference): Retargeting? {
        if (srcSkeleton == dstSkeleton) return null
        val data = cache.getEntry(
            DualFileKey(srcSkeleton, dstSkeleton),
            timeout, false
        ) { key ->
            val prefab = getOrCreatePrefab(key.file0, key.file1)
            CacheData(prefab?.getSampleInstance() as? Retargeting)
        } as CacheData<*>
        return data.value as Retargeting
    }

    @JvmStatic
    fun main(args: Array<String>) {
        // todo for testing, find an easier case: one, where the mesh isn't rotated/scaled
        ECSRegistry.init()
        // find two human meshes with different skeletons
        val meshFile = OS.downloads.getChild("3d/trooper gltf/scene.gltf")
        val animFile = OS.downloads.getChild("fbx/Walking.fbx")
        val scene = PrefabCache[meshFile]!!.createInstance() as Entity
        val animation = animFile.getChild("animations").listChildren()!!.first().getChild("BoneByBone.json")
        lateinit var testedComponent: AnimMeshComponent
        scene.forAllComponentsInChildren(AnimMeshComponent::class) { mesh ->
            mesh.animations = listOf(AnimationState(animation, 1f, 0f, 1f, LoopingState.PLAY_LOOP))
            testedComponent = mesh
        }
        val retargeting = getRetargeting(AnimationCache[animation]!!.skeleton, testedComponent.skeleton)!!
        sampleModel = testedComponent
        sampleAnimation = animation
        SceneView.testSceneWithUI("Retargeting", retargeting)
    }
}