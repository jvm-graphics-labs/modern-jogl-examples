#version 330

#include semantic.glsl

layout (location = POSITION) in vec4 position;
layout (location = COLOR) in vec4 color;

smooth out vec4 theColor;

void main()
{
    gl_Position = position;
    theColor = color;
}