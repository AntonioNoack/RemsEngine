package me.anno.gpu

object FinalRendering {

    // for final rendering we need to use the GPU anyway;
    // so just use a static variable
    var isFinalRendering = false
        private set

    var missingFrameException: String? = null
        private set

    fun onMissingResource(message: String, source: Any?) {
        if (isFinalRendering && missingFrameException == null) {
            missingFrameException = if (source != null) "$message ($source)" else message
        }
    }

    fun runFinalRendering(renderScene: () -> Unit): String? {
        val prevFinalRendering = isFinalRendering
        val prevMissingFrame = missingFrameException
        isFinalRendering = true
        missingFrameException = null
        try {
            renderScene()
        } catch (e: Throwable) {
            missingFrameException = e.toString()
        }
        val missingFrameEx = missingFrameException
        isFinalRendering = prevFinalRendering
        missingFrameException = prevMissingFrame
        return missingFrameEx
    }
}