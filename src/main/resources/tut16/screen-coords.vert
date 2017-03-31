
#version 330

#include semantic.glsl


layout(std140) uniform;

layout(location = POSITION) in vec2 position;
layout(location = TEX_COORD) in vec2 texCoord;

layout(std140) uniform Projection
{
	mat4 cameraToClipMatrix;
};

out vec2 colorCoord;

void main()
{
	gl_Position = cameraToClipMatrix * vec4(position, 0.0, 1.0);
	colorCoord = texCoord;
}
