package me.anno.tests.poisson

import me.anno.io.files.FileReference
import me.anno.utils.Clock

interface Poisson<V> {

    fun V.next(): V

    fun V.dx(dst: V = next()): V

    fun V.dy(dst: V = next()): V

    fun V.blur(sigma: Float, tmp: V = next(), dst: V = next()): V {
        blurX(sigma, tmp)
        tmp.blurY(sigma, dst)
        return dst
    }

    fun V.blurX(sigma: Float, dst: V = next()): V

    fun V.blurY(sigma: Float, dst: V = next()): V

    fun V.blurXSigned(sigma: Float, dst: V = next()): V

    fun V.blurYSigned(sigma: Float, dst: V = next()): V

    fun V.added(b: V, c: V, dst: V = next()): V

    fun V.absDifference(other: V, dst: V = next()): V

    /**
     * for debugging only
     * */
    fun V.added(m: Float, n: Float, dst: V = next()): V

    // compute proper poisson reconstruction
    // result loses contrast a little???...
    fun iterate(src: V, dst: V, dx: V, dy: V, blurred: V): V

    fun V.writeInto(dst: FileReference)

    fun V.copyInto(dst: V): V

    // original.width, original.height, 5.0,
    //            dst.getChild("trick-to-result.mp4"), 50
    fun V.renderVideo(iterations: Int, dst: FileReference, run: (Long) -> V)

    fun execute(original: V, dst: FileReference, ext: String) {

        val clock = Clock()

        val dx = original.dx()
        val dy = original.dy()

        clock.stop("delta")

        val sigma = 6f

        val blurred = original.blur(sigma)

        // would need to be blurred, and not blurred at the same time...
        // how?
        val bdx = dx.blurXSigned(sigma) // .blurY(sigma)
        val bdy = dy.blurYSigned(sigma) // .blurX(sigma)


        clock.stop("convolve")

        val result = blurred
            .added(bdx, bdy)

        val error = original
            .absDifference(result)

        clock.stop("error & result")

        blurred.writeInto(dst.getChild("blurred$ext"))

        val normalScale = 1f
        dx.added(normalScale, 0.5f).writeInto(dst.getChild("dx$ext"))
        dy.added(normalScale, 0.5f).writeInto(dst.getChild("dy$ext"))

        bdx.added(normalScale, 0.5f).writeInto(dst.getChild("bdx$ext"))
        bdy.added(normalScale, 0.5f).writeInto(dst.getChild("bdy$ext"))

        result.writeInto(dst.getChild("trick$ext"))

        error.writeInto(dst.getChild("error-gaussian$ext"))

        val tmp = result.next()
        result.copyInto(tmp)
        original.renderVideo(50, dst.getChild("trick-to-result.mp4")) {
            if (it > 0L) {
                val s = if (it.and(1) == 1L) result else tmp
                val d = if (s == result) tmp else result
                iterate(s, d, dx, dy, blurred)
            } else result
        }

        result.writeInto(dst.getChild("result-trick$ext"))

        val error0 = original
            .absDifference(result)

        error0.writeInto(dst.getChild("error-trick$ext"))

        blurred.copyInto(result)

        original.renderVideo(50, dst.getChild("blurred-to-result.mp4")) {
            if (it > 0L) {
                val s = if (it.and(1) == 1L) result else tmp
                val d = if (s == result) tmp else result
                iterate(s, d, dx, dy, blurred)
            } else result
        }

        result.writeInto(dst.getChild("result-blurred$ext"))

        val error1 = original
            .absDifference(result)

        error1.writeInto(dst.getChild("error-blurred$ext"))

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