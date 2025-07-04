package me.anno.ecs.components.anim

import me.anno.cache.AsyncCacheData
import me.anno.cache.DualCacheSection
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.EngineBase.Companion.workspace
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.gpu.GFX
import me.anno.io.files.FileKey
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.language.translation.NameDesc
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import me.anno.utils.Color
import me.anno.utils.structures.lists.Lists.createArrayList
import org.apache.logging.log4j.LogManager
import kotlin.math.abs

/**
 * utility for retargeting
 * */
object Retargetings {

    private val LOGGER = LogManager.getLogger(Retargetings::class)

    val noBoneMapped = "?"

    var sampleModel: AnimMeshComponent? = null

    var sampleAnimation: FileReference = InvalidRef

    val srcColor = 0xa95555 or Color.black
    val dstColor = 0x00a7f2 or Color.black

    val srcColor1 = 0xa91111 or Color.black
    val dstColor1 = 0x0037f2 or Color.black

    val srcMat = Material.diffuse(srcColor)
    val dstMat = Material.diffuse(dstColor)

    private val cache = DualCacheSection<FileKey, FileKey, Retargeting>("Retargeting")
    private const val timeoutMillis = 500_000L

    fun openUI(anim: AnimMeshComponent) {
        // find all animation states, where the skeleton needs mapping, and ask the user, which to edit
        val window = GFX.someWindow
        val states = anim.animations
        val dstSkeleton = anim.getMesh()?.skeleton ?: InvalidRef
        val baseSkeletonCheck = SkeletonCache.getEntry(dstSkeleton).waitFor()
        if (baseSkeletonCheck == null) {
            LOGGER.warn("Base skeleton is null")
        }
        val srcSkeletons = states
            .map { it to (AnimationCache.getEntry(it.source).waitFor()?.skeleton ?: InvalidRef) }
            .filter { SkeletonCache.getEntry(it.second).waitFor() != null }
        if (srcSkeletons.isEmpty()) {
            LOGGER.warn("No skeletons could be found in animation states")
        }
        val srcSkeletons1 = srcSkeletons.filter { it.second != dstSkeleton }
        when (srcSkeletons1.size) {
            0 -> LOGGER.warn("All skeletons were identical, so no retargeting required")
            1 -> openUI(srcSkeletons1.first(), dstSkeleton, anim)
            else -> {
                Menu.openMenu(
                    window.windowStack, NameDesc("Choose skeleton to edit"),
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
        ECSSceneTabs.open(prefab.sourceFile, PlayMode.EDITING, true)
    }

    private fun getOrCreatePrefab(srcSkeletonFile: FileReference, dstSkeletonFile: FileReference): Prefab? {
        val configReference = getConfigFile(srcSkeletonFile, dstSkeletonFile)
        var prefab = PrefabCache[configReference].waitFor()?.prefab
        if (prefab == null) {
            // create new file
            prefab = Prefab("Retargeting")
            prefab.sourceFile = configReference
            prefab["srcSkeleton"] = srcSkeletonFile
            prefab["dstSkeleton"] = dstSkeletonFile
            val srcSkeleton = SkeletonCache.getEntry(srcSkeletonFile).waitFor() ?: return null
            val dstSkeleton = SkeletonCache.getEntry(dstSkeletonFile).waitFor() ?: return null
            defineDefaultMapping(srcSkeleton, dstSkeleton, prefab)
            configReference.getParent().tryMkdirs()
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
        dstSkeleton: Skeleton,
        dstBoneMapping: List<String>,
    ): List<Bone> {
        var ancestor = bone
        var ancestorMap = noBoneMapped
        while (ancestorMap == noBoneMapped) {
            val parentId = ancestor.parentIndex
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
        val map = createArrayList(dstSkeleton.bones.size) { dstBoneId ->
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
        val skeleton = SkeletonCache.getEntry(skeleton).waitFor() ?: return "0"
        val hash = skeleton.bones.joinToString("/") { it.name }.hashCode()
        return Integer.toUnsignedString(hash, 36)
    }

    fun getConfigFile(srcSkeleton: FileReference, dstSkeleton: FileReference): FileReference {
        println("getting config file '$srcSkeleton' -> '$dstSkeleton'")
        // todo since we hide the names, we could also use our hierarchical database...
        val hash1 = getConfigName(srcSkeleton)
        val hash2 = getConfigName(dstSkeleton)
        // database, which stores bone assignments for a project
        val config = "retargeting/$hash1-$hash2.json"
        return workspace.getChild(config)
    }

    fun getRetargeting(srcSkeleton: FileReference, dstSkeleton: FileReference): AsyncCacheData<Retargeting> {
        if (srcSkeleton == dstSkeleton) return AsyncCacheData.empty()
        return cache.getDualEntry(
            srcSkeleton.getFileKey(), dstSkeleton.getFileKey(),
            timeoutMillis
        ) { key1, key2, result ->
            val prefab = getOrCreatePrefab(key1.file, key2.file)
            result.value = prefab?.getSampleInstance() as? Retargeting
        }
    }
}