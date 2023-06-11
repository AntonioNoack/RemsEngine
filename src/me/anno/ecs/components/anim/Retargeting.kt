package me.anno.ecs.components.anim

import me.anno.animation.LoopingState
import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter
import me.anno.io.config.ConfigBasics
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.mesh.assimp.Bone
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.LOGGER
import me.anno.utils.OS.downloads
import me.anno.utils.structures.tuples.LongPair
import org.joml.Matrix4x3f
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.min
import kotlin.math.sqrt

class Retargeting : NamedSaveable() {

    companion object {

        private val cache = CacheSection("Retargeting")
        private const val timeout = 500_000L

        fun getRetargeting(srcSkeleton: FileReference, dstSkeleton: FileReference): Retargeting? {
            if (srcSkeleton == dstSkeleton) return null
            val data = cache.getEntry(
                Pair(srcSkeleton, dstSkeleton),
                LongPair(srcSkeleton.lastModified, dstSkeleton.lastModified),
                timeout,
                false
            ) { k12, _ ->

                // todo hash skeleton instead of skeleton path
                val hash1 = srcSkeleton.hashCode
                val hash2 = dstSkeleton.hashCode
                // database, which stores bone assignments for a project
                val config = "retargeting-$hash1-$hash2.json"
                val config1 = ConfigBasics.getConfigFile(config)

                LOGGER.debug("Getting retargeting $k12")

                val ret: Retargeting
                if (config1.exists) {
                    ret = TextReader.readFirstOrNull<Retargeting>(config1, InvalidRef, true)
                        ?: Retargeting()
                } else {
                    ret = Retargeting()
                    ret.srcSkeleton = k12.first
                    ret.dstSkeleton = k12.second
                    config1.getParent()?.tryMkdirs()
                    config1.writeText(TextWriter.toText(ret, InvalidRef))
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
            // todo target stands still... were no bones mapped?
            testUI {
                // fbx animation seems to be broken for the trooper... probably Assimps fault or incorrect decoding from our side
                val testRetargeting = true
                ECSRegistry.initMeshes()
                // find two human meshes with different skeletons
                val file1 = downloads.getChild("3d/trooper gltf/scene.gltf")
                val file2 = downloads.getChild("fbx/Walking.fbx")
                val animation = (if (testRetargeting) file2 else file1).getChild("animations").listChildren()!!.first()
                val mesh = AnimRenderer()
                mesh.mesh = file1
                mesh.skeleton = file1.getChild("Skeleton.json")
                mesh.animations = listOf(AnimationState(animation, 1f, 0f, 1f, LoopingState.PLAY_LOOP))
                testScene(mesh)
            }
        }

    }

    var srcSkeleton: FileReference = InvalidRef
    var dstSkeleton: FileReference = InvalidRef

    // for each dstBone, which bone is responsible for the transform
    var dstBoneMapping: Array<String>? = null

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