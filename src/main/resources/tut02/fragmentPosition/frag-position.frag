#version 330

#include semantic.glsl

layout (location = FRAG_COLOR) out vec4 outputColor;

void main()
{
    float lerpValue = gl_FragCoord.y / 500.0f;

    outputColor = mix(vec4(1.0f, 1.0f, 1.0f, 1.0f), vec4(0.2f, 0.2, 0.2, 1.0f), lerpValue);
}