package me.anno.ecs.components.anim

import me.anno.Time
import me.anno.animation.LoopingState
import me.anno.animation.Type
import me.anno.ecs.Entity
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
import me.anno.gpu.pipeline.Pipeline
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.serialization.SerializedProperty
import me.anno.language.translation.NameDesc
import me.anno.studio.Inspectable
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.SettingCategory
import me.anno.ui.input.EnumInput
import me.anno.ui.input.FloatVectorInput
import me.anno.utils.Color.hex32
import me.anno.utils.LOGGER
import me.anno.utils.structures.lists.UpdatingList
import me.anno.utils.types.Vectors
import org.joml.*
import kotlin.math.min
import kotlin.math.sqrt

class Retargeting : PrefabSaveable(), Renderable {

    var srcSkeleton: FileReference = InvalidRef
    var dstSkeleton: FileReference = InvalidRef

    // for each dstBone, which bone is responsible for the transform
    @HideInInspector
    @SerializedProperty("dstBoneIndexToSrcName")
    var dstBoneIndexToSrcName: Array<String>? = null

    @HideInInspector
    @SerializedProperty("dstBoneIndexToSrcName")
    var dstBoneRotations: Array<Quaternionf>? = null

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

    override fun fill(pipeline: Pipeline, entity: Entity, clickId: Int): Int {
        // different colors
        // todo show mapping clearer, maybe connecting lines
        // todo apply transform onto one of them, so we can check their matches better
        val sm = sampleModel
        val sa = AnimationCache[sampleAnimation]
        val srcSkeleton1 = SkeletonCache[srcSkeleton]
        val dstSkeleton1 = SkeletonCache[dstSkeleton]
        val transform = entity.transform.getDrawMatrix()
        if (showSampleAnimation && sa != null && sm != null && srcSkeleton1 != null && dstSkeleton1 != null) {
            if (showSampleMesh) {
                // show retargeted mesh
                // todo why is this one not animated???
                sm.fill(pipeline, entity, clickId)
            }
            if (srcPreviewData == null) srcPreviewData = Animation.PreviewData(srcSkeleton1, sa)
            fillSkeletonAnimated(pipeline, entity, clickId, srcPreviewData!!)
            val mappedAnimation = mappedAnimations.firstOrNull { it.first == sa }?.second
                ?: getMappedAnimation(sa, dstSkeleton1)
            if (mappedAnimation != null) {
                if (dstPreviewData == null) dstPreviewData = Animation.PreviewData(
                    dstSkeleton1, mappedAnimation
                )
                fillSkeletonAnimated(pipeline, entity, clickId, dstPreviewData!!)
            } else println("Missing mapped animation!!")
            val loop = LoopingState.PLAY_LOOP
            val frame = loop[Time.gameTime.toFloat(), sa.duration] * sa.numFrames / sa.duration
            drawBoneNames(srcSkeleton1, sa.getMappedMatrices(frame, tmpMapping0, srcSkeleton), transform, srcColor)
            drawBoneNames(dstSkeleton1, sa.getMappedMatrices(frame, tmpMapping1, dstSkeleton), transform, dstColor)
        } else {
            srcSkeleton1?.fill(pipeline, clickId, srcMat)
            dstSkeleton1?.fill(pipeline, clickId, dstMat)
        }
        return clickId + 1
    }

    private var srcPreviewData: Animation.PreviewData? = null
    private var dstPreviewData: Animation.PreviewData? = null
    private fun fillSkeletonAnimated(
        pipeline: Pipeline, entity: Entity, clickId: Int,
        previewData: Animation.PreviewData,
    ) {
        previewData.state.set(Time.gameTime.toFloat(), false)
        previewData.renderer.updateAnimState()
        previewData.renderer.fill(pipeline, entity, clickId)
    }

