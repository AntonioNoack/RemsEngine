package org.apache.logging.log4j

/** for other libraries; whatever this does ^^ */
object MarkerManager {

    private val marker = Marker()

    @JvmStatic
    fun getMarker(name: String?): Marker {
        return marker
    }

}