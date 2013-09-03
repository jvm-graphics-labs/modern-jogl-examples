#version 330

layout (location = 0) in vec4 position;
layout (location = 1) in vec2 vertexUV;

out vec2 fragmentUV;

void main()
{
    gl_Position = position;
   
    fragmentUV = vertexUV;
}

