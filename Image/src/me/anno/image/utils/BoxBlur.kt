package me.anno.image.utils

object BoxBlur {

    @JvmStatic
    fun boxBlurX(
        image: FloatArray, w: Int, h: Int, i0: Int, stride: Int, thickness: Int, normalize: Boolean,
        tmp: FloatArray = FloatArray(w)
    ) {
        if (thickness <= 1) return
        if (thickness > w) return boxBlurX(image, w, h, i0, stride, w, normalize, tmp)
        val th2 = thickness shr 1
        val th1 = thickness - th2
        for (y in 0 until h) {
            val i1 = i0 + y * stride
            val old0 = image[i1]
            // make border not appear black by duplicating first pixel
            var sum = old0 * th1
            // start of sum 1
            for (x in 0 until th2) {
                sum += image[i1 + x]
            }
            image.copyInto(tmp, 0, i1, i1 + w - th1)
            // start of sum 2
            for (x in 0 until th1) {
                val i = i1 + x
                sum += image[i + th2] - old0
                image[i] = sum
            }
            // updated sum
            for (x in th1 until w - th2) {
                val i = i1 + x
                sum += image[i + th2] - tmp[x - th1]
                image[i] = sum
            }
            // end of sum
            val new0 = image[i1 + w - 1]
            for (x in w - th2 until w) {
                val i = i1 + x
                sum += new0 - tmp[x - th1]
                image[i] = sum
            }
        }
        if (normalize) multiply(image, w, h, i0, stride, 1f / thickness)
    }

    /**
     * box blur for large images:
     * allocates an array, however for large images, this is faster,
     * because of the inefficient access pattern otherwise :)
     *
     * reduces runtime for w=h=2048, thickness=25 from 24-25ns/px to 13-15ns/px
     * */
    @JvmStatic
    fun boxBlurY(
        image: FloatArray,
        w: Int,
        h: Int,
        i0: Int,
        stride: Int,
        thickness: Int,
        normalize: Boolean,
        sum: FloatArray = FloatArray(w),
        tmp: FloatArray = FloatArray(w * (h - thickness.shr(1)))
    ) {
        if (thickness <= 1) return
        if (thickness > h) return boxBlurY(image, w, h, i0, stride, h, normalize, sum)
        sum.fill(0f, 0, w)

        val th2 = thickness shr 1
        val th1 = thickness - th2

        for (y in 0 until h - th1) {
            image.copyInto(tmp, y * w, y * stride + i0, y * stride + i0 + w)
        }

        val th2y = th2 * stride
        val th1y = th1 * w

        // make border not appear black by duplicating first pixel
        val th1f = th1.toFloat()
        for (x in 0 until w) {
            sum[x] = image[i0 + x] * th1f
        }
        // start of sum 1
        for (y in 0 until th2) {
            val i1 = i0 + y * stride
            for (x in 0 until w) {
                sum[x] += image[i1 + x]
            }
        }
        // start of sum 2
        for (y in 0 until th1) {
            val i1 = i0 + y * stride
            val ni = i1 + th2y
            for (x in 0 until w) {
                sum[x] += image[ni + x] - tmp[x]
                image[i1 + x] = sum[x]
            }
        }
        // updated sum
        for (y in th1 until h - th2) {
            val i1 = i0 + y * stride
            val oi = y * w - th1y
            val ni = i1 + th2y
            for (x in 0 until w) {
                sum[x] += image[ni + x] - tmp[oi + x]
                image[i1 + x] = sum[x]
            }
        }
        // end of sum
        val ni = i0 + (h - 1) * stride // last row
        for (y in h - th2 until h) {
            val i1 = i0 + y * stride
            val oi = y * w - th1y
            for (x in 0 until w) {
                sum[x] += image[ni + x] - tmp[oi + x]
                image[i1 + x] = sum[x]
            }
        }
        if (normalize) multiply(image, w, h, i0, stride, 1f / thickness)
    }

    @JvmStatic
    fun multiply(image: FloatArray, w: Int, h: Int, i0: Int, stride: Int, f: Float) {
        for (y in 0 until h) {
            val i1 = y * stride + i0
            val i2 = i1 + w
            for (i in i1 until i2) {
                image[i] *= f
            }
        }
    }
}