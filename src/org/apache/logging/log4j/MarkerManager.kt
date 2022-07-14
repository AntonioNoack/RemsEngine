package org.apache.logging.log4j

/** for other libraries; whatever this does ^^ */
@Suppress("unused")
object MarkerManager {

    private val marker = Marker()

    @JvmStatic
    @Suppress("unused_parameter")
    fun getMarker(name: String?): Marker {
        return marker
    }

}