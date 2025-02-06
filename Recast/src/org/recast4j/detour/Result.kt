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

class Result<T> private constructor(val result: T?, val status: Status, val message: String?) {

    fun failed() = status.isFailed
    fun succeeded() = status.isSuccess

    companion object {

        fun <T> success(result: T): Result<T> {
            return Result(result, Status.SUCCESS, null)
        }

        fun <T> failure(): Result<T> {
            return Result(null, Status.FAILURE, null)
        }

        fun <T> invalidParam(): Result<T> {
            return Result(null, Status.FAILURE_INVALID_PARAM, null)
        }

        fun <T> invalidParam(message: String?): Result<T?> {
            return Result(null, Status.FAILURE_INVALID_PARAM, message)
        }

        fun <T> failure(result: T): Result<T> {
            return Result(result, Status.FAILURE, null)
        }

        fun <T> of(status: Status, message: String?): Result<T?> {
            return Result(null, status, message)
        }

        fun <T> of(status: Status, result: T): Result<T> {
            return Result(result, status, null)
        }
    }
}