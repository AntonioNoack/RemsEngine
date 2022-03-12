package me.anno.ecs.components.shaders.sdf

import org.joml.Vector3f

class SDFBox : SDFShape() {

    var halfExtends = Vector3f(1f)

    // float sdBox( vec3 p, vec3 b ){
    //    vec3 d = abs(p) - b;
    //    return min(max(d.x,max(d.y,d.z)),0.0) + length(max(d,0.0));
    //}

}