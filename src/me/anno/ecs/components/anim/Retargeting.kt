package me.anno.ecs.components.anim

import me.anno.animation.LoopingState
import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.ecs.Entity
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ECSRegistry
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugShapes
import me.anno.engine.debug.DebugTriangle
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.gpu.GFX
import me.anno.gpu.pipeline.Pipeline
import me.anno.graph.ui.GraphPanel.Companion.blue
import me.anno.graph.ui.GraphPanel.Companion.red
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.language.translation.NameDesc
import me.anno.studio.Inspectable
import me.anno.studio.StudioBase.Companion.workspace
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.SettingCategory
import me.anno.ui.input.EnumInput
import me.anno.utils.LOGGER
import me.anno.utils.OS.downloads
import me.anno.utils.types.Vectors
import org.joml.*
import kotlin.math.min
import kotlin.math.sqrt

class Retargeting : PrefabSaveable(), Renderable {

    companion object {

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
                .mapNotNull { AnimationCache[it.source]?.skeleton }
                .filter { SkeletonCache[it] != null }
            if (srcSkeletons.isEmpty()) {
                LOGGER.warn("No skeletons could be found in animation states")
            }
            val srcSkeletons1 = srcSkeletons - dstSkeleton
            when (srcSkeletons1.size) {
                0 -> LOGGER.warn("All skeletons were identical, so no retargeting required")
                1 -> openUI(srcSkeletons1.first(), dstSkeleton)
                else -> {
                    openMenu(window.windowStack, NameDesc("Choose skeleton to edit"),
                        srcSkeletons1.map { srcSkeleton ->
                            MenuOption(NameDesc(srcSkeleton.toLocalPath())) {
                                openUI(srcSkeleton, dstSkeleton)
                            }
                        })
                }
            }
        }

        fun openUI(srcSkeleton: FileReference, dstSkeleton: FileReference) {
            // then edit that one
            //  - prefab inspector
            //  - save button
            //  - history?
            val configReference = getConfigFile(srcSkeleton, dstSkeleton)
            val prefab = PrefabCache[configReference]
            if (prefab == null) {
                // create new file
                val prefab1 = Prefab("Retargeting")
                prefab1["srcSkeleton"] = srcSkeleton
                prefab1["dstSkeleton"] = dstSkeleton
                configReference.getParent()?.tryMkdirs()
                configReference.writeText(JsonStringWriter.toText(prefab1, workspace))
            } else if (prefab.clazzName != "Retargeting") {
                LOGGER.warn("Class mismatch for $configReference!")
                return
            }
            ECSSceneTabs.open(configReference, PlayMode.EDITING, true)
        }

        fun getConfigFile(srcSkeleton: FileReference, dstSkeleton: FileReference): FileReference {
            // todo since we hide the names, we could also use our hierarchical database...
            val hash1 = srcSkeleton.toLocalPath().hashCode()
            val hash2 = dstSkeleton.toLocalPath().hashCode()
            // database, which stores bone assignments for a project
            val config = "retargeting/$hash1-$hash2.json"
            return workspace.getChild(config)
        }

        fun getRetargeting(srcSkeleton: FileReference, dstSkeleton: FileReference): Retargeting? {
            if (srcSkeleton == dstSkeleton) return null
            val data = cache.getEntry(
                DualFileKey(srcSkeleton, dstSkeleton),
                timeout, false
            ) { k12 ->

                // todo hash skeleton instead of skeleton path
                // database, which stores bone assignments for a project
                val config1 = getConfigFile(srcSkeleton, dstSkeleton)

                LOGGER.debug("Getting retargeting {}", k12)

                val ret: Retargeting
                if (config1.exists) {
                    ret = JsonStringReader.readFirstOrNull<Retargeting>(config1, InvalidRef, true)
                        ?: Retargeting()
                } else {
                    ret = Retargeting()
                    ret.srcSkeleton = k12.file0
                    ret.dstSkeleton = k12.file1
                    config1.getParent()?.tryMkdirs()
                    config1.writeText(JsonStringWriter.toText(ret, InvalidRef))
                }

                // todo automatic bone-assignment, if none is found
                //  - use similar assignments, if some are found in the database
                // todo merge skeletons, if they are very similar (names, positions, structure)

                CacheData(ret)
            } as CacheData<*>
            return data.value as Retargeting
        }

