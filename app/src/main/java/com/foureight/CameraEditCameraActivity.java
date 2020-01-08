package com.foureight;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
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
import java.util.ArrayList;
import java.util.List;

import gun0912.tedbottompicker.TedBottomPicker;


public class CameraEditCameraActivity extends AppCompatActivity implements SurfaceHolder.Callback{
    final static String TAG = "CameraEditActivity";
    Camera camera;
    SurfaceHolder surfaceHolder;
    SurfaceView surfaceView;
    boolean previewImg = false;
    boolean isRecording = false;
    boolean videoFlag = true;
    int mCameraFacing;
    final int MY_PERMISSION_REQUEST_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 30;
    private static final int GALLERY_CODE = 1112;
    int serverResponseCode = 0;
    int apiVersion = Build.VERSION.SDK_INT;
    int angle,dgree,width,height,controlwidth,controlheight;
    public Button camBtn,saveBtn,videoBtn,photoBtn,skipBtn,cam_toggle;
    private int camCount = 0,timer = 0, timer2 = 0;
    int viewWidth,viewHeight;
    TextView textView;
    String url,path,sd,videoname,mPath,index,mb_id,realname;
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
            setContentView(R.layout.activity_video);
        }else{
            setContentView(R.layout.activity_video_land);
        }
        //sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory().getAbsolutePath())));

        Log.d(TAG, "onCreate: 시작");
        sd = Environment.getExternalStorageDirectory().getAbsolutePath()+"/foureight/";
        File dir = new File(sd);

        String[] children = dir.list();
        if (children != null) {
            for (int i=0; i<children.length; i++) {
                String filename = children[i];
                File f = new File(sd + filename);

                if (f.exists()) {
                    f.delete();
                }
            }
        }

        url = "http://484848.co.kr/mobile/photo_upload.php";
        sd = Environment.getExternalStorageDirectory().getAbsolutePath()+"/foureight";

        textView = (TextView)findViewById(R.id.camCount2);
        //textView.setVisibility(View.GONE);
        //camBtn = (Button)findViewById(R.id.camBtn);
        //camBtn.setVisibility(View.GONE);
        saveBtn = (Button)findViewById(R.id.saveBtn);
        videoBtn = (Button)findViewById(R.id.videoBtn);
        photoBtn = (Button)findViewById(R.id.photoBtn);
        skipBtn = (Button)findViewById(R.id.skipBtn);
        //saveBtn.setVisibility(View.GONE);
        cam_toggle = (Button)findViewById(R.id.cam_toggle);

        if(getIntent().getStringExtra("index") != null) {
            index = getIntent().getStringExtra("index");
        }
        if(getIntent().getStringExtra("mb_id") != null) {
            mb_id = getIntent().getStringExtra("mb_id");
        }

        getPreferences();

        getWindow().setFormat(PixelFormat.UNKNOWN);
        surfaceView = (SurfaceView)findViewById(R.id.surfaceView2);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mPreview = (TextureView) findViewById(R.id.surface_view2);

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


        /*camBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                camera.autoFocus(camAutoFocuse);
            }
        });*/

        skipBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CameraEditCameraActivity.this, MainActivity.class);
                intent.putExtra("skip" , "skip");
                setResult(RESULT_OK,intent);
                finish();
            }
        });

        photoBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                sd = Environment.getExternalStorageDirectory().getAbsolutePath() + "/foureight";
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,Uri.parse("file://"+sd));
                File f = new File(sd); //새로고침할 사진경로
                Uri contentUri = Uri.fromFile(f);
                mediaScanIntent.setData(contentUri);
                sendBroadcast(mediaScanIntent);
                TedBottomPicker bottomSheetDialogFragment = new TedBottomPicker.Builder(CameraEditCameraActivity.this)
                        .setOnMultiImageSelectedListener(new TedBottomPicker.OnMultiImageSelectedListener() {
                            @Override
                            public void onImagesSelected(ArrayList<Uri> uriList) {
                                int videosize = 0;
                                for(int i = 0; i < uriList.size(); i++) {
                                    try {
                                        //File file = new File(sd);
                                        //file.mkdir();
                                        videoname = uriList.get(i).toString();

                                        String[] filename = videoname.split("//");
                                        String[] real = filename[1].split("/");
                                        int count = real.length;
                                        realname = real[count-1];//filename[1];
                                        path = filename[1];
                                        sd = path.replace(realname,"");

                                        Log.d(TAG, "sd : " + sd + " // path : "+path + "// realname : " + realname + "// videonam : " + videoname);

                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Log.d(TAG, "run upload.. ");
                                                    }
                                                });
                                                if(realname != null) {
                                                    url = "http://484848.co.kr/mobile/video_upload.php";
                                                    int result = uploadFile(path, sd, realname, url);
                                                    Log.d(TAG, "run: " + result);
                                                    if(result==200){
                                                        Intent intent = new Intent(CameraEditCameraActivity.this, MainActivity.class);
                                                        intent.putExtra("videoname", realname);
                                                        intent.putExtra("mb_id", mb_id);

                                                        //Toast.makeText(CameraEditActivity.this, "파일 업로드 완료", Toast.LENGTH_SHORT).show();
                                                        removePreferences();
                                                        setResult(RESULT_OK, intent);
                                                        finish();
                                                    }
                                                }
                                            }
                                        }).start();
                                    }/*catch (FileNotFoundException e){
                                        e.printStackTrace();
                                    }*/catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                //Toast.makeText(CameraEditCameraActivity.this, "파일 용량은 10MB 이하로 등록 바랍니다.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .showVideoMedia()
                        .setPeekHeight(1600)
                        .showTitle(false)
                        .showCameraTile(false)
                        .setPreviewMaxCount(100)
                        .setSelectMaxCount(1)
                        .setCompleteButtonText("등록")
                        .setEmptySelectionText("동영상을 선택해주세요.")
                        .create();

                bottomSheetDialogFragment.show(getSupportFragmentManager());
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                //Log.d(TAG, "filename : "+filename + " videoname : " +videoname);
                if(videoname != null ) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d(TAG, "run upload.. ");
                                }
                            });
                            /*if(filename != null) {
                                url = "http://484848.co.kr/mobile/photo_upload.php";
                                //String[] files = filename.split(",");
                                //String[] paths = mPath.split(",");
                                //for (int i = 0; i < files.length; i++) {
                                    //Log.d(TAG, "run: " + files[i] + " // path : " + paths[i]);
                                    uploadFile(mPath, sd, filename, url);
                                //}
                            }*/
                            if(videoname!=null) {
                                url = "http://484848.co.kr/mobile/video_upload.php";
                                String vpath = sd +"/"+ videoname;
                                Log.d(TAG, "video : " + videoname + "// vpath : " + vpath);
                                uploadFile(vpath, sd, videoname, url);
                            }
                        }
                    }).start();

                    Intent intent = new Intent(CameraEditCameraActivity.this, MainActivity.class);
                    intent.putExtra("videoname", videoname);
                    intent.putExtra("mb_id", mb_id);
                    /*intent.putExtra("videoname", videoname);
                    intent.putExtra("timer2", timer2);
                    intent.putExtra("mb_id",mb_id);
                    intent.putExtra("title",title);
                    intent.putExtra("type1",type1);
                    intent.putExtra("type2",type2);
                    intent.putExtra("cate1",cate1);
                    intent.putExtra("cate2",cate2);
                    intent.putExtra("wr_price",wr_price);
                    intent.putExtra("wr_price2",wr_price2);*/

                    Toast.makeText(CameraEditCameraActivity.this, "파일 업로드 완료", Toast.LENGTH_SHORT).show();
                    removePreferences();
                    //startActivity(intent);
                    setResult(RESULT_OK, intent);
                    finish();
                }else{
                    Toast.makeText(CameraEditCameraActivity.this,"영상을 등록/선택해 주세요",Toast.LENGTH_SHORT).show();
                }
            }
        });

        //비디오 버튼
        videoBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                isRecord();
            }
        });

        cam_toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(camera!=null){
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }

                mCameraFacing = (mCameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK) ? Camera.CameraInfo.CAMERA_FACING_FRONT: Camera.CameraInfo.CAMERA_FACING_BACK;
                camera = Camera.open(mCameraFacing);
                initCamera();
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
                    isRecord();
                    timer = 0;
                }
            }
        };
    }

    public void isRecord(){
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
            saveBtn.setEnabled(true);
            photoBtn.setEnabled(true);
        } else {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }
            timer2 = 0;
            Log.d(TAG, "onClick: 1");

            videoBtn.setBackgroundResource(R.drawable.video_btn_stop);
            cam_toggle.setVisibility(View.GONE);
            saveBtn.setEnabled(false);
            photoBtn.setEnabled(false);
            // BEGIN_INCLUDE(prepare_start_media_recorder)
            new MediaPrepareTask().execute(null, null, null);
            // END_INCLUDE(prepare_start_media_recorder)

        }
    }
