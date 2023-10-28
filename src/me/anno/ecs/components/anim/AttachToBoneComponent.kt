package me.anno.ecs.components.anim

import me.anno.ecs.Component
import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabSaveable

/**
 * Controls its entity such that it follows the bone of another entity's AnimRenderer.
 * */
class AttachToBoneComponent() : Component() {

    constructor(boneName: String, animRenderer: AnimRenderer) : this() {
        this.boneName = boneName
        this.animRenderer = animRenderer
    }

    var boneName = ""

    // todo make this look good / selectable / draggable in editor from our scene
    @Type("AnimRenderer/PrefabSaveable")
    var animRenderer: AnimRenderer? = null

    @HideInInspector
    var bone: Bone? = null

    override fun onCreate() {
        super.onCreate()
        findBone()
    }

    fun findBone() {
        bone = SkeletonCache[animRenderer?.skeleton]
            ?.bones?.firstOrNull { it.name == boneName }
    }

    override fun onUpdate(): Int {
        val target = entity
        val bone = bone
        val entity = animRenderer?.entity
        lastWarning = if (target != null && bone != null && entity != null && entity !== target) {
            val offsetMatrix = animRenderer?.getMatrix(bone.id)
            val animGlobal = entity.transform.getDrawMatrix()
            val tmp = target.transform.globalTransform // overridden anyway
            // apply animation, if is animated
            val newGlobal = if (offsetMatrix != null) animGlobal.mul(offsetMatrix, tmp) else animGlobal
            newGlobal.mul(bone.bindPose) // move us to actual bone
            target.transform.setGlobal(newGlobal)
            null // all is fine
        } else "Incomplete setup"
        return 1
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as AttachToBoneComponent
        dst.boneName = boneName
        dst.bone = bone
        dst.animRenderer = getInClone(animRenderer, dst)
    }

    override val className: String
        get() = "AttachToBoneComponent"
}