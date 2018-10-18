package com.foureight;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.*;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
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
import java.net.URLEncoder;
import java.util.List;

import static android.Manifest.permission.CAMERA;


public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback{
    final static String TAG = "CameraActivity";
    Camera camera;
    SurfaceHolder surfaceHolder;
    SurfaceView surfaceView;
    boolean previewImg = false;
    boolean isRecording = false;
    boolean videoFlag = true;
    final int MY_PERMISSION_REQUEST_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 30;
    private static final int GALLERY_CODE = 1112;
    int serverResponseCode = 0;
    int apiVersion = Build.VERSION.SDK_INT;
    int angle,width,height,controlwidth,controlheight;
    public Button camBtn,saveBtn,videoBtn,photoBtn,skipBtn;
    private int camCount = 0,timer = 0, timer2 = 0;
    int viewWidth,viewHeight;
    TextView textView;
    String mb_id,title,cate1,cate2,url,path,sd,file_name,filename,mPath,imgPath,imgName,type,videoname;
    Uri selPhotoUri;
    byte[] bytes;
    MediaRecorder mRecorder;
    Handler handler;

    private TextureView mPreview;

    private MediaRecorder mMediaRecorder;
    private File mOutputFile;

    Camera.Parameters camParams;

    Camera.Size previewSize;
    Camera.Size optimalSize;

    private OrientationEventListener orientationEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_camera);
        }else{
            setContentView(R.layout.activity_camera_land);
        }
        Log.d(TAG, "onCreate: 시작");

        url = "http://mave01.cafe24.com/mobile/photo_upload.php";
        sd = Environment.getExternalStorageDirectory().getAbsolutePath()+"/foureight";

        textView = (TextView)findViewById(R.id.camCount);
        camBtn = (Button)findViewById(R.id.camBtn);
        saveBtn = (Button)findViewById(R.id.saveBtn);
        videoBtn = (Button)findViewById(R.id.videoBtn);
        photoBtn = (Button)findViewById(R.id.photoBtn);
        skipBtn = (Button)findViewById(R.id.skipBtn);
        if(getIntent().getStringExtra("mb_id") != null) {
            mb_id = getIntent().getStringExtra("mb_id");
        }
        if( getIntent().getStringExtra("title") != null) {
            title = getIntent().getStringExtra("title");
        }
        if(getIntent().getStringExtra("cate1") != null) {
            cate1 = getIntent().getStringExtra("cate1");
        }
        if(getIntent().getStringExtra("cate2") != null) {
            cate2 = getIntent().getStringExtra("cate2");
        }
        if(getIntent().getStringExtra("type") != null) {
            type = getIntent().getStringExtra("type");
        }

        Log.d(TAG, "Skip type : " + type);

        if(type.equals("2") || type == "2"){
            Log.d(TAG, "Skip A");
            skipBtn.setVisibility(View.VISIBLE);
        }else{
            Log.d(TAG, "Skip B");
            skipBtn.setVisibility(View.GONE);
        }

        getPreferences();

        if(filename!=null || filename != ""){
            Log.d(TAG, "onCreate: " + filename);
            if(filename!=null ){
                String[] files = filename.split(",");
                int filenum = files.length;
                textView.setText(filenum+"/5");
                camCount=filenum;
            }
        }

        getWindow().setFormat(PixelFormat.UNKNOWN);
        surfaceView = (SurfaceView)findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mPreview = (TextureView) findViewById(R.id.surface_view);

