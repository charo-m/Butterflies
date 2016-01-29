package co.charo.butterflies;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import javax.microedition.khronos.egl.EGLConfig;



public class MainActivity extends CardboardActivity implements CardboardView.StereoRenderer {

    private Vibrator vibrator;
    private CardboardOverlayView overlayView;
    private SceneData sceneData;
    private float[] view;
    private float[] headView;
    private float[] modelViewProjection;
    private float[] modelView;

    private static final String TAG = "MainActivity";

    // Activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        CardboardView cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        cardboardView.setRestoreGLStateEnabled(false);
        cardboardView.setRenderer(this);
        setCardboardView(cardboardView);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        overlayView = (CardboardOverlayView) findViewById(R.id.overlay);
        overlayView.show3DToast("Pull the magnet when you find an object.");
    }

    // CardboardView.StereoRenderer
    @Override
    public void onSurfaceCreated(EGLConfig config)
    {
        Log.i(TAG, "onSurfaceCreated");

        sceneData = new SceneData(this);
        sceneData.loadSkyGeometry();
        sceneData.loadSkyEffect();

        view = new float[16];
        headView = new float[16];
        modelViewProjection = new float[16];
        modelView = new float[16];
    }

    // CardboardView.StereoRenderer
    @Override
    public void onSurfaceChanged(int width, int height) {
        Log.i(TAG, "onSurfaceChanged");
    }

    // CardboardView.StereoRenderer
    @Override
    public void onNewFrame(HeadTransform headTransform) {


        headTransform.getHeadView(headView, 0);

    }

    // CardboardView.StereoRenderer
    @Override
    public void onDrawEye(Eye eye) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);


        // Apply the eye transformation to the camera.
        Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, sceneData.camera.transform, 0);


        // Build the ModelView and ModelViewProjection matrices
        float[] perspective = eye.getPerspective(sceneData.camera.zNear, sceneData.camera.zFar);
        Matrix.multiplyMM(modelView, 0, view, 0, sceneData.modelSky, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);


        // Update shader uniform
        sceneData.updateUniform("u_modelViewProjection", modelViewProjection);
        // Draw
        sceneData.drawSkyCube();

    }

    // CardboardView.StereoRenderer
    @Override
    public void onFinishFrame(Viewport viewport) {
    }

    // CardboardView.StereoRenderer
    @Override
    public void onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown");
    }

    // CardboardActivity (inherited from sensors.SensorConnection.SensorListener)
    @Override
    public void onCardboardTrigger() {
        Log.i(TAG, "onCardboardTrigger");


        overlayView.show3DToast("Hi there!");

        // Haptic feedback
        vibrator.vibrate(50);
    }


}
