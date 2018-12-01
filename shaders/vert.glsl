#version 450

layout (location = 0) in vec3 position;
layout (location = 1) in vec2 tex_coord;
layout (location = 2) in vec3 normal;
out vec2 tc;
out vec3 varyingNormal; // eye-space vertex normal
out vec3 varyingLightDir; // vector pointing to the light
out vec3 varyingVertPos; // vertex position in eye-space
out vec3 varyingHalfVector; // additional varying output
out vec4 shadowCoordinates;

struct PositionalLight
{
    vec4 ambient;
    vec4 diffuse;
    vec4 specular;
    vec3 position;
};

uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform mat4 mv_matrix;
uniform mat4 proj_matrix;
uniform mat4 norm_matrix; // for transforming normals
uniform mat4 shadowMVP2;
layout (binding = 0) uniform sampler2DShadow shTex;
layout (binding = 1) uniform sampler2D s;

void main(void)
{
    varyingVertPos = (mv_matrix * vec4(position, 1.0)).xyz;
    varyingLightDir = light.position - varyingVertPos;
    varyingNormal = (norm_matrix * vec4(normal, 1.0)).xyz;
    varyingHalfVector = (varyingLightDir + (-varyingVertPos)).xyz;
    shadowCoordinates = shadowMVP2 * vec4(position, 1.0);

    gl_Position = proj_matrix * mv_matrix * vec4(position, 1.0);
	tc = tex_coord;
}