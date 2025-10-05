package me.anno.experiments.emojitext

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.fonts.Font
import me.anno.fonts.mesh.TextMeshGroup

fun main() {
    OfficialExtensions.initForTests()

    // todo implement separating the emojis properly not by codepoints but by codepoint groups for TextMesh(Group)
    // todo bug: our text sometimes has weird cyan pixels... why??

    val scene = Entity()
    testSceneWithUI("Emoji Mesh", scene) {
        val text = "Folder \uD83D\uDCC1 123\n" +
                "\uD83E\uDD75: \uD83C\uDDF5\uD83C\uDDF2"
        val font = Font("Verdana", 120f)
        val meshGroup = TextMeshGroup(font, text, 0f, true)
        val mesh = meshGroup.getOrCreateMesh()
        scene.add(MeshComponent(mesh))
    }
}