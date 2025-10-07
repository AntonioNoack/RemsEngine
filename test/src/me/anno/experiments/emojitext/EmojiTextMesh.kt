package me.anno.experiments.emojitext

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshAttributes.color0
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.text.MeshTextComponent
import me.anno.ecs.components.text.SDFTextComponent
import me.anno.ecs.components.text.TextureTextComponent
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.fonts.Codepoints.codepoints
import me.anno.fonts.Font
import me.anno.fonts.mesh.MeshGlyphLayout
import me.anno.ui.base.components.AxisAlignment
import me.anno.utils.Color.toHexColor

fun main() {
    OfficialExtensions.initForTests()

    println("9⃣".codepoints().toList())
    println("9\uFE0F".codepoints().toList())
    println("9\uFE0F⃣".codepoints().toList())
    println(" 5\uFE0F⃣ ".codepoints().toList())

    // todo bug: our text sometimes has weird cyan pixels... why??

    val scene = Entity()
    testSceneWithUI("Emoji Mesh", scene) {

        val text = "Text 5\uFE0F⃣ \uD83C\uDDF5\uD83C\uDDF2|\uD83D\uDC4B\uD83C\uDFFD|❤\uFE0F"
        val font = Font("Verdana", 240f)

        val meshGroup = MeshGlyphLayout(font, text, 0f, Int.MAX_VALUE, true)
        val mesh = meshGroup.getOrCreateMesh()
        scene.add(MeshComponent(mesh))

        println("pos: ${mesh.getBounds()}")
        println("col: ${mesh.color0?.toList()?.distinct()?.map { it.toHexColor() }}")

        Entity("Texture", scene)
            .setPosition(0.0, -2.0, 0.0)
            .add(TextureTextComponent(text, font, AxisAlignment.MAX))

        Entity("SDF", scene)
            .setPosition(0.0, -4.0, 0.0)
            .add(SDFTextComponent(text, font.withSize(96f), AxisAlignment.MAX))

        Entity("Mesh", scene) // alignment & size are good
            .setPosition(0.0, -6.0, 0.0)
            .add(MeshTextComponent(text, font, AxisAlignment.MAX))
    }
}