    private fun drawBoneNames(skeleton: Skeleton, matrices: Array<Matrix4x3f>?, transform: Matrix4x3d, color: Int) {
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
            println("identity mapping, so returning self")
            return src
        }
        println("adding mapping to list in ${hex32(System.identityHashCode(this))}")
        mappedAnimations.add(src to dst)
        return recalculate(src, dst)
    }

    fun recalculate(src: BoneByBoneAnimation, dst: BoneByBoneAnimation): BoneByBoneAnimation? {
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
        // when we set nothing, everything should be identity, right? yes, correct

        val srcBones = srcSkel.associateBy { it.name }
        val dstToSrc = dstBoneIndexToSrcName ?: return dst
        val tmpM = Matrix4x3f()
        val tmpV = Vector3f()
        val tmpQ = Quaternionf()
        val tmpS = Vector3f()
        // find base skeleton scale each, and then scale all bones
        val srcScaleSq = srcSkel.sumOf { it.bindPosition.lengthSquared().toDouble() } / srcSkel.size
        val dstScaleSq = dstSkel.sumOf { it.bindPosition.lengthSquared().toDouble() } / dstSkel.size
        val translationScale = sqrt(dstScaleSq / srcScaleSq).toFloat()
        for (dstBone in 0 until min(dstSkel.size, dstToSrc.size)) {
            val srcBone = srcBones[dstToSrc[dstBone]]
            if (srcBone == null) {
                // println("Skipping ${dstSkel[dstBone].name}, because it's unmapped")
                continue
            }
            var srcBone0: Bone? = null
            var lastValidDst = dstSkel[dstBone].parentId
            while (lastValidDst >= 0) {
                // check if this bone is mapped
                srcBone0 = srcBones[dstToSrc[lastValidDst]]
                if (srcBone0 != null) break // found valid bone :)
                lastValidDst = dstSkel[lastValidDst].parentId // next ancestor
            }
            // println("Mapping ${dstSkel[dstBone].name} to ${srcBone.name}, relative-root: ${srcBone0?.name}")
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
                        src.getScale(frame, srcBone1, tmpS)
                    } else {
                        if (index == 1) {
                            tmpM.translationRotateScale(tmpV, tmpQ, tmpS)
                        }
                        src.getTranslation(frame, srcBone1, tmpV)
                        src.getRotation(frame, srcBone1, tmpQ)
                        src.getScale(frame, srcBone1, tmpS)
                        // correct order? should be
                        tmpM.translate(tmpV)
                        tmpM.rotate(tmpQ)
                        tmpM.scale(tmpS)
                    }
                    index++
                }
                applyBone(srcBone)
                if (index > 1) { // we used the matrix, so extract the result
                    tmpM.getTranslation(tmpV)
                    tmpM.getUnnormalizedRotation(tmpQ)
                    tmpM.getScale(tmpS)
                }
                // tmpV.mul(translationScale)
                dst.setTranslation(frame, dstBone, tmpV)
                dst.setRotation(frame, dstBone, tmpQ)
                dst.setScale(frame, dstBone, tmpS)
            }
        }
        return dst
    }

    @DebugAction
    fun defineDefaultMapping() {
        val prefab = prefab ?: return
        Retargetings.defineDefaultMapping(
            SkeletonCache[srcSkeleton]!!,
            SkeletonCache[dstSkeleton]!!,
            prefab
        )
        invalidate()
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
            var dstBoneMapping = dstBoneIndexToSrcName
            if (dstBoneMapping == null) dstBoneMapping = Array(dstSkeleton.bones.size) { noBoneMapped }
            else if (dstBoneMapping.size != dstSkeleton.bones.size) {
                dstBoneMapping = Array(dstSkeleton.bones.size) { dstBoneMapping!!.getOrNull(it) ?: noBoneMapped }
            }
            var dstBoneRotations = dstBoneRotations
            if (dstBoneRotations == null) dstBoneRotations = Array(dstSkeleton.bones.size) { Quaternionf() }
            else if (dstBoneRotations.size != dstSkeleton.bones.size) {
                dstBoneRotations = Array(dstSkeleton.bones.size) { dstBoneRotations!!.getOrNull(it) ?: Quaternionf() }
            }
            this.dstBoneIndexToSrcName = dstBoneMapping
            this.dstBoneRotations = dstBoneRotations
            // for each bone, create an enum input
            for ((i, bone) in dstSkeleton.bones.withIndex()) {
                // todo change color based on whether it is set to a valid bone
                val options = UpdatingList(100) {
                    // filter elements such that only bones below our ancestors are allowed
                    // find our first ancestor with a mapping
                    var ancestor = bone
                    var ancestorMap = noBoneMapped
                    while (ancestorMap == noBoneMapped) {
                        val parentId = ancestor.parentId
                        ancestorMap = dstBoneMapping.getOrNull(parentId) ?: break
                        ancestor = dstSkeleton.bones[parentId]
                    }
                    // now filter
                    val availableBones = if (ancestorMap == noBoneMapped) srcSkeleton.bones
                    else srcSkeleton.bones.filter { it.hasBoneInHierarchy(ancestorMap, srcSkeleton.bones) }
                    availableBones.map { NameDesc(it.name) } + listOf(NameDesc(noBoneMapped))
                }
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
                            val srcId = srcSkeleton.bones.firstOrNull { it.name == value.name }?.id
                            if (srcId != null) showBoneInUI(srcId, srcSkeleton.bones, srcColor1)
                        }
                    }
                }.setChangeListener { value, _, _ ->
                    dstBoneMapping[i] = value
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
                    val type = Type.ROT_YXZ.withDefault(Vector3f(0f))
                    FloatVectorInput(
                        "Correction", "${bone.name}-rot",
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
        val dstBoneMapping = dstBoneIndexToSrcName
        if (dstBoneMapping != null) {
            writer.writeStringArray("dstBoneIndexToSrcName", dstBoneMapping)
        }
        val dstBoneRotations = dstBoneRotations
        if (dstBoneRotations != null) {
            writer.writeQuaternionfArray("dstBoneRotations", dstBoneRotations)
        }
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
            "dstBoneIndexToSrcName" -> dstBoneIndexToSrcName = values
            else -> super.readStringArray(name, values)
        }
    }

    override fun readQuaternionfArray(name: String, values: Array<Quaternionf>) {
        when (name) {
            "dstBoneRotations" -> dstBoneRotations = values
            else -> super.readQuaternionfArray(name, values)
        }
    }

    override val className: String get() = "Retargeting"
    override val approxSize get() = 20
}