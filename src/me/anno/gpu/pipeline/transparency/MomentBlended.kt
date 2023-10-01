package me.anno.gpu.pipeline.transparency

import me.anno.gpu.pipeline.Pipeline

// todo implement Moment-Based Order-Independent Transparency by Cedric Münstermann
class MomentBlended: TransparentPass() {

    override fun draw1(pipeline: Pipeline) {
        TODO("Not yet implemented")
    }

}


// todo there is per-framebuffer blending since OpenGL 4.0 😍
// todo and it somehow can be used for order-independent transparency
// (Weighted Blended Order-Independent Transparency by Morgan McGuire Louis Bavoil)
// glBlendFunci()
