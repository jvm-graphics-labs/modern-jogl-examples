//--------------------------------------------------------------------------------------
// Order Independent Transparency with Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

layout (location = 0) in vec4 position;

layout(std140) uniform mvpMatrixes
{
    mat4 cameraToClipMatrix;
    mat4 modelToCameraMatrix;
};

vec3 ShadeVertex();

void main(void)
{
	//gl_Position = ftransform();
	gl_Position = viewMatrix * modelMatrix * position;
	gl_TexCoord[0].xyz = ShadeVertex();
}