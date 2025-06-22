package me.anno.ecs.components.anim

import me.anno.Time
import me.anno.animation.LoopingState
import me.anno.ecs.Transform
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.components.anim.AnimMeshComponent.Companion.tmpMapping0
import me.anno.ecs.components.anim.AnimMeshComponent.Companion.tmpMapping1
import me.anno.ecs.components.anim.AnimationCache.getMappedAnimation
import me.anno.ecs.components.anim.Retargetings.dstColor
import me.anno.ecs.components.anim.Retargetings.dstColor1
import me.anno.ecs.components.anim.Retargetings.dstMat
import me.anno.ecs.components.anim.Retargetings.noBoneMapped
import me.anno.ecs.components.anim.Retargetings.sampleAnimation
import me.anno.ecs.components.anim.Retargetings.sampleModel
import me.anno.ecs.components.anim.Retargetings.srcColor
import me.anno.ecs.components.anim.Retargetings.srcColor1
import me.anno.ecs.components.anim.Retargetings.srcMat
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.debug.DebugShapes
import me.anno.engine.debug.DebugText
import me.anno.engine.debug.DebugTriangle
import me.anno.engine.inspector.Inspectable
import me.anno.gpu.pipeline.Pipeline
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.language.translation.NameDesc
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.SettingCategory
import me.anno.ui.input.EnumInput
import me.anno.ui.input.FloatVectorInput
import me.anno.ui.input.NumberType
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.structures.lists.UpdatingList
import me.anno.utils.types.Vectors
import org.apache.logging.log4j.LogManager
import org.joml.Matrix3f
import org.joml.Matrix4x3
import org.joml.Matrix4x3f
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.min

/**
 * defines how an animation with its own skeleton is mapped onto another skeleton
 * */
class Retargeting : PrefabSaveable(), Renderable {

    companion object {
        private val LOGGER = LogManager.getLogger(Retargeting::class)
    }

    var srcSkeleton: FileReference = InvalidRef
    var dstSkeleton: FileReference = InvalidRef

    // for each dstBone, which bone is responsible for the transform
    @HideInInspector
    val dstBoneIndexToSrcName: ArrayList<String> = ArrayList()

    // todo (how) can we use these to convert A poses to T poses and vice-versa?
    @HideInInspector
    val dstBoneRotations: ArrayList<Quaternionf> = ArrayList()

    val isIdentityMapping get() = srcSkeleton == dstSkeleton

    fun map(src: BoneByBoneAnimation): BoneByBoneAnimation? {
        if (isIdentityMapping) return src
        return map(src, BoneByBoneAnimation())
    }

    // todo this is a memory leak... animations can get destroyed, and we won't remove them
    private val mappedAnimations = ArrayList<Pair<BoneByBoneAnimation, BoneByBoneAnimation>>()

    fun invalidate() {
        // good enough?
        // we need to remap them...
        for ((src, dst) in mappedAnimations) {
            AnimationCache.invalidate(dst)
            recalculate(src, dst)
        }
    }

    var showSampleMesh = false
    var showSampleAnimation = true

