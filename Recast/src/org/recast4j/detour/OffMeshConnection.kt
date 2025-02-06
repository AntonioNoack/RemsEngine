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
package org.recast4j.detour

import org.joml.Vector3f

/**
 * Defines a navigation mesh off-mesh connection within a dtMeshTile object. An off-mesh connection is a user defined
 * traversable connection made up to two vertices.
 */
class OffMeshConnection {
    /**
     * The endpoints of the connection. [(ax, ay, az, bx, by, bz)]
     */
    var posA = Vector3f()
    var posB = Vector3f()

    /**
     * The radius of the endpoints. [Limit: >= 0]
     */
    var rad = 0f

    /**
     * The polygon reference of the connection within the tile.
     */
    var poly = 0

    /**
     * Link flags. These are not the connection's user defined flags. Those are assigned via the connection's Poly definition.
     * These are link flags used for internal purposes.
     */
    var flags = 0

    /**
     * End point side.
     */
    var side = 0

    /**
     * The id of the offmesh connection. (User assigned when the navigation mesh is built.)
     */
    var userId = 0
}