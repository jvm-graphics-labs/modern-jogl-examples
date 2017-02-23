#version 330

#include semantic.glsl

layout (location = POSITION) in vec4 position;
layout (location = COLOR) in vec4 color;

smooth out vec4 theColor;

uniform vec3 offset;
uniform mat4 perspectiveMatrix;

void main()
{
    vec4 cameraPos = position + vec4(offset, 0.0f);

    gl_Position = perspectiveMatrix * cameraPos;
    theColor = color;
}