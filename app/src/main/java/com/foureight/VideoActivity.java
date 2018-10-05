/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.foureight;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static android.widget.Toast.LENGTH_SHORT;

/**
 *  This activity uses the camera/camcorder as the A/V source for the {@link android.media.MediaRecorder} API.
 *  A {@link android.view.TextureView} is used as the camera preview which limits the code to API 14+. This
 *  can be easily replaced with a {@link android.view.SurfaceView} to run on older devices.
 */
public class VideoActivity extends Activity implements SurfaceHolder.Callback{
    int serverResponseCode = 0;

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Camera mCamera;
    private TextureView mPreview;
    private MediaRecorder mMediaRecorder;
    private File mOutputFile;
    int apiVersion = Build.VERSION.SDK_INT;
    private boolean isRecording = false;
    private static final String TAG = "Recorder";
    private Button camBtn,saveBtn,videoBtn,photoBtn;
    private TextView textView;
    private int camCount = 0,timer = 0, timer2 = 0;
    private int angle;
    private boolean previewImg = false;
    private Camera.Size previewSize;
    private Camera.Parameters camParams;
    private String mb_id,title,cate,cate2,filename,sd,path,url,type,videoname,mPath;
    Handler handler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        url = "http://mave01.cafe24.com/mobile/video_upload.php";
        sd = Environment.getExternalStorageDirectory().getAbsolutePath()+"/foureight";

        mb_id=getIntent().getStringExtra("mb_id");
        title=getIntent().getStringExtra("title");
        cate=getIntent().getStringExtra("cate");
        cate2=getIntent().getStringExtra("cate2");
        type=getIntent().getStringExtra("type");
        if(getIntent().getStringExtra("filename") != null)
            filename=getIntent().getStringExtra("filename");
        if(getIntent().getStringExtra("videoname") != null)
            videoname=getIntent().getStringExtra("videoname");
        if(getIntent().getStringExtra("timer2") != null) {
            timer2 = Integer.parseInt(getIntent().getStringExtra("timer2"));
            if (timer2 > 0) {
                textView.setText(timer2 + "s/20s");
                timer = timer2;
            }
        }
        mPath=getIntent().getStringExtra("mPath");

        surfaceView = (SurfaceView)findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mPreview = (TextureView) findViewById(R.id.surface_view);
        //captureButton = (Button) findViewById(R.id.button_capture);
        textView = (TextView)findViewById(R.id.camCount);
        camBtn = (Button)findViewById(R.id.camBtn);
        saveBtn = (Button)findViewById(R.id.saveBtn);
        videoBtn = (Button)findViewById(R.id.videoBtn);
        photoBtn = (Button)findViewById(R.id.photoBtn);

        Display display = this.getWindowManager().getDefaultDisplay();

        switch (display.getRotation()){
            case Surface.ROTATION_0:
                angle = 90;
                break;
            case Surface.ROTATION_90:
                angle = 0;
                break;
            case Surface.ROTATION_180:
                angle = 270;
                break;
            case Surface.ROTATION_270:
                angle = 180;
                break;
            default:
                angle = 90;
                break;
        }

        //prepareVideoRecorder();

        videoBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (isRecording) {
                    Log.d(TAG, "onClick: 0");
                    // BEGIN_INCLUDE(stop_release_media_recorder)

                    // stop recording and release camera
                    try {
                        mMediaRecorder.stop();  // stop the recording
                    } catch (RuntimeException e) {
                        // RuntimeException is thrown when stop() is called immediately after start().
                        // In this case the output file is not properly constructed ans should be deleted.
                        Log.d(TAG, "RuntimeException: stop() is called immediately after start()");
                        //noinspection ResultOfMethodCallIgnored
                        mOutputFile.delete();
                    }
                    releaseMediaRecorder(); // release the MediaRecorder object
                    mCamera.lock();         // take camera access back from MediaRecorder

                    // inform the user that recording has stopped
                    //setCaptureButtonText("Capture");
                    isRecording = false;
                    //releaseCamera();
                    // END_INCLUDE(stop_release_media_recorder)
                    handler.removeMessages(0);
                    timer=0;
                } else {
                    timer2 = 0;
                    Log.d(TAG, "onClick: 1");

                    // BEGIN_INCLUDE(prepare_start_media_recorder)

                    new MediaPrepareTask().execute(null, null, null);

                    // END_INCLUDE(prepare_start_media_recorder)

                }
            }
        });

        saveBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(final View v) {
                Log.d(TAG, "filename : "+filename + " videoname : " +videoname);
                if(filename != null || videoname != null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d(TAG, "run upload.. ");
                                }
                            });
                            if(filename!=null) {
                                String[] files = filename.split(",");
                                String[] paths = mPath.split(",");
                                for (int i = 0; i < files.length; i++) {
                                    Log.d(TAG, "run: " + files[i]);
                                    uploadFile(paths[i], sd, files[i], url);
                                }
                            }
                            if(videoname != null) {
                                if(path==null){
                                    path = sd + "/foureight/" + videoname;
                                }
                                uploadFile(path, sd, videoname, url);
                            }
                        }
                    }).start();

                    Intent intent = new Intent(VideoActivity.this, MainActivity.class);
                    intent.putExtra("videoname", videoname);
                    intent.putExtra("filename", filename);
                    intent.putExtra("mb_id",mb_id);
                    intent.putExtra("title",title);
                    intent.putExtra("type",type);
                    intent.putExtra("cate1",cate);
                    intent.putExtra("cate2",cate2);
                    Toast.makeText(VideoActivity.this, "파일 업로드 완료", Toast.LENGTH_SHORT).show();

                    startActivity(intent);
                }else{
                    Toast.makeText(VideoActivity.this, "저장될 사진,영상 또는 선택된 사진이 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        camBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(VideoActivity.this, CameraActivity.class);
                intent.putExtra("mb_id",mb_id);
                intent.putExtra("title",title);
                intent.putExtra("cate",cate);
                intent.putExtra("cate2",cate2);
                if(videoname!=null)
                    intent.putExtra("videoname",videoname);
                intent.putExtra("timer2",timer2);
                if(filename!=null)
                    intent.putExtra("filename",filename);
                if(mPath != null)
                    intent.putExtra("mPath",mPath);
                startActivity(intent);
            }
        });

        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                timer++;
                timer2++;
                textView.setText(timer+"s/20s");

                if(timer < 20) {
                    handler.sendEmptyMessageDelayed(0, 1000);
                }else {
                    /*new Thread(new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d(TAG, "run upload.. ");
                                }
                            });
                            uploadFile(path,sd,videoname,url);
                            timer=0;
                        }
                    }).start();*/
                    handler.removeMessages(0);
                    timer = 0;
                }
            }
        };
    }

    private void setCaptureButtonText(String title) {
        //captureButton.setText(title);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: " );
        // if we are using MediaRecorder, release it first
        releaseMediaRecorder();
        // release the camera immediately on pause event
        releaseCamera();
    }

    private void releaseMediaRecorder(){
        Log.d(TAG, "releaseMediaRecorder: ");
        if (mMediaRecorder != null) {
            // clear recorder configuration
            mMediaRecorder.reset();
            // release the recorder object
            mMediaRecorder.release();
            mMediaRecorder = null;
            // Lock camera for later use i.e taking it back from MediaRecorder.
            // MediaRecorder doesn't need it anymore and we will release it if the activity pauses.
            mCamera.lock();
        }
    }

    private void releaseCamera(){
        Log.d(TAG, "releaseCamera: ");
        if (mCamera != null){
            // release the camera for other applications
            mCamera.release();
            mCamera = null;
        }
    }

    private boolean prepareVideoRecorder(){
        Log.d(TAG, "prepareVideoRecorder: ");
        // BEGIN_INCLUDE (configure_preview)
        mCamera = CameraHelper.getDefaultCameraInstance();

        // We need to make sure that our preview and recording video size are supported by the
        // camera. Query camera to find all the sizes and choose the optimal size given the
        // dimensions of our preview surface.
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
        List<Camera.Size> mSupportedVideoSizes = parameters.getSupportedVideoSizes();
        Camera.Size optimalSize = CameraHelper.getOptimalVideoSize(mSupportedVideoSizes,
                mSupportedPreviewSizes, mPreview.getWidth(), mPreview.getHeight());

        // Use the same size for recording profile.
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        profile.videoFrameWidth = optimalSize.width;
        profile.videoFrameHeight = optimalSize.height;
        // likewise for the camera object itself.
        parameters.setRotation(angle);
        parameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mCamera.setParameters(parameters);
        mCamera.setDisplayOrientation(angle);
        try {
                // Requires API level 11+, For backward compatibility use {@link setPreviewDisplay}
                // with {@link SurfaceView}
                mCamera.setPreviewTexture(mPreview.getSurfaceTexture());
        } catch (IOException e) {
            Log.e(TAG, "Surface texture is unavailable or unsuitable" + e.getMessage());
            return false;
        }
        // END_INCLUDE (configure_preview)


        // BEGIN_INCLUDE (configure_media_recorder)
        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT );
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(profile);
        mMediaRecorder.setMaxDuration(20000);

        mMediaRecorder.setOrientationHint(angle);
        // Step 4: Set output file
        mOutputFile = CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_VIDEO);
        if (mOutputFile == null) {
            return false;
        }

        File file = new File(sd);
        file.mkdir();
        videoname = System.currentTimeMillis() + "_" + mb_id +".mp4";
        path = sd + "/" + videoname;

        mMediaRecorder.setOutputFile(path);
        // END_INCLUDE (configure_media_recorder)

        // Step 5: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated: ");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged: ");
        if(!previewImg){
            if(apiVersion >= Build.VERSION_CODES.M){
                    mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                    camParams = mCamera.getParameters();
                    List<Camera.Size> mSupportedPreviewSizes = camParams.getSupportedPreviewSizes();
                    List<Camera.Size> mSupportedVideoSizes = camParams.getSupportedVideoSizes();
                    /*Camera.Size videoSize = camParams.getSupportedVideoSizes().get(0);
                    optimalSize = getBestViedoSize(videoSize.width,videoSize.height);*/
                    Camera.Size presize = camParams.getSupportedPreviewSizes().get(0);
                    previewSize = getBestPreviewSize(presize.width,presize.height);
                    camParams.setPictureSize(previewSize.width,previewSize.height);
                    camParams.setPreviewSize(previewSize.width,previewSize.height);
                    camParams.setRotation(angle);
            }
            //camera = Camera.open();
            if(mCamera != null){
                try{
                    camParams = mCamera.getParameters();
                    List<Camera.Size> mSupportedPreviewSizes = camParams.getSupportedPreviewSizes();
                    List<Camera.Size> mSupportedVideoSizes = camParams.getSupportedVideoSizes();
                    /*Camera.Size videoSize = camParams.getSupportedVideoSizes().get(0);
                    optimalSize = getBestViedoSize(videoSize.width,videoSize.height);*/
                    Camera.Size presize = camParams.getSupportedPreviewSizes().get(0);
                    previewSize = getBestPreviewSize(presize.width,presize.height);
                    camParams.setPictureSize(previewSize.width,previewSize.height);
                    camParams.setPreviewSize(previewSize.width,previewSize.height);
                    camParams.setRotation(angle);
                    mCamera.setParameters(camParams);
                    //Log.d(TAG, "프리뷰 사이즈 :: " + previewSize.width+"/"+previewSize.height);
                    mCamera.setDisplayOrientation(angle);
                    mCamera.setPreviewDisplay(surfaceHolder);
                    mCamera.startPreview();
                    previewImg = true;
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed: ");
    }

    private Camera.Size getBestPreviewSize(int width, int height){
        Camera.Size result=null;
        Camera.Parameters p = mCamera.getParameters();
        for (Camera.Size size : p.getSupportedPreviewSizes()) {
            if (size.width<=width && size.height<=height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;
                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Asynchronous task for preparing the {@link android.media.MediaRecorder} since it's a long blocking
     * operation.
     */
    class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {


        @Override
        protected Boolean doInBackground(Void... voids) {
            // initialize video camera
            if (prepareVideoRecorder()) {
                Log.d(TAG, "doInBackground: 1");
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording

                mMediaRecorder.start();
                handler.sendEmptyMessage(0);

                isRecording = true;
            } else {
                Log.d(TAG, "doInBackground: 2");
                // prepare didn't work, release the camera
                releaseMediaRecorder();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.d(TAG, "onPostExecute: ");
            Log.d(TAG, result.toString());
            if (!result) {
                VideoActivity.this.finish();
            }
            mCamera.startPreview();
            // inform the user that recording has started
            //setCaptureButtonText("Stop");

        }
    }

    public int uploadFile(String sourceFileUri,final String uploadFilePath,final String uploadFileName,String upLoadServerUri) {
        String fileName = sourceFileUri;
        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 10 * 1024 * 1024;
        File sourceFile = new File(sourceFileUri);
        if (!sourceFile.isFile()) {
            Log.e("uploadFile", "Source File not exist :" +uploadFilePath + "" + uploadFileName);
            runOnUiThread(new Runnable() {
                public void run() {
                    //messageText.setText("Source File not exist :"+uploadFilePath + "" + uploadFileName);
                }
            });
            return 0;
        }else{
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
                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""+ fileName + "\"" + lineEnd);
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

                if(serverResponseCode == 200){
                    runOnUiThread(new Runnable() {
                        public void run() {
                            String msg = "File Upload Completed.\n\n See uploaded file here : \n\n" + uploadFileName;
                            //messageText.setText(msg);
                            Toast.makeText(VideoActivity.this, "File Upload Complete.", Toast.LENGTH_SHORT).show();
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
                runOnUiThread(new Runnable() {
                    public void run() {
                        //messageText.setText("MalformedURLException Exception : check script url.");
                        Toast.makeText(VideoActivity.this, "MalformedURLException", Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {
                //dialog.dismiss();
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    public void run() {
                        //messageText.setText("Got Exception : see logcat ");
                        Toast.makeText(VideoActivity.this, "Got Exception : see logcat ",Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e(TAG, "Exception : "+ e.getMessage(), e);
            }
            //dialog.dismiss();
            return serverResponseCode;

        } // End else block
    }
}
