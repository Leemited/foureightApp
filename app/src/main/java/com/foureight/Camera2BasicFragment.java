package com.foureight;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import gun0912.tedbottompicker.TedBottomPicker;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static android.content.Context.MODE_PRIVATE;
import static android.support.v4.math.MathUtils.clamp;
import static com.foureight.CameraActivity.getExtension;

public class Camera2BasicFragment extends Fragment
        implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback{

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";

    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray(); //전면
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray(); //후면
    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private static final String CAMERA_FRONT = "1";
    private static final String CAMERA_BACK = "0";

    CameraManager mCameraManager;

    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "Camera2BasicFragment";

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private int mWidth = 0;
    private int mHeight = 0;

    private int previewRotate = 0;

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            Log.d(TAG, "onSurfaceTextureAvailable: " + width +"//"+height +"//"+((double)width/height));
            SharedPreferences checkLensFacing = getActivity().getSharedPreferences("lensFacingCheck", MODE_PRIVATE);
            int getLensFacing = checkLensFacing.getInt("lensFacing",1);
            if(getLensFacing != mCameraLensFacingDirection){
                mCameraLensFacingDirection = getLensFacing;
            }
            previewRotate = getActivity().getWindowManager().getDefaultDisplay().getRotation();

            if(previewRotate != Surface.ROTATION_0 && previewRotate != Surface.ROTATION_180) {
                Log.d(TAG, "onSurfaceTextureAvailable: 1111");
                mWidth = width;
                mHeight = height;
            }else{
                Log.d(TAG, "onSurfaceTextureAvailable: 2222");
                mWidth = width;
                mHeight = height; //+ getSoftMenuHeight();
            }
            Log.d(TAG, "onSurfaceTextureAvailable: " + mWidth +"//"+mHeight +"//"+((double)mHeight/mWidth));
            openCamera(mWidth, mHeight);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            previewRotate = getActivity().getWindowManager().getDefaultDisplay().getRotation();

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
            int displayRotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
            //Log.d(TAG, "onSurfaceTextureUpdated: " + displayRotation+"//"+previewRotate + "//"+mIsRecordingVideo);
            if(mIsRecordingVideo==false){
                if((displayRotation==1||displayRotation==3) && (previewRotate == 1 || previewRotate == 3)){
                    if(displayRotation != previewRotate) {
                        Log.d(TAG, "onSurfaceTextureUpdated: " + displayRotation + "//" + previewRotate);
                        previewRotate = displayRotation;
                        closeCamera();
                        reopenCamera();
                    }
                }
            }
            /*if(previewRotate != displayRotation){
                previewRotate = displayRotation;
                closeCamera();
                reopenCamera();
            }*/
        }

    };

    /**
     * ID of the current {@link CameraDevice}.
     */
    private String mCameraId;

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;

    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * The {@link android.util.Size} of camera preview.
     */
    private Size mPreviewSize;

    private Size mVideoSize;

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            Log.d(TAG, "onOpened");
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
            /* video
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            startPreview();
            if (null != mTextureView) {
                configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
            }
             */
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * An {@link ImageReader} that handles still image capture.
     */
    private ImageReader mImageReader;

    /**
     * This is the output file for our picture.
     */
    private File mFile;

    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     * 이미지 저장
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            int displayRotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
            mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile, mCameraLensFacingDirection,getActivity(),displayRotation));
            //getActivity().getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(mFile)));
        }

    };

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
     */
    private CaptureRequest mPreviewRequest;

    /**
     * The current state of camera state for taking pictures.
     *
     * @see #mCaptureCallback
     */
    private int mState = STATE_PREVIEW;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * Whether the current camera device supports Flash or not.
     */
    private boolean mFlashSupported;

    /**
     * Orientation of the camera sensor
     */
    private int mSensorOrientation;

    private int mCameraLensFacingDirection = 1;
    private String mb_id,title,cate1,cate2,url,path,sd,file_name,filename,mPath,imgPath,imgName,type1,type2,videoname,wr_price,wr_price2,pd_price_type;
    private int camCount = 0,value=0,serverResponseCode = 0,timer = 0, timer2 = 0,returnVal = 0,camUpCount = 0;
    private FileWrite fileWrite;
    private TextView textView;
    private Uri selPhotoUri;
    private byte[] bytes;
    private Button skipBtn,videoBtn,camBtn,photoBtn,cam_toggle,saveBtn;

    private Range<Integer> fpsRange; //AF RANGE

    MediaRecorder mMediaRecorder;

    private String mNextVideoAbsolutePath;
    private boolean mIsRecordingVideo;
    String sSec;
    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            /*if(camCount > 5){
                Log.d(TAG, "process: ");
                showToast("사진은 5개만 등록 가능합니다.");
            }else {*/
                switch (mState) {
                    case STATE_PREVIEW: {
                        // We have nothing to do when the camera preview is working normally.
                        break;
                    }
                    case STATE_WAITING_LOCK: {
                        Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                        if (afState == null) {
                            captureStillPicture();
                        } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                            // CONTROL_AE_STATE can be null on some devices
                            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                            if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                                mState = STATE_PICTURE_TAKEN;
                                captureStillPicture();
                            } else {
                                runPrecaptureSequence();
                            }
                        }
                        break;
                    }
                    case STATE_WAITING_PRECAPTURE: {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                                aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                            mState = STATE_WAITING_NON_PRECAPTURE;
                        }
                        break;
                    }
                    case STATE_WAITING_NON_PRECAPTURE: {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        }
                        break;
                    }
                }
            //}
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private static Size chooseVideoSize(Size[] choices) {
        Log.d(TAG, "chooseVideoSize: " + choices[0].getWidth()+"//"+choices[0].getHeight());
        for (Size size : choices) {
            Log.d(TAG, "chooseVideoSize: AA " + (size.getHeight() * 4 / 3) + "//" + size.getWidth() +"//"+ size.getHeight());
            if(size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080){
                Log.d(TAG, "chooseVideoSize: size = " +size.getWidth() + "//"+ size.getHeight());
                return size;
            }
        }
        return choices[choices.length - 1];
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight, int maxWidth, int maxHeight,Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();

        //그냥 보내면?
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        double ratio = 0;
        ratio = (double) textureViewWidth / textureViewHeight;
        double mRatio = (double) maxWidth / maxHeight;

        double ASPECT_TOLERANCE = 0.3; // 2020 3 6 17 40
        //double ASPECT_TOLERANCE = 0.1;
        double minDiff = Double.MAX_VALUE;
        for (Size option : choices) {
            Log.d(TAG, "chooseOptimalSize: optionSize = " + option.getWidth() + "//" + option.getHeight() +"//"+textureViewWidth +"//"+textureViewHeight+"//"+maxWidth+"//"+maxHeight);
            double cRatio = (double) option.getWidth() / option.getHeight();
            Log.d(TAG, "chooseOptimalSize: 비율 = " + ratio+"//"+mRatio+"//"+cRatio +"||"+Math.abs(mRatio - ratio) +"||"+Math.abs(cRatio - ratio));
            if(Math.abs(mRatio - ratio) > ASPECT_TOLERANCE && Math.abs(cRatio - ratio) > ASPECT_TOLERANCE) { //최적 사이즈 비율과 내 화면 비율의 차이비교 가장 근접한 사이즈 비교
                continue; // 0.05차이 이상의 근접값은 패스
            }
            /*if(option.getWidth() > textureViewWidth && option.getHeight() > textureViewHeight) { //프리뷰랑 큰사이즈는 패스
                if(option.getWidth() > maxWidth || option.getHeight() > maxHeight) {//최대값도 체크
                    Log.d(TAG, "chooseOptimalSize: pass?? " + option);
                    continue;
                }
            }*/
            Log.d(TAG, "chooseOptimalSize: check" + Math.abs(option.getWidth() - textureViewWidth) +"//"+minDiff);
            if(Math.abs(option.getWidth() - textureViewWidth) < minDiff){
                if (option.getWidth() >= textureViewWidth && option.getHeight() >= textureViewHeight) {
                    Log.d(TAG, "chooseOptimalSize: big ! " + option);
                    bigEnough.add(option);
                } else {
                    Log.d(TAG, "chooseOptimalSize: notbig ! " + option);
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            Log.d(TAG, "setUpMediaRecorder: 큰사이즈 중 작은거 :" + Collections.min(bigEnough, new CompareSizesByArea()).getWidth() + "//" + Collections.min(bigEnough, new CompareSizesByArea()).getHeight());
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            Log.d(TAG, "setUpMediaRecorder: 작은사이즈 중 가장 큰거 :" + Collections.max(notBigEnough, new CompareSizesByArea()).getWidth()+"//"+Collections.max(notBigEnough, new CompareSizesByArea()).getHeight());
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.d(TAG, "setUpMediaRecorder : 기본값 : " + choices[0].getWidth()+"//"+choices[0].getHeight());
            return choices[0];
        }
    }

    public static Camera2BasicFragment newInstance() {
        return new Camera2BasicFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        final Activity activity = getActivity();
        doFullScreen();

        /*int uiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;
        boolean isImmersiveModeEnabled = ((uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) == uiOptions);
        if (isImmersiveModeEnabled) {
            Log.i("Is on?", "Turning immersive mode mode off. ");
        } else {
            Log.i("Is on?", "Turning immersive mode mode on.");
        }
        // 몰입 모드를 꼭 적용해야 한다면 아래의 3가지 속성을 모두 적용시켜야 합니다
        newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        activity.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);*/

        if(savedInstanceState != null){
            if(savedInstanceState.getString("mb_id") != null){
                mb_id = savedInstanceState.getString("mb_id");
            }
            if(savedInstanceState.getString("title") != null){
                title = savedInstanceState.getString("title");
            }
            if(savedInstanceState.getString("cate1") != null){
                cate1 = savedInstanceState.getString("cate1");
            }
            if(savedInstanceState.getString("cate2") != null){
                cate2 = savedInstanceState.getString("cate2");
            }
            if(savedInstanceState.getString("type1") != null){
                type1 = savedInstanceState.getString("type1");
            }
            if(savedInstanceState.getString("type2") != null) {
                type2 = savedInstanceState.getString("type2");
            }
            if(savedInstanceState.getString("wr_price") != null) {
                wr_price = savedInstanceState.getString("wr_price");
            }
            if(savedInstanceState.getString("wr_price2") != null) {
                wr_price2 = savedInstanceState.getString("wr_price2");
            }
            if(savedInstanceState.getString("pd_price_type") != null) {
                pd_price_type = savedInstanceState.getString("pd_price_type");
            }
            if(savedInstanceState.getString("filename") != null) {
                filename = savedInstanceState.getString("filename");
            }
            if(savedInstanceState.getString("videoname") != null) {
                videoname = savedInstanceState.getString("videoname");
            }
            if(savedInstanceState.getInt("camCount") != 0) {
                camCount = savedInstanceState.getInt("camCount");
            }
            if(savedInstanceState.getInt("mCameraLensFacingDirection") != 0) {
                mCameraLensFacingDirection = savedInstanceState.getInt("mCameraLensFacingDirection");
            }

            if(savedInstanceState.getBoolean("mIsRecordingVideo ")) {
                mIsRecordingVideo  = savedInstanceState.getBoolean("mIsRecordingVideo ");
            }

        }else {
            if (activity.getIntent().getStringExtra("mb_id") != null) {
                mb_id = activity.getIntent().getStringExtra("mb_id");
            }
            if (activity.getIntent().getStringExtra("title") != null) {
                title = activity.getIntent().getStringExtra("title");
            }
            if (activity.getIntent().getStringExtra("cate1") != null) {
                cate1 = activity.getIntent().getStringExtra("cate1");
            }
            if (activity.getIntent().getStringExtra("cate2") != null) {
                cate2 = activity.getIntent().getStringExtra("cate2");
            }
            if (activity.getIntent().getStringExtra("type1") != null) {
                type1 = activity.getIntent().getStringExtra("type1");
            }
            if (activity.getIntent().getStringExtra("type2") != null) {
                type2 = activity.getIntent().getStringExtra("type2");
            }
            if (activity.getIntent().getStringExtra("wr_price") != null) {
                wr_price = activity.getIntent().getStringExtra("wr_price");
            }
            if (activity.getIntent().getStringExtra("wr_price2") != null) {
                wr_price2 = activity.getIntent().getStringExtra("wr_price2");
            }
            if (activity.getIntent().getStringExtra("pd_price_type") != null) {
                pd_price_type = activity.getIntent().getStringExtra("pd_price_type");
            }
        }
        mCompositeDisposable = new CompositeDisposable();

        int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        if(displayRotation == 1 || displayRotation == 3){
            Log.d(TAG, "onCreateView: 회전");
            return inflater.inflate(R.layout.fragment_camera2_basic_land, container, false);
        }else {
            Log.d(TAG, "onCreateView: 새로");
            return inflater.inflate(R.layout.fragment_camera2_basic, container, false);
        }
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated");
        skipBtn = view.findViewById(R.id.skipBtn2);
        videoBtn = view.findViewById(R.id.videoBtn2);
        cam_toggle = view.findViewById(R.id.cam_toggle2);
        camBtn = view.findViewById(R.id.camBtn2);
        photoBtn = view.findViewById(R.id.photoBtn2);
        saveBtn = view.findViewById(R.id.saveBtn2);
        if(type1.equals("2")){
            skipBtn.setVisibility(View.VISIBLE);
        }else{
            skipBtn.setVisibility(View.GONE);
        }
        url = "http://484848.co.kr/mobile/photo_upload.php";
        sd = Environment.getExternalStorageDirectory().getAbsolutePath()+"/foureight";
        textView = view.findViewById(R.id.camCount2);
        if(camCount > 0){
            textView.setText(camCount+"/5");
        }else {
            camCount = 0;
        }
        camBtn.setOnClickListener(this);
        cam_toggle.setOnClickListener(this);
        photoBtn.setOnClickListener(this);
        saveBtn.setOnClickListener(this);
        videoBtn.setOnClickListener(this);
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        //수동포커스?
        mTextureView.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                lockFocus2();
                return true;
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated");
        //파일명 지정
        File file = new File(sd);
        file.mkdir();
        file_name = System.currentTimeMillis() + "_" + mb_id + "_" + camCount + ".jpg";
        path = sd + "/" + file_name;
        mFile = new File(path);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        if(filename!=null && filename != ""){
            String[] files = filename.split(",");
            int filenum = files.length;
            textView.setText(filenum+"/5");
            //camCount=filenum;
        }
        startBackgroundThread();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (mTextureView.isAvailable()) {
            Log.d(TAG, "onResume: AAA");
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            Log.d(TAG, "onResume: BBB");
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause: ");
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ErrorDialog.newInstance(getString(R.string.request_permission))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int width, int height) {
        Activity activity = getActivity();
        mCameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : mCameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);

                Range<Integer>[] ranges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
                if(ranges != null) {
                    for (Range<Integer> range : ranges) {
                        int upper = range.getUpper(); Log.i(TAG, "[FPS Range Available]:" + range);
                        if (upper >= 10) {
                            if (fpsRange == null || upper < fpsRange.getUpper()) fpsRange = range;
                        }
                    }
                }

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing != mCameraLensFacingDirection) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                //noinspection ConstantConditions
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                Log.d(TAG, "setUpCameraOutputs: " + mSensorOrientation);
                if(mCameraLensFacingDirection==0) {
                    switch (displayRotation) {
                        case Surface.ROTATION_0:
                        case Surface.ROTATION_180:
                            if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                                Log.d(TAG, "setUpCameraOutputs: 회전@@");
                                swappedDimensions = true;
                            }
                            break;
                        case Surface.ROTATION_90:
                        case Surface.ROTATION_270:
                            if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                                swappedDimensions = true;
                            }
                            break;
                        default:
                            Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                    }
                }else{
                    switch (displayRotation) {
                        case Surface.ROTATION_0:
                        case Surface.ROTATION_180:
                            if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                                Log.d(TAG, "setUpCameraOutputs: 회전@@");
                                swappedDimensions = true;
                            }
                            break;
                        case Surface.ROTATION_90:
                        case Surface.ROTATION_270:
                            if (mSensorOrientation == 0) {
                                swappedDimensions = true;
                            }
                            break;
                        default:
                            Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                    }
                }

                Log.d(TAG, "setUpMediaRecorder: " + swappedDimensions);

                //프리뷰 시작위치
                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);

                //회전 사이즈 , 최대 사이즈
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth < MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight < MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                for(Size option : map.getOutputSizes(SurfaceTexture.class)){
                    Log.d(TAG, "preview Size : " + option.getWidth() +"||"+option.getHeight() +", preview Ratio : " + ((double)option.getWidth()/option.getHeight()));
                }

                for(Size jpgOption : map.getOutputSizes(ImageFormat.JPEG)){
                    Log.d(TAG, "jpg Size : " + jpgOption.getWidth() +"||"+jpgOption.getHeight());
                }


                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
                Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),new CompareSizesByArea());
                //if(mPreviewSize==null) {
                    mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight,largest);
                //}
                Log.d(TAG, "setUpMediaRecorder: " + mPreviewSize.getWidth()+"//"+mPreviewSize.getHeight() +"//"+rotatedPreviewWidth+"//"+rotatedPreviewHeight);
                // For still image captures, we use the largest available size. 사진 저장 사이즈
                mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.JPEG, /*maxImages*/10);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

                // We fit the aspect ratio of TextureView to the size of preview we picked. 프리뷰 사이즈
                //int orientation = getResources().getConfiguration().orientation;
                //if (orientation == Configuration.ORIENTATION_LANDSCAPE) {//가로모드
                //    mTextureView.setAspectRatio(rotatedPreviewHeight, rotatedPreviewWidth);
                //} else {//세로모드
                    mTextureView.setAspectRatio(rotatedPreviewHeight, rotatedPreviewWidth);
                //}
                // Check if the flash is supported.
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;

                mCameraId = cameraId;

                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
    }

    /**
     * Opens the camera specified by {@link Camera2BasicFragment#mCameraId}.
     */
    private void openCamera(int width, int height) {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }

        setUpCameraOutputs(width, height);
        //configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
        configureTransform(width, height);
        Activity activity = getActivity();
        mCameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            mCameraManager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
            mMediaRecorder = new MediaRecorder();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        Log.d(TAG, "closeCameracloseCamera: ");
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private void closePreviewSession() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        /*if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }*/
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.d(TAG, "onConfigured: " +cameraCaptureSession);
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                //mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);
                                // Flash is automatically enabled when necessary.
                                setAutoFlash(mPreviewRequestBuilder);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed" + cameraCaptureSession);
                        }

                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPreview() {
        Log.d(TAG, "startPreview: ");
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            //closePreviewSession();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            Surface previewSurface = new Surface(texture);
            mPreviewRequestBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mCaptureSession = session;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Activity activity = getActivity();
                            if (null != activity) {
                                showToast("Failed StartPreview");
                                //Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    /**
     * Update the camera preview. {@link #startPreview()} needs to be called in advance.
     */
    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(mPreviewRequestBuilder);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();

        int bufferWidth = mPreviewSize.getWidth();
        int bufferHeight = mPreviewSize.getHeight();

        Log.d(TAG, "configureTransform: " +bufferWidth + "//"+bufferHeight +"//"+ viewWidth+"//"+viewHeight);

        RectF viewRect = new RectF(0, 0,viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, bufferWidth, bufferHeight);
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();

        /*
        * LG F620K = 전 [ 세 : O , 가 : O ] , 후 [ 세 : O , 가 : O]
        * LG V300S = 전 [ 세 : O , 가 : X ] , 후 [ 세 : O , 가 : X]
        * LG F700S = 전 [ 세 : X , 가 : X ] , 후 [ 세 : O , 가 : X]
        * SM E330S = 전 [ 세 : O , 가 : O ] , 후 [ 세 : X , 가 : X]
        * SM G977N = 전 [ 세 : O , 가 : X ] , 후 [ 세 : O , 가 : X]
        */
        if(mCameraLensFacingDirection == 0){ //전면
            Log.d(TAG, "configureTransform: 앞 카메라??");
            if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
                //bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                //matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                float scale = Math.min((float) viewHeight / bufferWidth, (float) viewWidth / bufferHeight);
                float scales = Math.max((float) viewHeight / bufferWidth, (float) viewWidth / bufferHeight);
                float newScale = (float) bufferHeight / viewHeight;
                Log.d(TAG, "configureTransform: 앞 scale = " + scale +"//"+(scale+1)+"//"+newScale+"//"+bufferRect.width()+"//"+bufferRect.height()+"//"+scales);
                //matrix.postScale(newScale - scale,scales, centerX, centerY);
                matrix.postScale(scale,scales, centerX, centerY);
                matrix.postRotate(90 * (rotation - 2), centerX, centerY);
            }/*else{
                //bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                //matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                //float scale = Math.max((float) viewHeight / bufferWidth, (float) viewWidth / bufferHeight);
                float newScale = (float) bufferWidth / viewHeight;
                float newScales = (float) bufferHeight / viewWidth;
                Log.d(TAG, "configureTransform: newScale = " + newScale + "//"+newScales);
                matrix.postScale(1, newScale, centerX, centerY);
            }*/
        }else { // 후면
            Log.d(TAG, "configureTransform: 뒤 카메라??");
            if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                float scale = Math.min((float) viewHeight / bufferWidth, (float) viewWidth / bufferHeight);
                float scales = Math.max((float) viewHeight / bufferWidth, (float) viewWidth / bufferHeight);
                float newScale = (float) bufferHeight / viewHeight;
                Log.d(TAG, "configureTransform: "+scale+"//"+newScale+"//"+scales);
                //matrix.postScale(scale - ratio, scale, centerX, centerY);
                //matrix.postScale((newScale - scale) ,scales , centerX, centerY); , LG F620K
                matrix.postScale(newScale,scales , centerX, centerY);
                matrix.postRotate( 90 * (rotation - 2), centerX, centerY);
            }
        }

        /*if(mCameraLensFacingDirection == 0){//전면
            Log.d(TAG, "setUpMediaRecorder: 앞 // " + rotation);
            if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
                Log.d(TAG, "configureTransform: 회전");
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                float scale = Math.max((float) viewHeight / bufferWidth, (float) viewWidth / bufferHeight);
                matrix.postScale(scale, scale, centerX, centerY);
                matrix.postRotate(90 * (rotation - 2), centerX, centerY);
            }else if (Surface.ROTATION_180 == rotation) {
                matrix.postScale(1, 1, centerX, centerY);
                matrix.postRotate(180, centerX, centerY);
            }else if(Surface.ROTATION_0 == rotation){
                matrix.setScale(1,1, centerX, centerY);
                matrix.postRotate(0, centerX, centerY);
            }
        }else {
            Log.d(TAG, "setUpMediaRecorder: 뒤 // " + rotation);
            if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
                Log.d(TAG, "configureTransform: 회전");
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                float scale = Math.max((float) viewHeight / bufferWidth, (float) viewWidth / bufferHeight);
                matrix.postScale(scale, scale, centerX, centerY);
                matrix.postRotate(90 * (rotation - 2), centerX, centerY);
            }else if (Surface.ROTATION_180 == rotation) {
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                matrix.postScale(1, 1, centerX, centerY);
                matrix.postRotate(180, centerX, centerY);
            }else if(Surface.ROTATION_0 == rotation){
                //bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                //float scale = Math.max((float) viewHeight / bufferWidth, (float) viewWidth / bufferHeight);
                //float scale = Math.max((float) viewHeight / bufferWidth, (float) viewWidth / bufferHeight);
                matrix.postScale(1, 1 , centerX, centerY); // 확대?
                matrix.postRotate(0, centerX, centerY);
            }
        }*/
        Log.d(TAG, "setUpMediaRecorder : 최종 영역 : " + viewRect.width() + "//" + viewRect.height() + "//"+bufferRect.width() + "//"+ bufferRect.height());
        mTextureView.setTransform(matrix);
    }

    /**
     * Initiate a still image capture.
     */
    private void takePicture() {

        if(camCount>=5){
            showToast("사진은 5개까지 등록 가능합니다.");
        }else {
            if (mCameraLensFacingDirection == CameraCharacteristics.LENS_FACING_FRONT) {
                //lockFocus();
                Log.d(TAG, "takePicture: 전면");
                captureStillPicture();
            } else {
                Log.d(TAG, "takePicture: 후면");
                lockFocus();
            }
        }
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,mBackgroundHandler);

            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
            captureBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void lockFocus2() {
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            //mState = STATE_WAITING_LOCK;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
     */
    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {@link #lockFocus()}.
     */
    private void captureStillPicture() {
        if(mImageReader==null){
            mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.JPEG, /*maxImages*/10);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);
        }
        Log.d(TAG, "captureStillPicture: " + mCaptureSession);
        try {
            final Activity activity = getActivity();
            if (null == activity || null == mCameraDevice) {
                return;
            }

            file_name = System.currentTimeMillis() + "_" + mb_id + "_" + camCount + ".jpg";
            path = sd + "/" + file_name;
            mFile = new File(path);

            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);

            // Orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {

                    //showToast("Saved: " + mFile + "//"+ mPath);
                    if (filename == null || filename == "") {
                        filename = file_name;
                    } else {
                        filename = filename + "," + file_name;
                    }
                    if (mPath == null) {
                        mPath = path;
                    } else {
                        mPath = mPath + "," + path;
                    }
                    camCount++;
                    if(textView!=null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(camCount + "/5");
                            }
                        });
                    }
                    Log.d(TAG, mFile.toString());
                    unlockFocus();
                }
            };
            MediaActionSound mediaActionSound = new MediaActionSound();
            mediaActionSound.play(MediaActionSound.SHUTTER_CLICK);
            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mPreviewRequestBuilder);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.camBtn2: {
                takePicture();
                break;
            }
            case R.id.info: {
                Activity activity = getActivity();
                if (null != activity) {
                    new AlertDialog.Builder(activity)
                            .setMessage(R.string.intro_message)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
                break;
            }
            case R.id.cam_toggle2:{
                switchCam();
                break;
            }
            case R.id.photoBtn2:{
                isPhoto();
                break;
            }
            case R.id.saveBtn2:{
                isSave();
                break;
            }
            case R.id.skipBtn2:{
                isSkip();
                break;
            }
            case R.id.videoBtn2:{
                isRecord();
                break;
            }
        }
    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        /**
         * The file we save the image into.
         */
        private final Integer mFrontCheck;

        private final Activity mActivity;

        private final int mRotate;

        ImageSaver(Image image, File file, Integer frontCheck,Activity activity,int rotate) {
            mImage = image;
            mFile = file;
            mFrontCheck = frontCheck;
            mActivity = activity;
            mRotate = rotate;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];

            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length,null);

                if(mFrontCheck==0) {
                    //Log.d(TAG, "run: 앞카메라 //좌우 반전 필요//회전 조율!!" + mRotate);
                    //270은 - 180
                    //90은 정상
                    //0은 -90
                    //일단 반전
                    Matrix matrix = new Matrix();

                    matrix.postScale(-1, 1);
                    if (mRotate == Surface.ROTATION_0) {
                        //Log.d(TAG, "run: 정방향");
                        matrix.postRotate(90);
                    }
                    if (mRotate == Surface.ROTATION_270) {
                        //Log.d(TAG, "run: 우측돌림");
                        //matrix.postScale(-1,1);
                        matrix.postRotate(-180);
                    }
                    if (mRotate == Surface.ROTATION_90) {
                        //Log.d(TAG, "run: 좌측돌림");
                    }

                    Bitmap newImg = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                    bytes = bitmapToByteArray(newImg);
                }

                output = new FileOutputStream(mFile);
                output.write(bytes);

                //앨범 등록
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(mFile));
                mActivity.sendBroadcast(intent);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

    /**
     * Shows OK/Cancel confirmation dialog about camera permission.
     */
    public static class ConfirmationDialog extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.request_permission)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            parent.requestPermissions(new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Activity activity = parent.getActivity();
                                    if (activity != null) {
                                        activity.finish();
                                    }
                                }
                            })
                    .create();
        }
    }

    public void switchCam(){
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mCameraLensFacingDirection == CameraCharacteristics.LENS_FACING_BACK) {
                        mCameraLensFacingDirection = CameraCharacteristics.LENS_FACING_FRONT;
                        saveLensFacing();
                        closeCamera();
                        reopenCamera();
                    } else if (mCameraLensFacingDirection == CameraCharacteristics.LENS_FACING_FRONT) {
                        mCameraLensFacingDirection = CameraCharacteristics.LENS_FACING_BACK;
                        saveLensFacing();
                        closeCamera();
                        reopenCamera();
                    }
                }
            });
        }
    }

    public void reopenCamera() {
        //Log.d(TAG, "reopenCamera: ");
        //startBackgroundThread();
        //Log.d(TAG, "reopenCamera: mCameraLensFacingDirection = " + mCameraLensFacingDirection);
        if (mTextureView.isAvailable()) {
            Log.d(TAG, "reopenCamera: textureview" + mTextureView.getWidth()+"//"+mTextureView.getHeight());
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            Log.d(TAG, "reopenCamera: notextureview");
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    public void isPhoto(){
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TedBottomPicker bottomSheetDialogFragment = new TedBottomPicker.Builder(activity).
                    setOnMultiImageSelectedListener(new TedBottomPicker.OnMultiImageSelectedListener() {
                        @Override
                        public void onImagesSelected(ArrayList<Uri> uriList) {
                            //Log.d(TAG, "onImagesSelected: " + uriList.toString());
                            int thisCount = camCount;
                            thisCount = thisCount + uriList.size();
                            if (thisCount > 5) {
                                showToast( "사진은 5개까지 선택가능합니다.");
                                return;
                            }

                            for (int i = 0; i < uriList.size(); i++) {

                                try {
                                    fileWrite = new FileWrite();
                                    value = 0;
                                    fileWrite.execute(uriList.get(i));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                    }).setPeekHeight(1600)
                      .showTitle(false)
                      .showCameraTile(false)
                      .setPreviewMaxCount(100)
                      .setSelectMaxCount(5)
                      .setCompleteButtonText("등록")
                      .setEmptySelectionText("사진을 선택해주세요.")
                      .setImageProvider(null)
                      .create();

                    bottomSheetDialogFragment.show(getFragmentManager());
                }
            });
        }
    }

    class FileWrite extends AsyncTask<Uri,Integer,Integer> {
        Activity activity = getActivity();
        ProgressDialog asyncDialog = new ProgressDialog(activity);
        FileInputStream fin;
        FileOutputStream fos;
        FileChannel fcin;
        FileChannel fcout;
        @Override
        protected void onPreExecute() {
            asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            asyncDialog.setMessage("이미지 등록중입니다...");
            asyncDialog.show();
        }

        @Override
        protected void onPostExecute(Integer integer) {
            textView.setText(camCount + "/5");
            asyncDialog.dismiss();
        }

        @Override
        protected Integer doInBackground(Uri... files) {
            try{
                //String match = "[]";
                String file_Path = files[0].toString();
                //String new_file = new String(file_Path.getBytes("utf-8"),"utf-8");
                String[] realPath = file_Path.split("//");
                String filePath = realPath[1];
                filePath = filePath.replaceAll("@","");
                filePath = URLDecoder.decode(filePath);
                selPhotoUri = files[0];

                String ext = getExtension(filePath);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;

                Bitmap image = BitmapFactory.decodeStream(activity.getContentResolver().openInputStream(selPhotoUri));

                //ExifInterface exif = new ExifInterface(filePath);
                image = ExifUtils.rotateBitmap(filePath,image);

                if(image!=null) {
                    //가로 세로 구분
                    Bitmap resizeImg = Bitmap.createScaledBitmap(image, image.getWidth(), image.getHeight(), true);
                    bytes = bitmapToByteArray(resizeImg);
                    //url = "http://484848.co.kr/mobile/photo_upload.php";
                    File file = new File(sd);
                    file.mkdir();
                    file_name = System.currentTimeMillis() + "_" + mb_id + "_" + camCount + "." + ext;
                    path = sd + "/" + file_name;

                    file = new File(path);
                    fin = new FileInputStream(filePath);
                    fos = new FileOutputStream(file);

                    fcin = fin.getChannel();
                    fcout = fos.getChannel();
                    long size = fcin.size();
                    fcin.transferTo(0,size,fcout);

                    if (filename == null || filename == "") {
                        filename = file_name;
                    } else {
                        filename = filename + "," + file_name;
                    }
                    if (mPath == null) {
                        mPath = path;
                    } else {
                        mPath = mPath + "," + path;
                    }
                    camCount++;
                }
            }catch (FileNotFoundException e){
                e.printStackTrace();
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                try {
                    fcout.close();
                    fcin.close();
                    fos.close();
                    fin.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            return camCount;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            Log.d(TAG, "onProgressUpdate: ");
        }
    }

    public static byte[] bitmapToByteArray(Bitmap bitmap) {
        byte[] byteArray = null;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress( Bitmap.CompressFormat.JPEG, 100, stream) ;
        byteArray = stream.toByteArray() ;
        return byteArray;
    }

    public void isSave(){
        /*if(isRecording){
            isRecord();
        }*/
        final Activity activity = getActivity();
        if (filename != null || videoname != null) {
            if (activity != null) {
                if (filename != null) {
                    url = "http://484848.co.kr/mobile/photo_upload.php";
                    String[] files = filename.split(",");
                    camUpCount = camCount;
                    for (int i = 0; i < files.length; i++) {
                        mPath = sd + "/" + files[i];
                        UpdateFile updateFile = new UpdateFile();
                        updateFile.execute(mPath, sd, files[i], url);
                    }
                }
                if (videoname != null) {
                    camUpCount = camCount + 1;
                    url = "http://484848.co.kr/mobile/video_upload.php";
                    String vpath = sd + "/" + videoname;
                    UpdateFile updateFile = new UpdateFile();
                    updateFile.execute(vpath, sd, videoname, url);
                }
            }
        } else {
            showToast("사진 또는 영상을 등록/선택해 주세요");
        }
    }

    public void saveComplete(){
        Activity activity = getActivity();
        Intent intent = new Intent(activity, MainActivity.class);
        intent.putExtra("filename", filename);
        intent.putExtra("videoname", videoname);
        intent.putExtra("timer2", timer2);
        intent.putExtra("mb_id", mb_id);
        intent.putExtra("title", title);
        intent.putExtra("type1", type1);
        intent.putExtra("type2", type2);
        intent.putExtra("cate1", cate1);
        intent.putExtra("cate2", cate2);
        intent.putExtra("wr_price", wr_price);
        intent.putExtra("wr_price2", wr_price2);
        intent.putExtra("pd_price_type", pd_price_type);
        showToast("파일 업로드 완료");

        startActivity(intent);
        activity.finish();
    }

    class UpdateFile extends AsyncTask<String,Integer,Integer> {
        Activity activity = getActivity();
        ProgressDialog asyncDialog = new ProgressDialog(activity);

        @Override
        protected void onPreExecute() {
            asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            asyncDialog.setMessage("이미지 등록중입니다...");
            asyncDialog.show();
        }

        @Override
        protected void onPostExecute(Integer integer) {
            asyncDialog.dismiss();
            camUpCount--;
            if(camUpCount==0){
                saveComplete();
            }
        }

        @Override
        protected Integer doInBackground(String... fileName) {
            String sourceFileUri = fileName[0].toString();
            String uploadFilePath = fileName[1].toString();
            String uploadFileName = fileName[2].toString();
            String upLoadServerUri = fileName[3].toString();
            HttpURLConnection conn = null;
            DataOutputStream dos = null;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 10 * 1024 * 1024;
            File sourceFile = new File(sourceFileUri);

            try {
                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);

                URL url = new URL(upLoadServerUri);
                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", sourceFileUri);
                conn.setRequestProperty("mb_id", mb_id);
                dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\"" + sourceFileUri + "\"" + lineEnd);
                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }
                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();
                Log.i("uploadFile", "HTTP Response is : " + serverResponseMessage + ": " + serverResponseCode);

                if (serverResponseCode == 200) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String msg = "File Upload Completed.\n\n See uploaded file here : \n\n" + uploadFileName;
                            //messageText.setText(msg);
                            //Toast.makeText(CameraActivity.this, "File Upload Complete.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();
            } catch (MalformedURLException ex) {
                //dialog.dismiss();
                ex.printStackTrace();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //messageText.setText("MalformedURLException Exception : check script url.");
                        showToast("MalformedURLException");
                    }
                });
                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {
                //dialog.dismiss();
                e.printStackTrace();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //messageText.setText("Got Exception : see logcat ");
                        showToast("Got Exception : see logcat ");
                    }
                });
                Log.e(TAG, "Exception : " + e.getMessage(), e);
            }
            return camUpCount;
        }
    }

    public void isSkip(){
        final Activity activity = getActivity();
        if (activity != null) {
            Intent intent = new Intent(activity, MainActivity.class);
            intent.putExtra("mb_id", mb_id);
            intent.putExtra("title", title);
            intent.putExtra("type1", type1);
            intent.putExtra("type2", type2);
            intent.putExtra("cate1", cate1);
            intent.putExtra("cate2", cate2);
            intent.putExtra("wr_price", wr_price);
            intent.putExtra("wr_price2", wr_price2);
            intent.putExtra("pd_price_type", pd_price_type);
            intent.putExtra("skip", "skip");
            startActivity(intent);
        }
    }

    public void isRecord(){
        if(mIsRecordingVideo){
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

            stopRecordingVideo();
        }else {
            if(videoname!=null){
                showToast("이미 촬영된 영상은 삭제 됩니다.");
            }
            MediaActionSound mediaActionSound = new MediaActionSound();
            mediaActionSound.play(MediaActionSound.START_VIDEO_RECORDING);
            //video 프리뷰 최적화??
            //startPreview();
            int displayRotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
            Log.d(TAG, "isRecord: " + displayRotation);
            //회면회전 고정
            if (displayRotation == 1 || displayRotation == 3) {
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            } else {
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            startRecordingVideo();
            //lockFocus();
        }
    }

    //회전시 데이터 유지
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("mb_id", mb_id);
        outState.putString("title", title);
        outState.putString("type1", type1);
        outState.putString("type2", type2);
        outState.putString("cate1", cate1);
        outState.putString("cate2", cate2);
        outState.putString("sSec", sSec);
        outState.putString("wr_price", wr_price);
        outState.putString("wr_price2", wr_price2);
        outState.putString("pd_price_type", pd_price_type);
        outState.putString("skip", "skip");
        outState.putInt("camCount",camCount);
        outState.putString("filename",filename);
        outState.putString("videoname",videoname);
        outState.putBoolean("mIsRecordingVideo",mIsRecordingVideo);
        outState.putInt("mCameraLensFacingDirection",mCameraLensFacingDirection);
    }
    
    private void saveLensFacing(){
        SharedPreferences pref = getActivity().getSharedPreferences("lensFacingCheck", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("lensFacing", mCameraLensFacingDirection);
        editor.commit();
    }

    //영상녹화 설정
    private void setUpMediaRecorder() throws IOException {
        final Activity activity = getActivity();
        if(null == activity) return;


        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        if (mNextVideoAbsolutePath == null || mNextVideoAbsolutePath.isEmpty()) {
            mNextVideoAbsolutePath = getVideoFilePath();
        }
        mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        /*profile.videoFrameWidth = mPreviewSize.getWidth();
        profile.videoFrameHeight = mPreviewSize.getHeight();*/

        Log.d(TAG, "setUpMediaRecorder: " + mVideoSize.getWidth() + "//"+mVideoSize.getHeight() + "||"+mPreviewSize.getWidth() + "//"+mPreviewSize.getHeight());
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();

        mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
        mMediaRecorder.setVideoFrameRate(profile.videoFrameRate);
        mMediaRecorder.setVideoSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        //configureTransform(mPreviewSize.getWidth(),mPreviewSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);

        //noinspection ConstantConditions
        if(mCameraLensFacingDirection==0){
            switch (rotation) {
                case Surface.ROTATION_0:
                    mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                    break;
                case Surface.ROTATION_90:
                    mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                    break;
                case Surface.ROTATION_270:
                    mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                    break;
                case Surface.ROTATION_180:
                    mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                    break;
                default:
                    mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                    break;
            }
        }else {
            switch (rotation) {
                case Surface.ROTATION_0:
                    mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                    break;
                case Surface.ROTATION_90:
                    mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                    break;
                case Surface.ROTATION_270:
                    mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                    break;
                case Surface.ROTATION_180:
                    mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                    break;
                default:
                    mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                    break;
            }
        }
        mMediaRecorder.prepare();
    }

    //파일 이름 및 저장경로를 만듭니다.
    private String getVideoFilePath() {
        /*final File dir = getActivity().getExternalFilesDir(null);
        return (dir == null ? "" : (dir.getAbsolutePath() + "/"))
                + System.currentTimeMillis() + ".mp4";*/
        File dst = new File(sd);
        if(!dst.exists()) dst.mkdirs();
        videoname = System.currentTimeMillis() + ".mp4";
        return sd +"/"+ System.currentTimeMillis() + ".mp4";
    }

    //녹화시작
    private void startRecordingVideo() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        assert getActivity() != null;

        try {
            //stopBackgroundThread();
            //closePreviewSession();
            startPreview();
            //closeCamera();
            //reopenCamera();
            setUpMediaRecorder();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

            List<Surface> surfaces = new ArrayList<>();

            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewRequestBuilder.addTarget(previewSurface);

            Surface recordSurface = mMediaRecorder.getSurface();
            surfaces.add(recordSurface);
            mPreviewRequestBuilder.addTarget(recordSurface);

            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mCaptureSession = session;
                    updatePreview();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            videoBtn.setBackgroundResource(R.mipmap.video_btn_stop);
                            cam_toggle.setVisibility(View.GONE);
                            camBtn.setEnabled(false);
                            photoBtn.setEnabled(false);
                            saveBtn.setEnabled(false);
                            mIsRecordingVideo = true;
                            Log.d(TAG, "run: mMediaRecorder Start");
                            mMediaRecorder.start();
                        }
                    });
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Activity activity = getActivity();
                    if (null != activity) {
                        Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }, mBackgroundHandler);

            timer();
        } catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }
    }

    //녹화 중지
    private void stopRecordingVideo(){

        MediaActionSound mediaActionSound = new MediaActionSound();
        mediaActionSound.play(MediaActionSound.STOP_VIDEO_RECORDING);
        mIsRecordingVideo = false;
        videoBtn.setBackgroundResource(R.mipmap.video_btn);
        cam_toggle.setVisibility(View.VISIBLE);
        camBtn.setEnabled(true);
        saveBtn.setEnabled(true);
        photoBtn.setEnabled(true);
        try{
            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
        try {
            mMediaRecorder.stop();
        }catch (RuntimeException e){
            e.printStackTrace();
        }
        mMediaRecorder.reset();
        //mMediaRecorder.release();
        Activity activity = getActivity();
        if (null != activity) {
            showToast("Video saved: " + mNextVideoAbsolutePath);
            File file = new File(mNextVideoAbsolutePath);

            // 아래 코드가 없으면 갤러리 저장 적용이 안됨.
            if(!file.exists()) file.mkdir();
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(file));
            activity.sendBroadcast(intent);
            //getActivity().getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
        }
        mNextVideoAbsolutePath = null;
        stop();
        closeCamera();
        reopenCamera();
    }

    // 여기부터
