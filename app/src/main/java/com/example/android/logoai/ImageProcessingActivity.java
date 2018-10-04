package com.example.android.logoai;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;

import java.io.IOException;

public class ImageProcessingActivity extends AppCompatActivity {

    private static String serverUrl = "http://www.google.com";

    private int processingStep;
    private static String[] steps = new String[]{
            "Envoi de l'image vers le serveur",
            "Détection des zones d'intérêt",
            "Reconnaissance d'informations sur la zone d'intérêt"
    };

    private VolleyError lastError = null;

    private RequestQueue requestQueue;

    private Bitmap image;
    private int maxWidth;
    private int maxHeight;
    private Bitmap.Config bitmapConfig;

    public ImageView imageView;
    public TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_processing);

        Intent intent = getIntent();
        if (intent.hasExtra(MainActivity.IMAGE_BITMAP)) {
            image = intent.getParcelableExtra(MainActivity.IMAGE_BITMAP);
        } else if (intent.hasExtra(MainActivity.IMAGE_PATH)) {
            Uri imageUri = Uri.parse(intent.getStringExtra(MainActivity.IMAGE_PATH));
            try {
                image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        } else {
            throw new IllegalArgumentException();
        }

        maxWidth = image.getWidth();
        maxHeight = image.getHeight();
        bitmapConfig = image.getConfig();

        imageView = findViewById(R.id.imageProcessing);
        textView = findViewById(R.id.consoleTextView);

        processingStep = 0;
        requestQueue = Volley.newRequestQueue(this);

        requestStep();

    }

    private void updateView() {
        imageView.setImageBitmap(image);
        textView.append(System.getProperty("line.separator"));
        if (lastError == null)
            textView.append(steps[processingStep]);
        else {
            textView.append("Erreur : " + lastError.getMessage());
        }
    }

    private void requestStep() {
        ImageRequest request = new ImageRequest(
                serverUrl,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap response) {
                        image = response;
                        processingStep++;
                        updateView();
                        if(processingStep < steps.length)
                            requestStep();
                    }
                },
                maxWidth,
                maxHeight,
                ImageView.ScaleType.CENTER,
                bitmapConfig,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        lastError = error;
                        updateView();
                    }
                });
        requestQueue.add(request);
    }
}
