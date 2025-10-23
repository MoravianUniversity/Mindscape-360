package cardboard.util

/**
 * Represents a quaternion used for 3D rotations in the Cardboard VR framework.
 */
class Quat(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f, var w: Float = 1f) {
    constructor(q: FloatArray): this(q[0], q[1], q[2], q[3])
    constructor(q: Quat): this(q.x, q.y, q.z, q.w)
    fun set(x: Float = this.x, y: Float = this.y, z: Float = this.z, w: Float = this.w): Quat {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
        return this
    }
    fun set(q: FloatArray): Quat {
        require (q.size == 4) { "Quaternion must have 4 elements" }
        this.x = q[0]
        this.y = q[1]
        this.z = q[2]
        this.w = q[3]
        return this
    }
    fun set(q: Quat): Quat {
        this.x = q.x
        this.y = q.y
        this.z = q.z
        this.w = q.w
        return this
    }
    fun toMatrix(m: Matrix = Matrix()): Matrix {
        val xx = 2f * x * x
        val yy = 2f * y * y
        val zz = 2f * z * z

        val xy = 2f * x * y
        val xz = 2f * x * z
        val yz = 2f * y * z

        val xw = 2f * x * w
        val yw = 2f * y * w
        val zw = 2f * z * w

        m[0, 0] = 1f - yy - zz
        m[0, 1] = xy + zw
        m[0, 2] = xz - yw
        m[0, 3] = 0f

        m[1, 0] = xy - zw
        m[1, 1] = 1f - xx - zz
        m[1, 2] = yz + xw
        m[1, 3] = 0f

        m[2, 0] = xz + yw
        m[2, 1] = yz - xw
        m[2, 2] = 1f - xx - yy
        m[2, 3] = 0f

        m[3, 0] = 0f
        m[3, 1] = 0f
        m[3, 2] = 0f
        m[3, 3] = 1f

        return m
    }
}
