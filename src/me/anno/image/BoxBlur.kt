package me.anno.image

object BoxBlur {

    @JvmStatic
    fun gaussianBlur(image: FloatArray, w: Int, h: Int, thickness: Int): Boolean {
        // box blur 3x with a third of the thickness is a nice gaussian blur approximation :),
        // which in turn is a bokeh-blur approximation
        val f0 = thickness / 3
        val f1 = thickness - 2 * f0
        if (f0 < 2 && f1 < 2) return false
        val tmp = FloatArray(w)
        // if the first row in the result is guaranteed to be zero,
        // we could use the image itself as buffer; (but only we waste space in the first place ->
        // don't optimize that case)
        if (f0 > 1) {
            boxBlurX(image, w, h, f0)
            boxBlurY(image, w, h, f0, tmp)
            boxBlurX(image, w, h, f0)
            boxBlurY(image, w, h, f0, tmp)
        }
        if (f1 > 1) {
            boxBlurX(image, w, h, f1)
            boxBlurY(image, w, h, f1, tmp)
        }
        return true
    }

    @JvmStatic
    fun boxBlurX(image: FloatArray, w: Int, h: Int, thickness: Int) {
        if (thickness <= 1) return
        for (y in 0 until h) {
            val i0 = y * w
            var sum = 0f
            // start of sum
            for (x in 0 until thickness) {
                sum += image[i0 + x]
            }
            // updated sum
            for (x in 0 until w - thickness) {
                val i = i0 + x
                val v = image[i + thickness]
                val old = image[i]
                image[i] = sum
                sum += v - old
            }
            // end of sum
            for (x in w - thickness until w) {
                val i = i0 + x
                val old = image[i]
                image[i] = sum
                sum -= old
            }
        }
    }

    /**
     * box blur for large images:
     * allocates an array, however for large images, this is faster,
     * because of the inefficient access pattern otherwise :)
     *
     * reduces runtime for w=h=2048, thickness=25 from 24-25ns/px to 13-15ns/px
     * */
    @JvmStatic
    fun boxBlurY(image: FloatArray, w: Int, h: Int, thickness: Int, sum: FloatArray) {
        if (thickness <= 1) return
        sum.fill(0f, 0, w)
        // start of sum
        var i = 0
        for (y in 0 until thickness) {
            for (x in 0 until w) {
                sum[x] += image[i++]
            }
        }
        // updated sum
        var j = i
        i = 0
        for (y in 0 until h - thickness) {
            for (x in 0 until w) {
                val v = image[j++]
                val old = image[i]
                val oldSum = sum[x]
                image[i++] = oldSum
                sum[x] = oldSum + v - old
            }
        }
        // end of sum
        for (y in h - thickness until h) {
            for (x in 0 until w) {
                val old = image[i]
                val oldSum = sum[x]
                image[i++] = oldSum
                sum[x] = oldSum - old
            }
        }
    }

}