/*
    public int getOrientationOfImage(String filepath) {
        ExifInterface exif = null;

        try {
            exif = new ExifInterface(filepath);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }

        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);

        if (orientation != -1) {
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
            }
        }

        return 0;
    }*/
/*
    public Bitmap getRotatedBitmap(Bitmap bitmap, int degrees) throws Exception {
        if(bitmap == null) return null;
        if (degrees == 0) return bitmap;

        Matrix m = new Matrix();
        m.setRotate(degrees, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }
*/
    /*Camera.AutoFocusCallback camAutoFocuse = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera cam) {
            //if(success && camCount < 5) {
            camera.takePicture(mySutterCallback, null, myPictureCallback_JPG);
            //}else{
            //    Toast.makeText(CameraEditActivity.this, "5장 모두 찰영하였습니다.",Toast.LENGTH_SHORT).show();
            //}
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
            file_name = System.currentTimeMillis() + "_" + mb_id + "_" + index + ".jpg";
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

            Log.d(TAG, "filename picture: " + file_name + "// path : " + path);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "run upload.. ");
                        }
                    });
                    if(file_name != null) {
                        url = "http://484848.co.kr/mobile/photo_upload.php";
                        int result = uploadFile(path, sd, file_name, url);
                        Log.d(TAG, "run: " + result);
                        if(result==200){
                            Intent intent = new Intent(CameraEditCameraActivity.this, MainActivity.class);
                            intent.putExtra("filename", file_name);
                            intent.putExtra("index", index);
                            intent.putExtra("mb_id", mb_id);

                            //Toast.makeText(CameraEditActivity.this, "파일 업로드 완료", Toast.LENGTH_SHORT).show();
                            removePreferences();
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    }
                }
            }).start();


        }
    };*/

    /*public byte[] bitmapToByteArray( Bitmap bitmap ) {
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
    }*/

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surfaceCreated: " + this.surfaceHolder.getSurface() + "// me surface : " + surfaceHolder.getSurface());
        mCameraFacing = (mCameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK) ? Camera.CameraInfo.CAMERA_FACING_FRONT: Camera.CameraInfo.CAMERA_FACING_BACK;
        camera = Camera.open(mCameraFacing);
        try {
            camera.setPreviewDisplay(surfaceHolder);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void initCamera(){
        Log.d(TAG, "initCamera: " + surfaceHolder + camera.getParameters().getPreviewSize().width);
        try{
            Display display = getWindowManager().getDefaultDisplay();

            if(mCameraFacing==0) {
                switch (display.getRotation()) {
                    case Surface.ROTATION_0:
                        angle = 90;
                        dgree = 90;
                        break;
                    case Surface.ROTATION_90:
                        angle = 0;
                        dgree = 0;
                        break;
                    case Surface.ROTATION_180:
                        angle = 270;
                        dgree = 270;
                        break;
                    case Surface.ROTATION_270:
                        angle = 180;
                        dgree = 180;
                        break;
                    default:
                        angle = 90;
                        dgree = 90;
                        break;
                }
            }
            if(mCameraFacing==1){
                switch (display.getRotation()) {
                    case Surface.ROTATION_0:
                        // 0
                        angle = 90;
                        dgree = 270;
                        break;
                    case Surface.ROTATION_90:
                        // 1
                        angle = 0;
                        dgree = 0;
                        break;
                    case Surface.ROTATION_180:
                        // 2
                        angle = 270;
                        dgree = 0;
                        break;
                    case Surface.ROTATION_270:
                        // 3
                        angle = 180;
                        dgree = 180;
                        break;
                    default:
                        angle = 0;
                        dgree = 270;
                        break;
                }
            }

            camParams = camera.getParameters();
            List<Camera.Size> mSupportedPreviewSizes = camParams.getSupportedPreviewSizes();
            List<Camera.Size> mSupportedVideoSizes = camParams.getSupportedVideoSizes();
            Camera.Size presize = camParams.getSupportedPreviewSizes().get(0);
            previewSize = getBestPreviewSize(presize.width,presize.height);
            optimalSize = CameraHelper.getOptimalVideoSize(mSupportedPreviewSizes,mSupportedVideoSizes, surfaceView.getWidth(),surfaceView.getHeight());
            Log.d(TAG, "initCamera: " + optimalSize.width + "//" + optimalSize.height + "//" + angle + "//" + display.getRotation());
            camParams.setPictureSize(optimalSize.width,optimalSize.height);
            camParams.setPreviewSize(optimalSize.width,optimalSize.height);
            //Log.d(TAG, "프리뷰 사이즈 :: " + previewSize.width+"/"+previewSize.height);
            camParams.setRotation(dgree);
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
                Log.d(TAG, "uploadFile: " + bytesAvailable);
                if(bytesAvailable > 20971520){
                    return bytesAvailable;
                }

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
                            Toast.makeText(CameraEditCameraActivity.this, "파일 업로드 완료.", Toast.LENGTH_SHORT).show();

                            /*Intent intent = new Intent(CameraEditCameraActivity.this, MainActivity.class);
                            intent.putExtra("videoname", videoname);
                            intent.putExtra("mb_id", mb_id);
                            //Toast.makeText(CameraEditCameraActivity.this, "파일 업로드 완료", Toast.LENGTH_SHORT).show();
                            removePreferences();
                            //startActivity(intent);
                            setResult(RESULT_OK, intent);
                            finish();*/
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
                        Toast.makeText(CameraEditCameraActivity.this, "MalformedURLException", Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {
                //dialog.dismiss();
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    public void run() {
                        //messageText.setText("Got Exception : see logcat ");
                        Toast.makeText(CameraEditCameraActivity.this, "Got Exception : see logcat ",Toast.LENGTH_SHORT).show();
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
        videoname = pref.getString("videoname",videoname);
        mPath = pref.getString("mPath", mPath);
        mb_id = pref.getString("mb_id", mb_id);
        //camCount = pref.getInt("camCount", camCount);
        Log.d(TAG, "getPreferences:" + videoname  + "// "+ pref);
    }

    // 값 저장하기
    private void savePreferences(){
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("videoname", videoname);
        editor.putString("mPath", mPath);
        editor.putString("mb_id", mb_id);
        //editor.putInt("camCount", camCount);
        editor.commit();
        Log.d(TAG, "savePreferences: " + videoname + "//" + pref);
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

        if(videoname!=null && videoname != ""){
            textView.setText(timer+"/20s");
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            // clear recorder configuration
            mMediaRecorder.reset();
            // release the recorder object
            mMediaRecorder.release();
            mMediaRecorder = null;
            // Lock camera for later use i.e taking it back from MediaRecorder.
            // MediaRecorder doesn't need it anymore and we will release it if the activity pauses.
            camera.lock();

            /*if(videoname!="") {
                url = "http://484848.co.kr/mobile/video_upload.php";
                String vpath = sd + "/" + videoname;
                Log.d(TAG, "video : " + videoname + "// vpath : " + vpath);
                uploadFile(vpath, sd, videoname, url);
            }*/
        }else{
            Log.d(TAG, "releaseMediaRecorder: " + mMediaRecorder);
            //mMediaRecorder = new MediaRecorder();
        }
    }

    private boolean prepareVideoRecorder(){
        Display display = this.getWindowManager().getDefaultDisplay();
        if(mCameraFacing==0) {
            switch (display.getRotation()) {
                case Surface.ROTATION_0:
                    angle = 90;
                    dgree = 90;
                    break;
                case Surface.ROTATION_90:
                    angle = 0;
                    dgree = 0;
                    break;
                case Surface.ROTATION_180:
                    angle = 270;
                    dgree = 270;
                    break;
                case Surface.ROTATION_270:
                    angle = 180;
                    dgree = 180;
                    break;
                default:
                    angle = 90;
                    dgree = 90;
                    break;
            }
        }
        if(mCameraFacing==1){
            switch (display.getRotation()) {
                case Surface.ROTATION_0:
                    // 0
                    angle = 90;
                    dgree = 270;
                    break;
                case Surface.ROTATION_90:
                    // 1
                    angle = 0;
                    dgree = 0;
                    break;
                case Surface.ROTATION_180:
                    // 2
                    angle = 270;
                    dgree = 90;
                    break;
                case Surface.ROTATION_270:
                    // 3
                    angle = 180;
                    dgree = 180;
                    break;
                default:
                    angle = 0;
                    dgree = 0;
                    break;
            }
        }
        Log.d(TAG, "prepareVideoRecorder: "+camera + "//"+ isRecording + "//" + dgree);
        // BEGIN_INCLUDE (configure_preview)
        //camera = CameraHelper.getDefaultCameraInstance();
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
        parameters.setRotation(dgree);
        parameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);
        camera.setParameters(parameters);
        camera.setDisplayOrientation(angle);
        try {
            // Requires API level 11+, For backward compatibility use {@link setPreviewDisplay}
            // with {@link SurfaceView}
            camera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            Log.e(TAG, "Surface texture is unavailable or unsuitable " + e.getMessage());
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

        mMediaRecorder.setOrientationHint(dgree);
        // Step 4: Set output file
        mOutputFile = CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_VIDEO);
        if (mOutputFile == null) {
            return false;
        }

        File file = new File(sd);
        file.mkdir();
        videoname = System.currentTimeMillis() + "_" + mb_id +".mp4";
        path = sd + "/" + videoname;
        //savePreferences();
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
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording

                mMediaRecorder.start();
                handler.sendEmptyMessage(0);

                isRecording = true;
            } else {
                // prepare didn't work, release the camera
                releaseMediaRecorder();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.d(TAG, result.toString());
            if (!result) {
                CameraEditCameraActivity.this.finish();
            }

            if(camera!=null) {

            }else{
                camera.startPreview();
            }
            // inform the user that recording has started
            //setCaptureButtonText("Stop");

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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //backkey Event
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("사진수정을 취소 하시겠습니까?")
                    .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    })
                    .setPositiveButton("종료", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(CameraEditCameraActivity.this,MainActivity.class);
                            intent.putExtra("skip" , "skip");
                            setResult(RESULT_OK,intent);
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