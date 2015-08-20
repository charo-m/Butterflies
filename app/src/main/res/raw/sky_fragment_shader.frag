precision mediump float;

const float m_PI  = 3.1415926535897932384626433832795;
const float inv2PI = 1.0/(2.0*m_PI);
const float invPI = 1.0/m_PI;
const float halfPI = m_PI/2.0;

varying vec3 v_texCoord;
uniform sampler2D u_equirectangularPhotosphereTexture;

// Do normal 2D texture lookup after converting 3D coordinates to longitude
// and lattitude.  Assumes texture is equirectangular 360x180 photosphere image.

void main()
{

     float r = length(v_texCoord);


     // Spherical coordinates
     float lon = atan(v_texCoord.x, v_texCoord.z);    // theta, or heading
     float lat = acos(v_texCoord.y / r) - halfPI;     // phi - 90, or pitch


     // Map longitude (azimuth) -180-180 to 0-1
     float x = (lon + m_PI) * inv2PI;

     // Map lattitude (inclination or 90 - zenith) from -90-90 to 0-1
     float y = (lat + halfPI) * invPI;

     gl_FragColor.rgb = texture2D(u_equirectangularPhotosphereTexture, vec2(x,y)).rgb;
     gl_FragColor.a = 1.0;
     //gl_FragColor = vec4(vec2(x,y), 0.0, 1.0);
}