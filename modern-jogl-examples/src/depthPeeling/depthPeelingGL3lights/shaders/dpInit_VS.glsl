//--------------------------------------------------------------------------------------
// Order Independent Transparency with Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

layout (location = 0) in vec3 position;
layout (location = 1) in vec3 normal;

uniform vec3 color;
uniform vec3 dirToLight;

smooth out vec4 interpColor;

layout(std140) uniform mvpMatrixes  {

    mat4 projectionMatrix;
    mat4 cameraMatrix;
};

void main(void) {

	gl_Position = projectionMatrix * cameraMatrix * vec4(position, 1.0);

        mat3 normalCameraMatrix = mat3(cameraMatrix);

        vec3 normalCameraSpace = normalize(normalCameraMatrix * normal);

        float cosIncidenceAngle = dot(normalCameraSpace, dirToLight);

        cosIncidenceAngle = clamp(cosIncidenceAngle, 0, 1);

        interpColor = vec4(1.0f) * cosIncidenceAngle * vec4(color, 1.0f);

        interpColor.a = 1.0;
}