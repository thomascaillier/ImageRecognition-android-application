package com.example.android.logoai;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class ImageProcessingActivity extends AppCompatActivity {

    private static String serverUrl = "http://192.168.1.37:8000/imagerecognition";

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
    HttpEntity httpEntity;

    public ImageView imageView;
    public TextView textView;
    private int imageServerId;

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
            if (lastError.networkResponse != null)
                addLine("Erreur : " + lastError.networkResponse.statusCode);
            else
                addLine("Erreur : " + lastError.getMessage());
        }
    }

    private void postImage() {

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        final byte[] bitmapData = stream.toByteArray();

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

        if (bitmapData != null) {
            builder.addBinaryBody(
                    "image",
                    bitmapData,
                    ContentType.create("image/jpeg"),
                    "image_file.jpeg");
            httpEntity = builder.build();

            MyRequest myRequest = new MyRequest(
                    Request.Method.POST,
                    serverUrl + "/engage",
                    new Response.Listener<NetworkResponse>() {
                        @Override
                        public void onResponse(NetworkResponse response) {
                            try {
                                String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                                JSONObject json = new JSONObject(jsonString);
                                Log.d("Response", jsonString);
                                if (json.getBoolean("Success")) {
                                    imageServerId = json.getInt("Key");
                                    processingStep++;
                                    updateView();
                                    if (processingStep < stepsLabels.length - 1)
                                        requestStep();
                                } else {
                                    lastError = new VolleyError(json.getString("Message"));
                                    updateView();
                                }
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    lastError = error;
                    updateView();
                }
            }) {
                @Override
                public String getBodyContentType() {
                    return httpEntity.getContentType().getValue();
                }

                @Override
                public byte[] getBody() {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    try {
                        httpEntity.writeTo(bos);
                    } catch (IOException e) {
                        VolleyLog.e("IOException writing to ByteArrayOutputStream");
                    }
                    return bos.toByteArray();
                }
            };
            requestQueue.add(myRequest);
        }
    }

    private void requestStep() {
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                serverUrl + "/step/" + imageServerId + "/" + Integer.toString(processingStep),
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.getBoolean("Success")) {

                                if (processingStep < stepsLabels.length - 1) {
                                    String b64EncodedBytes = response.getString("Data");
                                    ByteArrayInputStream bais = new ByteArrayInputStream(Base64.decode(b64EncodedBytes, Base64.DEFAULT));
                                    image = BitmapFactory.decodeStream(bais);
                                    requestStep();
                                } else {
                                    addLine((response.getJSONObject("Data")).toString());
                                }
                                processingStep++;
                                updateView();
                            } else {
                                lastError = new VolleyError(response.getString("Message"));
                                updateView();
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