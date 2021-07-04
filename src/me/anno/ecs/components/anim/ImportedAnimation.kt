package me.anno.ecs.components.anim

import com.jme3.anim.*

class ImportedAnimation : Animation() {

    // todo use jMonkey AnimClips?

    var joints = ArrayList<Joint>()

    class Joint {

        var positions = FloatArray(0)
        var rotations = FloatArray(0)
        var scales = FloatArray(0)

    }

    init {

        val clip = AnimClip("")
        val animFac = AnimFactory(10f, "name", 60f)
        // animFac.addKeyFrameRotation()
        // animFac.addTimeScale()
        // animFac.buildAnimation()

        val joint = com.jme3.anim.Joint("Hips")
        val armature = Armature(arrayOf(joint))
        armature.computeSkinningMatrices()

        val skinning = SkinningControl(armature)
        skinning.spatial // probably the node, which contains the geometry children
        // skinning.update(1f / 60f)
        // = skinning.controlUpdate()
        // =  wasMeshUpdated = false;
        //    armature.update();

        armature.update()

    }

}