    override fun fill(pipeline: Pipeline, transform: Transform) {
        // different colors
        // todo show mapping clearer, maybe connecting lines
        // todo apply transform onto one of them, so we can check their matches better
        val sm = sampleModel
        val sa = AnimationCache.getEntry(sampleAnimation).waitFor()
        val srcSkeleton1 = SkeletonCache.getEntry(srcSkeleton).waitFor()
        val dstSkeleton1 = SkeletonCache.getEntry(dstSkeleton).waitFor()
        val transformI = transform.getDrawMatrix()
        if (showSampleAnimation && sa != null && sm != null && srcSkeleton1 != null && dstSkeleton1 != null) {
            if (showSampleMesh) {
                // show retargeted mesh
                // todo why is this one not animated???
                sm.fill(pipeline, transform)
            }
            val srcPreviewData = srcPreviewData ?: Animation.PreviewData(srcSkeleton1, sa)
            this.srcPreviewData = srcPreviewData
            fillSkeletonAnimated(pipeline, transform, srcPreviewData)
            val mappedAnimation = mappedAnimations.firstOrNull { it.first == sa }?.second
                ?: getMappedAnimation(sa, dstSkeleton1).waitFor()
            if (mappedAnimation != null) {
                if (dstPreviewData == null) dstPreviewData = Animation.PreviewData(
                    dstSkeleton1, mappedAnimation
                )
                fillSkeletonAnimated(pipeline, transform, dstPreviewData!!)
            } else println("Missing mapped animation!!")
            val loop = LoopingState.PLAY_LOOP
            val frame = loop[Time.gameTime.toFloat(), sa.duration] * sa.numFrames / sa.duration
            drawBoneNames(srcSkeleton1, sa.getMappedMatrices(frame, tmpMapping0, srcSkeleton), transformI, srcColor)
            drawBoneNames(dstSkeleton1, sa.getMappedMatrices(frame, tmpMapping1, dstSkeleton), transformI, dstColor)
        } else {
            srcSkeleton1?.fill(pipeline, srcMat)
            dstSkeleton1?.fill(pipeline, dstMat)
        }
    }

    private var srcPreviewData: Animation.PreviewData? = null
    private var dstPreviewData: Animation.PreviewData? = null
    private fun fillSkeletonAnimated(
        pipeline: Pipeline, transform: Transform,
        previewData: Animation.PreviewData,
    ) {
        previewData.state.setTime(Time.gameTime.toFloat())
        previewData.renderer.updateAnimState()
        previewData.renderer.fill(pipeline, transform)
    }

    private fun drawBoneNames(skeleton: Skeleton, matrices: List<Matrix4x3f>?, transform: Matrix4x3, color: Int) {
        matrices ?: return
        // draw bone names where they are
        for (i in 0 until min(skeleton.bones.size, matrices.size)) {
            val bone = skeleton.bones[i]
            val pos = Vector3f(bone.bindPosition)
            matrices[i].transformPosition(pos)
            val pos1 = Vector3d(pos)
            transform.transformPosition(pos1)
            DebugShapes.debugTexts.add(DebugText(pos1, bone.name, color, 0f))
        }
    }

    fun map(src: BoneByBoneAnimation, dst: BoneByBoneAnimation): BoneByBoneAnimation? {
        if (isIdentityMapping || src === dst) {
            // println("identity mapping, so returning self")
            return src
        }
        // println("adding mapping to list in ${hash32(this)}")
        mappedAnimations.add(src to dst)
        return recalculate(src, dst)
    }

