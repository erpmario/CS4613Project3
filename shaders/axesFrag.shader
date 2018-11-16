#version 450

in vec2 col;
out vec4 color;

uniform mat4 mv_matrix;
uniform mat4 proj_matrix;

void main(void)
{
	color = col;
}