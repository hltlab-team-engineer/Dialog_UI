package xyz.hiroshifuu.speechapp.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;

import xyz.hiroshifuu.speechapp.R;

public class MainActivity extends AppCompatActivity implements QRCodeReaderView.OnQRCodeReadListener {

    private QRCodeReaderView qrCodeReaderView;

    private View mLlFlashLight;

    private static final long VIBRATE_DURATION = 1000L;


    private static final int MY_PERMISSION_REQUEST_CAMERA = 0;
    private String preResult = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.active_qr_scanner);
        Log.d("create main layout","create main layout");
        checkPermission();
    }

    private void checkPermission(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            // create qr code reader layout
            initQRCodeReaderView();
        } else {
            requestCameraPermission();
        }
    }
    private void initQRCodeReaderView() {
        qrCodeReaderView = (QRCodeReaderView) findViewById(R.id.qr_code_view_finder);
        qrCodeReaderView.setAutofocusInterval(VIBRATE_DURATION);
        qrCodeReaderView.setOnQRCodeReadListener(this);
        qrCodeReaderView.setBackCamera();
        qrCodeReaderView.startCamera();
        qrCodeReaderView.setVisibility(View.VISIBLE);

        mLlFlashLight = findViewById(R.id.qr_code_ll_flash_light);
        mLlFlashLight.setVisibility(View.VISIBLE);
    }

    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.CAMERA
            }, MY_PERMISSION_REQUEST_CAMERA);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA
            }, MY_PERMISSION_REQUEST_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != MY_PERMISSION_REQUEST_CAMERA) {
            return;
        }

        if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initQRCodeReaderView();
        } else {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (qrCodeReaderView != null) {
            qrCodeReaderView.startCamera();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();

        if (qrCodeReaderView != null) {
            qrCodeReaderView.stopCamera();
        }
    }

    @Override
    public void onQRCodeRead(String text, PointF[] points) {
        if (preResult.compareTo(text) == 0)
            return;
        Log.d("onQRCodeRead ", text);
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        preResult = text;
        Intent intent = new Intent(MainActivity.this, SpeechActivity.class);
        Bundle b = new Bundle();
        b.putString("bus", text); //Your id
        intent.putExtras(b); //Put your id to your next Intent
        startActivity(intent);
    }
}

