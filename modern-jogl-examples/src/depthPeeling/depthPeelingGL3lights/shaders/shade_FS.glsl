//--------------------------------------------------------------------------------------
// Order Independent Transparency Fragment Shader
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

uniform float Alpha;

vec4 ShadeFragment()
{
	vec4 color;
	color.rgb = vec3(.4,.85,.0);
	color.a = Alpha;
	return color;
}