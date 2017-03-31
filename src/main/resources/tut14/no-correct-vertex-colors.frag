
#version 330

#include semantic.glsl


noperspective in vec4 theColor;

layout (location = FRAG_COLOR) out vec4 outputColor;

void main()
{
    outputColor = theColor;
}
