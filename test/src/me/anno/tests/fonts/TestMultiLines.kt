package me.anno.tests.fonts

import me.anno.ecs.Entity
import me.anno.ecs.components.text.MeshTextComponent
import me.anno.ecs.components.text.SDFTextComponent
import me.anno.ecs.components.text.TextureTextComponent
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.fonts.Font
import me.anno.ui.base.components.AxisAlignment

fun main() {
    OfficialExtensions.initForTests()

    val scene = Entity()
    testSceneWithUI("Emoji Mesh", scene) {

        val text = "First Line\n" +
                "2nd\n" +
                "3rd Line"
        val font = Font("Verdana", 240f)

        Entity("Texture", scene)
            .setPosition(0.0, 6.0, 0.0)
            .add(TextureTextComponent(text, font, AxisAlignment.MAX))

        Entity("SDF", scene)
            .setPosition(0.0, 0.0, 0.0)
            .add(SDFTextComponent(text, font.withSize(96f), AxisAlignment.MAX))

        Entity("Mesh", scene) // alignment & size are good
            .setPosition(0.0, -6.0, 0.0)
            .add(MeshTextComponent(text, font, AxisAlignment.MAX))
    }
}