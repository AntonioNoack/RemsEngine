package me.anno.ecs.components.anim

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.components.cache.SkeletonCache
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.base.BaseWriter
import me.anno.maths.Maths.mix
import me.anno.utils.OS.downloads
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Floats.f3s
import org.joml.Matrix4x3f
import org.joml.Quaternionf
import org.joml.Vector3f

/**
 * a basic animation for characters, where animated translation only exists at the root node
 * this can be easier retargeted and customized than ImportedAnimation
 * */
class BoneByBoneAnimation : Animation() {

    val globalTransform = Matrix4x3f()
    val globalInvTransform = Matrix4x3f()

    var boneCount = 0
    var frameCount = 0

    var translations: FloatArray? = null // Array(frameCount) { Vector3f() }
    var rotations: FloatArray? = null // Array(frameCount * boneCount) { Quaternionf() }

    override val numFrames get() = frameCount

    fun getRootMotion(frame: Int, bone: Int, dst: Vector3f = Vector3f()): Vector3f {
        val index = 3 * (frame * boneCount + bone)
        val rm = translations!!
        return dst.set(rm[index], rm[index + 1], rm[index + 2])
    }

    fun getRootMotion(fraction: Float, frame0: Int, frame1: Int, bone: Int, dst: Vector3f = Vector3f()): Vector3f {
        val index0 = 3 * (frame0 * boneCount + bone)
        val index1 = 3 * (frame1 * boneCount + bone)
        val rm = translations!!
        return dst.set(
            mix(rm[index0], rm[index1], fraction),
            mix(rm[index0 + 1], rm[index1 + 1], fraction),
            mix(rm[index0 + 2], rm[index1 + 2], fraction)
        )
    }

    fun setRootMotion(frame: Int, bone: Int, v: Vector3f) {
        val index = 3 * (frame * boneCount + bone)
        val rm = translations!!
        rm[index + 0] = v.x
        rm[index + 1] = v.y
        rm[index + 2] = v.z
    }

    fun getRotation(frame: Int, bone: Int, dst: Quaternionf = Quaternionf()): Quaternionf {
        val index = 4 * (frame * boneCount + bone)
        val src = rotations!!
        return dst.set(src[index], src[index + 1], src[index + 2], src[index + 3])
    }

    fun getRotation(
        fraction: Float, frame0: Int, frame1: Int, bone: Int,
        dst: Quaternionf = Quaternionf()
    ): Quaternionf {
        val idx0 = 4 * (frame0 * boneCount + bone)
        val idx1 = 4 * (frame1 * boneCount + bone)
        val src = rotations!!
        val tmp = JomlPools.quat4f.borrow()
        dst.set(src[idx0], src[idx0 + 1], src[idx0 + 2], src[idx0 + 3])
        dst.slerp(tmp.set(src[idx1], src[idx1 + 1], src[idx1 + 2], src[idx1 + 3]), fraction)
        return dst
    }

