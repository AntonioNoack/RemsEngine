package me.anno.experiments.emojitext

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.text.TextureTextComponent
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.fonts.Font
import me.anno.fonts.mesh.TextMeshGroup
import me.anno.ui.base.components.AxisAlignment

fun main() {
    OfficialExtensions.initForTests()

    // todo bug: our text sometimes has weird cyan pixels... why??

    val scene = Entity()
    testSceneWithUI("Emoji Mesh", scene) {
        val text = "\uD83C\uDDF5\uD83C\uDDF2|\uD83D\uDC4B\uD83C\uDFFD|❤\uFE0F"
        val font = Font("Verdana", 240f)
        val meshGroup = TextMeshGroup(font, text, 0f, true)
        val mesh = meshGroup.getOrCreateMesh()
        scene.add(MeshComponent(mesh))

        Entity(scene)
            .setPosition(0.0, -1.0, 0.0)
            .add(TextureTextComponent(text, font, AxisAlignment.MAX))
    }
}