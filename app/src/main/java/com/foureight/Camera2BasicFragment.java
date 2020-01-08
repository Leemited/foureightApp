package com.foureight;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
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
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
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

import static com.foureight.CameraActivity.getExtension;
import static java.lang.Math.round;

public class Camera2BasicFragment extends Fragment
        implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private static final String CAMERA_FRONT = "1";
    private static final String CAMERA_BACK = "0";

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

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            Log.d(TAG, "onSurfaceTextureAvailable: " + width + "//"+ height);
            mCameraLensFacingDirection = 1;
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            //Log.d(TAG, "onSurfaceTextureSizeChanged: " + width + "//" + height);
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
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
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
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

    private int mCameraLensFacingDirection;
    private String mb_id,title,cate1,cate2,url,path,sd,file_name,filename,mPath,imgPath,imgName,type1,type2,videoname,wr_price,wr_price2,pd_price_type;
    private int camCount = 0,value=0,serverResponseCode = 0,timer = 0, timer2 = 0,returnVal = 0;
    private FileWrite fileWrite;
    private TextView textView;
    private Uri selPhotoUri;
    private byte[] bytes;
    private Button skipBtn;

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    break;
                }
                case STATE_WAITING_LOCK: {
                    if(camCount<5) {
                        Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                        if (afState == null) {
                            captureStillPicture();
                        } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                                CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                            // CONTROL_AE_STATE can be null on some devices
                            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                            if (aeState == null ||
                                    aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                                mState = STATE_PICTURE_TAKEN;
                                captureStillPicture();

                            } else {
                                runPrecaptureSequence();
                            }
                        }
                    }else{
                        showToast("사진은 5개 이상 등록 할 수 없습니다.");
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
                    if(camCount<5) {
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        }
                    }else{
                        showToast("사진은 5개 이상 등록 할 수 없습니다.");
                    }
                    break;
                }
            }
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
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            Log.d(TAG, "chooseOptimalSize: option " + option.getWidth() + "//" + option.getHeight() +"//"+ maxWidth + "//" + maxHeight + "//" + textureViewWidth + "//" + textureViewHeight + "//" + w + "//" + h);
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight && option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth && option.getHeight() >= textureViewHeight) {
                    Log.d(TAG, "chooseOptimalSize: result = " + option.getWidth() + "//"+option.getHeight());
                    bigEnough.add(option);
                } else {
                    Log.d(TAG, "chooseOptimalSize: result = " + option.getWidth() + "//"+option.getHeight());
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            return choices[0];
        }
    }

    public static Camera2BasicFragment newInstance() {
        return new Camera2BasicFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        final Activity activity = getActivity();

        if(savedInstanceState != null){
            Log.d(TAG, "onCreateView: " + savedInstanceState);
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

        int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        if(displayRotation == 1 || displayRotation == 3){
            return inflater.inflate(R.layout.fragment_camera2_basic_land, container, false);
        }else {
            return inflater.inflate(R.layout.fragment_camera2_basic, container, false);
        }
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated");
        skipBtn = view.findViewById(R.id.skipBtn2);
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
        view.findViewById(R.id.camBtn2).setOnClickListener(this);
        view.findViewById(R.id.cam_toggle2).setOnClickListener(this);
        view.findViewById(R.id.photoBtn2).setOnClickListener(this);
        view.findViewById(R.id.saveBtn2).setOnClickListener(this);
        view.findViewById(R.id.videoBtn2).setOnClickListener(this);
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
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

        Log.d(TAG, "onResume: " + camCount);
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
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
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
        Log.d(TAG, "setUpCameraOutputs: width = " + width + " : height = " + height + "//"+ (height/width));
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing != mCameraLensFacingDirection) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                // For still image captures, we use the largest available size.
                Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),new CompareSizesByArea());
                mImageReader = ImageReader.newInstance(largest.getWidth(), height, ImageFormat.JPEG, /*maxImages*/5);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                //noinspection ConstantConditions
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
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

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {//가로모드
                    mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {//세로모드
                    mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }
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
        Log.d(TAG, "openCamera");
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
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
        try {
            Log.d(TAG, "createCameraPreviewSession");
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mTextureView.getWidth(), mTextureView.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                setAutoFlash(mPreviewRequestBuilder);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
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
        Log.d(TAG, "configureTransform");
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max((float) viewHeight / mPreviewSize.getHeight(),(float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    /**
     * Initiate a still image capture.
     */
    private void takePicture() {
        lockFocus();
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
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
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

                    showToast("Saved: " + mFile);
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
                    textView.setText(camCount+"/5");
                    Log.d(TAG, mFile.toString());
                    unlockFocus();
                }
            };

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

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
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
                        closeCamera();
                        stopBackgroundThread();
                        reopenCamera();
                    } else if (mCameraLensFacingDirection == CameraCharacteristics.LENS_FACING_FRONT) {
                        mCameraLensFacingDirection = CameraCharacteristics.LENS_FACING_BACK;
                        closeCamera();
                        stopBackgroundThread();
                        reopenCamera();
                    }
                }
            });
        }
    }

    public void reopenCamera() {
        startBackgroundThread();

        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
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
                            Log.d(TAG, "onImagesSelected: " + uriList.toString());
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
            Log.d(TAG, "onPreExecute: ");
            asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            asyncDialog.setMessage("이미지 등록중입니다...");
            asyncDialog.show();
        }

        @Override
        protected void onPostExecute(Integer integer) {
            Log.d(TAG, "onPostExecute: 완료");
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
                Log.d(TAG, "doInBackground: " + selPhotoUri.toString() + "// replace>>"+filePath);

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

                    /*fos.write(bytes);
                    fos.flush();
                    fos.close();*/

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

    public byte[] bitmapToByteArray( Bitmap bitmap ) {
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
                /*activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {*/
                if (filename != null) {
                    Log.d(TAG, "isSave: " + filename + "//"+ mPath);
                    url = "http://484848.co.kr/mobile/photo_upload.php";
                    String[] files = filename.split(",");
                    String[] paths = mPath.split(",");
                    for (int i = 0; i < files.length; i++) {
                        Log.d(TAG, "run: " + files[i] + " // path : " + paths[i]);
                        int result = uploadFile(paths[i], sd, files[i], url);
                        Log.d(TAG, "isSave: result = " + result);
                    }
                }
                if (videoname != null) {
                    url = "http://484848.co.kr/mobile/video_upload.php";
                    String vpath = sd + "/" + videoname;
                    Log.d(TAG, "video : " + videoname + "// vpath : " + vpath);
                    uploadFile(vpath, sd, videoname, url);
                }
                   /* }
                });*/

                /*Intent intent = new Intent(activity, MainActivity.class);
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

                //removePreferences();
                startActivity(intent);
                activity.finish();*/
            }
        } else {
            showToast("사진 또는 영상을 등록/선택해 주세요");
        }
    }

    public int uploadFile(String sourceFileUri,final String uploadFilePath,final String uploadFileName,String upLoadServerUri) {
        final Activity activity = getActivity();
        if (activity != null) {
            String fileName = sourceFileUri;
            HttpURLConnection conn = null;
            DataOutputStream dos = null;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 10 * 1024 * 1024;
            File sourceFile = new File(uploadFileName);
            if (!sourceFile.isFile()) {
                Log.d(TAG, "uploadFile: not-file");
                returnVal = 0;
            } else {
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
                    conn.setRequestProperty("uploaded_file", fileName);
                    conn.setRequestProperty("mb_id", mb_id);
                    dos = new DataOutputStream(conn.getOutputStream());
                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\"" + fileName + "\"" + lineEnd);
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
                            showToast( "Got Exception : see logcat ");
                        }
                    });
                    Log.e(TAG, "Exception : " + e.getMessage(), e);
                }
                //dialog.dismiss();
                returnVal = serverResponseCode;

            } // End else block
        }
        return returnVal;
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
        outState.putString("wr_price", wr_price);
        outState.putString("wr_price2", wr_price2);
        outState.putString("pd_price_type", pd_price_type);
        outState.putString("skip", "skip");
        outState.putInt("camCount",camCount);
        outState.putString("filename",filename);
        outState.putString("videoname",videoname);
    }
}