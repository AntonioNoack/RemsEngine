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
package org.recast4j.detour.tilecache.io

import org.recast4j.detour.NavMeshParams
import org.recast4j.detour.tilecache.TileCacheParams

class TileCacheSetHeader {
    var magic = 0
    var version = 0
    var numTiles = 0
    var meshParams = NavMeshParams()
    var cacheParams = TileCacheParams()

    companion object {
        const val TILECACHESET_MAGIC = 'T'.code shl 24 or ('S'.code shl 16) or ('E'.code shl 8) or 'T'.code // 'TSET';
        const val TILECACHESET_VERSION = 1
        const val TILECACHESET_VERSION_RECAST4J = 0x8801
    }
}