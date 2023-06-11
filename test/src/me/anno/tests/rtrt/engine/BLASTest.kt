package me.anno.tests.rtrt.engine

import me.anno.engine.raycast.RayHit
import me.anno.utils.hpc.ThreadLocal2

val localResult = ThreadLocal2 { RayHit() }
const val sky0 = 0x2f5293
const val sky1 = 0x5c729b
const val sky0BGR = 0x93522f
const val sky1BGR = 0x9b725c
