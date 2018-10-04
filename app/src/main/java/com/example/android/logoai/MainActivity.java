package com.example.android.logoai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_CAMERA_PERMISSION = 2;
    static final int REQUEST_GALLERY_PERMISSION = 3;
    static final int REQUEST_PICK_FROM_GALLERY = 4;

    static final String IMAGE_BITMAP = "com.example.android.logoai.IMAGE_BITMAP";
    static final String IMAGE_PATH = "com.example.android.logoai.IMAGE_PATH";

    public void getPicture(View view) {
        switch (view.getId()) {
            case R.id.imageButtonCamera:
                takePhoto(false);
                break;
            case R.id.imageButtonGallery:
                pickFromGallery(false);
                break;
        }
    }

    private void pickFromGallery(boolean granted) {
        if ( ! granted & ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},REQUEST_GALLERY_PERMISSION);
            }
        } else {
            Intent takePictureIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                MainActivity.this.startActivityForResult(takePictureIntent, REQUEST_PICK_FROM_GALLERY);
            }
        }
    }

    private void takePhoto(boolean granted) {
        if ( ! granted & ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},REQUEST_CAMERA_PERMISSION);
            }
        } else {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                MainActivity.this.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            Intent intent = new Intent(this, ImageProcessingActivity.class);
            intent.putExtra(IMAGE_BITMAP, imageBitmap);
            startActivity(intent);
        }
        if (requestCode == REQUEST_PICK_FROM_GALLERY && resultCode == RESULT_OK) {
                Uri imagePath = data.getData();
                Intent intent = new Intent(this, ImageProcessingActivity.class);
                intent.putExtra(IMAGE_PATH, imagePath.toString());
                startActivity(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePhoto(true);
                } else {
                    Toast.makeText(MainActivity.this, "Impossible d'accéder à votre caméra", Toast.LENGTH_LONG).show();
                }
            }
            case REQUEST_GALLERY_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickFromGallery(true);
                } else {
                    Toast.makeText(MainActivity.this, "Impossible d'accéder à votre galerie d'images", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