    fun setRotation(frame: Int, bone: Int, v: Quaternionf) {
        val index = 4 * (frame * boneCount + bone)
        val dst = rotations!!
        dst[index] = v.x
        dst[index + 1] = v.y
        dst[index + 2] = v.z
        dst[index + 3] = v.w
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("boneCount", boneCount)
        writer.writeInt("frameCount", frameCount)
        writer.writeMatrix4x3f("globalTransform", globalTransform)
        writer.writeMatrix4x3f("globalInvTransform", globalInvTransform)
        writer.writeFloatArray("rootMotion", translations)
        writer.writeFloatArray("rotations", rotations)
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "boneCount" -> boneCount = value
            "frameCount" -> frameCount = value
            else -> super.readInt(name, value)
        }
    }

    override fun readMatrix4x3f(name: String, value: Matrix4x3f) {
        when (name) {
            "globalTransform" -> globalTransform.set(value)
            "globalInvTransform" -> globalInvTransform.set(value)
            else -> super.readMatrix4x3f(name, value)
        }
    }

    override fun readFloatArray(name: String, values: FloatArray) {
        when (name) {
            "rootMotion" -> translations = values
            "rotations" -> rotations = values
            else -> super.readFloatArray(name, values)
        }
    }

    // todo we could cache the matrices at the frames in question :)
    // todo or we could be more generic, and allow other skeletons, and their rough mapping...

    override fun getMatrices(entity: Entity?, time: Float, dst: Array<Matrix4x3f>): Array<Matrix4x3f>? {
        val skeleton = SkeletonCache[skeleton] ?: return null
        val (fraction, frameIndex0, frameIndex1) = calculateMonotonousTime(time, frameCount)
        val bones = skeleton.bones
        val rotation = Quaternionf()
        val translation = Vector3f()
        val localTransforms = Array(bones.size) { Matrix4x3f() }
        for (boneId in bones.indices) {
            val bone = bones[boneId]
            getRotation(fraction, frameIndex0, frameIndex1, boneId, rotation)
            if (boneId == 0) {
                // the root may be animated
                getRootMotion(fraction, frameIndex0, frameIndex1, boneId, translation)
            } else {
                // get the offset from the skeleton: from parent to this
                bone.relativeTransform.getTranslation(translation)
            }
            val localTransform = localTransforms[boneId]
                .identity()
                .translate(translation)
                .rotate(rotation)
            val parentLocalTransform =
                if (bone.parentId < 0) null
                else localTransforms[bone.parentId]
            parentLocalTransform?.mul(localTransform, localTransform)
            dst[boneId].identity()
                //.set(globalInvTransform)
                .mul(localTransform)
                .mul(bone.inverseBindPose)
            //.mul(globalTransform)
        }
        return dst
    }

    override fun getMatrices(index: Int, dst: Array<Matrix4x3f>): Array<Matrix4x3f>? {
        val skeleton = SkeletonCache[skeleton] ?: return null
        val bones = skeleton.bones
        val rotation = Quaternionf()
        val translation = Vector3f()
        val localTransforms = Array(bones.size) { Matrix4x3f() }
        for (boneId in bones.indices) {
            val bone = bones[boneId]
            getRotation(index, boneId, rotation)
            if (boneId == 0) {
                // the root may be animated
                getRootMotion(index, boneId, translation)
            } else {
                // get the offset from the skeleton: from parent to this
                bone.relativeTransform.getTranslation(translation)
            }
            val localTransform = localTransforms[boneId]
                .identity()
                .translate(translation)
                .rotate(rotation)
            val parentLocalTransform =
                if (bone.parentId < 0) null
                else localTransforms[bone.parentId]
            parentLocalTransform?.mul(localTransform, localTransform)
            dst[boneId].identity()
                //.set(globalInvTransform)
                .mul(localTransform)
                .mul(bone.inverseBindPose)
            //.mul(globalTransform)
        }
        return dst
    }

    override fun clone(): BoneByBoneAnimation {
        val clone = BoneByBoneAnimation()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as BoneByBoneAnimation
        clone.boneCount = boneCount
        clone.frameCount = frameCount
        clone.translations = translations
        clone.rotations = rotations
        clone.globalTransform.set(globalTransform)
        clone.globalInvTransform.set(globalInvTransform)
    }

    fun fromImported(anim: ImportedAnimation) {
        skeleton = anim.skeleton
        val skel = SkeletonCache[skeleton]!!
        boneCount = skel.bones.size
        frameCount = anim.frames.size
        val boneCount = skel.bones.size
        translations = FloatArray(boneCount * frameCount * 3)
        rotations = FloatArray(boneCount * frameCount * 4)
        val tmp = Vector3f()
        val tmp2 = Quaternionf()
        val tmp3 = Vector3f()
        for ((i, frame) in anim.frames.withIndex()) {
            // calculate rotations
            for ((j, bone) in skel.bones.withIndex()) {
                val pj = bone.parentId
                val pose = frame[j] // bind pose [world space] -> animated pose [world space]
                // todo transform translation into a space, where it is always nearly zero except for one root bone
                pose.getTranslation(tmp) // animation offset by root motion + animation rotations
                pose.getUnnormalizedRotation(tmp2)
                if (pj >= 0) {
                    val parentPose = frame[pj]
                    tmp.sub(parentPose.getTranslation(tmp3))
                    fun deltaRot(pos: Vector3f, rot: Quaternionf): Vector3f {
                        val px = pos.x
                        val py = pos.y
                        val pz = pos.z
                        return pos.rotate(rot).sub(px, py, pz)
                    }
                    if (i < 5 && j < 5) {
                        println("$i,$j -> ${pose.getTranslation(Vector3f())} -> $tmp, $tmp2, #${bone.name}")
                        // println("  ${format(bone.relativeTransform)}")
                        // println("  ${format(bone.originalTransform)}")
                        // this is the "original" matrix of the parent:
                        // println("  ${format(Matrix4x3f(bone.relativeTransform).mul(Matrix4x3f(bone.originalTransform)))}")
                        // println("  ${deltaRot(bone.relativeTransform.getTranslation(Vector3f()), tmp2)}")
                        // println("  ${bone.bindPose}")

                        // these matrices and up to four of them combined, are not enough to recreate the translation :(
                        /*val parent = skel.bones[pj]
                        val matrices0 = listOf(
                            bone.relativeTransform, Matrix4x3f(bone.relativeTransform).invert(),
                            bone.originalTransform, Matrix4x3f(bone.originalTransform).invert(),
                            bone.bindPose, bone.inverseBindPose,
                            Matrix4x3f().rotate(tmp2), Matrix4x3f().rotate(tmp2).invert(),
                            // pose, Matrix4x3f(pose).invert(), not available; this is what we try to reconstruct

                            parent.relativeTransform, Matrix4x3f(parent.relativeTransform).invert(),
                            parent.originalTransform, Matrix4x3f(parent.originalTransform).invert(),
                            parent.bindPose, parent.inverseBindPose,
                            parentPose, Matrix4x3f(parentPose).invert(),
                        )

                        val matrices = listOf(Matrix4x3f()) +
                                matrices0 +
                                matrices0.map { Matrix4x3f(it).setTranslation(0f,0f,0f) }.filter { !it.isIdentity() } +
                                matrices0.map { Matrix4x3f().setTranslation(it.getTranslation(Vector3f())) }.filter { !it.isIdentity() }

                        var bestP = Vector3f()
                        var bestL = Float.POSITIVE_INFINITY
                        var k = 0
                        var x = 0
                        val x0 = matrices.cross(matrices)
                        for ((x1, x2) in x0.cross(x0)) {
                            val m = Matrix4x3f(x1.first).mul(x1.second).mul(x2.first).mul(x2.second)
                            val p0 = m.transformPosition(Vector3f(tmp))
                            val l = p0.length()
                            if (l < bestL) {
                                bestL = l
                                bestP = p0
                                x = k
                            }
                            k++
                        }
                        println("  ${tmp.length()} -$x/$k> $bestL $bestP")*/

                    }
                }
                setRootMotion(i, j, tmp)
                setRotation(i, j, tmp2)
            }
            if (i < 5) println()
        }
    }

    fun toImported(anim: ImportedAnimation = ImportedAnimation()): ImportedAnimation {
        anim.skeleton = skeleton
        val skel = SkeletonCache[skeleton]!!
        val tmp = Vector3f()
        val tmp2 = Quaternionf()
        val tmp3 = Vector3f()
        anim.frames = Array(frameCount) { i ->
            val matrices = Array(skel.bones.size) { Matrix4x3f() }
            for ((j, bone) in skel.bones.withIndex()) {
                val pj = bone.parentId
                getRootMotion(i, j, tmp)
                if (pj >= 0) {
                    tmp.add(matrices[pj].getTranslation(tmp3))
                }
                matrices[j]
                    .rotate(getRotation(i, j, tmp2))
                    .setTranslation(tmp)
            }
            matrices
        }
        return anim
    }

    override val className = "BoneByBoneAnimation"

    companion object {

        fun format(m: Matrix4x3f): String {
            return "[[${m.m00.f3s()} ${m.m01.f3s()} ${m.m02.f3s()}] " +
                    "[${m.m10.f3s()} ${m.m11.f3s()} ${m.m12.f3s()}] " +
                    "[${m.m20.f3s()} ${m.m21.f3s()} ${m.m22.f3s()}] " +
                    "[${m.m30}f, ${m.m31}f, ${m.m32}f]]"
        }

        @JvmStatic
        fun main(args: Array<String>) {

            // todo ImportedAnimation -> BoneByBone -> ImportedAnimation
            // if this works, and animations are stored in global space, we're probably good to continue :)

            val imported = PrefabCache.getPrefabInstance(
                downloads.getChild("3d/azeria/scene.gltf/animations").listChildren()!!.first()
            ) as ImportedAnimation
            val boneByBone = BoneByBoneAnimation()
            boneByBone.fromImported(imported)

            val exported = boneByBone.toImported()

            for (i in 0 until 3) {
                println()
                println(imported.frames[i].joinToString { format(it) })
                println(boneByBone.translations!!.joinToString())
                println(exported.frames[i].joinToString { format(it) })
            }

            // testUI { testScene(exported) }

            Engine.requestShutdown()

        }

    }

}