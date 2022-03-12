package me.anno.ecs.components.shaders.sdf

class SDFTorus : SDFShape() {

    var outerRadius = 1.0f
    var innerRadius = 0.8f

    // float sdTorus( vec3 p, vec2 t ){
    //    return length( vec2(length(p.xz)-t.x,p.y) )-t.y;
    //}

}