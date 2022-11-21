package me.anno.tests.poisson

import me.anno.image.Image
import me.anno.image.ImageCPUCache
import me.anno.image.raw.IntImage
import me.anno.io.files.FileReference
import me.anno.maths.Maths.sq
import me.anno.utils.Clock
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.rgba
import org.apache.logging.log4j.LogManager
import kotlin.math.abs
import kotlin.math.sqrt

interface PoissonReconstruction<V> {

    fun V.next(): V

    fun V.dx(dst: V = next()): V

    fun V.dy(dst: V = next()): V

    fun V.blur(sizeOf1Sigma: Float, tmp: V = next(), dst: V = next()): V {
        blurX(sizeOf1Sigma, tmp)
        tmp.blurY(sizeOf1Sigma, dst)
        return dst
    }

    fun V.blurX(sizeOf1Sigma: Float, dst: V = next()): V

    fun V.blurY(sizeOf1Sigma: Float, dst: V = next()): V

    fun V.blurXSigned(sizeOf1Sigma: Float, dst: V = next()): V

    fun V.blurYSigned(sizeOf1Sigma: Float, dst: V = next()): V

    fun V.added(b: V, c: V, dst: V = next()): V

    fun V.absDifference(other: V, dst: V = next()): V

    /**
     * for debugging only
     * */
    fun V.added(m: Float, n: Float, dst: V = next()): V

    // compute proper poisson reconstruction
    fun iterate(src: V, dst: V, dx: V, dy: V, blurred: V): V

    fun V.writeInto(dst: FileReference) {
        toImage().write(dst)
    }

    fun V.toImage(): Image

    fun V.copyInto(dst: V): V

    // original.width, original.height, 5.0,
    //            dst.getChild("trick-to-result.mp4"), 50
    fun V.renderVideo(iterations: Int, dst: FileReference, run: (Long) -> V)

    fun execute(original: V, sizeOf1Sigma: Float, iterations: Int, dst: FileReference, ext: String, calculateErrors: Boolean = true) {

        val clock = Clock()

        original.writeInto(dst.getChild("original$ext"))

        val dx = original.dx()
        val dy = original.dy()

        clock.stop("delta")

        val blurred = original.blur(sizeOf1Sigma)

        // would need to be blurred, and not blurred at the same time...
        // how?
        val bdx = dx.blurXSigned(sizeOf1Sigma) // .blurY(sigma)
        val bdy = dy.blurYSigned(sizeOf1Sigma) // .blurX(sigma)

        fun writeDiff(img1: Image, img2: Image, name: String) {
            val err = IntImage(img2.width, img2.height, false)
            for (y in 0 until img2.height) {
                for (x in 0 until img2.width) {
                    val c0 = img2.getRGB(x, y)
                    val c1 = img1.getRGB(x, y)
                    err.setRGB(
                        x, y, rgba(
                            abs(c0.r() - c1.r()),
                            abs(c0.g() - c1.g()),
                            abs(c0.b() - c1.b()),
                            255
                        )
                    )
                }
            }
            err.write(dst.getChild(name))
        }

        fun calcDiff(img1: Image, img2: Image): Double {
            var error = 0.0
            for (y in 0 until img2.height) {
                for (x in 0 until img2.width) {
                    val c0 = img2.getRGB(x, y)
                    val c1 = img1.getRGB(x, y)
                    error += sq(c0.r() - c1.r()) + sq(c0.g() - c1.g()) + sq(c0.b() - c1.b())
                }
            }
            return sqrt(error / (3 * img2.width * img2.height)) / 255f
        }


        clock.stop("convolve")

        val result = blurred
            .added(bdx, bdy)

        val img0 = if (calculateErrors) {
            ImageCPUCache[dst.getChild("original$ext"), false]!!
        } else null

        result.writeInto(dst.getChild("trick$ext"))

        if (calculateErrors) {
            val img1 = ImageCPUCache[dst.getChild("trick$ext"), false]!!
            writeDiff(img0!!, img1, "error-gaussian$ext")
        }

        clock.stop("error & result")

        blurred.writeInto(dst.getChild("blurred$ext"))

        val normalScale = 1f
        dx.added(normalScale, 0.5f).writeInto(dst.getChild("dx$ext"))
        dy.added(normalScale, 0.5f).writeInto(dst.getChild("dy$ext"))

        bdx.added(normalScale, 0.5f).writeInto(dst.getChild("bdx$ext"))
        bdy.added(normalScale, 0.5f).writeInto(dst.getChild("bdy$ext"))

        val tmp = result.next()
        result.copyInto(tmp)

        if (calculateErrors) {
           // LogManager.disableLogger("VideoCreator")
           // LogManager.disableLogger("OpenGLShader")
            println("Tricked:")
            println("iteration,error")
        }

        original.renderVideo(iterations, dst.getChild("trick-to-result.mp4")) {
            val dstI = if (it > 0L) {
                val srcI = if (it.and(1) == 1L) result else tmp
                val dstI = if (srcI == result) tmp else result
                iterate(srcI, dstI, dx, dy, blurred)
            } else result
            if (calculateErrors) println("$it,${calcDiff(img0!!, dstI.toImage())}")
            dstI
        }

        result.writeInto(dst.getChild("result-trick$ext"))

        if (calculateErrors) {
            val img1 = ImageCPUCache[dst.getChild("result-trick$ext"), false]!!
            writeDiff(img0!!, img1, "error-trick$ext")
        }

        blurred.copyInto(result)
        result.copyInto(tmp)

        if (calculateErrors) {
            println("Blurred:")
            println("iteration,error")
        }

        original.renderVideo(iterations, dst.getChild("blurred-to-result.mp4")) {
            val dstI = if (it > 0L) {
                val srcI = if (it.and(1) == 1L) result else tmp
                val dstI = if (srcI == result) tmp else result
                iterate(srcI, dstI, dx, dy, blurred)
            } else result
            if (calculateErrors) println("$it,${calcDiff(img0!!, dstI.toImage())}")
            dstI
        }

        result.writeInto(dst.getChild("result-blurred$ext"))

        if (calculateErrors) {
            val img2 = ImageCPUCache[dst.getChild("result-blurred$ext"), false]!! as IntImage
            writeDiff(img0!!, img2, "error-blurred$ext")
        }

        // a test of when src == dst, so the improvement gets passed along
        /*System.arraycopy(blurred.data, 0, result.data, 0, result.data.size)
    
        renderVideo2(
            original.width, original.height, 5.0,
            dst.getChild("fromBlurred2.mp4"), 50
        ) { if (it > 0L) iterate(result, result, dx, dy, blurred) else result }
    
        result.writeInto(dst.getChild("result-blurred2.jpg"))*/

        clock.stop("writing results")

    }

}