    fun recalculate(src: BoneByBoneAnimation, dst: BoneByBoneAnimation): BoneByBoneAnimation? {
        val srcSkel = SkeletonCache.getEntry(srcSkeleton).waitFor()?.bones
        val dstSkel = SkeletonCache.getEntry(dstSkeleton).waitFor()?.bones

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
        // when we set nothing, everything should be identity, right? yes, correct

        val srcBones = srcSkel.associateBy { it.name }
        val dstToSrc = dstBoneIndexToSrcName
        val tmpM = Matrix4x3f()
        val pos = Vector3f()
        val rot = Quaternionf()
        val sca = Vector3f()
        // find base skeleton scale each, and then scale all bones
        // todo small skeletons don't work yet
        // val srcScaleSq = srcSkel.sumOf { it.bindPosition.lengthSquared().toDouble() } / srcSkel.size
        // val dstScaleSq = dstSkel.sumOf { it.bindPosition.lengthSquared().toDouble() } / dstSkel.size
        // val translationScale = sqrt(dstScaleSq / srcScaleSq).toFloat()
        for (dstBoneIndex in 0 until min(dstSkel.size, dstToSrc.size)) {
            val srcBone = srcBones[dstToSrc[dstBoneIndex]]
            if (srcBone == null) {
                // println("Skipping ${dstSkel[dstBone].name}, because it's unmapped")
                continue
            }
            var srcBone0: Bone? = null
            var lastValidDst = dstSkel[dstBoneIndex].parentIndex
            while (lastValidDst >= 0) {
                // check if this bone is mapped
                srcBone0 = srcBones[dstToSrc[lastValidDst]]
                if (srcBone0 != null) break // found valid bone :)
                lastValidDst = dstSkel[lastValidDst].parentIndex // next ancestor
            }
            // println("Mapping ${dstSkel[dstBone].name} to ${srcBone.name}, relative-root: ${srcBone0?.name}")
            for (frameIndex in 0 until dst.frameCount) {
                // collect bones from previously mapped parent to this one
                // to do/warn!: if srcBone0 is not an ancestor of srcBone, the result will be broken;
                // we'd have to invert the result of srcBone0 to their common root
                var index = 0
                fun applyBone(bone: Bone) {
                    // handle parent
                    if (bone.parentIndex >= 0) {
                        val parentBone = srcSkel[bone.parentIndex]
                        if (parentBone != srcBone0) applyBone(parentBone)
                    }
                    val srcBone1 = bone.index
                    if (index == 0) {
                        src.getTranslation(frameIndex, srcBone1, pos)
                        src.getRotation(frameIndex, srcBone1, rot)
                        src.getScale(frameIndex, srcBone1, sca)
                    } else {
                        if (index == 1) {
                            tmpM.translationRotateScale(pos, rot, sca)
                        }
                        src.getTranslation(frameIndex, srcBone1, pos)
                        src.getRotation(frameIndex, srcBone1, rot)
                        src.getScale(frameIndex, srcBone1, sca)
                        // correct order? should be
                        tmpM.translate(pos)
                        tmpM.rotate(rot)
                        tmpM.scale(sca)
                    }
                    index++
                }
                applyBone(srcBone)
                if (index > 1) { // we used the matrix, so extract the result
                    tmpM.getTranslation(pos)
                    tmpM.getUnnormalizedRotation(rot)
                    tmpM.getScale(sca)
                }
                // tmpV.mul(translationScale)
                if (frameIndex == 0 && dstBoneIndex == 0) {
                    println("retargeting setting transform: $pos,$rot,$sca")
                }
                dst.setTranslation(frameIndex, dstBoneIndex, pos)
                dst.setRotation(frameIndex, dstBoneIndex, rot)
                dst.setScale(frameIndex, dstBoneIndex, sca)
            }
        }
        return dst
    }

    @DebugAction
    fun defineDefaultMapping() {
        val prefab = prefab ?: return
        Retargetings.defineDefaultMapping(
            SkeletonCache.getEntry(srcSkeleton).waitFor()!!,
            SkeletonCache.getEntry(dstSkeleton).waitFor()!!,
            prefab
        )
        invalidate()
    }

