package me.anno.ecs.components.anim

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.ecs.components.anim.BoneEmbeddings.getWEs
import me.anno.ecs.components.anim.BoneEmbeddings.helperWE
import me.anno.ecs.components.cache.SkeletonCache
import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.OS.downloads
import me.anno.utils.structures.lists.Lists.firstOrNull2
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4x3f
import kotlin.math.min

class Retargeting : NamedSaveable() {

    companion object {

        private val LOGGER = LogManager.getLogger(Retargeting::class)
        private val cache = CacheSection("Retargeting")
        private const val timeout = 10_000L

        fun getRetargeting(srcSkeleton: FileReference, dstSkeleton: FileReference): Retargeting? {
            if (srcSkeleton == dstSkeleton) return null
            val data = cache.getEntry(
                Pair(srcSkeleton, srcSkeleton.lastModified),
                Pair(dstSkeleton, dstSkeleton.lastModified),
                timeout,
                false
            ) { k1, k2 ->
                val ret = Retargeting()
                ret.srcSkeleton = k1.first
                ret.dstSkeleton = k2.first
                CacheData(ret)
            } as CacheData<*>
            return data.value as Retargeting
        }

        @JvmStatic
        fun main(args: Array<String>) {
            // find two human meshes with different skeletons
            val human = downloads.getChild("3d/trooper fbx/silly_dancing.fbx")
            val animation = downloads.getChild("fbx/walk.fbx/animations").listChildren()!!.first()
            // todo and render the retargeted result

        }

    }

    // todo find retargeting from the skeleton to the new skeleton...
    // todo if not found, generate it automatically, and try our best to do it perfectly
    // todo retargeting probably needs to include a max/min-angle and angle multiplier and change of base matrices
    // (or all animations need to be defined in some common animation space)

    var srcSkeleton: FileReference = InvalidRef
    var dstSkeleton: FileReference = InvalidRef

    var srcBoneMapping = emptyList<String>()

    // mapping matrices: [srcBoneSpace -> dstBoneSpace] & inverse
    // [srcBoneSpace -> dstBoneSpace] = [srcBoneSpace -> worldSpace] * [worldSpace -> dstBoneSpace]
    var srcToDstM = emptyArray<Matrix4x3f>()
    var dstToSrcM = emptyArray<Matrix4x3f>()

    var isValid = false

    val isIdentityMapping get() = srcSkeleton == dstSkeleton

    // mapping[dstBoneId] is srcBoneId or -1
    var dstToSrc = IntArray(0)

    fun validate() {
        synchronized(this) {
            if (isValid) return
            // calculate all the indices
            val srcSkeleton = SkeletonCache[srcSkeleton]!!
            val dstSkeleton = SkeletonCache[dstSkeleton]!!
            val srcBones = srcSkeleton.bones
            val srcBoneNames = srcBones.map { it.name }
            val srcMap = srcBoneNames.withIndex().associate { it.value to it.index }
            val size = dstSkeleton.bones.size
            if (dstToSrc.size != size) {// no need to reallocate
                dstToSrc = IntArray(size)
                dstToSrcM = Array(size) { Matrix4x3f() }
                srcToDstM = Array(size) { Matrix4x3f() }
            }
            if (srcBoneMapping.size != size) {
                val newNames = ArrayList<String>(size)
                newNames.addAll(srcBoneMapping.subList(0, min(srcBoneMapping.size, size)))
                for (index in newNames.size until size) {
                    val mapping = findMapping(srcSkeleton, dstSkeleton, index)
                    LOGGER.debug("Mapped $mapping to ${dstSkeleton.bones[index].name}")
                    newNames.add(mapping)
                }
                srcBoneMapping = newNames
            }
            LOGGER.info("Total Mapping: ${this.srcBoneMapping}")
            val dstToSrcM = dstToSrcM
            val srcToDstM = srcToDstM
            for (dstBoneIndex in 0 until size) {
                val srcBoneIndex = srcMap[this.srcBoneMapping[dstBoneIndex]]
                if (srcBoneIndex != null) {
                    // todo local bone spaces could be modified to allow T pose -> A pose retargetings
                    // define retargeting transform
                    val srcBone = srcSkeleton.bones[srcBoneIndex]
                    val dstBone = dstSkeleton.bones[dstBoneIndex]
                    val dstToSrcMi = dstToSrcM[dstBoneIndex]
                    val srcToDstMi = srcToDstM[dstBoneIndex]
                    // todo test this or sth similar; is incorrect
                    // idea: transform the animation matrix = an offset matrix, so be transformed from src space to dst space
                    val s0 = srcBone.bindPose
                    val s1 = srcBone.inverseBindPose
                    val d0 = dstBone.bindPose
                    val d1 = dstBone.inverseBindPose
                    // todo this will include a scale when the model is scaled
                    dstToSrcMi.set(d0).mul(s1)
                    srcToDstMi.set(dstToSrcMi).invert() // can be simplified in the future, when dstToSrcMi is correct
                }
                dstToSrc[dstBoneIndex] = srcBoneIndex ?: -1
            }
            isValid = true
        }
    }

    /**
     * given a bone index in dstSkeleton, find the name of the bone in srcSkeleton;
     * or empty if nothing matches
     * */
    fun findMapping(srcSkeleton: Skeleton, dstSkeleton: Skeleton, dstIndex: Int): String {
        val dstBone = dstSkeleton.bones[dstIndex]
        val name = dstBone.name
        val srcMatch0 = srcSkeleton.bones.firstOrNull2 { it.name == name }
        if (srcMatch0 != null) return srcMatch0.name
        val srcMatch1 = srcSkeleton.bones.firstOrNull2 { it.name.equals(name, true) }
        if (srcMatch1 != null) return srcMatch1.name
        // if not found, find best match
        // replace synonymous terms... upper leg<->pelvis, hip<->pelvis,
        // word embeddings for best match? :)
        val srcEmbeddings = getWEs(srcSkeleton)
        val dstEmbedding = getWEs(dstSkeleton)[dstIndex] ?: return ""
        // to do hierarchy for matches? :)
        val index = helperWE.find(srcEmbeddings, dstEmbedding, 0f)
        if (index < 0) return ""
        return srcSkeleton.bones[index].name
    }

    fun invalidate() {
        isValid = false
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("srcSkeleton", srcSkeleton)
        writer.writeFile("dstSkeleton", dstSkeleton)
        writer.writeStringArray("dstNames", srcBoneMapping.toTypedArray())
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
            "dstNames" -> {
                srcBoneMapping = values.toList()
                invalidate()
            }
            else -> super.readStringArray(name, values)
        }
    }

    override val className: String = "Retargeting"
    override val approxSize: Int = 20

}