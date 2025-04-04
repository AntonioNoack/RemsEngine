/*
Copyright (c) 2009-2010 Mikko Mononen memon@inside.org
recast4j Copyright (c) 2015-2019 Piotr Piastucki piotr@jtilia.org

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
package org.recast4j.recast

import org.apache.logging.log4j.LogManager
import org.joml.AABBf

class RecastBuilderConfig(
    val cfg: RecastConfig,
    bounds: AABBf,
    val tileX: Int = 0,
    val tileZ: Int = 0
) {
    /**
     * The width of the field along the x-axis. [Limit: >= 0] [Units: vx]
     */
    var width = 0

    /**
     * The height of the field along the z-axis. [Limit: >= 0] [Units: vx]
     */
    var height = 0

    /**
     * The bounds of the field's AABB. [(x, y, z)] [Units: wu]
     */
    val bounds = AABBf(bounds)

    init {
        if (cfg.useTiles) {
            val tsx = cfg.tileSizeX * cfg.cellSize
            val tsz = cfg.tileSizeZ * cfg.cellSize
            this.bounds.minX += tileX * tsx
            this.bounds.minZ += tileZ * tsz
            this.bounds.maxX = this.bounds.minX + tsx
            this.bounds.maxZ = this.bounds.minZ + tsz
            // Expand the heightfield bounding box by border size to find the extents of geometry we need to build this
            // tile.
            //
            // This is done to make sure that the navmesh tiles connect correctly at the borders,
            // and the obstacles close to the border work correctly with the dilation process.
            // No polygons (or contours) will be created on the border area.
            //
            // IMPORTANT!
            //
            // :''''''''':
            // : +-----+ :
            // : | | :
            // : | |<--- tile to build
            // : | | :
            // : +-----+ :<-- geometry needed
            // :.........:
            //
            // You should use this bounding box to query your input geometry.
            //
            // For example if you build a navmesh for terrain, and want the navmesh tiles to match the terrain tile size
            // you will need to pass in data from neighbour terrain tiles too! In a simple case, just pass in all the 8
            // neighbours,
            // or use the bounding box below to only pass in a sliver of each of the 8 neighbours.
            val ds = cfg.borderSize * cfg.cellSize
            this.bounds.minX -= ds
            this.bounds.minZ -= ds
            this.bounds.maxX += ds
            this.bounds.maxZ += ds
            width = cfg.tileSizeX + cfg.borderSize * 2
            height = cfg.tileSizeZ + cfg.borderSize * 2
        } else {
            width = Recast.calcGridSizeX(this.bounds, cfg.cellSize)
            height = Recast.calcGridSizeY(this.bounds, cfg.cellSize)
        }

        LOGGER.info("Building $width x $height, for $bounds bounds and ${cfg.cellSize} cell size")
    }

    companion object {
        private val LOGGER = LogManager.getLogger(RecastBuilderConfig::class)
    }
}