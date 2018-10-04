package com.example.android.logoai;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ImageProcessingActivity extends AppCompatActivity {

    private static String serverUrl = "http://192.168.1.37:8080/imagerecognition";

    private int processingStep;
    private static String[] stepsLabels = new String[]{
            "Envoi de l'image vers le serveur",
            "Détection des zones d'intérêt",
            "Reconnaissance d'informations sur la zone d'intérêt",
            "Information reçue"
    };

    private VolleyError lastError = null;

    private RequestQueue requestQueue;

    private Bitmap image;

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

        imageView = findViewById(R.id.imageProcessing);
        textView = findViewById(R.id.consoleTextView);

        processingStep = 0;
        requestQueue = Volley.newRequestQueue(this);

        updateView();
        postImage();

    }

    private void addLine(String message) {
        textView.append(System.getProperty("line.separator"));
        textView.append(message);

    }

    private void updateView() {
        imageView.setImageBitmap(image);
        if (lastError == null)
            addLine(stepsLabels[processingStep]);
        else {
            addLine("Erreur : " + lastError.getMessage());
        }
    }

    public String getEncodedImage() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.URL_SAFE);
    }

    public void setDecodedImage(String data) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String base64Encoded = Base64.encodeToString(imageBytes, Base64.URL_SAFE);
    }

    private void postImage() {
        StringRequest postRequest = new StringRequest(
                Request.Method.POST,
                serverUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        processingStep++;
                        updateView();
                        if (processingStep < stepsLabels.length)
                            requestStep();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        lastError = error;
                        updateView();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("image", getEncodedImage());
                System.out.println(getEncodedImage());
                return params;
            }
        };
        requestQueue.add(postRequest);
    }

    private void requestStep() {
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                serverUrl + "?step=" + Integer.toString(processingStep),
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (Boolean.valueOf((String) response.get("Success"))) {
                                setDecodedImage((String) response.get("Image"));
                            }
                            addLine((String) response.get("Message"));
                            processingStep++;
                            updateView();
                            if (processingStep < stepsLabels.length - 1)
                                requestStep();
                            else {
                                addLine((String) response.get("Url"));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                },
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
