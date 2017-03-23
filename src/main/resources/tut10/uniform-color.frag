
#version 330

#include semantic.glsl


uniform vec4 objectColor;

layout (location = FRAG_COLOR) out vec4 outputColor;

void main()
{
    outputColor = objectColor;
}