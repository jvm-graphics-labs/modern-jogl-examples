package glsl;

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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.URL;
import java.util.ArrayList;
import javax.media.opengl.GL3;

public class GLSLProgramObject {

//    protected Vector<Integer> _vertexShaders = new Vector<>();
//    protected Vector<Integer> _fragmentShaders = new Vector<>();
    protected ArrayList<Integer> _vertexShaders = new ArrayList<>();
    protected ArrayList<Integer> _fragmentShaders = new ArrayList<>();
    private Integer _progId;
//    private String shadersPath = "/shaders/";

    public GLSLProgramObject(GL3 gl3) {
        _progId = 0;
    }

    public GLSLProgramObject(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader) {

        this(gl3);

        attachVertexShader(gl3, shadersFilepath + vertexShader);
        attachFragmentShader(gl3, shadersFilepath + fragmentShader);

        initializeProgram(gl3, true);
    }

    public GLSLProgramObject(GL3 gl3, String shadersFilepath, String[] vertexShaders, String[] fragmentShaders) {

        this(gl3);

        for (String vertexShader : vertexShaders) {
            attachVertexShader(gl3, shadersFilepath + vertexShader);
        }
        for (String fragmentShader : fragmentShaders) {
            attachFragmentShader(gl3, shadersFilepath + fragmentShader);
        }

        initializeProgram(gl3, true);
    }

    public void destroy(GL3 gl3) {
        for (int i = 0; i < _vertexShaders.size(); i++) {
            gl3.glDeleteShader(_vertexShaders.get(i));
        }
        for (int i = 0; i < _fragmentShaders.size(); i++) {
            gl3.glDeleteShader(_fragmentShaders.get(i));
        }
        if (_progId != 0) {
            gl3.glDeleteProgram(_progId);
        }
    }

    public void bind(GL3 gl3) {
        gl3.glUseProgram(_progId);
    }

    public void unbind(GL3 gl3) {
        gl3.glUseProgram(0);
    }

    public void setUniform(GL3 gl3, String name, float[] val, int count) {
        int id = gl3.glGetUniformLocation(_progId, name);
        if (id == -1) {
            System.err.println("Warning: Invalid uniform parameter " + name);
            return;
        }
        switch (count) {
            case 1:
                gl3.glUniform1fv(id, 1, val, 0);
                break;
            case 2:
                gl3.glUniform2fv(id, 1, val, 0);
                break;
            case 3:
                gl3.glUniform3fv(id, 1, val, 0);
                break;
            case 4:
                gl3.glUniform4fv(id, 1, val, 0);
                break;
        }
    }

    public void setTextureUnit(GL3 gl3, String texname, int texunit) {
        int[] params = new int[]{0};
        gl3.glGetProgramiv(_progId, GL3.GL_LINK_STATUS, params, 0);
        if (params[0] != 1) {
            System.err.println("Error: setTextureUnit needs program to be linked.");
        }
        int id = gl3.glGetUniformLocation(_progId, texname);
        if (id == -1) {
            System.err.println("Warning: Invalid texture " + texname);
            return;
        }
        gl3.glUniform1i(id, texunit);
    }

    public void bindTexture(GL3 gl3, int target, String texname, int texid, int texunit) {
        gl3.glActiveTexture(GL3.GL_TEXTURE0 + texunit);
        gl3.glBindTexture(target, texid);
        setTextureUnit(gl3, texname, texunit);
        gl3.glActiveTexture(GL3.GL_TEXTURE0);
    }

    public void bindTexture2D(GL3 gl3, String texname, int texid, int texunit) {
        bindTexture(gl3, GL3.GL_TEXTURE_2D, texname, texid, texunit);
    }

    public void bindTexture3D(GL3 gl3, String texname, int texid, int texunit) {
        bindTexture(gl3, GL3.GL_TEXTURE_3D, texname, texid, texunit);
    }

    public void bindTextureRECT(GL3 gl3, String texname, int texid, int texunit) {
        bindTexture(gl3, GL3.GL_TEXTURE_RECTANGLE, texname, texid, texunit);
    }

