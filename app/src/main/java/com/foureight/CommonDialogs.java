package com.foureight;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.EditText;

/**
 * Created by Pc on 2016-03-30.
 */
public class CommonDialogs extends Dialog {
    private Context mContext;
    private ProgressDialog progressDialog;
    protected boolean destroyed = false;

    public CommonDialogs(Context context) {
        super(context);
        mContext = context;
    }

    public void showProgressDialog(final Activity activity, CharSequence message) {

        if (progressDialog == null) {
            progressDialog = new ProgressDialog(mContext);
            progressDialog.setIndeterminate(true);
        }

        progressDialog.setCancelable(true);
        progressDialog.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                destroyed = true;
                dialog.dismiss();
                activity.finish();
            }

        });

        progressDialog.setMessage(message);
        progressDialog.show();

    }

    public void dismissProgressDialog() {
        if (progressDialog != null && !destroyed) {
            progressDialog.dismiss();
        }
    }

    public void showAlertDialog(Context context, int stringId) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alertDialog.setMessage(stringId);
        alertDialog.show();
    }

    public void showAlertDialog(Context context, int stringId, DialogInterface.OnClickListener okListener,
                                DialogInterface.OnClickListener cancelListener) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setCancelable(false);
        if (okListener != null) {
            alertDialog.setPositiveButton(android.R.string.ok,okListener);
        }
        if (cancelListener != null) {
            alertDialog.setNegativeButton(android.R.string.cancel, cancelListener);
        }
        alertDialog.setMessage(stringId);
        alertDialog.show();
    }
    public void showAlertDialog(Context context, String message, DialogInterface.OnClickListener okListener,
                                DialogInterface.OnClickListener cancelListener) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setCancelable(false);
        if (okListener != null) {
            alertDialog.setPositiveButton(android.R.string.ok,okListener);
        }
        if (cancelListener != null) {
            alertDialog.setNegativeButton(android.R.string.cancel, cancelListener);
        }
        alertDialog.setMessage(message);
        alertDialog.show();
    }

    @Override
    public boolean isShowing() {
        return super.isShowing();
    }


    public AlertDialog showListDialog(String title, int stringArrayId, DialogInterface.OnClickListener listener ) {

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
//		List<String> list = Arrays.asList( mContext.getResources().getStringArray(stringArrayId) );
//		ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, list);
        if ( title != null ) {
            builder.setTitle(title);
        }
        builder.setItems(stringArrayId, listener);
        AlertDialog dialog = builder.create();
        dialog.show();
        return dialog;
    }
    public AlertDialog showListDialog(String title, String[] strings, DialogInterface.OnClickListener listener ) {

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
//		List<String> list = Arrays.asList( mContext.getResources().getStringArray(stringArrayId) );
//		ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, list);
        if ( title != null ) {
            builder.setTitle(title);
        }
        builder.setItems(strings, listener);
        AlertDialog dialog = builder.create();
        dialog.show();
        return dialog;
    }
    public AlertDialog showListDialog(int title, int stringArrayId, DialogInterface.OnClickListener listener ) {

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
//		List<String> list = Arrays.asList( mContext.getResources().getStringArray(stringArrayId) );
//		ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, list);
        if ( title > 0 ) {
            builder.setTitle(title);
        }
        builder.setItems(stringArrayId, listener);
        AlertDialog dialog = builder.create();
        dialog.show();
        return dialog;
    }


    public interface OnClickListener {
        void onClick(boolean isPositive, String text, AlertDialog dialog, View button, EditText editText);
    }
}
