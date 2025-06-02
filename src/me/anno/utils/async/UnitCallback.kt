package me.anno.utils.async

import me.anno.utils.async.Callback.Companion.USE_COROUTINES_INSTEAD

/**
 * callback without value
 * */
@Deprecated(USE_COROUTINES_INSTEAD)
typealias UnitCallback = Callback<Unit>