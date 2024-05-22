package me.anno.io.base64

/**
 * Base64 encoder/decoder,
 * with support for multiple combinations of end characters.
 * Output will be '+/'.
 * */
object Base64 : Base64Impl('+', '/') {
    init {
        // extra codes for better decoding support:
        register('-', 62)
        register(',', 63)
        register('_', 63)
    }
}