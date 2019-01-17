package com.foureight;


import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import gun0912.tedbottompicker.TedBottomPicker;

public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback{
    final static String TAG = "CameraActivity";
    Camera camera;
    SurfaceHolder surfaceHolder;
    SurfaceView surfaceView;
    boolean previewImg = false;
    boolean isRecording = false;
    boolean videoFlag = true;
    boolean isPicture = false;
    int mCameraFacing = 0;

    int serverResponseCode = 0;
    int angle,dgree,height;
    public Button camBtn,saveBtn,videoBtn,photoBtn,skipBtn,cam_toggle;
    private int camCount = 0,timer = 0, timer2 = 0;
    int viewWidth,viewHeight;
    TextView textView;
    String mb_id,title,cate1,cate2,url,path,sd,file_name,filename,mPath,imgPath,imgName,type1,type2,videoname,wr_price,wr_price2,pd_price_type;
    Uri selPhotoUri;
    byte[] bytes;
    Handler handler;

    private TextureView mPreview;

    private MediaRecorder mMediaRecorder;
    private File mOutputFile;

    Camera.Parameters camParams;

    Camera.Size previewSize;
    Camera.Size optimalSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_camera);
        }else{
            setContentView(R.layout.activity_camera_land);
        }
        //sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory().getAbsolutePath())));

        sd = Environment.getExternalStorageDirectory().getAbsolutePath()+"/foureight";
        File dir = new File(sd);

        String[] children = dir.list();
        Log.d(TAG, "onCreate: "+children.length);
        if (children != null) {
            for (int i=0; i<children.length; i++) {
                String filename = children[i];
                File f = new File(sd + "/" +filename);

                if (f.exists()) {
                    f.delete();
                }
            }
        }
        /*try{
            File file = new File(sd);
            File[] flist = file.listFiles();
            Log.d(TAG, "delete: " + flist.length);
            //Toast.makeText(getApplicationContext(), "imgcnt = " + flist.length, Toast.LENGTH_SHORT).show();
            String[] ext;
            resolver = getContentResolver();
            for(int i = 0 ; i < flist.length ; i++)
            {
                String fname = flist[i].getName();
                ext = fname.split("\\.");
                Uri uri = null;

                if (ext[1].toLowerCase().equals("jpg") || ext[1].toLowerCase().equals("png") || ext[1].toLowerCase().equals("gif")) {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else {
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                }
                String selection = MediaStore.Images.Media.DATA + " = ?";
                String[] selectionArgs = {sd}; // 실제 파일의 경로
                int count = resolver.delete(uri, selection, selectionArgs);
                Log.d(TAG, "onCreate ContentResolver: " + count);
                flist[i].delete();
            }

            for(File childFile : flist) {
                childFile.delete();    //하위 파일
            }

        }catch(Exception e){
            e.printStackTrace();
        }*/
        getWindow().setFormat(PixelFormat.UNKNOWN);
        surfaceView = findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(CameraActivity.this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mPreview =  findViewById(R.id.surface_view);

        textView = findViewById(R.id.camCount);
        camBtn = findViewById(R.id.camBtn);
        saveBtn = findViewById(R.id.saveBtn);
        videoBtn = findViewById(R.id.videoBtn);
        photoBtn = findViewById(R.id.photoBtn);
        skipBtn = findViewById(R.id.skipBtn);
        cam_toggle = findViewById(R.id.cam_toggle);

        /*cam_toggle = (Button)findViewById(R.id.cam_toggle);*/
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
        if(getIntent().getStringExtra("type1") != null) {
            type1 = getIntent().getStringExtra("type1");
        }
        if(getIntent().getStringExtra("type2") != null) {
            type2 = getIntent().getStringExtra("type2");
        }
        if(getIntent().getStringExtra("wr_price") != null) {
            wr_price = getIntent().getStringExtra("wr_price");
        }
        if(getIntent().getStringExtra("wr_price2") != null) {
            wr_price2 = getIntent().getStringExtra("wr_price2");
        }
        if(getIntent().getStringExtra("pd_price_type") != null) {
            pd_price_type = getIntent().getStringExtra("pd_price_type");
        }

        if(type1.equals("2") || type1 == "2"){
            Log.d(TAG, "Skip A // " + type1);
            skipBtn.setVisibility(View.VISIBLE);
        }else{
            Log.d(TAG, "Skip B // " + type1 );
            skipBtn.setVisibility(View.GONE);
        }

        firstAction();

    }

    private void firstAction(){
        (new AsyncTask <CameraActivity, Void, CameraActivity>(){
            @Override
            protected void onPostExecute(CameraActivity cameraActivity) {
                //super.onPostExecute(cameraActivity);

                Log.d(TAG, "onCreate: 시작");

                url = "http://mave01.cafe24.com/mobile/photo_upload.php";
                sd = Environment.getExternalStorageDirectory().getAbsolutePath()+"/foureight";

                camBtn.setOnClickListener(mClickListner);
                skipBtn.setOnClickListener(mClickListner);
                photoBtn.setOnClickListener(mClickListner);
                saveBtn.setOnClickListener(mClickListner);
                videoBtn.setOnClickListener(mClickListner);
                cam_toggle.setOnClickListener(mClickListner);

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

            @Override
            protected CameraActivity doInBackground(CameraActivity... cameraActivities) {
                return cameraActivities[0];
            }
        }).execute(this);
    }

    Button.OnClickListener mClickListner = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.videoBtn :
                    isRecord();
                    break;
                case R.id.camBtn :
                    isCapture();
                    break;
                case R.id.skipBtn :
                    isSkip();
                    break;
                case R.id.saveBtn :
                    isSave();
                    break;
                case R.id.photoBtn :
                    isPhoto();
                    break;
                case R.id.cam_toggle :
                    cameraSwith();
                    break;
            }
        }
    };

    public void isPhoto(){
        sd = Environment.getExternalStorageDirectory().getAbsolutePath() + "/foureight";

        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(sd); //새로고침할 사진경로
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
        TedBottomPicker bottomSheetDialogFragment = new TedBottomPicker.Builder(CameraActivity.this)
            .setOnMultiImageSelectedListener(new TedBottomPicker.OnMultiImageSelectedListener() {
                @Override
                public void onImagesSelected(ArrayList<Uri> uriList) {

                    int thisCount = camCount;
                    thisCount = thisCount + uriList.size();
                    if(thisCount > 5 ){
                        Toast.makeText(CameraActivity.this, "사진은 5개까지 선택가능합니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    for(int i = 0; i < uriList.size(); i++) {

                        try {

                            String file_Path = uriList.get(i).toString();
                            String new_file = new String(file_Path.getBytes("utf-8"),"utf-8");
                            Log.d(TAG, "onImagesSelected: " + file_Path);
                            String[] realPath = file_Path.split("//");
                            String filePath = realPath[1];
                            selPhotoUri = uriList.get(i);
                            Log.d(TAG, "onImagesSelected: " + filePath);

                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inSampleSize = 2;

                            //Bitmap image = BitmapFactory.decodeFile(file_Path,options);
                            Bitmap image = BitmapFactory.decodeStream(getContentResolver().openInputStream(uriList.get(i)));

                            ExifInterface exif = new ExifInterface(filePath);
                            Log.d(TAG, "onImagesSelected: exif " + exif);
                            image = ExifUtils.rotateBitmap(filePath,image);

                            Log.d(TAG, "onImagesSelected: exif " + image);

                            if(image!=null) {
                                //가로 세로 구분
                                int ratio = image.getWidth() / image.getHeight();

                                Log.d(TAG, "onImagesSelected: " +ratio);

                                Bitmap resizeImg = Bitmap.createScaledBitmap(image, image.getWidth(), image.getHeight(), true);
                                bytes = bitmapToByteArray(resizeImg);
                                url = "http://mave01.cafe24.com/mobile/photo_upload.php";
                                File file = new File(sd);
                                file.mkdir();
                                file_name = System.currentTimeMillis() + "_" + mb_id + "_" + camCount + ".jpg";
                                path = sd + "/" + file_name;

                                file = new File(path);

                                FileOutputStream fos = new FileOutputStream(file);
                                fos.write(bytes);
                                fos.flush();
                                fos.close();

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
                                textView.setText(camCount + "/5");
                            }else{

                                continue;
                            }
                        }catch (FileNotFoundException e){
                            e.printStackTrace();
                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            })
            .setPeekHeight(1600)
            .showTitle(false)
            .showCameraTile(false)
            .setPreviewMaxCount(100)
            .setSelectMaxCount(5)
            .setCompleteButtonText("등록")
            .setEmptySelectionText("사진을 선택해주세요.")
            .setImageProvider(null)
            .create();

        bottomSheetDialogFragment.show(getSupportFragmentManager());
    }
    private static int getExifOrientation(String src) throws IOException {
        int orientation = 1;
        try {
            /** * if your are targeting only api level >= 5 ExifInterface exif = * new ExifInterface(src); orientation = * exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1); */
            if (Build.VERSION.SDK_INT >= 5) {
                Class<?> exifClass = Class .forName("android.media.ExifInterface");
                Constructor<?> exifConstructor = exifClass .getConstructor(new Class[]{String.class});
                Object exifInstance = exifConstructor .newInstance(new Object[]{src});
                Method getAttributeInt = exifClass.getMethod("getAttributeInt", new Class[]{String.class, int.class});
                Field tagOrientationField = exifClass .getField("TAG_ORIENTATION");

                String tagOrientation = (String) tagOrientationField.get(null);

                orientation = (Integer) getAttributeInt.invoke(exifInstance, new Object[]{tagOrientation, 1});
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return orientation;
    }

    public void isCapture(){
        if(isPicture==false) {
            if(camCount < 5) {
                camera.autoFocus(camAutoFocuse);
            }else{
                Toast.makeText(CameraActivity.this, "5장 모두 찰영하였습니다.",Toast.LENGTH_SHORT).show();
            }
            isPicture = true;
        }
    }

    public void isRecord(){
        if (isRecording) {
            videoBtn.setBackgroundResource(R.mipmap.video_btn);
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
            cam_toggle.setVisibility(View.VISIBLE);
            camBtn.setEnabled(true);
            saveBtn.setEnabled(true);
            photoBtn.setEnabled(true);
        } else {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }
            timer2 = 0;
            videoBtn.setBackgroundResource(R.mipmap.video_btn_stop);
            cam_toggle.setVisibility(View.GONE);
            camBtn.setEnabled(false);
            photoBtn.setEnabled(false);
            saveBtn.setEnabled(false);
            // BEGIN_INCLUDE(prepare_start_media_recorder)
            new MediaPrepareTask().execute(null, null, null);
            // END_INCLUDE(prepare_start_media_recorder)

        }
    }

    public void isSkip(){
        Intent intent = new Intent(CameraActivity.this, MainActivity.class);
        intent.putExtra("mb_id",mb_id);
        intent.putExtra("title",title);
        intent.putExtra("type1",type1);
        intent.putExtra("type2",type2);
        intent.putExtra("cate1",cate1);
        intent.putExtra("cate2",cate2);
        intent.putExtra("wr_price",wr_price);
        intent.putExtra("wr_price2",wr_price2);
        intent.putExtra("pd_price_type",pd_price_type);
        intent.putExtra("skip" , "skip");
        startActivity(intent);
        finish();
    }

    public void isSave(){
        if(isRecording){
            isRecord();
        }
        if (filename != null || videoname != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "run upload.. ");
                        }
                    });
                    if (filename != null) {
                        url = "http://mave01.cafe24.com/mobile/photo_upload.php";
                        String[] files = filename.split(",");
                        String[] paths = mPath.split(",");
                        for (int i = 0; i < files.length; i++) {
                            Log.d(TAG, "run: " + files[i] + " // path : " + paths[i]);
                            uploadFile(paths[i], sd, files[i], url);
                        }
                    }
                    if (videoname != null) {
                        url = "http://mave01.cafe24.com/mobile/video_upload.php";
                        String vpath = sd + "/" + videoname;
                        Log.d(TAG, "video : " + videoname + "// vpath : " + vpath);
                        uploadFile(vpath, sd, videoname, url);
                    }
                }
            }).start();

            Intent intent = new Intent(CameraActivity.this, MainActivity.class);
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
            Toast.makeText(CameraActivity.this, "파일 업로드 완료", Toast.LENGTH_SHORT).show();

            //removePreferences();
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(CameraActivity.this, "사진 또는 영상을 등록/선택해 주세요", Toast.LENGTH_SHORT).show();
        }
    }

    public void cameraSwith(){
        if(camera!=null){
            camera.stopPreview();
            camera.release();
            camera = null;
        }

        mCameraFacing = (mCameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK) ? Camera.CameraInfo.CAMERA_FACING_FRONT: Camera.CameraInfo.CAMERA_FACING_BACK;
        camera = Camera.open(mCameraFacing);
        initCamera();
    }

    Camera.AutoFocusCallback camAutoFocuse = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera cam) {
            Log.d(TAG, "onAutoFocus: " + success);
            if(success) {
                camera.takePicture(mySutterCallback, null, myPictureCallback_JPG);
            }else{
                if(mCameraFacing==1){
                    camera.takePicture(mySutterCallback, null, myPictureCallback_JPG);
                }else {
                    Toast.makeText(CameraActivity.this, "포커스를 잡지 못했습니다.", Toast.LENGTH_SHORT).show();
                    isPicture = false;
                }
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

            /*BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;
            Bitmap image = BitmapFactory.decodeFile(filePath,options);
            image = ExifUtils.rotateBitmap(filePath,image);
            */

            Log.d(TAG, "onPictureTaken: " + data.toString());
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
            isPicture = false;
            camera.startPreview();
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Log.d(TAG, "onConfigurationChanged: "+mb_id+"// mCameraFacing : " + mCameraFacing);
        if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            setContentView(R.layout.activity_camera);
        }else if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
            setContentView(R.layout.activity_camera_land);
        }

        getWindow().setFormat(PixelFormat.UNKNOWN);
        surfaceView = findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(CameraActivity.this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mPreview =  findViewById(R.id.surface_view);

        textView = findViewById(R.id.camCount);
        camBtn = findViewById(R.id.camBtn);
        saveBtn = findViewById(R.id.saveBtn);
        videoBtn = findViewById(R.id.videoBtn);
        photoBtn = findViewById(R.id.photoBtn);
        skipBtn = findViewById(R.id.skipBtn);
        cam_toggle = findViewById(R.id.cam_toggle);

        /*cam_toggle = (Button)findViewById(R.id.cam_toggle);*/
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
        if(getIntent().getStringExtra("type1") != null) {
            type1 = getIntent().getStringExtra("type1");
        }
        if(getIntent().getStringExtra("type2") != null) {
            type2 = getIntent().getStringExtra("type2");
        }
        if(getIntent().getStringExtra("wr_price") != null) {
            wr_price = getIntent().getStringExtra("wr_price");
        }
        if(getIntent().getStringExtra("wr_price2") != null) {
            wr_price2 = getIntent().getStringExtra("wr_price2");
        }
        if(getIntent().getStringExtra("pd_price_type") != null) {
            pd_price_type = getIntent().getStringExtra("pd_price_type");
        }

        if(type1.equals("2") || type1 == "2"){
            Log.d(TAG, "Skip A // " + type1);
            skipBtn.setVisibility(View.VISIBLE);
        }else{
            Log.d(TAG, "Skip B // " + type1 );
            skipBtn.setVisibility(View.GONE);
        }
        if(camCount>0){
            textView.setText(camCount+"/5");
        }

        firstAction();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surfaceCreated: " + this.surfaceHolder.getSurface() + "// me surface : " + surfaceHolder.getSurface() + "//"+ camCount);
        if(mCameraFacing == 0) {
            mCameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        camera = Camera.open(mCameraFacing);
        initCamera();
        try {
            camera.setPreviewDisplay(surfaceHolder);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void initCamera(){
        try{
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
            Log.d(TAG, "initCamera: " + optimalSize.width + "//" + optimalSize.height + "//" + angle);
            camParams.setPictureSize(previewSize.width,previewSize.height);
            camParams.setPreviewSize(previewSize.width,previewSize.height);
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
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Log.d(TAG, "surfaceChanged: ");
        if(surfaceHolder.getSurface()== null){
            Log.d(TAG, "surfaceChanged: not surface");
            return;
        }
        if(previewImg){
            camera.stopPreview();
            previewImg = false;
        }
        if(camera!=null){
            initCamera();
        }else{

        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        try {
            camera.stopPreview();
            camera.release();
            camera = null;
            previewImg = false;
        }catch (Exception e){
            e.printStackTrace();
        }
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
        Log.d(TAG, "onPause: ");
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: ");
        if(filename!=null && filename != ""){
            String[] files = filename.split(",");
            int filenum = files.length;
            textView.setText(filenum+"/5");
            //camCount=filenum;
        }
        super.onResume();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        try {
        camera.stopPreview();
        camera.release();
        camera = null;

        }catch (Exception e){
            e.printStackTrace();
        }
        previewImg = false;
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
        Log.d(TAG, "prepareVideoRecorder: "+angle + "//"+ mCameraFacing + "//" + dgree + "//" + display.getRotation());
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
        parameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);
        parameters.setRotation(dgree);
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
                CameraActivity.this.finish();
            }

            if(camera!=null) {

            }else{
                camera.startPreview();
            }
            // inform the user that recording has started
            //setCaptureButtonText("Stop");

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
                            //removePreferences();
                            textView.setText("0/5");
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