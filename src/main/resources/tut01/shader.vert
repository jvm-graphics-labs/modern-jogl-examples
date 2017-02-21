#version 330

#include semantic.glsl

layout (location = POSITION) in vec4 position;

void main()
{
    gl_Position = position;
}