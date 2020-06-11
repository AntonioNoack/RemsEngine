package ffmpeg.libavcodec

import ffmpeg.libavutil.AVClass
import ffmpeg.libavutil.AVRational

class AVBSFContext {
    /**
     * A class for logging and AVOptions
     */
    var av_class: AVClass? = null;

    /**
     * The bitstream filter this context is an instance of.
     */
    // var filter: AVBitStreamFilter? = null;

    /**
     * Opaque libavcodec internal data. Must not be touched by the caller in any
     * way.
     */
    // var internal: AVBSFInternal? = null;

    /**
     * Opaque filter-specific private data. If filter->priv_class is non-NULL,
     * this is an AVOptions-enabled struct.
     */
    var priv_data: Any? = null

    /**
     * Parameters of the input stream. This field is allocated in
     * av_bsf_alloc(), it needs to be filled by the caller before
     * av_bsf_init().
     */
    var par_in: AVCodecParameters? = null

    /**
     * Parameters of the output stream. This field is allocated in
     * av_bsf_alloc(), it is set by the filter in av_bsf_init().
     */
    var par_out: AVCodecParameters? = null

    /**
     * The timebase used for the timestamps of the input packets. Set by the
     * caller before av_bsf_init().
     */
    var time_base_in: AVRational? = null;

    /**
     * The timebase used for the timestamps of the output packets. Set by the
     * filter in av_bsf_init().
     */
    var time_base_out: AVRational? = null
}