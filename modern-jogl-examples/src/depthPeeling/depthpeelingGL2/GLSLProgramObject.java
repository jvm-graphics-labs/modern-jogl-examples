package depthPeeling.depthpeelingGL2;

// Translated from C++ Version see below:
//
// GLSLProgramObject.h - Wrapper for GLSL program objects
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
////////////////////////////////////////////////////////////////////////////////
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;

import javax.media.opengl.GL2;

import java.net.URL;

public class GLSLProgramObject {

    private String shadersFilepath = "/depthpeeling/depthpeelingGL2/shaders/";
    
    public GLSLProgramObject() {
        _progId = 0;
    }

    public void destroy(GL2 gl) {
        for (int i = 0; i < _vertexShaders.size(); i++) {
            gl.glDeleteShader(_vertexShaders.elementAt(i));
        }
        for (int i = 0; i < _fragmentShaders.size(); i++) {
            gl.glDeleteShader(_fragmentShaders.elementAt(i));
        }
        if (_progId != 0) {
            gl.glDeleteProgram(_progId);
        }
    }

    public void bind(GL2 gl) {
        gl.glUseProgram(_progId);
    }

    public void unbind(GL2 gl) {
        gl.glUseProgram(0);
    }

    public void setUniform(GL2 gl, String name, float[] val, int count) {
        int id = gl.glGetUniformLocation(_progId, name);
        if (id == -1) {
            System.err.println("Warning: Invalid uniform parameter " + name);
            return;
        }
        switch (count) {
            case 1:
                gl.glUniform1fv(id, 1, val, 0);
                break;
            case 2:
                gl.glUniform2fv(id, 1, val, 0);
                break;
            case 3:
                gl.glUniform3fv(id, 1, val, 0);
                break;
            case 4:
                gl.glUniform4fv(id, 1, val, 0);
                break;
        }
    }

    public void setTextureUnit(GL2 gl, String texname, int texunit) {
        int[] params = new int[]{0};
        gl.glGetProgramiv(_progId, GL2.GL_LINK_STATUS, params, 0);
        if (params[0] != 1) {
            System.err.println("Error: setTextureUnit needs program to be linked.");
        }
        int id = gl.glGetUniformLocation(_progId, texname);
        if (id == -1) {
            System.err.println("Warning: Invalid texture " + texname);
            return;
        }
        gl.glUniform1i(id, texunit);
    }

    public void bindTexture(GL2 gl, int target, String texname, int texid, int texunit) {
        gl.glActiveTexture(GL2.GL_TEXTURE0 + texunit);
        gl.glBindTexture(target, texid);
        setTextureUnit(gl, texname, texunit);
        gl.glActiveTexture(GL2.GL_TEXTURE0);
    }

    public void bindTexture2D(GL2 gl, String texname, int texid, int texunit) {
        bindTexture(gl, GL2.GL_TEXTURE_2D, texname, texid, texunit);
    }

    public void bindTexture3D(GL2 gl, String texname, int texid, int texunit) {
        bindTexture(gl, GL2.GL_TEXTURE_3D, texname, texid, texunit);
    }

    public void bindTextureRECT(GL2 gl, String texname, int texid, int texunit) {
        bindTexture(gl, GL2.GL_TEXTURE_RECTANGLE_ARB, texname, texid, texunit);
    }

