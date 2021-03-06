/*
 * Copyright (c) 2018 Schibsted Products & Technology AS. Licensed under the terms of the MIT license. See LICENSE in the project root.
 */

package android.util

object Base64 {
    @JvmField
    val NO_WRAP = 2

    @JvmStatic
    fun encodeToString(byteArray: ByteArray, flags: Int): String {
        return byteArray.toString(Charsets.UTF_8)
    }

    @JvmStatic
    fun decode(str: String, flags: Int): ByteArray {
        return str.toByteArray(Charsets.UTF_8)
    }
}
