package ffmpeg.libavutil

// everything in C would kind of be like an object in Kotlin...
object Log {


    /**
     * @addtogroup lavu_log
     *
     * @{
     *
     * @defgroup lavu_log_constants Logging Constants
     *
     * @{
     */

    /**
     * Print no output.
     */
    val AV_LOG_QUIET    = -8

    /**
     * Something went really wrong and we will crash now.
     */
    val AV_LOG_PANIC    = 0

    /**
     * Something went wrong and recovery is not possible.
     * For example, no header was found for a format which depends
     * on headers or an illegal combination of parameters is used.
     */
    val AV_LOG_FATAL    = 8

    /**
     * Something went wrong and cannot losslessly be recovered.
     * However, not all future data is affected.
     */
    val AV_LOG_ERROR    = 16

    /**
     * Something somehow does not look correct. This may or may not
     * lead to problems. An example would be the use of '-vstrict -2'.
     */
    val AV_LOG_WARNING = 24

    /**
     * Standard information.
     */
    val AV_LOG_INFO    = 32

    /**
     * Detailed information.
     */
    val AV_LOG_VERBOSE = 40

    /**
     * Stuff which is only useful for libav* developers.
     */
    val AV_LOG_DEBUG   = 48

    /**
     * Extremely verbose debugging, useful for libav* development.
     */
    val AV_LOG_TRACE   = 56

    val AV_LOG_MAX_OFFSET = (AV_LOG_TRACE - AV_LOG_QUIET)

    /**
     * @}
     */

    /**
     * Sets additional colors for extended debugging sessions.
     * @code
    av_log(ctx, AV_LOG_DEBUG|AV_LOG_C(134), "Message in purple\n");
    @endcode
     * Requires 256color terminal support. Uses outside debugging is not
     * recommended.
     */
    fun AV_LOG_C(x: Int) = ((x) shl 8)


    fun av_log(clazz: Any?, level: Int, format: String, vararg any: Any) {
        println("[FFMPEG] $clazz: $level, ${format.format(any)}")
    }


}