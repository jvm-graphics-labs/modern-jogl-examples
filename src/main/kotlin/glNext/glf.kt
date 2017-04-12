package glNext

import com.jogamp.opengl.GL
import glm.L
import glm.vec._2.Vec2
import glm.vec._2.Vec2s
import glm.vec._2.Vec2us
import glm.vec._3.Vec3
import glm.vec._4.Vec4
import glm.vec._4.Vec4ub
import main.framework.Semantic

/**
 * Created by GBarbieri on 11.04.2017.
 */

object glf {

    object pos4 : VertexLayout {
        override var attribute = arrayOf(
                VertexAttribute(Semantic.Attr.POSITION, Vec4.length, GL.GL_FLOAT, false, Vec4.SIZE, 0))
    }

    object pos3_col4 : VertexLayout {
        override var attribute = arrayOf(
                VertexAttribute(Semantic.Attr.POSITION, Vec3.length, GL.GL_FLOAT, false, Vec3.SIZE + Vec4.SIZE, 0),
                VertexAttribute(Semantic.Attr.COLOR, Vec4.length, GL.GL_FLOAT, false, Vec3.SIZE + Vec4.SIZE, Vec4.SIZE.L))
    }

    object pos4_col4 : VertexLayout {
        override var attribute = arrayOf(
                VertexAttribute(Semantic.Attr.POSITION, Vec4.length, GL.GL_FLOAT, false, Vec4.SIZE * 2, 0),
                VertexAttribute(Semantic.Attr.COLOR, Vec4.length, GL.GL_FLOAT, false, Vec4.SIZE * 2, Vec4.SIZE.L))
    }


    object pos2_tc2 : VertexLayout {
        override var attribute = arrayOf(
                VertexAttribute(Semantic.Attr.POSITION, Vec2.length, GL.GL_FLOAT, false, Vec2.SIZE * 2, 0),
                VertexAttribute(Semantic.Attr.TEX_COORD, Vec2.length, GL.GL_FLOAT, false, Vec2.SIZE * 2, Vec2.SIZE.L))
    }

    object pos2us_tc2us : VertexLayout {
        override var attribute = arrayOf(
                VertexAttribute(Semantic.Attr.POSITION, Vec2us.length, GL.GL_UNSIGNED_SHORT, false, Vec2us.SIZE * 2, 0),
                VertexAttribute(Semantic.Attr.TEX_COORD, Vec2us.length, GL.GL_UNSIGNED_SHORT, false, Vec2us.SIZE * 2, Vec2us.SIZE.L))
    }

    object pos3_tc2 : VertexLayout {
        override var attribute = arrayOf(
                VertexAttribute(Semantic.Attr.POSITION, Vec3.length, GL.GL_FLOAT, false, Vec3.SIZE + Vec2.SIZE, 0),
                VertexAttribute(Semantic.Attr.TEX_COORD, Vec2.length, GL.GL_FLOAT, false, Vec3.SIZE + Vec2.SIZE, Vec3.SIZE.L))
    }

    object pos3_col4ub : VertexLayout {
        override var attribute = arrayOf(
                VertexAttribute(Semantic.Attr.POSITION, Vec3.length, GL.GL_FLOAT, false, Vec3.SIZE + Vec4ub.SIZE, 0),
                VertexAttribute(Semantic.Attr.COLOR, Vec4ub.length, GL.GL_UNSIGNED_BYTE, false, Vec3.SIZE + Vec4ub.SIZE, Vec3.SIZE.L))
    }

    object pos2_tc3 : VertexLayout {
        override var attribute = arrayOf(
                VertexAttribute(Semantic.Attr.POSITION, Vec2.length, GL.GL_FLOAT, false, Vec2.SIZE + Vec3.SIZE, 0),
                VertexAttribute(Semantic.Attr.TEX_COORD, Vec3.length, GL.GL_FLOAT, false, Vec2.SIZE + Vec3.SIZE, Vec2.SIZE.L))
    }

    object pos3_tc3 : VertexLayout {
        override var attribute = arrayOf(
                VertexAttribute(Semantic.Attr.POSITION, Vec3.length, GL.GL_FLOAT, false, Vec3.SIZE * 2, 0),
                VertexAttribute(Semantic.Attr.TEX_COORD, Vec3.length, GL.GL_FLOAT, false, Vec3.SIZE * 2, Vec3.SIZE.L))
    }
}

interface VertexLayout {
    var attribute: Array<VertexAttribute>
    operator fun get(index: Int) = attribute[index]
}

class VertexAttribute(
        var index: Int,
        var size: Int,
        var type: Int,
        var normalized: Boolean,
        var interleavedStride: Int,
        var pointer: Long)