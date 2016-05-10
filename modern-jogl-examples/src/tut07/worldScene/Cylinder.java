/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tut07.worldScene;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_ELEMENT_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TRIANGLE_FAN;
import static com.jogamp.opengl.GL.GL_UNSIGNED_SHORT;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import glm.vec._3.Vec3;
import glm.vec._4.Vec4;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 *
 * @author GBarbieri
 */
public class Cylinder {

    private class Buffer {

        public static final int VERTEX = 0;
        public static final int INDEX = 1;
        public static final int MAX = 2;
    }

    private IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX),
            vertexArrayName = GLBuffers.newDirectIntBuffer(1);

    public Cylinder(GL3 gl3) {

        initBuffers(gl3);
        
        initVertexArray(gl3);
    }

    private void initBuffers(GL3 gl3) {

        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);
        ShortBuffer indexBuffer = GLBuffers.newDirectShortBuffer(indexData);

        gl3.glGenBuffers(Buffer.MAX, bufferName);

        gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        gl3.glBufferData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * Float.BYTES, vertexBuffer, GL_STATIC_DRAW);
        gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.INDEX));
        gl3.glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * Short.BYTES, indexBuffer, GL_STATIC_DRAW);
    }

    private void initVertexArray(GL3 gl3) {

        gl3.glGenVertexArrays(1, vertexArrayName);

        gl3.glBindVertexArray(vertexArrayName.get(0));

        gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        
        gl3.glEnableVertexAttribArray(0);
        gl3.glVertexAttribPointer(0, 3, GL_FLOAT, false, Vec3.SIZE, 0);
        
        gl3.glEnableVertexAttribArray(1);
        gl3.glVertexAttribPointer(1, 4, GL_FLOAT, false, Vec4.SIZE, 62 * Vec3.SIZE);

        gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.INDEX));
        
        gl3.glBindVertexArray(0);
    }

    public void render(GL3 gl3) {
        
        gl3.glBindVertexArray(vertexArrayName.get(0));
        
        gl3.glDrawElements(GL_TRIANGLE_FAN, 32, GL_UNSIGNED_SHORT, 0);
        gl3.glDrawElements(GL_TRIANGLE_FAN, 32, GL_UNSIGNED_SHORT, 32);
    }
    
    private float[] vertexData = {
        +0.0f, +0.5f, 0f,
        +0.5f, +0.5f, 0f,
        +0.5f, -0.5f, 0f,
        +0.489073818757310f, +0.5f, +0.1039557588888f,
        +0.489073818757310f, -0.5f, +0.1039557588888f,
        +0.456772800775420f, +0.5f, +0.20336815992623f,
        +0.456772800775420f, -0.5f, +0.20336815992623f,
        +0.404508653161510f, +0.5f, +0.29389241146627f,
        +0.404508653161510f, -0.5f, +0.29389241146627f,
        +0.334565566112880f, +0.5f, +0.37157217599218f,
        +0.334565566112880f, -0.5f, +0.37157217599218f,
        +0.250000383012600f, +0.5f, +0.43301248075957f,
        +0.250000383012600f, -0.5f, +0.43301248075957f,
        +0.154509001930160f, +0.5f, +0.47552809414644f,
        +0.154509001930160f, -0.5f, +0.47552809414644f,
        +0.052264847412855f, +0.5f, +0.49726088296277f,
        +0.052264847412855f, -0.5f, +0.49726088296277f,
        -0.052263527886268f, +0.5f, +0.49726102165048f,
        -0.052263527886268f, -0.5f, +0.49726102165048f,
        -0.154507740073120f, +0.5f, +0.47552850414828f,
        -0.154507740073120f, -0.5f, +0.47552850414828f,
        -0.249999233974220f, +0.5f, +0.43301314415651f,
        -0.249999233974220f, -0.5f, +0.43301314415651f,
        -0.334564580111570f, +0.5f, +0.37157306379065f,
        -0.334564580111570f, -0.5f, +0.37157306379065f,
        -0.404507873290180f, +0.5f, +0.29389348486527f,
        -0.404507873290180f, -0.5f, +0.29389348486527f,
        -0.456772261118140f, +0.5f, +0.20336937201315f,
        -0.456772261118140f, -0.5f, +0.20336937201315f,
        -0.489073542899640f, +0.5f, +0.10395705668972f,
        -0.489073542899640f, -0.5f, +0.10395705668972f,
        -0.499999999998240f, +0.5f, +1.3267948966764e-006f,
        -0.499999999998240f, -0.5f, +1.3267948966764e-006f,
        -0.489074094611530f, +0.5f, -0.10395446108714f,
        -0.489074094611530f, -0.5f, -0.10395446108714f,
        -0.456773340429480f, +0.5f, -0.20336694783787f,
        -0.456773340429480f, -0.5f, -0.20336694783787f,
        -0.404509433029990f, +0.5f, -0.2938913380652f,
        -0.404509433029990f, -0.5f, -0.2938913380652f,
        -0.334566552111840f, +0.5f, -0.3715712881911f,
        -0.334566552111840f, -0.5f, -0.3715712881911f,
        -0.250001532049220f, +0.5f, -0.43301181735958f,
        -0.250001532049220f, -0.5f, -0.43301181735958f,
        -0.154510263786110f, +0.5f, -0.47552768414126f,
        -0.154510263786110f, -0.5f, -0.47552768414126f,
        -0.052266166939075f, +0.5f, -0.49726074427155f,
        -0.052266166939075f, -0.5f, -0.49726074427155f,
        +0.052262208359312f, +0.5f, -0.4972611603347f,
        +0.052262208359312f, -0.5f, -0.4972611603347f,
        +0.154506478214990f, +0.5f, -0.47552891414676f,
        +0.154506478214990f, -0.5f, -0.47552891414676f,
        +0.249998084934080f, +0.5f, -0.4330138075504f,
        +0.249998084934080f, -0.5f, -0.4330138075504f,
        +0.334563594107900f, +0.5f, -0.37157395158649f,
        +0.334563594107900f, -0.5f, -0.37157395158649f,
        +0.404507093416010f, +0.5f, -0.2938945582622f,
        +0.404507093416010f, -0.5f, -0.2938945582622f,
        +0.456771721457640f, +0.5f, -0.20337058409865f,
        +0.456771721457640f, -0.5f, -0.20337058409865f,
        +0.489073267038540f, +0.5f, -0.10395835448992f,
        +0.489073267038540f, -0.5f, -0.10395835448992f,
        +0f, -0.5f, 0f,
        1.00f, 1.00f, 1.00f, 1,
        0.90f, 0.90f, 0.90f, 1,
        0.90f, 0.90f, 0.90f, 1,
        0.82f, 0.82f, 0.82f, 1,
        0.82f, 0.82f, 0.82f, 1,
        0.74f, 0.74f, 0.74f, 1,
        0.74f, 0.74f, 0.74f, 1,
        0.66f, 0.66f, 0.66f, 1,
        0.66f, 0.66f, 0.66f, 1,
        0.58f, 0.58f, 0.58f, 1,
        0.58f, 0.58f, 0.58f, 1,
        0.50f, 0.50f, 0.50f, 1,
        0.50f, 0.50f, 0.50f, 1,
        0.58f, 0.58f, 0.58f, 1,
        0.58f, 0.58f, 0.58f, 1,
        0.66f, 0.66f, 0.66f, 1,
        0.66f, 0.66f, 0.66f, 1,
        0.74f, 0.74f, 0.74f, 1,
        0.74f, 0.74f, 0.74f, 1,
        0.82f, 0.82f, 0.82f, 1,
        0.82f, 0.82f, 0.82f, 1,
        0.90f, 0.90f, 0.90f, 1,
        0.90f, 0.90f, 0.90f, 1,
        0.82f, 0.82f, 0.82f, 1,
        0.82f, 0.82f, 0.82f, 1,
        0.74f, 0.74f, 0.74f, 1,
        0.74f, 0.74f, 0.74f, 1,
        0.66f, 0.66f, 0.66f, 1,
        0.66f, 0.66f, 0.66f, 1,
        0.58f, 0.58f, 0.58f, 1,
        0.58f, 0.58f, 0.58f, 1,
        0.50f, 0.50f, 0.50f, 1,
        0.50f, 0.50f, 0.50f, 1,
        0.58f, 0.58f, 0.58f, 1,
        0.58f, 0.58f, 0.58f, 1,
        0.66f, 0.66f, 0.66f, 1,
        0.66f, 0.66f, 0.66f, 1,
        0.74f, 0.74f, 0.74f, 1,
        0.74f, 0.74f, 0.74f, 1,
        0.82f, 0.82f, 0.82f, 1,
        0.82f, 0.82f, 0.82f, 1,
        0.90f, 0.90f, 0.90f, 1,
        0.90f, 0.90f, 0.90f, 1,
        0.82f, 0.82f, 0.82f, 1,
        0.82f, 0.82f, 0.82f, 1,
        0.74f, 0.74f, 0.74f, 1,
        0.74f, 0.74f, 0.74f, 1,
        0.66f, 0.66f, 0.66f, 1,
        0.66f, 0.66f, 0.66f, 1,
        0.58f, 0.58f, 0.58f, 1,
        0.58f, 0.58f, 0.58f, 1,
        0.50f, 0.50f, 0.50f, 1,
        0.50f, 0.50f, 0.50f, 1,
        0.58f, 0.58f, 0.58f, 1,
        0.58f, 0.58f, 0.58f, 1,
        0.66f, 0.66f, 0.66f, 1,
        0.66f, 0.66f, 0.66f, 1,
        0.74f, 0.74f, 0.74f, 1,
        0.74f, 0.74f, 0.74f, 1,
        0.82f, 0.82f, 0.82f, 1,
        0.82f, 0.82f, 0.82f, 1,
        1.00f, 1.00f, 1.00f, 1};
    private short[] indexData = {0, 1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 31, 33, 35, 37, 39, 41, 43,
        45, 47, 49, 51, 53, 55, 57, 59, 1, 61, 60, 58, 56, 54, 52, 50, 48, 46, 44, 42, 40, 38, 36, 34, 32, 30, 28, 26, 24, 22, 20, 18, 16,
        14, 12, 10, 8, 6, 4, 2, 60, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26,
        27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52,
        53, 54, 55, 56, 57, 58, 59, 60, 1, 2};
}
