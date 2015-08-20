package co.charo.butterflies;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.Matrix;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import java.util.ArrayList;

/**
 * Created by charo on 8/6/15.
 */
public class SceneData {

    private final Context activityContext;
    private static final String TAG = "SceneData";

    private static final float CUBE_VERTEX_DATA[] = new float[]{
            // positions only so far

            // front
            -1.0f, -1.0f, 1.0f,
            1.0f, -1.0f, 1.0f,
            1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f, 1.0f,
            // back
            -1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            1.0f, 1.0f, -1.0f,
            -1.0f, 1.0f, -1.0f
    };

    private static final short CUBE_INDICES[] = new short[]{

            // front
            0, 1, 2,
            2, 3, 0,
            // top
            3, 2, 6,
            6, 7, 3,
            // back
            7, 6, 5,
            5, 4, 7,
            // bottom
            4, 5, 1,
            1, 0, 4,
            // left
            4, 0, 3,
            3, 7, 4,
            // right
            1, 5, 6,
            6, 2, 1
    };

    // Index for vertex attribute data provided to vertex shader.
    private enum VertexAttribIndex {
        POSITION(0),
        NORMAL(1),
        TEXCOORD(2);

        private int value;

        VertexAttribIndex(int v) {
            value = v;
        }

        public int getValue() {
            return value;
        }
    }


    private enum UniformType {
        UNIFORM1F,
        UNIFORM3FV,
        UNIFORMMATRIX4FV,
        UNIFORM1I;
    }


    public class Geometry {
        public int[] vao;
        public int[] vertexVbo;
        public int[] indexVbo;
        public FloatBuffer vertexData;
        public ShortBuffer indices;

        public Geometry() {
            vao = new int[1];
            vertexVbo = new int[1];
            indexVbo = new int[1];
        }
    }


    public class Uniform {
        public UniformType type;
        public String name;
        public int location;
        public int count;
        public float[] data;
        public int texHandle;
    }


    public class Effect {
        public int program;
        public ArrayList<Uniform> uniforms;
    }


    public class Camera {
        public float [] transform;
        public float zNear;
        public float zFar;
    }

    public Geometry skyBox;
    public Effect skyEffect;
    public float[] modelSky;
    public Camera camera;

    // Constructor
    public SceneData(final Context context) {
        activityContext = context;
        modelSky = new float[16];
        Matrix.setIdentityM(modelSky, 0);
        camera = new Camera();
        camera.transform = new float[16];
        Matrix.setLookAtM(camera.transform, 0,
                          0.0f, 0.0f, 0.01f, // origin
                          0.0f, 0.0f, 0.0f,  // lookAt point);
                          0.0f, 1.0f, 0.0f); // up vector
        camera.zNear = 0.1f;
        camera.zFar = 100.0f;
    }

    public void loadSkyGeometry() {

        skyBox = new Geometry();

        // Put data into memory allocated on the native heap so OpenGL can access it.

        ByteBuffer bbv = ByteBuffer.allocateDirect(CUBE_VERTEX_DATA.length * 4);
        bbv.order(ByteOrder.nativeOrder());
        skyBox.vertexData = bbv.asFloatBuffer();
        skyBox.vertexData.put(CUBE_VERTEX_DATA);    // copy from Java heap to native heap
        skyBox.vertexData.position(0);

        ByteBuffer bbi = ByteBuffer.allocateDirect(CUBE_INDICES.length * 2);
        bbi.order(ByteOrder.nativeOrder());
        skyBox.indices = bbi.asShortBuffer();
        skyBox.indices.put(CUBE_INDICES);          // copy from Java heap to native heap
        skyBox.indices.position(0);


        // Create and bind vao
        GLES30.glGenVertexArrays(1, skyBox.vao, 0);
        GLES30.glBindVertexArray(skyBox.vao[0]);

        // Create and bind vertex data buffer
        GLES30.glGenBuffers(1, skyBox.vertexVbo, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, skyBox.vertexVbo[0]);
        skyBox.vertexData.position(0);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, CUBE_VERTEX_DATA.length * 4, skyBox.vertexData, GLES30.GL_STATIC_DRAW);

        GLES30.glEnableVertexAttribArray(VertexAttribIndex.POSITION.getValue());
        GLES30.glVertexAttribPointer(VertexAttribIndex.POSITION.getValue(), 3, GLES30.GL_FLOAT, false, 0, 0);

