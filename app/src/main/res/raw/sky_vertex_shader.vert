
attribute vec3 a_position;

uniform mat4 u_modelViewProjection;

varying vec3 v_texCoord;

void main() {
    v_texCoord = normalize(a_position);
    gl_Position = u_modelViewProjection * vec4(a_position.xyz, 1.0);
}