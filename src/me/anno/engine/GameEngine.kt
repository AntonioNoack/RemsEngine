package me.anno.engine

object GameEngine {

    // todo most relevant todos:
    // todo test input events
    // todo - index all assets, sorted by type, of the current project
    // todo - use this indexed data to create search functions for materials, skeletons, ...

    // todo - this way to retargetings: just sort all assets for the matching skeleton

    var timeFactor = 1.0
    var scaledNanos = 0L
    var scaledTime = 0.0
    var scaledDeltaTime = 1.0

    // todo make everything more modular:
    //  - so it can be excluded from an export, e.g. assimp is probably not needed in the final build, nor pdf stuff
    //  - they can be plugins or mods
    // todo export screen:
    //  - would give you a checkbox-list, of what to include in your export

}