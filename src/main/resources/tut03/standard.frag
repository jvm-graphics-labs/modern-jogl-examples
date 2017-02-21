#version 330

#include semantic.glsl

layout (location = FRAG_COLOR) out vec4 outputColor;

void main()
{
    outputColor = vec4(1.0f, 1.0f, 1.0f, 1.0f);
}