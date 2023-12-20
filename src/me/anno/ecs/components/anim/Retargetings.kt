package me.anno.ecs.components.anim

import me.anno.animation.LoopingState
import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllComponentsInChildren
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
import me.anno.language.translation.NameDesc
import me.anno.studio.StudioBase.Companion.workspace
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import me.anno.utils.Color
import me.anno.utils.LOGGER
import me.anno.utils.OS
import kotlin.math.abs

/**
 * utility for retargeting
 * */
object Retargetings {

    val noBoneMapped = "?"

    var sampleModel: AnimMeshComponent? = null

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
            defineDefaultMapping(
                SkeletonCache[srcSkeleton]!!,
                SkeletonCache[dstSkeleton]!!, prefab
            )
            configReference.getParent()?.tryMkdirs()
            configReference.writeText(JsonStringWriter.toText(prefab, workspace))
        } else if (prefab.clazzName != "Retargeting") {
            LOGGER.warn("Class mismatch for $configReference!")
            return null
        }
        return prefab
    }

    fun getAllowedBones(
        bone: Bone,
        srcSkeleton: Skeleton,
        dstSkeleton: Skeleton, dstBoneMapping: Array<String>,
    ): List<Bone> {
        var ancestor = bone
        var ancestorMap = noBoneMapped
        while (ancestorMap == noBoneMapped) {
            val parentId = ancestor.parentId
            ancestorMap = dstBoneMapping.getOrNull(parentId) ?: break
            ancestor = dstSkeleton.bones[parentId]
        }
        // now filter
        return if (ancestorMap == noBoneMapped) srcSkeleton.bones
        else srcSkeleton.bones.filter { it.hasBoneInHierarchy(ancestorMap, srcSkeleton.bones) }
    }

    fun defineDefaultMapping(srcSkeleton: Skeleton, dstSkeleton: Skeleton, prefab: Prefab) {
        // automatic bone-assignment, if none is found
        // todo - use similar assignments, if some are found in the database
        // todo merge skeletons, if they are very similar (names, positions, structure)
        val map = Array(dstSkeleton.bones.size) { dstBoneId ->
            val dstName = dstSkeleton.bones[dstBoneId].name
            srcSkeleton.bones.firstOrNull { it.name == dstName }?.name // perfect match
                ?: noBoneMapped
        }
        if (srcSkeleton.bones.isNotEmpty() && dstSkeleton.bones.isNotEmpty() && map[0] == noBoneMapped) {
            // map root bone
            map[0] = srcSkeleton.bones.first().name
        }
        val usedBones = map.toHashSet()
        for (i in map.lastIndex downTo 1) {
            if (map[i] == noBoneMapped) {
                // todo find viable bone from allowed bones
                //  - close in name
                //  - close in location???
                //  - allowed to be set
                //  - not yet used
                val dstBone = dstSkeleton.bones[i]
                val dstName = dstBone.name
                val candidates = getAllowedBones(dstBone, srcSkeleton, dstSkeleton, map)
                    .filter { it.name !in usedBones }
                    .filter {
                        val srcName = it.name
                        abs(srcName.length - dstName.length) <= 2 &&
                                (srcName.startsWith(dstName) || dstName.startsWith(srcName))
                    }
                if (candidates.size == 1) {
                    val srcName = candidates.first().name
                    LOGGER.info("Mapped $dstName to $srcName")
                    map[i] = srcName
                    usedBones.add(srcName)
                }
            }
        }
        prefab["dstBoneIndexToSrcName"] = map
    }

    private fun getConfigName(skeleton: FileReference): String {
        return SkeletonCache[skeleton]!!.bones.joinToString("/") { it.name }
            .hashCode().toUInt().toString(36)
    }

    fun getConfigFile(srcSkeleton: FileReference, dstSkeleton: FileReference): FileReference {
        // todo since we hide the names, we could also use our hierarchical database...
        val hash1 = getConfigName(srcSkeleton)
        val hash2 = getConfigName(dstSkeleton)
        // database, which stores bone assignments for a project
        val config = "retargeting/$hash1-$hash2.json"
        return workspace.getChild(config)
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
        workspace = OS.documents.getChild("RemsEngine\\YandereSim")
        // find two human meshes with different skeletons
        val meshFile = workspace.getChild("Characters/SK_Chr_Asian_Gangster_Male_01.json")
        val animFile = workspace.getChild("Characters/anim-files/Walking-inPlace.fbx")
        val scene = PrefabCache[meshFile]!!.createInstance() as Entity
        val animation = animFile.getChild("animations/mixamo.com/BoneByBone.json")
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