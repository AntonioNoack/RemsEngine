package me.anno.engine

object GameEngine {

    // todo most relevant todos:
    // done - playing mode, with input events
    // todo test input events
    // done - drop things into scene folder -> create assets
    // todo - index all assets, sorted by type, of the current project
    // todo - use this indexed data to create search functions for materials, skeletons, ...

    // done controller test with input system

    // done - hot prefab reloading

    // todo - this way to retargetings: just sort all assets for the matching skeleton

    var timeFactor = 1.0
    var scaledNanos = 0L
    var scaledTime = 0.0
    var scaledDeltaTime = 1.0

    // todo sky cubemap
    // todo sky shaders

    // todo recursive ray-tracing like convergence rendering for photo-realistic results
    //  - close patches are shaded the same way, modulated by their PBR properties (sharing the same cubemap)
    //  - = a lot of cubemaps for every section of the image + pixel clustering for assignment of cubemap-rendering-spots
    //  - more distant cubemaps need less resolution (pixel buffers + geometry)


}