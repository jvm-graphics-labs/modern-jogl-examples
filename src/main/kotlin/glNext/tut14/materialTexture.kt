//package glNext.tut14
//
//import com.jogamp.newt.event.KeyEvent
//import com.jogamp.newt.event.MouseEvent
//import com.jogamp.opengl.GL2ES2.GL_RED
//import com.jogamp.opengl.GL2ES3.*
//import com.jogamp.opengl.GL2GL3.GL_TEXTURE_1D
//import com.jogamp.opengl.GL3
//import com.jogamp.opengl.GL3.GL_DEPTH_CLAMP
//import com.jogamp.opengl.util.texture.spi.DDSImage
//import glNext.*
//import glm.*
//import glm.vec._4.Vec4
//import main.framework.Framework
//import main.framework.Semantic
//import main.framework.component.Mesh
//import uno.buffer.*
//import uno.gl.UniformBlockArray
//import uno.glm.MatrixStack
//import uno.glsl.programOf
//import uno.mousePole.*
//import uno.time.Timer
//import java.io.File
//import java.nio.ByteBuffer
//
///**
// * Created by elect on 29/03/17.
// */
//
//fun main(args: Array<String>) {
//    MaterialTexture_().setup("Tutorial 14 - Material Texture")
//}
//
//class MaterialTexture_() : Framework() {
//
//    lateinit var programs: Array<ProgramData>
//    lateinit var unlit: UnlitProgData
//
//    val initialObjectData = ObjectData(
//            glm.vec._3.Vec3(0.0f, 0.5f, 0.0f),
//            glm.quat.Quat(1.0f, 0.0f, 0.0f, 0.0f))
//    val initialViewData = ViewData(
//            glm.vec._3.Vec3(initialObjectData.position),
//            glm.quat.Quat(0.92387953f, 0.3826834f, 0.0f, 0.0f),
//            10.0f,
//            0.0f)
//    val viewScale = ViewScale(
//            1.5f, 70.0f,
//            1.5f, 0.5f,
//            0.0f, 0.0f, //No camera movement.
//            90.0f / 250.0f)
//    val viewPole = ViewPole(initialViewData, viewScale, MouseEvent.BUTTON1)
//    val objectPole = ObjectPole(initialObjectData, 90.0f / 250.0f, MouseEvent.BUTTON3, viewPole)
//
//    val shaderPairs = arrayOf(Pair("pn.vert", "fixed-shininess.frag"), Pair("pnt.vert", "texture-shininess.frag"), Pair("pnt.vert", "texture-compute.frag"))
//
//    lateinit var objectMesh: Mesh
//    lateinit var cube: Mesh
//    lateinit var plane: Mesh
//
//    object Buffer {
//        val PROJECTION = 0
//        val LIGHT = 1
//        val MATERIAL = 2
//        val MAX = 3
//    }
//
//    object Texture {
//        val SHINE = NUM_GAUSSIAN_TEXTURES
//        val MAX = SHINE + 1
//    }
//
//    val bufferName = intBufferBig(Buffer.MAX)
//    val textureName = intBufferBig(Texture.MAX)
//    val samplerName = intBufferBig(1)
//
//    var materialOffset = 0
//    var currMaterial = 0
//    var currTexture = NUM_GAUSSIAN_TEXTURES - 1
//
//    val lightTimer = Timer(Timer.Type.Loop, 6.0f)
//
//    val halfLightDistance = 25.0f
//    val lightAttenuation = 1.0f / (halfLightDistance * halfLightDistance)
//    val lightHeight = 1.0f
//    val lightRadius = 3.0f
//
//    var mode = ShaderMode.FIXED
//
//    var drawLights = true
//    var drawCameraPos = false
//    var useInfinity = true
//
//    override fun init(gl: GL3) = with(gl) {
//
//        initializePrograms(gl)
//
//        objectMesh = Mesh(gl, javaClass, "tut14/Infinity.xml")
//        cube = Mesh(gl, javaClass, "tut14/UnitCube.xml")
//        plane = Mesh(gl, javaClass, "tut14/UnitPlane.xml")
//
//        val depthZNear = 0.0f
//        val depthZFar = 1.0f
//
//        glEnable(GL_CULL_FACE)
//        glCullFace(GL_BACK)
//        glFrontFace(GL_CW)
//
//        glEnable(GL_DEPTH_TEST)
//        glDepthMask(true)
//        glDepthFunc(GL_LEQUAL)
//        glDepthRangef(depthZNear, depthZFar)
//        glEnable(GL_DEPTH_CLAMP)
//
//        glGenBuffers(Buffer.MAX, bufferName)
//
//        //Setup our Uniform Buffers
//        setupMaterials(gl)
//
//        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.LIGHT])
//        glBufferData(GL_UNIFORM_BUFFER, LightBlock.SIZE, GL_DYNAMIC_DRAW)
//
//        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.PROJECTION])
//        glBufferData(GL_UNIFORM_BUFFER, glm.mat.Mat4.Companion.SIZE, GL_DYNAMIC_DRAW)
//
//        //Bind the static buffers.
//        glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.LIGHT, bufferName[Buffer.LIGHT], 0, LightBlock.SIZE.L)
//        glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, bufferName[Buffer.PROJECTION], 0, glm.mat.Mat4.Companion.SIZE.L)
//        glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName[Buffer.MATERIAL], 0, MaterialBlock.SIZE.L)
//
//        glBindBuffer(GL_UNIFORM_BUFFER)
//
//        glGenTextures(textureName)
//        createGaussianTextures(gl)
//        createShininessTexture(gl)
//    }
//
//    fun initializePrograms(gl: GL3) {
//
//        programs = Array(ShaderMode.MAX, { ProgramData(gl, shaderPairs[it]) })
//
//        unlit = UnlitProgData(gl, "unlit")
//    }
//
//    fun setupMaterials(gl: GL3) {
//
//        val mtls = UniformBlockArray(gl, MaterialBlock.SIZE, NUM_MATERIALS)
//
//        val mtl = MaterialBlock
//        MaterialBlock.diffuseColor = glm.vec._4.Vec4(1.0f, 0.673f, 0.043f, 1.0f)
//        MaterialBlock.specularColor = glm.vec._4.Vec4(1.0f, 0.673f, 0.043f, 1.0f).times(0.4f)
//        MaterialBlock.specularShininess = 0.125f
//        mtls[0] = MaterialBlock.toBuffer()
//
//        MaterialBlock.diffuseColor = glm.vec._4.Vec4(0.01f, 0.01f, 0.01f, 1.0f)
//        MaterialBlock.specularColor = glm.vec._4.Vec4(0.99f, 0.99f, 0.99f, 1.0f)
//        MaterialBlock.specularShininess = 0.125f
//        mtls[1] = MaterialBlock.toBuffer()
//
//        mtls.uploadBufferObject(gl, bufferName[Buffer.MATERIAL])
//        materialOffset = mtls.arrayOffset
//
//        mtls.dispose()
//    }
//
//    fun createGaussianTextures(gl: GL3) = with(gl) {
//
//        repeat(NUM_GAUSSIAN_TEXTURES) {
//            val cosAngleResolution = calcCosAngleResolution(it)
//            createGaussianTexture(gl, it, cosAngleResolution, 128)
//        }
//        glGenSampler(samplerName)
//        glSamplerParameteri(samplerName, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
//        glSamplerParameteri(samplerName, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
//        glSamplerParameteri(samplerName, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
//        glSamplerParameteri(samplerName, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
//    }
//
//    fun calcCosAngleResolution(level: Int): Int {
//        val cosAngleStart = 64
//        return cosAngleStart * glm.func.func_exponential.pow(2f, level.toFloat()).toInt()
//    }
//
//    fun createGaussianTexture(gl: GL3, index: Int, cosAngleResolution: Int, shininessResolution: Int) = with(gl) {
//
//        val textureData = buildGaussianData(cosAngleResolution, shininessResolution)
//
//        glBindTexture(GL_TEXTURE_1D, textureName[index])
//        glTexImage1D(GL_TEXTURE_1D, 0, GL_R8, cosAngleResolution, 0, GL_RED, GL_UNSIGNED_BYTE, textureData)
//        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_BASE_LEVEL, 0)
//        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAX_LEVEL, 0)
//        glBindTexture(GL_TEXTURE_1D)
//
//        textureData.destroy()
//    }
//
//    fun buildGaussianData(cosAngleResolution: Int, shininessResolution: Int): ByteBuffer {
//
//        val textureData = byteBufferBig(cosAngleResolution * shininessResolution)
//
//        repeat(shininessResolution) { iShin ->
//
//            val shininess = iShin / shininessResolution.f
//
//            repeat(cosAngleResolution) { iCosAng ->
//
//                val cosAng = iCosAng / (cosAngleResolution - 1).f
//                val angle = glm.func.func_trigonometric.acos(cosAng)
//                var exponent = angle / shininess
//                exponent = -(exponent * exponent)
//                val gaussianTerm = glm.func.func_exponential.exp(exponent)
//
//                textureData[iCosAng] = (gaussianTerm * 255f).b
//            }
//        }
//        return textureData
//    }
//
//    fun createShininessTexture(gl: GL3) = with(gl) {
//
//        val file = File(javaClass.getResource("/tut14/main.dds").toURI())
//
//        val image = DDSImage.read(file)
//
//        glBindTexture(GL_TEXTURE_2D, textureName[Texture.SHINE])
//
//        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, image.width, image.height, 0, GL_RED, GL_UNSIGNED_BYTE, image.getMipMap(0).data)
//
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0)
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0)
//
//        glBindTexture(GL_TEXTURE_2D)
//    }
//
//    override fun display(gl: GL3) = with(gl) {
//
//        lightTimer.update()
//
//        glClearBufferf(GL_COLOR, 0.75f, 0.75f, 1.0f, 1.0f)
//        glClearBufferf(GL_DEPTH)
//
//        val modelMatrix = MatrixStack(viewPole.calcMatrix())
//        val worldToCamMat = modelMatrix.top()
//
//        val lightData = LightBlock
//
//        LightBlock.ambientIntensity = glm.vec._4.Vec4(0.2f, 0.2f, 0.2f, 1.0f)
//        LightBlock.lightAttenuation = lightAttenuation
//
//        val globalLightDirection = glm.vec._3.Vec3(0.707f, 0.707f, 0.0f)
//
//        LightBlock.lights[0].cameraSpaceLightPos = worldToCamMat * glm.vec._4.Vec4(globalLightDirection, 0.0f)
//        LightBlock.lights[0].lightIntensity = glm.vec._4.Vec4(0.6f, 0.6f, 0.6f, 1.0f)
//
//        LightBlock.lights[1].cameraSpaceLightPos = worldToCamMat * calcLightPosition()
//        LightBlock.lights[1].lightIntensity = glm.vec._4.Vec4(0.4f, 0.4f, 0.4f, 1.0f)
//
//        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.LIGHT])
//        glBufferSubData(GL_UNIFORM_BUFFER, LightBlock.toBuffer())
//        glBindBuffer(GL_UNIFORM_BUFFER)
//
//        run {
//
//            val mesh = if (useInfinity) objectMesh else plane
//
//            glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName[Buffer.MATERIAL],
//                    currMaterial * materialOffset.L, MaterialBlock.SIZE.L)
//
//            modelMatrix run {
//
//                applyMatrix(objectPole.calcMatrix())
//                scale(if (useInfinity) 2.0f else 4.0f)
//
//                val normMatrix = modelMatrix.top().toMat3()
//                normMatrix.inverse_().transpose_()
//
//                val prog = programs[mode]
//
//                glUseProgram(prog.theProgram)
//                glUniformMatrix4f(prog.modelToCameraMatrixUnif, top())
//                glUniformMatrix3f(prog.normalModelToCameraMatrixUnif, normMatrix)
//
//                glActiveTexture(GL_TEXTURE0 + Semantic.Sampler.GAUSSIAN_TEXTURE)
//                glBindTexture(GL_TEXTURE_2D, textureName[currTexture])
//                glBindSampler(Semantic.Sampler.GAUSSIAN_TEXTURE, samplerName)
//
//                glActiveTexture(GL_TEXTURE0 + Semantic.Sampler.SHININESS_TEXTURE)
//                glBindTexture(GL_TEXTURE_2D, textureName[Texture.SHINE])
//                glBindSampler(Semantic.Sampler.SHININESS_TEXTURE, samplerName)
//
//                if (mode != ShaderMode.FIXED)
//                    mesh.render(gl, "lit-tex")
//                else
//                    mesh.render(gl, "lit")
//
//                glBindSampler(Semantic.Sampler.GAUSSIAN_TEXTURE)
//                glBindSampler(Semantic.Sampler.SHININESS_TEXTURE)
//                glBindTexture(GL_TEXTURE_2D)
//
//                glUseProgram()
//                glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL)
//            }
//        }
//
//        if (drawLights)
//
//            modelMatrix run {
//
//                translate(calcLightPosition())
//                scale(0.25f)
//
//                glUseProgram(unlit.theProgram)
//                glUniformMatrix4f(unlit.modelToCameraMatrixUnif, top())
//
//                val lightColor = glm.vec._4.Vec4(1.0f)
//                glUniform4f(unlit.objectColorUnif, lightColor)
//                cube.render(gl, "flat")
//
//                reset()
//                translate(globalLightDirection * 100.0f)
//                scale(5.0f)
//
//                glUniformMatrix4f(unlit.modelToCameraMatrixUnif, top())
//                cube.render(gl, "flat")
//
//                glUseProgram(0)
//            }
//
//        if (drawCameraPos)
//
//            modelMatrix run {
//
//                setIdentity()
//                translate(glm.vec._3.Vec3(0.0f, 0.0f, -viewPole.getView().radius))
//                scale(0.25f)
//
//                glDisable(GL_DEPTH_TEST)
//                glDepthMask(false)
//                glUseProgram(unlit.theProgram)
//                glUniformMatrix4f(unlit.modelToCameraMatrixUnif, top())
//                glUniform4f(unlit.objectColorUnif, 0.25f, 0.25f, 0.25f, 1.0f)
//                cube.render(gl, "flat")
//
//                glDepthMask(true)
//                glEnable(GL_DEPTH_TEST)
//                glUniform4f(unlit.objectColorUnif, 1.0f)
//                cube.render(gl, "flat")
//            }
//    }
//
//    fun calcLightPosition(): Vec4 {
//
//        val scale = glm.Glm.PIf * 2.0f
//
//        val timeThroughLoop = lightTimer.getAlpha()
//        val ret = glm.vec._4.Vec4(0.0f, lightHeight, 0.0f, 1.0f)
//
//        ret.x = glm.func.func_trigonometric.cos(timeThroughLoop * scale) * lightRadius
//        ret.z = glm.func.func_trigonometric.sin(timeThroughLoop * scale) * lightRadius
//
//        return ret
//    }
//
//    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl) {
//
//        val zNear = 1.0f
//        val zFar = 1_000f
//        val perspMatrix = MatrixStack()
//
//        val proj = perspMatrix.perspective(45.0f, w.f / h, zNear, zFar).top()
//
//        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.PROJECTION])
//        glBufferSubData(GL_UNIFORM_BUFFER, proj)
//        glBindBuffer(GL_UNIFORM_BUFFER)
//
//        glViewport(w, h)
//    }
//
//    override fun mousePressed(e: MouseEvent) {
//        viewPole.mousePressed(e)
//        objectPole.mousePressed(e)
//    }
//
//    override fun mouseDragged(e: MouseEvent) {
//        viewPole.mouseDragged(e)
//        objectPole.mouseDragged(e)
//    }
//
//    override fun mouseReleased(e: MouseEvent) {
//        viewPole.mouseReleased(e)
//        objectPole.mouseReleased(e)
//    }
//
//    override fun mouseWheelMoved(e: MouseEvent) {
//        viewPole.mouseWheel(e)
//    }
//
//    override fun keyPressed(e: KeyEvent) {
//
//        when (e.keyCode) {
//
//            KeyEvent.VK_ESCAPE -> quit()
//
//            KeyEvent.VK_P -> lightTimer.togglePause()
//            KeyEvent.VK_MINUS -> lightTimer.rewind(0.5f)
//            KeyEvent.VK_PLUS -> lightTimer.fastForward(0.5f)
//            KeyEvent.VK_T -> drawCameraPos = !drawCameraPos
//            KeyEvent.VK_G -> drawLights = !drawLights
//            KeyEvent.VK_Y -> useInfinity = !useInfinity
//
//            KeyEvent.VK_SPACE -> {
//                mode = (mode + 1) % ShaderMode.MAX
//                println(shaderModeNames[mode])
//            }
//        }
//
//        if (e.keyCode in KeyEvent.VK_1..KeyEvent.VK_9) {
//            var number = e.keyCode - KeyEvent.VK_1
//            if (number < NUM_GAUSSIAN_TEXTURES) {
//                println("Angle Resolution: " + calcCosAngleResolution(number))
//                currTexture = number
//            }
//            if (number >= 9 - NUM_MATERIALS) {
//                number -= 9 - NUM_MATERIALS
//                println("Material Number: " + number)
//                currMaterial = number
//            }
//        }
//
//        viewPole.keyPressed(e)
//    }
//
//    val shaderModeNames = arrayOf("Fixed Shininess with Gaussian Texture", "Texture Shininess with Gaussian Texture", "Texture Shininess with computed Gaussian")
//
//    override fun end(gl: GL3) = with(gl) {
//
//        repeat(ShaderMode.MAX) { glDeleteProgram(programs[it].theProgram) }
//        glDeleteProgram(unlit.theProgram)
//
//        glDeleteBuffers(bufferName)
//        glDeleteSampler(samplerName)
//        glDeleteTextures(textureName)
//
//        objectMesh.dispose(gl)
//        cube.dispose(gl)
//
//        destroyBuffers(bufferName, samplerName, textureName, LightBlock.buffer, MaterialBlock.buffer)
//    }
//
//    object ShaderMode {
//        val FIXED = 0
//        val TEXTURED = 1
//        val TEXTURED_COMPUTE = 2
//        val MAX = 3
//    }
//
//    class PerLight {
//
//        lateinit var cameraSpaceLightPos: Vec4
//        lateinit var lightIntensity: Vec4
//
//        fun to(buffer: ByteBuffer, offset: Int): ByteBuffer {
//            cameraSpaceLightPos.to(buffer, offset)
//            return lightIntensity.to(buffer, offset + glm.vec._4.Vec4.Companion.SIZE)
//        }
//
//        companion object {
//            val SIZE = glm.vec._4.Vec4.Companion.SIZE * 2
//        }
//    }
//
//    object LightBlock {
//
//        lateinit var ambientIntensity: Vec4
//        var lightAttenuation = 0f
//        var padding = FloatArray(3)
//        var lights = arrayOf(PerLight(), PerLight())
//
//        fun toBuffer(): ByteBuffer {
//            ambientIntensity to buffer
//            buffer.putFloat(glm.vec._4.Vec4.Companion.SIZE, lightAttenuation)
//            repeat(NUMBER_OF_LIGHTS) { lights[it].to(buffer, 2 * glm.vec._4.Vec4.Companion.SIZE + it * PerLight.SIZE) }
//            return buffer
//        }
//
//        val SIZE = glm.vec._4.Vec4.Companion.SIZE * 2 + NUMBER_OF_LIGHTS * PerLight.SIZE
//        val buffer = byteBufferBig(SIZE)
//    }
//
//    object MaterialBlock {
//
//        lateinit var diffuseColor: Vec4
//        lateinit var specularColor: Vec4
//        var specularShininess = 0f
//        var padding = FloatArray(3)
//
//        fun toBuffer(): ByteBuffer {
//            diffuseColor to buffer
//            specularColor.to(buffer, glm.vec._4.Vec4.Companion.SIZE)
//            return buffer.putFloat(2 * glm.vec._4.Vec4.Companion.SIZE, specularShininess)
//        }
//
//        val SIZE = 3 * glm.vec._4.Vec4.Companion.SIZE
//        val buffer = byteBufferBig(SIZE)
//    }
//
//    class ProgramData(gl: GL3, shaderPair: Pair<String, String>) {
//
//        val theProgram = programOf(gl, javaClass, "tut14", shaderPair.first, shaderPair.second)
//
//        val modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix")
//        val normalModelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "normalModelToCameraMatrix")
//
//        init {
//            with(gl) {
//                glUniformBlockBinding(
//                        theProgram,
//                        glGetUniformBlockIndex(theProgram, "Projection"),
//                        Semantic.Uniform.PROJECTION)
//                glUniformBlockBinding(
//                        theProgram,
//                        glGetUniformBlockIndex(theProgram, "Material"),
//                        Semantic.Uniform.MATERIAL)
//                glUniformBlockBinding(
//                        theProgram,
//                        glGetUniformBlockIndex(theProgram, "Light"),
//                        Semantic.Uniform.LIGHT)
//
//                glUseProgram(theProgram)
//                glUniform1i(
//                        glGetUniformLocation(theProgram, "gaussianTexture"),
//                        Semantic.Sampler.GAUSSIAN_TEXTURE)
//                glUniform1i(
//                        glGetUniformLocation(theProgram, "shininessTexture"),
//                        Semantic.Sampler.SHININESS_TEXTURE)
//                glUseProgram(theProgram)
//            }
//        }
//    }
//
//    class UnlitProgData(gl: GL3, shader: String) {
//
//        val theProgram = programOf(gl, javaClass, "tut14", shader + ".vert", shader + ".frag")
//
//        val objectColorUnif = gl.glGetUniformLocation(theProgram, "objectColor")
//        val modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix")
//
//        init {
//            gl.glUniformBlockBinding(
//                    theProgram,
//                    gl.glGetUniformBlockIndex(theProgram, "Projection"),
//                    Semantic.Uniform.PROJECTION)
//        }
//    }
//
//    companion object {
//        val NUMBER_OF_LIGHTS = 2
//        val NUM_MATERIALS = 2
//        val NUM_GAUSSIAN_TEXTURES = 4
//    }
//}