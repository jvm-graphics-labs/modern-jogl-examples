
#version 330

#include semantic.glsl


in vec2 colorCoord;

uniform sampler2D colorTexture;

layout (location = FRAG_COLOR) out vec4 outputColor;

void main()
{
	outputColor = texture(colorTexture, colorCoord);
	//outputColor = vec4(1, 0, 0, 1);
}
