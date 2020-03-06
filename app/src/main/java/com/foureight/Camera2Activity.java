package com.foureight;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

public class Camera2Activity extends AppCompatActivity {
    private final static String TAG = "CAMERA2ACTIVITY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);
        Log.d(TAG, "onCreate: " + savedInstanceState);

        if (null == savedInstanceState) {
            Log.d(TAG, "onCreate: 있네");
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, Camera2BasicFragment.newInstance())
                    .commit();
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
                            Intent intent = new Intent(Camera2Activity.this,MainActivity.class);
                            startActivity(intent);
                            //removePreferences();
                            //textView.setText("0/5");
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