    public final void attachVertexShader(GL3 gl3, String filename) {
//        URL fileURL = getClass().getClassLoader().getResource(
//                File.separator + "depthPeeling"
//                + File.separator + "shaders" + File.separator + filename);
//        String resourcePath = shadersPath + filename;
        String resourcePath = filename;
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
            } catch (FileNotFoundException fileNotFoundException) {
                System.err.println("Unable to find the shader file " + filename);
            } catch (IOException iOException) {
                System.err.println("Problem reading the shader file " + filename);
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException iOException) {
                    System.out.println("Problem closing the BufferedReader, " + filename);
                }
            }

            int iID = gl3.glCreateShader(GL3.GL_VERTEX_SHADER);

            String[] akProgramText = new String[1];
            // find and replace program name with "main"
            akProgramText[0] = content;

            int[] params = new int[]{0};

            int[] aiLength = new int[1];
            aiLength[0] = akProgramText[0].length();
            int iCount = 1;

            gl3.glShaderSource(iID, iCount, akProgramText, aiLength, 0);

            gl3.glCompileShader(iID);

            gl3.glGetShaderiv(iID, GL3.GL_COMPILE_STATUS, params, 0);

            if (params[0] != 1) {
                System.err.println(filename);
                System.err.println("compile status: " + params[0]);
                gl3.glGetShaderiv(iID, GL3.GL_INFO_LOG_LENGTH, params, 0);
                System.err.println("log length: " + params[0]);
                byte[] abInfoLog = new byte[params[0]];
                gl3.glGetShaderInfoLog(iID, params[0], params, 0, abInfoLog, 0);
                System.err.println(new String(abInfoLog));
                System.exit(-1);
            }
            _vertexShaders.add(iID);
        } else {
            System.err.println("Unable to find the shader file " + filename);
        }
    }

    public final void attachFragmentShader(GL3 gl3, String filename) {
//        URL fileURL = getClass().getClassLoader().getResource(
//                File.separator + "depthPeeling"
//                + File.separator + "shaders" + File.separator + filename);
//        String resourcePath = shadersPath + filename;
        String resourcePath = filename;
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
            } catch (FileNotFoundException fileNotFoundException) {
                System.err.println("Unable to find the shader file " + filename);
            } catch (IOException iOException) {
                System.err.println("Problem reading the shader file " + filename);
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException iOException) {
                    System.out.println("Problem closing the BufferedReader, " + filename);
                }
            }


            int iID = gl3.glCreateShader(GL3.GL_FRAGMENT_SHADER);

            String[] akProgramText = new String[1];
            // find and replace program name with "main"
            akProgramText[0] = content;

            int[] params = new int[]{0};

            int[] aiLength = new int[1];
            aiLength[0] = akProgramText[0].length();
            int iCount = 1;

            gl3.glShaderSource(iID, iCount, akProgramText, aiLength, 0);

            gl3.glCompileShader(iID);

            gl3.glGetShaderiv(iID, GL3.GL_COMPILE_STATUS, params, 0);

            if (params[0] != 1) {
                System.err.println(filename);
                System.err.println("compile status: " + params[0]);
                gl3.glGetShaderiv(iID, GL3.GL_INFO_LOG_LENGTH, params, 0);
                System.err.println("log length: " + params[0]);
                byte[] abInfoLog = new byte[params[0]];
                gl3.glGetShaderInfoLog(iID, params[0], params, 0, abInfoLog, 0);
                System.err.println(new String(abInfoLog));
                System.exit(-1);
            }
            _fragmentShaders.add(iID);
        } else {
            System.err.println("Unable to find the shader file " + filename);
        }
    }

    public final void initializeProgram(GL3 gl3, boolean cleanUp) {
        _progId = gl3.glCreateProgram();

        for (int i = 0; i < _vertexShaders.size(); i++) {
            gl3.glAttachShader(_progId, _vertexShaders.get(i));
        }

        for (int i = 0; i < _fragmentShaders.size(); i++) {
            gl3.glAttachShader(_progId, _fragmentShaders.get(i));
        }

        gl3.glLinkProgram(_progId);

        int[] params = new int[]{0};
        gl3.glGetProgramiv(_progId, GL3.GL_LINK_STATUS, params, 0);

        if (params[0] != 1) {

            System.err.println("link status: " + params[0]);
            gl3.glGetProgramiv(_progId, GL3.GL_INFO_LOG_LENGTH, params, 0);
            System.err.println("log length: " + params[0]);

            byte[] abInfoLog = new byte[params[0]];
            gl3.glGetProgramInfoLog(_progId, params[0], params, 0, abInfoLog, 0);
            System.err.println(new String(abInfoLog));
        }

        gl3.glValidateProgram(_progId);

        if (cleanUp) {
            for (int i = 0; i < _vertexShaders.size(); i++) {
                gl3.glDetachShader(_progId, _vertexShaders.get(i));
                gl3.glDeleteShader(_vertexShaders.get(i));
            }

            for (int i = 0; i < _fragmentShaders.size(); i++) {
                gl3.glDetachShader(_progId, _fragmentShaders.get(i));
                gl3.glDeleteShader(_fragmentShaders.get(i));
            }
        }
    }

    public Integer getProgramId() {
        return _progId;
    }
};