    override fun createInspector(
        list: PanelListY, style: Style,
        getGroup: (nameDesc: NameDesc) -> SettingCategory
    ) {
        super.createInspector(listOf(this), list, style, getGroup)
        // todo value / UI for scale change preview?
        // todo register change, so we can save/reset it
        val srcSkeleton = SkeletonCache.getEntry(srcSkeleton).waitFor()
        val dstSkeleton = SkeletonCache.getEntry(dstSkeleton).waitFor()
        if (srcSkeleton != null && dstSkeleton != null) {
            val dstBoneMapping = dstBoneIndexToSrcName
            while (dstBoneMapping.size < dstSkeleton.bones.size) {
                dstBoneMapping.add(noBoneMapped)
            }
            val dstBoneRotations = dstBoneRotations
            while (dstBoneRotations.size < dstSkeleton.bones.size) {
                dstBoneRotations.add(Quaternionf())
            }
            // for each bone, create an enum input
            for ((i, bone) in dstSkeleton.bones.withIndex()) {
                // todo change color based on whether it is set to a valid bone
                //  (to more easily differentiate filled from empty slots)
                val options = UpdatingList(100) {
                    // filter elements such that only bones below our ancestors are allowed
                    val availableBones = Retargetings.getAllowedBones(bone, srcSkeleton, dstSkeleton, dstBoneMapping)
                    availableBones.map { it.nameDesc } + listOf(NameDesc(noBoneMapped))
                }
                // todo create tree-hierarchy for these
                //  -> padding left by bone depth
                list.add(object : EnumInput(
                    NameDesc(bone.name), NameDesc(dstBoneMapping[i]),
                    options, style
                ) {
                    override fun onUpdate() {
                        super.onUpdate()
                        if (isHovered && window == windowStack.peek()) {
                            // when we hover over a bone, highlight that bone in the UI
                            // todo when we open the enum-select-menu, highlight that bone, too (from there somehow)
                            showBoneInUI(i, dstSkeleton.bones, dstColor1)
                            val srcId = srcSkeleton.bones.firstOrNull { it.name == value.name }?.index
                            if (srcId != null) showBoneInUI(srcId, srcSkeleton.bones, srcColor1)
                        }
                    }
                }.setChangeListener { value, _, _ ->
                    dstBoneMapping[i] = value.englishName
                    val prefab = prefab
                    if (prefab != null) {
                        PrefabInspector.currentInspector?.change(
                            prefabPath, this,
                            "dstBoneIndexToSrcName", dstBoneMapping
                        )
                    }
                    invalidate()
                })
                if (false) list.add(run {
                    val value = dstBoneRotations[i]
                    val type = NumberType.ROT_YXZ.withDefault(Vector3f(0f))
                    FloatVectorInput(
                        NameDesc("Correction"), "${bone.name}-rot",
                        value.toEulerAnglesDegrees(), type, style
                    ).apply {
                        // property.init(this)
                        // askForReset(property) { setValue((it as Quaternionf).toEulerAnglesDegrees(), false) }
                        setResetListener { Quaternionf() }
                        addChangeListener { x, y, z, _, _ ->
                            val dst = dstBoneRotations[i]
                            Vector3f(x.toFloat(), y.toFloat(), z.toFloat()).toQuaternionDegrees(dst)
                            invalidate()
                        }
                    }
                })
            }
        } else {
            if (srcSkeleton == null) list.add(TextPanel("Missing src skeleton", style)) // todo warning color
            if (dstSkeleton == null) list.add(TextPanel("Missing dst skeleton", style)) // todo warning color
        }
    }

    fun showBoneInUI(boneIndex: Int, bones: List<Bone>, color: Int) {
        // todo could be much more efficient
        val dstBone = bones[boneIndex]
        if (dstBone.parentIndex < 0) return
        val srcBone = bones[dstBone.parentIndex]
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
        forLoopSafely(vertices.size, 3) { i ->
            tmp.set(vertices, i)
            tmp.sub(0f, 0.005f, 0f)
            tmp.mul(1.01f) // scale up slightly for better visibility
            mat.transform(tmp)
            tmp.add(srcPos)
            list.add(Vector3d(tmp))
        }
        // draw triangles
        forLoopSafely(list.size, 3) { i ->
            val triangle = DebugTriangle(list[i], list[i + 1], list[i + 2], color, 0f)
            DebugShapes.debugTriangles.add(triangle)
        }
    }

    override fun createInspector(
        inspected: List<Inspectable>,
        list: PanelListY,
        style: Style,
        getGroup: (nameDesc: NameDesc) -> SettingCategory
    ) {
        createInspector(list, style, getGroup)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("srcSkeleton", srcSkeleton)
        writer.writeFile("dstSkeleton", dstSkeleton)
        writer.writeStringList("dstBoneIndexToSrcName", dstBoneIndexToSrcName)
        writer.writeQuaternionfList("dstBoneRotations", dstBoneRotations)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "srcSkeleton" -> srcSkeleton = value as? FileReference ?: InvalidRef
            "dstSkeleton" -> dstSkeleton = value as? FileReference ?: InvalidRef
            "dstBoneIndexToSrcName" -> {
                if (value !is List<*>) return
                dstBoneIndexToSrcName.clear()
                dstBoneIndexToSrcName.addAll(value.filterIsInstance<String>())
            }
            "dstBoneRotations" -> {
                if (value !is List<*>) return
                dstBoneRotations.clear()
                dstBoneRotations.addAll(value.filterIsInstance<Quaternionf>())
            }
            else -> super.setProperty(name, value)
        }
    }

    override val approxSize get() = 20
}