//        if(!hasPermission()){
//            requestNecessryPermissions();
//        }
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


        camBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                camera.autoFocus(camAutoFocuse);
            }
        });

        skipBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CameraActivity.this, MainActivity.class);
                intent.putExtra("mb_id",mb_id);
                intent.putExtra("title",title);
                intent.putExtra("type",type);
                intent.putExtra("cate1",cate1);
                intent.putExtra("cate2",cate2);
                intent.putExtra("skip" , "skip");
                startActivity(intent);
                finish();
            }
        });



        photoBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setType("image/*");
                startActivityForResult(intent,GALLERY_CODE);
                // TODO: 2018-10-17 : 갤럭시에서 여러장 선택이 안됨 
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Log.d(TAG, "filename : "+filename + " videoname : " +videoname);

                if(filename != null || filename != "" || videoname != null || videoname != "") {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d(TAG, "run upload.. ");
                                }
                            });
                            if(filename != null || filename != "") {
                                url = "http://mave01.cafe24.com/mobile/photo_upload.php";
                                String[] files = filename.split(",");
                                String[] paths = mPath.split(",");
                                for (int i = 0; i < files.length; i++) {
                                    Log.d(TAG, "run: " + files[i] + " // path : " + paths[i]);
                                    uploadFile(paths[i], sd, files[i], url);
                                }
                            }
                            if(videoname!=null || videoname != "") {
                                url = "http://mave01.cafe24.com/mobile/video_upload.php";
                                String vpath = sd +"/"+ videoname;
                                Log.d(TAG, "video : " + videoname + "// vpath : " + vpath);
                                uploadFile(vpath, sd, videoname, url);
                            }
                        }
                    }).start();

                    Intent intent = new Intent(CameraActivity.this, MainActivity.class);
                    intent.putExtra("filename", filename);
                    intent.putExtra("videoname", videoname);
                    intent.putExtra("timer2", timer2);
                    intent.putExtra("mb_id",mb_id);
                    intent.putExtra("title",title);
                    intent.putExtra("type",type);
                    intent.putExtra("cate1",cate1);
                    intent.putExtra("cate2",cate2);
                    Toast.makeText(CameraActivity.this, "파일 업로드 완료", Toast.LENGTH_SHORT).show();
                    removePreferences();
                    startActivity(intent);
                    finish();
                }else{
                    Toast.makeText(CameraActivity.this, "저장될 사진,영상 또는 선택된 사진이 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //비디오 버튼
        videoBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isRecording) {
                    videoBtn.setBackgroundResource(R.drawable.video_btn);
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
                    camera.lock();         // take camera access back from MediaRecorder

                    // inform the user that recording has stopped
                    //setCaptureButtonText("Capture");
                    isRecording = false;

                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

                    //releaseCamera();
                    // END_INCLUDE(stop_release_media_recorder)
                    handler.removeMessages(0);
                    timer = 0;
                } else {
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    } else {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    }
                    timer2 = 0;
                    Log.d(TAG, "onClick: 1");

                    videoBtn.setBackgroundResource(R.drawable.video_btn_stop);
                    // BEGIN_INCLUDE(prepare_start_media_recorder)
                    new MediaPrepareTask().execute(null, null, null);
                    // END_INCLUDE(prepare_start_media_recorder)

                }
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

                    handler.removeMessages(0);
                    timer = 0;
                }
            }
        };
    }

    public boolean showDials(){
        CommonDialogs dialogs = new CommonDialogs(CameraActivity.this);
        dialogs.showAlertDialog(this, "이미 녹화된 영상이 있습니다. 새로 촬영 할까요?", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                videoFlag = true;
            }
        }, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                videoFlag  = false;
            }
        });
        return videoFlag;
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //ArrayList imageList = new ArrayList<>();
        Log.d(TAG, "onActivityResult: " + data);
        if(resultCode == RESULT_OK){
            switch (requestCode){
                case GALLERY_CODE:
                    if(data.getClipData() == null){
                        String name_Str = getImageNameToUri(data.getData());
                        selPhotoUri = data.getData();

                        try {
                            Bitmap image = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                            bytes = bitmapToByteArray(image);

                            File file = new File(sd);
                            file.mkdir();
                            file_name = System.currentTimeMillis() + "_" + mb_id + "_" + camCount + ".jpg";
                            path = sd + "/" + file_name;
                            try {
                                file = new File(path);

                                FileOutputStream fos = new FileOutputStream(file);
                                fos.write(bytes);
                                fos.flush();
                                fos.close();
                            }catch (FileNotFoundException e){
                                e.printStackTrace();
                            }catch (Exception e) {
                                e.printStackTrace();
                            }

                            if(filename == null || filename == ""){
                                filename = file_name;
                            }else {
                                filename = filename + ","+file_name;
                            }
                            if(mPath == null){
                                mPath = path;
                            }else {
                                mPath = mPath + ","+path;
                            }

                            Log.d(TAG, "filename picture: " + filename + " // path : " + mPath);
                            savePreferences();
                            camCount++;
                            textView.setText(camCount+"/5");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //imageList.add(String.valueOf(data.getData()));
                    }else{
                        ClipData clipData = data.getClipData();
                        if(clipData.getItemCount() > 5){
                            Toast.makeText(CameraActivity.this, "사진은 5개까지 선택가능합니다.", Toast.LENGTH_SHORT).show();
                            return;
                        }else if(clipData.getItemCount() > 1 && clipData.getItemCount() <= 5){
                            for(int i = 0; i < clipData.getItemCount(); i++){
                                String name_Str = getImageNameToUri(clipData.getItemAt(0).getUri());
                                selPhotoUri = data.getClipData().getItemAt(i).getUri();

                                try {
                                    Bitmap image = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getClipData().getItemAt(i).getUri());
                                    bytes = bitmapToByteArray(image);
                                    url = "http://mave01.cafe24.com/mobile/photo_upload.php";
                                    sd = Environment.getExternalStorageDirectory().getAbsolutePath()+"/foureight";
                                    File file = new File(sd);
                                    file.mkdir();
                                    file_name = System.currentTimeMillis() + "_" + mb_id + "_" + camCount + ".jpg";
                                    path = sd + "/" + file_name;

                                        file = new File(path);

                                        FileOutputStream fos = new FileOutputStream(file);
                                        fos.write(bytes);
                                        fos.flush();
                                        fos.close();


                                    if(filename == null || filename == ""){
                                        filename = file_name;
                                    }else {
                                        filename = filename + ","+file_name;
                                    }
                                    if(mPath == null){
                                        mPath = path;
                                    }else {
                                        mPath = mPath + ","+path;
                                    }

                                    Log.d(TAG, "filename picture: " + filename+" // path : " + mPath);
                                    savePreferences();
                                    camCount++;
                                    textView.setText(camCount+"/5");
                                }catch (FileNotFoundException e){
                                    e.printStackTrace();
                                }catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    Camera.AutoFocusCallback camAutoFocuse = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera cam) {
            // TODO: 2018-10-17 : 리얼타임 5장과 재접속 시 확인 필요 
            if(success && camCount < 5) {
                camera.takePicture(mySutterCallback, null, myPictureCallback_JPG);
            }else{
                Toast.makeText(CameraActivity.this, "5장 모두 찰영하였습니다.",Toast.LENGTH_SHORT).show();
            }
        }
    };

    Camera.ShutterCallback mySutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {

        }
    };

    Camera.PictureCallback myPictureCallback_JPG = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera cam) {
            Bitmap bitmapPicture = BitmapFactory.decodeByteArray(data, 0, data.length);
            bytes = bitmapToByteArray(bitmapPicture);

            File file = new File(sd);
            file.mkdir();
            file_name = System.currentTimeMillis() + "_" + mb_id + "_" + camCount + ".jpg";
            path = sd + "/" + file_name;

            try {
                file = new File(path);

                FileOutputStream fos = new FileOutputStream(file);
                fos.write(data);
                fos.flush();
                fos.close();
            }catch (FileNotFoundException e){
                e.printStackTrace();
            }catch (Exception e) {
                e.printStackTrace();
            }

            if(filename == null || filename == ""){
                filename = file_name;
            }else {
                filename = filename + ","+file_name;
            }
            if(mPath == null){
                mPath = path;
            }else {
                mPath = mPath + ","+path;
            }

            Log.d(TAG, "filename picture: " + filename + "// path : " + mPath);

            camCount++;
            textView.setText(camCount+"/5");
            savePreferences();
            camera.startPreview();

            //Log.d(TAG,"A::"+path+" and "+files[0]+" and "+files[1]+" and "+files[2]+" and "+files[3]+" and " + files[4] );
        }
    };

    public byte[] bitmapToByteArray( Bitmap bitmap ) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream() ;
        bitmap.compress( Bitmap.CompressFormat.JPEG, 100, stream) ;
        byte[] byteArray = stream.toByteArray() ;
        String image = Base64.encodeToString(byteArray,Base64.DEFAULT);
        String temp = "";
        try{
            temp = "&image1="+ URLEncoder.encode(image,"utf-8");
        }catch (Exception e){
            e.printStackTrace();
        }
        return byteArray ;
    }

    /////   Uri 에서 파일명을 추출하는 로직
    public String getImageNameToUri(Uri data)
    {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(data, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

        cursor.moveToFirst();

        imgPath = cursor.getString(column_index);
        imgName = imgPath.substring(imgPath.lastIndexOf("/") + 1);

        return imgName;
    }

    public boolean checkCAMERAPermission(){
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), CAMERA);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surfaceCreated: " + this.surfaceHolder.getSurface() + "// me surface : " + surfaceHolder.getSurface());
        camera = Camera.open();
        try {
            camera.setPreviewDisplay(surfaceHolder);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void initCamera(){
        Log.d(TAG, "initCamera: " + surfaceHolder + camera.getParameters().getPreviewSize().width);
        try{
            camParams = camera.getParameters();
            List<Camera.Size> mSupportedPreviewSizes = camParams.getSupportedPreviewSizes();
            List<Camera.Size> mSupportedVideoSizes = camParams.getSupportedVideoSizes();
            Camera.Size presize = camParams.getSupportedPreviewSizes().get(0);
            previewSize = getBestPreviewSize(presize.width,presize.height);
            optimalSize = CameraHelper.getOptimalVideoSize(mSupportedPreviewSizes,mSupportedVideoSizes, surfaceView.getWidth(),surfaceView.getHeight());
            Log.d(TAG, "initCamera: " + optimalSize.width + "//" + optimalSize.height + "//" + angle);
            camParams.setPictureSize(previewSize.width,previewSize.height);
            camParams.setPreviewSize(previewSize.width,previewSize.height);
            //Log.d(TAG, "프리뷰 사이즈 :: " + previewSize.width+"/"+previewSize.height);
            camParams.setRotation(angle);
            //Log.d(TAG, "initCamera: " + camParams.getPreviewSize().width + "//" + camParams.getPreviewSize().height+ "//" + surfaceView.getHeight() + "//" + surfaceView.getWidth());
            camera.setParameters(camParams);
            camera.setDisplayOrientation(angle);
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
            previewImg = true;
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        viewWidth = surfaceView.getWidth();
        viewHeight = surfaceView.getHeight();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Log.d(TAG, "surfaceChanged: " + camera+ " i : " + i + " i1 : " + i1 + " i2 : " + i2 );
        if(surfaceHolder.getSurface()== null){
            Log.d(TAG, "surfaceChanged: not surface");
            return;
        }
        if(previewImg){
            Log.d(TAG, "surfaceChanged: previewImg" + this.surfaceHolder + "//" + surfaceHolder);
            camera.stopPreview();
            previewImg = false;
        }
        if(camera!=null){
            Log.d(TAG, "surfaceChanged: camera not null" + this.surfaceHolder + "//" + surfaceHolder);
            initCamera();
        }else{
            Log.d(TAG, "surfaceChanged: camera null"+ this.surfaceHolder + "//" + surfaceHolder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        camera.stopPreview();
        camera.release();
        camera = null;
        previewImg = false;
        camCount = 0;
        Log.d(TAG, "surfaceDestroyed: " + camera + " previewImg : " + previewImg);
    }

    private Camera.Size getBestPreviewSize(int width, int height){
        Camera.Size result=null;
        Camera.Parameters p = camera.getParameters();
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
/*
    private boolean hasPermission(){
        int res = 0;
        String[] permissions = new String[]{CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        for (String prems : permissions){
            res = checkCallingOrSelfPermission(prems);
            if(!(res == PackageManager.PERMISSION_GRANTED)){
                return false;
            }
        }
        return true;
    }

    private void requestNecessryPermissions(){
        String[] permissions = new String[]{CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            requestPermissions(permissions, MY_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean allowed = true;
        switch (requestCode){
            case MY_PERMISSION_REQUEST_CODE:
                for(int res : grantResults){
                    allowed = allowed && (res == PackageManager.PERMISSION_GRANTED);
                }
                break;
            default:
                allowed = false;
                break;
//                if(grantResults.length > 0){
//                    boolean cameraAccepted = (grantResults[0] == PackageManager.PERMISSION_GRANTED);
//                    if(cameraAccepted){
//                        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
//                    }else{
//                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
//                            if(shouldShowRequestPermissionRationale(CAMERA)){
//                                showMessagePermission(
//                                        "카메라를 사용하려면 권한 허가가 필요합니다.",
//                                        new DialogInterface.OnClickListener(){
//                                            @Override
//                                            public void onClick(DialogInterface dialogInterface, int i) {
//                                                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
//                                                    requestPermissions(new String[]{CAMERA},MY_PERMISSION_REQUEST_CODE);
//                                                }
//                                            }
//                                        }
//                                );
//                            }
//                        }
//                    }
//                }
//                break;
        }

        if(allowed){
            doRestart(this);
        }else{
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if(shouldShowRequestPermissionRationale(CAMERA)){
                    Toast.makeText(CameraActivity.this, "카메라 권한이 없습니다.", Toast.LENGTH_SHORT).show();
                }else if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                    Toast.makeText(CameraActivity.this,"파일쓰기 권한이 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public static void doRestart(Context c){
        try{
            if(c != null){
                PackageManager pm = c.getPackageManager();
                if(pm != null){
                    Intent mStartActivity = pm.getLaunchIntentForPackage(c.getPackageName());
                    if(mStartActivity != null){
                        mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        int mPendingIntentId = 213141;
                        PendingIntent mPendingIntent = PendingIntent.getActivity(c,mPendingIntentId,mStartActivity,PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                        System.exit(0);
                    }else{
                        Log.d(TAG,"앱을 재실행하지 못했습니다.(mStartActivity null)");
                    }
                }else{
                    Log.d(TAG, "앱을 재실행하지 못했습니다.(PM null)");
                }
            }else{
                Log.d(TAG, "앱을 재실행하지 못했습니다.(Context null)");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
*/
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w/h;
        if (sizes==null)
            return null;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = h; // Find size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size; minDiff = Math.abs(size.height - targetHeight);
            }
        }
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size; minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
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
                runOnUiThread(new Runnable() {
                    public void run() {
                        //messageText.setText("MalformedURLException Exception : check script url.");
                        Toast.makeText(CameraActivity.this, "MalformedURLException", Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {
                //dialog.dismiss();
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    public void run() {
                        //messageText.setText("Got Exception : see logcat ");
                        Toast.makeText(CameraActivity.this, "Got Exception : see logcat ",Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e(TAG, "Exception : "+ e.getMessage(), e);
            }
            //dialog.dismiss();
            return serverResponseCode;

        } // End else block
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    // 값 불러오기
    private void getPreferences(){
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        filename = pref.getString("filename",filename);
        videoname = pref.getString("videoname", videoname);
        mPath = pref.getString("mPath", mPath);
        mb_id = pref.getString("mb_id", mb_id);
        title = pref.getString("title", title);
        cate1 = pref.getString("cate1", cate1);
        cate2 = pref.getString("cate2", cate2);
        type = pref.getString("type", type);
        Log.d(TAG, "getPreferences:" + filename  + "// "+ pref);
    }

    // 값 저장하기
    private void savePreferences(){
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("filename", filename);
        editor.putString("videoname", videoname);
        editor.putString("mPath", mPath);
        editor.putString("mb_id", mb_id);
        editor.putString("title", title);
        editor.putString("cate1", cate1);
        editor.putString("cate2", cate2);
        editor.putString("type", type);
        editor.commit();
        Log.d(TAG, "savePreferences: " + filename + "//" + pref);
    }

    // 값(Key Data) 삭제하기
    private void removePreferences(){
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.clear();
        editor.commit();
    }


    @Override
    protected void onResume() {

        if(filename!=null && filename != ""){
            String[] files = filename.split(",");
            int filenum = files.length;
            textView.setText(filenum+"/5");
            camCount=filenum;
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
            camera.lock();
        }
    }

    private void releaseCamera(){
        Log.d(TAG, "releaseCamera: ");
        if (camera != null){
            // release the camera for other applications
            camera.release();
            camera = null;
        }
    }

    private boolean prepareVideoRecorder(){
        Log.d(TAG, "prepareVideoRecorder: ");
        // BEGIN_INCLUDE (configure_preview)
        camera = CameraHelper.getDefaultCameraInstance();

        // We need to make sure that our preview and recording video size are supported by the
        // camera. Query camera to find all the sizes and choose the optimal size given the
        // dimensions of our preview surface.
        Camera.Parameters parameters = camera.getParameters();
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
        camera.setParameters(parameters);
        camera.setDisplayOrientation(angle);
        try {
            // Requires API level 11+, For backward compatibility use {@link setPreviewDisplay}
            // with {@link SurfaceView}
            camera.setPreviewTexture(mPreview.getSurfaceTexture());
        } catch (IOException e) {
            Log.e(TAG, "Surface texture is unavailable or unsuitable" + e.getMessage());
            return false;
        }
        // END_INCLUDE (configure_preview)


        // BEGIN_INCLUDE (configure_media_recorder)
        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        camera.unlock();
        mMediaRecorder.setCamera(camera);

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
        savePreferences();
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
                CameraActivity.this.finish();
            }
            camera.startPreview();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //backkey Event
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("글 등록을 취소 하시겠습니까?")
                    .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    })
                    .setPositiveButton("종료", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(CameraActivity.this,MainActivity.class);
                            startActivity(intent);
                            removePreferences();
                            finish();
                        }
                    });
            AlertDialog alertDialog = alert.create();
            alert.show();
            return true;
        }
        return false;
    }
}