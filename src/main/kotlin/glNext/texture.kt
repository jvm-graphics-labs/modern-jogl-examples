package glNext

import com.jogamp.opengl.GL3
import java.nio.IntBuffer

/**
 * Created by GBarbieri on 12.04.2017.
 */

fun GL3.glGenTextures(textures: IntBuffer) = glGenTextures(textures.capacity(), textures)

fun GL3.glBindTexture(target: Int) = glBindTexture(target, 0)

// TODO change capacity() with remaining()?
fun GL3.glDeleteTextures(textures: IntBuffer) = glDeleteTextures(textures.capacity(), textures)