        @JvmStatic
        fun main(args: Array<String>) {
            ECSRegistry.init()
            val testRetargeting = true
            // find two human meshes with different skeletons
            val file1 = downloads.getChild("3d/trooper gltf/scene.gltf")
            val file2 = downloads.getChild("fbx/Walking.fbx")
            val fileI = if (testRetargeting) file2 else file1
            val scene = PrefabCache[file1]!!.createInstance() as Entity
            val animation = fileI.getChild("animations").listChildren()!!.first().getChild("BoneByBone.json")
            scene.forAllComponentsInChildren(AnimMeshComponent::class) { mesh ->
                mesh.animations = listOf(AnimationState(animation, 1f, 0f, 1f, LoopingState.PLAY_LOOP))
            }
            // todo target stands still... were no bones mapped?
            //  the target is also crippled... why???
            testSceneWithUI("Retargeting", scene)
        }
    }

    var srcSkeleton: FileReference = InvalidRef
    var dstSkeleton: FileReference = InvalidRef

    // for each dstBone, which bone is responsible for the transform
    var dstBoneMapping: Array<String>? = null

    @DebugProperty
    val isIdentityMapping get() = srcSkeleton == dstSkeleton

    fun map(src: BoneByBoneAnimation): BoneByBoneAnimation? {
        if (isIdentityMapping) return src
        return map(src, BoneByBoneAnimation())
    }

    private val mappedAnimations = ArrayList<Pair<BoneByBoneAnimation, BoneByBoneAnimation>>()

    fun invalidate() {
        // good enough?
        // we need to remap them...
        // todo this is a memory leak... animations can get destroyed, and we won't remove them
        for ((src, dst) in mappedAnimations) {
            map2(src, dst)
        }
    }

    override fun fill(pipeline: Pipeline, entity: Entity, clickId: Int): Int {
        // todo different colors
        // todo show bone names / mapping
        // todo play sample animation (?)
        // todo show sample mesh (?)
        SkeletonCache[srcSkeleton]?.fill(pipeline, entity, clickId)
        SkeletonCache[dstSkeleton]?.fill(pipeline, entity, clickId)
        return clickId + 1
    }

    fun map(src: BoneByBoneAnimation, dst: BoneByBoneAnimation): BoneByBoneAnimation? {
        if (isIdentityMapping || src === dst) return src
        mappedAnimations.add(src to dst)
        return map2(src, dst)
    }

    fun map2(src: BoneByBoneAnimation, dst: BoneByBoneAnimation): BoneByBoneAnimation? {
        val srcSkel = SkeletonCache[srcSkeleton]?.bones
        val dstSkel = SkeletonCache[dstSkeleton]?.bones

        if (srcSkel == null || dstSkel == null) {
            LOGGER.warn(
                "Mapping is null, because ${
                    if (srcSkel == null && dstSkel == null) "both skels are null, '$srcSkeleton'/'$dstSkeleton'"
                    else if (srcSkel == null) "src skel is null, '$srcSkeleton'/'$dstSkeleton'"
                    else "dst skel is null, '$srcSkeleton'/'$dstSkeleton'"
                }"
            )
            return null
        }

        dst.skeleton = dstSkeleton
        dst.frameCount = src.frameCount
        dst.boneCount = dstSkel.size
        dst.duration = src.duration
        dst.prepareBuffers()

        val srcBones = srcSkel.associateBy { it.name }
        val mapping = dstBoneMapping ?: return dst
        val tmpM = Matrix4x3f()
        val tmpV = Vector3f()
        val tmpQ = Quaternionf()
        // find base skeleton scale each, and then scale all bones
        val srcScaleSq = srcSkel.sumOf { it.bindPosition.lengthSquared().toDouble() } / srcSkel.size
        val dstScaleSq = dstSkel.sumOf { it.bindPosition.lengthSquared().toDouble() } / dstSkel.size
        val translationScale = sqrt(dstScaleSq / srcScaleSq).toFloat()
        for (dstBone in 0 until min(dstSkel.size, mapping.size)) {
            val srcBone = srcBones[mapping[dstBone]] ?: continue
            var srcBone0: Bone? = null
            var lastValidDst = dstSkel[dstBone].parentId
            while (lastValidDst >= 0) {
                // check if this bone is mapped
                srcBone0 = srcBones[mapping[lastValidDst]]
                if (srcBone0 != null) break // found valid bone :)
                lastValidDst = dstSkel[lastValidDst].parentId // next ancestor
            }
            for (frame in 0 until dst.frameCount) {
                // collect bones from previously mapped parent to this one
                // to do/warn!: if srcBone0 is not an ancestor of srcBone, the result will be broken;
                // we'd have to invert the result of srcBone0 to their common root
                var index = 0
                fun applyBone(bone: Bone) {
                    // handle parent
                    if (bone.parentId >= 0) {
                        val parentBone = srcSkel[bone.parentId]
                        if (parentBone != srcBone0) applyBone(parentBone)
                    }
                    val srcBone1 = bone.id
                    if (index == 0) {
                        src.getTranslation(frame, srcBone1, tmpV)
                        src.getRotation(frame, srcBone1, tmpQ)
                    } else {
                        if (index == 1) tmpM.translationRotate(tmpV, tmpQ)
                        src.getTranslation(frame, srcBone1, tmpV)
                        src.getRotation(frame, srcBone1, tmpQ)
                        // correct order? should be
                        tmpM.translate(tmpV)
                        tmpM.rotate(tmpQ)
                    }
                    index++
                }
                applyBone(srcBone)
                if (index > 1) { // we used the matrix, so extract the result
                    tmpM.getTranslation(tmpV)
                    tmpM.getUnnormalizedRotation(tmpQ)
                }
                tmpV.mul(translationScale)
                dst.setTranslation(frame, dstBone, tmpV)
                dst.setRotation(frame, dstBone, tmpQ)
            }
        }
        return dst
    }

    override fun createInspector(
        list: PanelListY, style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        super.createInspector(listOf(this), list, style, getGroup)
        // todo value / UI for scale change preview?
        // todo register change, so we can save/reset it
        val srcSkeleton = SkeletonCache[srcSkeleton]
        val dstSkeleton = SkeletonCache[dstSkeleton]
        if (srcSkeleton != null && dstSkeleton != null) {
            var dstBoneMapping = dstBoneMapping
            if (dstBoneMapping == null) dstBoneMapping = Array(dstSkeleton.bones.size) { "?" }
            if (dstBoneMapping.size < dstSkeleton.bones.size) {
                dstBoneMapping = Array(dstSkeleton.bones.size) { dstBoneMapping!!.getOrNull(it) ?: "?" }
            }
            // for each bone, create an enum input
            this.dstBoneMapping = dstBoneMapping
            for ((i, bone) in dstSkeleton.bones.withIndex()) {
                list.add(object : EnumInput(NameDesc(bone.name), NameDesc(dstBoneMapping[i]), srcSkeleton.bones.map {
                    NameDesc(it.name)
                } + listOf(NameDesc("?")), style) {
                    override fun onUpdate() {
                        super.onUpdate()
                        if (isHovered && window == windowStack.peek()) {
                            // when we hover over a bone, highlight that bone in the UI
                            // todo when we open the enum-select-menu, highlight that bone, too (from there somehow)
                            showBoneInUI(i, dstSkeleton.bones, red)
                            val srcId = srcSkeleton.bones.firstOrNull { it.name == value.name }?.id
                            if (srcId != null) showBoneInUI(srcId, srcSkeleton.bones, blue)
                        }
                    }
                }.setChangeListener { value, _, _ ->
                    dstBoneMapping[i] = value
                    invalidate()
                })
            }
        } else {
            if (srcSkeleton == null) list.add(TextPanel("Missing src skeleton", style)) // todo warning color
            if (dstSkeleton == null) list.add(TextPanel("Missing dst skeleton", style)) // todo warning color
        }
    }

    fun showBoneInUI(boneId: Int, bones: List<Bone>, color: Int) {
        // todo could be much more efficient
        val dstBone = bones[boneId]
        if (dstBone.parentId < 0) return
        val srcBone = bones[dstBone.parentId]
        val srcPos = srcBone.bindPosition
        val dstPos = dstBone.bindPosition
        val dirY = Vector3f(dstPos).sub(srcPos)
        val length = dirY.length()
        val dirX = Vector3f()
        val dirZ = Vector3f()
        val mat = Matrix3f()
        val tmp = Vector3f()
        // find orthogonal directions
        Vectors.findTangent(dirY, dirX).normalize(length)
        dirZ.set(dirX).cross(dirY).normalize(length)
        mat.set(dirX, dirY, dirZ)
        // add a bone from src to dst
        val vertices = Skeleton.boneMeshVertices
        val list = ArrayList<Vector3d>()
        for (i in 0 until vertices.size - 2 step 3) {
            tmp.set(vertices, i)
            tmp.sub(0f, 0.005f, 0f)
            tmp.mul(1.01f) // scale up slightly for better visibility
            mat.transform(tmp)
            tmp.add(srcPos)
            list.add(Vector3d(tmp))
        }
        // draw triangles
        for (i in list.indices step 3) {
            val triangle = DebugTriangle(list[i], list[i + 1], list[i + 2], color, 0f)
            DebugShapes.debugTriangles.add(triangle)
        }
    }

    override fun createInspector(
        inspected: List<Inspectable>,
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        createInspector(list, style, getGroup)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("srcSkeleton", srcSkeleton)
        writer.writeFile("dstSkeleton", dstSkeleton)
        val dstBoneMapping = dstBoneMapping
        if (dstBoneMapping != null) writer.writeStringArray("dstNames", dstBoneMapping)
    }

    override fun readFile(name: String, value: FileReference) {
        when (name) {
            "srcSkeleton" -> srcSkeleton = value
            "dstSkeleton" -> dstSkeleton = value
            else -> super.readFile(name, value)
        }
    }

    override fun readStringArray(name: String, values: Array<String>) {
        when (name) {
            "dstNames" -> dstBoneMapping = values
            else -> super.readStringArray(name, values)
        }
    }

    override val className: String get() = "Retargeting"
    override val approxSize get() = 20
}