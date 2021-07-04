package me.anno.ecs.components.anim

class ImportedAnimation : Animation() {

    var joints = ArrayList<Joint>()

    class Joint {

        var positions = FloatArray(0)
        var rotations = FloatArray(0)
        var scales = FloatArray(0)

    }

}