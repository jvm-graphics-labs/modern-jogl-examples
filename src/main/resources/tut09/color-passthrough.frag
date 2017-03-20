
#version 330

#include semantic.glsl


smooth in vec4 interpColor;

layout (location = FRAG_COLOR) out vec4 outputColor;

void main()
{
    outputColor = interpColor;
}