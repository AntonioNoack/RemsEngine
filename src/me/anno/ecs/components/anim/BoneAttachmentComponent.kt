package me.anno.ecs.components.anim

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnChangeStructure
import me.anno.ecs.systems.OnUpdate
import me.anno.utils.pooling.JomlPools

/**
 * Controls its entity such that it follows the bone of another entity's AnimMeshComponent.
 * */
class BoneAttachmentComponent() : Component(), OnUpdate, OnChangeStructure {

    constructor(boneName: String, animMeshComponent: AnimMeshComponent) : this() {
        this.boneName = boneName
        this.animMeshComponent = animMeshComponent
    }

    var boneName = ""

    // todo make this look good / selectable / draggable in editor from our scene
    // todo if we hover over its UI input, show the entry highlighted in ECSTreeView
    @Type("AnimMeshComponent/SameSceneRef")
    var animMeshComponent: AnimMeshComponent? = null

    @HideInInspector
    var bone: Bone? = null

    override fun onChangeStructure(entity: Entity) {
        findBone()
    }

    @DebugAction
    fun findBone() {
        val meshSkeleton = animMeshComponent?.getMesh()?.skeleton
        bone = SkeletonCache.getEntry(meshSkeleton).waitFor()
            ?.bones?.firstOrNull { it.name == boneName }
    }

    override fun onUpdate() {
        val target = entity
        val bone = bone
        val entity = animMeshComponent?.entity
        lastWarning = if (target != null && bone != null && entity != null && entity !== target) {
            val offsetMatrix = animMeshComponent?.getMatrix(bone.index)
            if (target.parent === entity.parent) {
                // optimization: if they have the same parent, save a matrix-inverse by using setLocal() instead of setGlobal()
                val tmp = JomlPools.mat4x3m.borrow()
                val animGlobal = entity.transform.getLocalTransform(tmp)
                // apply animation, if is animated
                val newLocal = if (offsetMatrix != null) animGlobal.mul(offsetMatrix) else animGlobal
                val newLocal2 = newLocal.mul(bone.bindPose) // move us to actual bone
                target.transform.setLocal(newLocal2)
            } else { // normal path
                val animGlobal = entity.transform.getDrawMatrix()
                val tmp = target.transform.globalTransform // overridden anyway
                // apply animation, if is animated
                val newGlobal = if (offsetMatrix != null) animGlobal.mul(offsetMatrix, tmp) else animGlobal
                newGlobal.mul(bone.bindPose) // move us to actual bone
                target.transform.setGlobal(newGlobal)
            }
            null // all is fine
        } else "Incomplete setup"
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is BoneAttachmentComponent) return
        dst.boneName = boneName
        dst.bone = bone
        dst.animMeshComponent = getInClone(animMeshComponent, dst)
    }
}