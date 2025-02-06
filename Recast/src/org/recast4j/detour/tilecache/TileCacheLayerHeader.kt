/*
Copyright (c) 2009-2010 Mikko Mononen memon@inside.org
recast4j copyright (c) 2015-2019 Piotr Piastucki piotr@jtilia.org

This software is provided 'as-is', without any express or implied
warranty.  In no event will the authors be held liable for any damages
arising from the use of this software.
Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:
1. The origin of this software must not be misrepresented; you must not
 claim that you wrote the original software. If you use this software
 in a product, an acknowledgment in the product documentation would be
 appreciated but is not required.
2. Altered source versions must be plainly marked as such, and must not be
 misrepresented as being the original software.
3. This notice may not be removed or altered from any source distribution.
*/
package org.recast4j.detour.tilecache

import org.joml.Vector3f

open class TileCacheLayerHeader {

    var magic = 0 // < Data magic
    var version = 0 // < Data version

    var tx = 0
    var ty = 0
    var tlayer = 0
    var bmin = Vector3f()
    var bmax = Vector3f()

    // < Height min/max range
    var hmin = 0
    var hmax = 0

    // < Dimension of the layer.
    var width = 0
    var height = 0

    // < Usable sub-region.
    var minx = 0
    var maxx = 0
    var miny = 0
    var maxy = 0

    companion object {
        /**
         * 'DTLR'
         * */
        const val DT_TILECACHE_MAGIC =
            'D'.code shl 24 or ('T'.code shl 16) or ('L'.code shl 8) or 'R'.code
        const val DT_TILECACHE_VERSION = 1
    }
}