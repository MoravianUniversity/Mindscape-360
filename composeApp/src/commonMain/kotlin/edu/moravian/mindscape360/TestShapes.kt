package edu.moravian.mindscape360


import edu.moravian.cardboard.Mesh

/**
 * Create a simple rectangle directly in front of the camera for testing rendering.
 * This is the absolute simplest thing to draw - if this doesn't show, rendering is broken.
 */
@OptIn(ExperimentalUnsignedTypes::class)
fun testRectangle(): Mesh {
    // Simple quad at Z=-3 (3 meters in front of camera)
    // 2x2 meters in size

    val vertices = floatArrayOf(
        // Triangle 1
        -1f,  1f, -3f,  // top left
        -1f, -1f, -3f,  // bottom left
        1f, -1f, -3f,  // bottom right
        // Triangle 2
        -1f,  1f, -3f,  // top left
        1f, -1f, -3f,  // bottom right
        1f,  1f, -3f,  // top right
    )

    val normals = floatArrayOf(
        // All facing camera (positive Z)
        0f, 0f, 1f,
        0f, 0f, 1f,
        0f, 0f, 1f,
        0f, 0f, 1f,
        0f, 0f, 1f,
        0f, 0f, 1f,
    )

    val uvs = floatArrayOf(
        0f, 0f,
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 1f,
        1f, 0f,
    )

    val indices = ushortArrayOf(0u, 1u, 2u, 3u, 4u, 5u)

    return Mesh(indices, vertices, normals, uvs)
}