package me.anno.ecs.components.shaders.sdf

class SDFGroup : SDFComponent() {

    var smoothness = 0f

    enum class Merge {
        UNION, // A or B
        INTERSECTION, // A and B
        DIFFERENCE1, // A \ B
        DIFFERENCE2, // B \ A
        DIFFERENCE_SYM, // A xor B
    }

}