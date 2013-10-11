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

layout(std140) uniform mvpMatrixes  {

    mat4 projectionMatrix;
    mat4 cameraMatrix;
};

void main(void)
{
     gl_Position = cameraMatrix * position;
}
