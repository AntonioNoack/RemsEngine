package me.anno.tests.ecs

import me.anno.ecs.components.anim.AnimTexture
import me.anno.ecs.components.anim.Retargeting
import me.anno.ecs.components.cache.AnimationCache
import me.anno.ecs.components.cache.SkeletonCache
import me.anno.engine.ECSRegistry
import me.anno.utils.OS

fun main() {
    // create a test texture, so we can see whether the texture is correctly created
    ECSRegistry.initWithGFX()
    val source = OS.downloads.getChild("3d/azeria/scene.gltf") // animated mesh file
    val skeletonSource = source.getChild("Skeleton.json")
    val animationsSources = source.getChild("animations").listChildren()!!
    val skeleton = SkeletonCache[skeletonSource]!!
    val animations = animationsSources.map { AnimationCache[it]!! }
    val texture = AnimTexture(skeleton)
    val retargeting = Retargeting()
    for (anim in animations.sortedBy { it.name }) {
        texture.addAnimation(anim, retargeting)
    }
    texture.texture!!.createImage(flipY = false, withAlpha = false)
        .write(OS.desktop.getChild("animTexture.png"))
}