        // Create and bind triangle index buffer
        GLES30.glGenBuffers(1, skyBox.indexVbo, 0);
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, skyBox.indexVbo[0]);
        skyBox.indices.position(0);
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, CUBE_INDICES.length * 2, skyBox.indices, GLES30.GL_STATIC_DRAW);

        // Unbind vao
        //GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
        GLES30.glBindVertexArray(0);

        checkGLError("Geometry loaded");

    }

    private Uniform loadTexture(final String resourceName) {
        Uniform texUniform;

        int resourceId = activityContext.getResources().getIdentifier(resourceName,
                "drawable",
                activityContext.getPackageName());
        if (resourceId != 0) {

            final int[] texHandle = new int[1];
            GLES30.glGenTextures(1, texHandle, 0);

            if (texHandle[0] != 0) {

                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inScaled = false;   // Don't do any scaling based on resource folder.
                //options.inPremultiplied = false;

                final Bitmap bitmap = BitmapFactory.decodeResource(activityContext.getResources(),
                        resourceId, options);
                //Bitmap.Config conf = bitmap.getConfig();
                //boolean hasalpha = bitmap.hasAlpha();

                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texHandle[0]);

                // Set params
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

                // Load pixels into GL
                GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);

                // Can now free native memory and reset reference
                bitmap.recycle();


                texUniform = new Uniform();
                texUniform.type = UniformType.UNIFORM1I;  // 1i is texture, because the "data" is an integer handle
                texUniform.texHandle = texHandle[0];
                checkGLError("Texture loaded");
                return texUniform;

            }
            return null;
        };

        return null;
    }

    // Returns content of text file in String object or null if error.
    private String readRawTextFile(final String fileName) {
        int resId = activityContext.getResources().getIdentifier(fileName,
                "raw",
                activityContext.getPackageName());
        InputStream inputStream = activityContext.getResources().openRawResource(resId);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private int loadGLShader(final int type, final String fileName) {
        String code = readRawTextFile(fileName);
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, code);
        GLES30.glCompileShader(shader);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e(TAG, "Error compiling shader: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            shader = 0;
        }

        if (shader == 0) {
            throw new RuntimeException("Error creating shader.");
        }

        return shader;
    }

    public void loadSkyEffect() {

        skyEffect = new Effect();
        skyEffect.uniforms = new ArrayList<Uniform>(2);

        String texPath = "pano_20150701_treehouse";
        Uniform texUniform = loadTexture(texPath);
        if (texUniform == null) {
            Log.e(TAG, "Failed to load texture " + texPath);
        } else {
            texUniform.name = "u_equirectangularPhotosphereTexture";
            skyEffect.uniforms.add(texUniform);
        }

        Uniform matrixUniform = new Uniform();
        matrixUniform.type = UniformType.UNIFORMMATRIX4FV;
        matrixUniform.count = 1;
        matrixUniform.name = "u_modelViewProjection";
        skyEffect.uniforms.add(matrixUniform);

        int vertShader = loadGLShader(GLES30.GL_VERTEX_SHADER, "sky_vertex_shader");
        int fragShader = loadGLShader(GLES30.GL_FRAGMENT_SHADER, "sky_fragment_shader");
        skyEffect.program = GLES30.glCreateProgram();
        GLES30.glAttachShader(skyEffect.program, vertShader);
        GLES30.glAttachShader(skyEffect.program, fragShader);

        // Explicitly bind vertex attribute index to variable name.  Must be done prior to linking.
        GLES30.glBindAttribLocation(skyEffect.program, VertexAttribIndex.POSITION.getValue(), "a_position");

        GLES30.glLinkProgram(skyEffect.program);

        // Get uniform locations
        for (Uniform uni : skyEffect.uniforms) {
            uni.location = GLES30.glGetUniformLocation(skyEffect.program, uni.name);
        }

        checkGLError("Effect loaded");
    }

    public void updateUniform(final String name, final float data[]) {
        for (Uniform uni : skyEffect.uniforms) {
            if (uni.name.equals(name)) {
                uni.data = data;
            }
        }
    }

    public void drawSkyCube() {
        // Use program and set uniforms
        GLES30.glUseProgram(skyEffect.program);
        //GLES30.glUniform1i(skyEffect.uniforms.get(0).location, skyEffect.uniforms.get(0).texHandle);
        int texUnit = 0; // GL_TEXTURE0;
        GLES30.glUniform1i(skyEffect.uniforms.get(0).location, texUnit);
        GLES30.glUniformMatrix4fv(skyEffect.uniforms.get(1).location, 1, false, skyEffect.uniforms.get(1).data, 0);
        checkGLError("Use program, set uniforms");

        // Bind texture
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        int test = skyEffect.uniforms.get(0).texHandle;
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, skyEffect.uniforms.get(0).texHandle);
        checkGLError("Bind texture");

        // Bind vao
        GLES30.glBindVertexArray(skyBox.vao[0]);
        checkGLError("Bind vao");

        // Draw
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, CUBE_INDICES.length, GLES30.GL_UNSIGNED_SHORT, 0);

        GLES30.glBindVertexArray(0);

        checkGLError("Draw sky");
    }

   public static void checkGLError(String label) {
        int error;
        while ((error = GLES30.glGetError()) != GLES30.GL_NO_ERROR) {
            Log.e(TAG, label + ": glError " + error);
            throw new RuntimeException(label + ": glError " + error);
        }
    }

}
