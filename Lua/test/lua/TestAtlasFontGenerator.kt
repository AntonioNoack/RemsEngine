package lua

import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.image.ImageCache
import me.anno.image.raw.IntImage
import javax.imageio.ImageIO

/**
 * The Lua package currently doesn't depend on the JVM module, so use it to test the new AtlasFontGenerator
 * */
fun main() {
    OfficialExtensions.initForTests()
    // register image reader, so we can test the AtlasFontGenerator
    ImageCache.registerDirectStreamReader("png,jpg") { src ->
        val image = ImageIO.read(src)
        val intImage = IntImage(image.width, image.height, image.colorModel.hasAlpha())
        image.getRGB(0, 0, image.width, image.height, intImage.data, intImage.offset, intImage.stride)
        intImage
    }
    testSceneWithUI("AtlasFontGenerator", flatCube)
}