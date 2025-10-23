package cardboard.util

import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Represents a length-4 vector used for various transformations in the Cardboard VR framework.
 */
class Vector {
    constructor(vector: FloatArray) { set(vector) }
    constructor(vector: Vector) { set(vector) }
    constructor(x: Float = 0f, y: Float = 0f, z: Float = 0f, w: Float = 1f) { set(x, y, z, w) }

    val v: FloatArray = FloatArray(4)
    operator fun get(i: Int) = v[i]
    operator fun set(i: Int, value: Float) { v[i] = value }
    fun set(v: Vector): Vector { v.v.copyInto(this.v, 0, 0, 4); return this }
    fun set(v: FloatArray): Vector {
        require(v.size == 4) { "Vector must have 4 elements" }
        v.copyInto(this.v, 0, 0, 4)
        return this
    }
    fun set(x: Float = v[0], y: Float = v[1], z: Float = v[2], w: Float = v[3]): Vector {
        v[0] = x
        v[1] = y
        v[2] = z
        v[3] = w
        return this
    }

    operator fun timesAssign(scalar: Float) { for (i in 0 until 4) { v[i] *= scalar } }
    val norm get() = sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]) // + v[3]*v[3]
    fun dot(b: Vector) = v[0] * b.v[0] + v[1] * b.v[1] + v[2] * b.v[2] // + v[3] * b.v[3]
    fun angle(b: Vector) = acos((dot(b) / (norm * b.norm)).coerceIn(-1f, 1f))
}
