package me.anno.ecs.components.anim

import me.anno.ecs.Component
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabSaveable

/**
 * Controls its entity such that it follows the bone of another entity's AnimMeshComponent.
 * */
class BoneAttachmentComponent() : Component() {

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

    override fun onCreate() {
        super.onCreate()
        findBone()
    }

    @DebugAction
    fun findBone() {
        bone = SkeletonCache[animMeshComponent?.skeleton]
            ?.bones?.firstOrNull { it.name == boneName }
    }

    override fun onUpdate(): Int {
        val target = entity
        val bone = bone
        val entity = animMeshComponent?.entity
        lastWarning = if (target != null && bone != null && entity != null && entity !== target) {
            val offsetMatrix = animMeshComponent?.getMatrix(bone.id)
            if (target.parent === entity.parent) { // optimization: if they have the same parent, save a matrix-inverse by using setLocal() instead of setGlobal()
                val animGlobal = entity.transform.localTransform
                val tmp = target.transform.localTransform // overridden anyway
                // apply animation, if is animated
                val newLocal = if (offsetMatrix != null) animGlobal.mul(offsetMatrix, tmp) else animGlobal
                newLocal.mul(bone.bindPose) // move us to actual bone
                target.transform.setLocal(newLocal)
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
        return 1
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as BoneAttachmentComponent
        dst.boneName = boneName
        dst.bone = bone
        dst.animMeshComponent = getInClone(animMeshComponent, dst)
    }

    override val className: String
        get() = "AttachToBoneComponent"
}