package com.example.android.logoai;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import javax.xml.transform.Result;

public class ResultImageData {
    protected String id;
    protected Bitmap image;
    protected float probability;
    protected static final String ID_PREFIX = "Id_";
    protected static final String IMAGE_PREFIX = "image_";
    protected static final String PROBABILITY_PREFIX = "probability_";

    public ResultImageData(JSONObject json) throws JSONException {
        this.id = json.getString(ID_PREFIX);
        this.probability = (float) json.getDouble(PROBABILITY_PREFIX);
        byte[] decodedString = Base64.decode(
                json.getString(IMAGE_PREFIX),
                Base64.DEFAULT);
        this.image = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }

    public ResultImageData(String id, Bitmap image, float probability){
        this.id = id;
        this.image = image;
        this.probability = probability;
    }

}
