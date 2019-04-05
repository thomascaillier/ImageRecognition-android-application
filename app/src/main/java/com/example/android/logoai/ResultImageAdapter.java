package com.example.android.logoai;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ResultImageAdapter extends RecyclerView.Adapter<ResultImageAdapter.ResultImageViewHolder> {

    private List<ResultImageData> resultImageList;

    public void addItem(ResultImageData item){
        resultImageList.add(item);
        notifyDataSetChanged();
    }

    public void empty(){
        resultImageList.clear();
        notifyDataSetChanged();
    }

    public ResultImageAdapter(List<ResultImageData> resultImageList) {
        this.resultImageList = resultImageList;
    }

    public ResultImageAdapter(){
        this.resultImageList = new ArrayList<>();
    }

    @Override
    public int getItemCount() {
        return resultImageList.size();
    }

    @Override
    public void onBindViewHolder(ResultImageViewHolder contactViewHolder, int i) {
        ResultImageData rImage = resultImageList.get(i);
        contactViewHolder.vImage.setImageBitmap(rImage.image);
        contactViewHolder.vProbability.setText("probability : " + rImage.probability);
    }

    @Override
    public ResultImageViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.result_image_card, viewGroup, false);

        return new ResultImageViewHolder(itemView);
    }

    public class ResultImageViewHolder extends RecyclerView.ViewHolder {
        protected ImageView vImage;
        protected TextView vProbability;

        public ResultImageViewHolder(View v) {
            super(v);
            vImage = (ImageView) v.findViewById(R.id.imgImage);
            vProbability = (TextView) v.findViewById(R.id.txtProbability);
        }
    }
}