    public void attachVertexShader(GL2 gl, String filename) {
//        URL fileURL = getClass().getClassLoader().getResource(
//                File.separator + "depthPeeling"
//                + File.separator + "shaders" + File.separator + filename);
        String resourcePath = shadersFilepath + filename;
        URL fileURL = getClass().getResource(resourcePath);
        if (fileURL != null) {
            String content = "";
            BufferedReader input = null;
            try {

                input = new BufferedReader(new InputStreamReader(fileURL.openStream()));
                String line = null;

                while ((line = input.readLine()) != null) {
                    content += line + "\n";
                }
            } catch (FileNotFoundException kFNF) {
                System.err.println("Unable to find the shader file " + filename);
            } catch (IOException kIO) {
                System.err.println("Problem reading the shader file " + filename);
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException closee) {
                }
            }


            int iID = gl.glCreateShader(GL2.GL_VERTEX_SHADER);

            String[] akProgramText = new String[1];
            // find and replace program name with "main"
            akProgramText[0] = content;

            int[] params = new int[]{0};

            int[] aiLength = new int[1];
            aiLength[0] = akProgramText[0].length();
            int iCount = 1;
            gl.glShaderSource(iID, iCount, akProgramText, aiLength, 0);
            gl.glCompileShader(iID);
            gl.glGetShaderiv(iID, GL2.GL_COMPILE_STATUS, params, 0);
            if (params[0] != 1) {
                System.err.println(filename);
                System.err.println("compile status: " + params[0]);
                gl.glGetShaderiv(iID, GL2.GL_INFO_LOG_LENGTH, params, 0);
                System.err.println("log length: " + params[0]);
                byte[] abInfoLog = new byte[params[0]];
                gl.glGetShaderInfoLog(iID, params[0], params, 0, abInfoLog, 0);
                System.err.println(new String(abInfoLog));
                System.exit(-1);
            }
            _vertexShaders.add(iID);
        } else {
            System.err.println("Unable to find the shader file " + filename);
        }
    }

    public void attachFragmentShader(GL2 gl, String filename) {
//        URL fileURL = getClass().getClassLoader().getResource(
//                File.separator + "depthPeeling"
//                + File.separator + "shaders" + File.separator + filename);
        String resourcePath = shadersFilepath + filename;
        URL fileURL = getClass().getResource(resourcePath);
        if (fileURL != null) {
            String content = "";
            BufferedReader input = null;
            try {

                input = new BufferedReader(new InputStreamReader(fileURL.openStream()));
                String line = null;

                while ((line = input.readLine()) != null) {
                    content += line + "\n";
                }
            } catch (FileNotFoundException kFNF) {
                System.err.println("Unable to find the shader file " + filename);
            } catch (IOException kIO) {
                System.err.println("Problem reading the shader file " + filename);
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException closee) {
                }
            }


            int iID = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);

            String[] akProgramText = new String[1];
            // find and replace program name with "main"
            akProgramText[0] = content;

            int[] params = new int[]{0};

            int[] aiLength = new int[1];
            aiLength[0] = akProgramText[0].length();
            int iCount = 1;
            gl.glShaderSource(iID, iCount, akProgramText, aiLength, 0);
            gl.glCompileShader(iID);
            gl.glGetShaderiv(iID, GL2.GL_COMPILE_STATUS, params, 0);
            if (params[0] != 1) {
                System.err.println(filename);
                System.err.println("compile status: " + params[0]);
                gl.glGetShaderiv(iID, GL2.GL_INFO_LOG_LENGTH, params, 0);
                System.err.println("log length: " + params[0]);
                byte[] abInfoLog = new byte[params[0]];
                gl.glGetShaderInfoLog(iID, params[0], params, 0, abInfoLog, 0);
                System.err.println(new String(abInfoLog));
                System.exit(-1);
            }
            _fragmentShaders.add(iID);
        } else {
            System.err.println("Unable to find the shader file " + filename);
        }
    }

    public void link(GL2 gl) {
        _progId = gl.glCreateProgram();

        for (int i = 0; i < _vertexShaders.size(); i++) {
            gl.glAttachShader(_progId, _vertexShaders.elementAt(i));
        }

        for (int i = 0; i < _fragmentShaders.size(); i++) {
            gl.glAttachShader(_progId, _fragmentShaders.elementAt(i));
        }

        gl.glLinkProgram(_progId);

        int[] params = new int[]{0};
        gl.glGetProgramiv(_progId, GL2.GL_LINK_STATUS, params, 0);

        if (params[0] != 1) {

            System.err.println("link status: " + params[0]);
            gl.glGetProgramiv(_progId, GL2.GL_INFO_LOG_LENGTH, params, 0);
            System.err.println("log length: " + params[0]);

            byte[] abInfoLog = new byte[params[0]];
            gl.glGetProgramInfoLog(_progId, params[0], params, 0, abInfoLog, 0);
            System.err.println(new String(abInfoLog));
        }
    }

    public Integer getProgId() {
        return _progId;
    }
    protected Vector<Integer> _vertexShaders = new Vector<Integer>();
    protected Vector<Integer> _fragmentShaders = new Vector<Integer>();
    protected Integer _progId;
};
