/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tut12.sceneLighting;

import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER;
import static com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import framework.Framework;
import framework.Semantic;
import framework.component.Mesh;
import glm.mat._3.Mat3;
import glm.vec._4.Vec4;
import glutil.MatrixStack;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import one.util.streamex.IntStreamEx;
import org.xml.sax.SAXException;

/**
 *
 * @author elect
 */
public class Scene {

    static final String[] VERTEX_SHADERS_SRC = {"pcn", "pcn", "pn", "pn"},
            FRAGMENTS_SHADERS_SRC = {"diffuse-specular", "diffuse-only", "diffuse-specular-mtl", "diffuse-only-mtl"};

    private final String MESHES_ROOT = "/tut12/data/", GROUND_MESH_SRC = "Ground.xml", CUBE_MESH_SRC = "UnitCube.xml",
            TETRAHEDRON_MESH_SRC = "UnitTetrahedron.xml", CYLINDER_MESH_SRC = "UnitCylinder.xml",
            SPHERE_MESH_SRC = "UnitSphere.xml";

    private Mesh terrain, cube, tetra, cyl, sphere;

    private int sizeMaterialBlock;

    private IntBuffer materialUniformBuffer = GLBuffers.newDirectIntBuffer(1);

    //One for the ground, and one for each of the 5 objects.
    private final int MATERIAL_COUNT = 6;

    private ProgramData[] programs = new ProgramData[LightingProgramType.values().length];

    public Scene(GL3 gl3) {

        IntStreamEx.range(programs.length).forEach(i -> programs[i]
                = new ProgramData(gl3, SceneLighting.SHADERS_ROOT, VERTEX_SHADERS_SRC[i], FRAGMENTS_SHADERS_SRC[i]));

        try {
            terrain = new Mesh(MESHES_ROOT + GROUND_MESH_SRC, gl3);
            cube = new Mesh(MESHES_ROOT + CUBE_MESH_SRC, gl3);
            tetra = new Mesh(MESHES_ROOT + TETRAHEDRON_MESH_SRC, gl3);
            cyl = new Mesh(MESHES_ROOT + CYLINDER_MESH_SRC, gl3);
            sphere = new Mesh(MESHES_ROOT + SPHERE_MESH_SRC, gl3);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(SceneLighting.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Align the size of each MaterialBlock to the uniform buffer alignment.
        IntBuffer uniformBufferAlignSize = GLBuffers.newDirectIntBuffer(1);
        gl3.glGetIntegerv(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT, uniformBufferAlignSize);

        sizeMaterialBlock = MaterialBlock.SIZE;
        sizeMaterialBlock += uniformBufferAlignSize.get(0) - (sizeMaterialBlock % uniformBufferAlignSize.get(0));

        int sizeMaterialUniformBuffer = sizeMaterialBlock * MATERIAL_COUNT;
        List<MaterialBlock> materials = getMaterials();

        ByteBuffer buffer = GLBuffers.newDirectByteBuffer(sizeMaterialUniformBuffer);
        IntStreamEx.range(MATERIAL_COUNT).forEach(i -> materials.get(i).toDbb(buffer, i * MaterialBlock.SIZE));

        gl3.glGenBuffers(1, materialUniformBuffer);
        gl3.glBindBuffer(GL_UNIFORM_BUFFER, materialUniformBuffer.get(0));
        gl3.glBufferData(GL_UNIFORM_BUFFER, sizeMaterialUniformBuffer, buffer, GL_STATIC_DRAW);
        gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }

    private List<MaterialBlock> getMaterials() {

        List<MaterialBlock> materials = new ArrayList<>();

        // Ground
        materials.add(new MaterialBlock(new Vec4(1.0f), new Vec4(0.5f, 0.5f, 0.5f, 1.0f), 0.6f));

        // Tetrahedron
        materials.add(new MaterialBlock(new Vec4(0.5f), new Vec4(0.5f, 0.5f, 0.5f, 1.0f), 0.05f));

        // Monolith
        materials.add(new MaterialBlock(new Vec4(0.05f), new Vec4(0.95f, 0.95f, 0.95f, 1.0f), 0.4f));

        // Cube
        materials.add(new MaterialBlock(new Vec4(0.5f), new Vec4(0.3f, 0.3f, 0.3f, 1.0f), 0.1f));

        // Cylinder
        materials.add(new MaterialBlock(new Vec4(0.5f), new Vec4(0.0f, 0.0f, 0.0f, 1.0f), 0.6f));

        // Sphere
        materials.add(new MaterialBlock(new Vec4(0.63f, 0.60f, 0.02f, 1.0f), new Vec4(0.22f, 0.20f, 0.0f, 1.0f), 0.3f));

        return materials;
    }

    public void render(GL3 gl3, MatrixStack modelMatrix, float alphaTetra) {

        //Render the ground plane.
        {
            modelMatrix.push().rotateX(-90);

            drawObject(gl3, terrain, programs[LightingProgramType.VertColorDiffuse.ordinal()], 0, modelMatrix);
        }
    }

    private void drawObject(GL3 gl3, Mesh mesh, ProgramData prog, int mtlIx, MatrixStack modelMatrix) {

        gl3.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, materialUniformBuffer.get(0),
                mtlIx * sizeMaterialBlock, MaterialBlock.SIZE);

        Mat3 normMatrix = modelMatrix.top().toMat3_().inverse().transpose();

        gl3.glUseProgram(prog.theProgram);
        gl3.glUniformMatrix4fv(prog.modelToCameraMatrixUnif, 1, false, modelMatrix.top().toDfb(Framework.matBuffer));

        gl3.glUniformMatrix3fv(prog.normalModelToCameraMatrixUnif, 1, false, normMatrix.toDfb(Framework.matBuffer));
        mesh.render(gl3);
        gl3.glUseProgram(0);

        gl3.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, 0);
    }

    enum LightingProgramType {

        VertColorDiffuseSpecular,
        VertColorDiffuse,
        MtlColorDiffuseSpecular,
        MtlColorDiffuse;
    }

    private class MaterialBlock {

        public static final int SIZE = Vec4.SIZE * 3;

        public Vec4 diffuseColor;
        public Vec4 specularColor;
        public float specularShininess;
        public float[] padding = new float[3];

        public MaterialBlock(Vec4 diffuseColor, Vec4 specularColor, float specularShininess) {
            this.diffuseColor = diffuseColor;
            this.specularColor = specularColor;
            this.specularShininess = specularShininess;
        }

        public ByteBuffer toDbb(ByteBuffer bb, int offset) {
            diffuseColor.toDbb(bb, offset + 0);
            specularColor.toDbb(bb, offset + Vec4.SIZE);
            bb.putFloat(offset + 2 * Vec4.SIZE, specularShininess);
            return bb;
        }
    }
}