//녹화시간 카운트 시작
    private void timer() {
        Observable<Long> duration = Observable.interval(1, TimeUnit.SECONDS).map(sec -> sec += 1);
        Disposable disposable = duration.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(timeout -> {
                    //long min = timeout / 60;
                    long sec = timeout % 60;
                    //String sMin;


                    if (sec < 10) sSec = "0" + sec;
                    else sSec = String.valueOf(sec);

                    String elapseTime = sSec+"s/20s";
                    if(sec == 21){
                        Log.d(TAG, "timer: stop");
                        stopRecordingVideo();
                        //mIsRecordingVideo = false;
                    }else {
                        textView.setText(elapseTime);
                    }
                });
        mCompositeDisposable.add(disposable);
    }

    //녹화시간 카운트 정지
    private void stop() {
        if (!mCompositeDisposable.isDisposed()) {
            mCompositeDisposable.dispose();
        }
    }
    CompositeDisposable mCompositeDisposable;
// 여기까지는 타이머 부분이기 때문에 사용안하셔도 됩니다.


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        SharedPreferences preferences = getActivity().getSharedPreferences("checkLensFacing",MODE_PRIVATE);
        preferences.getString("lensFacing", "");
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.commit();
    }


    private boolean hasSoftMenu() {
        int uiOpations = getActivity().getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOpations;
        boolean isImmersiveModeEnabled = ((uiOpations | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)==uiOpations);
        if(isImmersiveModeEnabled){
            Log.d(TAG, "hasSoftMenu: OFF");
        }else{
            Log.d(TAG, "hasSoftMenu: ON");
        }

        //메뉴버튼 유무
        boolean hasMenuKey = ViewConfiguration.get(getActivity().getApplicationContext()).hasPermanentMenuKey();

        //뒤로가기 버튼 유무
        boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);

        if (!hasMenuKey && !hasBackKey) { // lg폰 소프트키일 경우
            return true;
        } else { // 삼성폰 등.. 메뉴 버튼, 뒤로가기 버튼 존재
            return false;
        }
    }

    private int getSoftMenuHeight() {
        Resources resources = this.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        int deviceHeight = 0;

        if (resourceId > 0) {
            deviceHeight = resources.getDimensionPixelSize(resourceId);
        }
        return deviceHeight;
    }

    private int getStatusBarHeight() {
        Resources resources = this.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");

        int statusHeight = 0;

        if(resourceId > 0){
            statusHeight = resources.getDimensionPixelSize(resourceId);
        }

        return statusHeight;
    }

    private void doFullScreen() {
        View decorView = getActivity().getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE|
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE|
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
                        View.SYSTEM_UI_FLAG_FULLSCREEN);
    }
}