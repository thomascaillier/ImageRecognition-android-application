package com.example.android.logoai;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.method.ScrollingMovementMethod;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class ImageProcessingActivity extends AppCompatActivity {

    private static String serverUrl = "http://192.168.1.37:8000/img_searches";

    private VolleyError lastError = null;

    private RequestQueue requestQueue;

    private Bitmap image;
    public ResultImageAdapter getAdapter(){
        return ((ResultImageAdapter)recyclerList.getAdapter());
    }
    HttpEntity httpEntity;

    public ImageView imageView;
    public TextView textView;
    public RecyclerView recyclerList;
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
        if (BuildConfig.DEBUG) {
            textView.setMovementMethod(new ScrollingMovementMethod());
        } else {
            textView.setHeight(0);
        }

        requestQueue = Volley.newRequestQueue(this);

        recyclerList = (RecyclerView) findViewById(R.id.cardList);
        recyclerList.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerList.setLayoutManager(llm);
        recyclerList.setAdapter(new ResultImageAdapter());

        updateView();
        postImage();

    }

    private void addLine(String message) {
        textView.append(System.getProperty("line.separator"));
        textView.append(message);

    }

    private void updateView(String message) {
        imageView.setImageBitmap(image);
        if (message != null) {
            //addLine(message);
        } else if (lastError != null){
            if (lastError.networkResponse != null)
                addLine("Erreur : " + lastError.networkResponse.statusCode);
            else
                addLine("Erreur : " + lastError.getMessage());
        }
    }
    private void updateView(){
        updateView(null);
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
                    serverUrl,
                    new Response.Listener<NetworkResponse>() {
                        @Override
                        public void onResponse(NetworkResponse response) {
                            try {
                                String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                                JSONObject json = new JSONObject(jsonString);
                                Log.d("Response", jsonString);
                                if (json.getBoolean("Success")) {
                                    imageServerId = json.getInt("Key");
                                    updateView("Image reçue par le serveur");
                                    getData();
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
            updateView("Envoi de l'image vers le serveur...");
            requestQueue.add(myRequest);
        }
    }

    private void getData() {
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                String.format("%s/%s", serverUrl, imageServerId),
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.getBoolean("Success")) {
                                updateView("Données reçues avec succès");
                                addLine((response.getJSONObject("Data")).toString());
                                JSONArray array = response.getJSONObject("Data").getJSONArray("ResultImages");
                                for(int i = 0; i < array.length(); i++){
                                    JSONObject jsonResult = array.getJSONObject(i);
                                    ResultImageData result = new ResultImageData(jsonResult);
                                    getAdapter().addItem(result);
                                }
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
        updateView("Récupération des données du serveur...");
        requestQueue.add(request);
    }
}