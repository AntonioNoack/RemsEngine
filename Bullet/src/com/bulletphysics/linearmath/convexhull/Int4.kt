/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Stan Melax Convex Hull Computation
 * Copyright (c) 2008 Stan Melax http://www.melax.com/
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from
 * the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */
package com.bulletphysics.linearmath.convexhull

/**
 *
 * @author jezek2
 */
internal class Int4 {
    var x: Int = 0
    var y: Int = 0
    var z: Int = 0
    var w: Int = 0

    fun set(x: Int, y: Int, z: Int, w: Int) {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
    }
}
