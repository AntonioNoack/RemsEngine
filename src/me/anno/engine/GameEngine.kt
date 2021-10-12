package me.anno.engine

object GameEngine {

    // todo most relevant todos:
    // todo - playing mode, with input events
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

    // todo grand theft waifu:
    // todo shoulder half press = target, shoulder full press = shoot

}