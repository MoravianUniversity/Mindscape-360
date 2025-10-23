package cardboard.util

/**
 * Represents a 4x4 matrix used for various transformations in the Cardboard VR
 * framework. It is stored in a one-dimensional array of 16 elements in
 * row-major order. It is designed to be continually reused to avoid
 * allocations.
 */
class Matrix() {
    constructor(matrix: FloatArray) : this() { set(matrix) }
    constructor(matrix: Matrix) : this() { set(matrix) }

    val m: FloatArray = FloatArray(16)

    operator fun get(row: Int, col: Int) = m[row * 4 + col]
    operator fun set(row: Int, col: Int, value: Float) { m[row * 4 + col] = value }

    operator fun get(row: Int, v: Vector = Vector()): Vector {
        m.copyInto(v.v, 0, row * 4, row * 4 + 4)
        return v
    }
    operator fun set(row: Int, v: Vector) { v.v.copyInto(m, row * 4, 0, 4) }

    fun set(m: Matrix): Matrix { m.m.copyInto(this.m, 0, 0, 16); return this }
    fun set(m: FloatArray): Matrix {
        require(m.size == 16) { "Matrix must have 16 elements" }
        m.copyInto(this.m, 0, 0, 16)
        return this
    }

    fun transpose(): Matrix {
        for (i in 0 until 4) {
            for (j in 0 until i) {
                m[i*4 + j] = m[j*4 + i]
            }
        }
        return this
    }

    operator fun timesAssign(scalar: Float) { for (i in 0 until 16) { m[i] *= scalar } }
    operator fun timesAssign(right: Matrix) {
        val result = FloatArray(16) // TODO: can this be optimized to avoid allocation?
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                result[i * 4 + j] = this.m[0 * 4 + j] * right.m[i * 4 + 0] +
                                    this.m[1 * 4 + j] * right.m[i * 4 + 1] +
                                    this.m[2 * 4 + j] * right.m[i * 4 + 2] +
                                    this.m[3 * 4 + j] * right.m[i * 4 + 3]
            }
        }
        set(result)
    }
    fun fromMultiply(a: Matrix, b: Matrix) = set(a).also { timesAssign(b) }

//    operator fun times(scalar: Float): Matrix { val m = Matrix(this); m *= scalar; return m }
//    operator fun times(right: Matrix): Matrix { val m = Matrix(this); m *= right; return m }
//    operator fun times(vec: Vector) = Vector { i ->
//        this.m[0*4 + i] * vec.v[0] +
//        this.m[1*4 + i] * vec.v[1] +
//        this.m[2*4 + i] * vec.v[2] +
//        this.m[3*4 + i] * vec.v[3]
//    }

    fun setIdentity(): Matrix {
        m[0*4 + 0] = 1f
        m[0*4 + 1] = 0f
        m[0*4 + 2] = 0f
        m[0*4 + 3] = 0f

        m[1*4 + 0] = 0f
        m[1*4 + 1] = 1f
        m[1*4 + 2] = 0f
        m[1*4 + 3] = 0f

        m[2*4 + 0] = 0f
        m[2*4 + 1] = 0f
        m[2*4 + 2] = 1f
        m[2*4 + 3] = 0f

        m[3*4 + 0] = 0f
        m[3*4 + 1] = 0f
        m[3*4 + 2] = 0f
        m[3*4 + 3] = 1f
        return this
    }

    fun setTranslation(x: Float, y: Float, z: Float): Matrix {
        m[0*4 + 0] = 1f
        m[0*4 + 1] = 0f
        m[0*4 + 2] = 0f
        m[0*4 + 3] = 0f

        m[1*4 + 0] = 0f
        m[1*4 + 1] = 1f
        m[1*4 + 2] = 0f
        m[1*4 + 3] = 0f

        m[2*4 + 0] = 0f
        m[2*4 + 1] = 0f
        m[2*4 + 2] = 1f
        m[2*4 + 3] = 0f

        m[3*4 + 0] = x
        m[3*4 + 1] = y
        m[3*4 + 2] = z
        m[3*4 + 3] = 1f
        return this
    }
    fun setTranslation(v: FloatArray): Matrix {
        require(v.size == 3 || (v.size == 4 && v[3] == 1f)) { "Translation vector must have 3 elements" }
        setTranslation(v[0], v[1], v[2])
        return this
    }
    fun setTranslation(v: Vector): Matrix {
        setTranslation(v.v[0], v.v[1], v.v[2])
        return this
    }

    override fun equals(other: Any?) = this === other || (other is Matrix && m.contentEquals(other.m))
    override fun hashCode() = m.contentHashCode()
    override fun toString() =
        "[[${m[0*4 + 0]}, ${m[0*4 + 1]}, ${m[0*4 + 2]}, ${m[0*4 + 3]}]\n" +
        " [${m[1*4 + 0]}, ${m[1*4 + 1]}, ${m[1*4 + 2]}, ${m[1*4 + 3]}]\n" +
        " [${m[2*4 + 0]}, ${m[2*4 + 1]}, ${m[2*4 + 2]}, ${m[2*4 + 3]}]\n" +
        " [${m[3*4 + 0]}, ${m[3*4 + 1]}, ${m[3*4 + 2]}, ${m[3*4 + 3]}]]"

}
