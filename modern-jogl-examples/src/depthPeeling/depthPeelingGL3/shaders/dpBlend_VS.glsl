//--------------------------------------------------------------------------------------
// Order Independent Transparency with Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

layout (location = 0) in vec4 position;

uniform mat4 modelToClipMatrix;

void main(void)
{
     gl_Position = modelToClipMatrix * position;
}