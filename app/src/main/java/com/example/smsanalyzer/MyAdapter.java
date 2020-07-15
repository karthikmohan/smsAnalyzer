package com.example.smsanalyzer;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// Adapter to connect row.xml with the recycler view
public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

    private Context context;
    List<Sms> smsList;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView smsType;
        public TextView smsAmt;
        public TextView smsDate;
        public TextView smsBal;
        public ImageView typeImage;

        public ViewHolder(View v) {
            super(v);

            typeImage = v.findViewById(R.id.typeImage);
            smsType = v.findViewById(R.id.smsType);
            smsAmt = v.findViewById(R.id.smsAmt);
            smsDate = v.findViewById(R.id.smsDate);
        }
    }

    public MyAdapter(List<Sms> smsList, Context context) {
        this.smsList = smsList;
        this.context = context;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String t = smsList.get(position).getMsgType();
        holder.smsType.setText(t);
        if (t.equals("Debit") || t.equals("Food") || t.equals("Transport")) {
            holder.smsAmt.setTextColor(Color.RED);
            holder.typeImage.setBackgroundResource(R.drawable.ic_action_arrow_top);

        } else {
            holder.smsAmt.setTextColor(Color.parseColor("#b79be344"));
            holder.typeImage.setBackgroundResource(R.drawable.ic_action_arrow_bottom);
        }
        holder.smsAmt.setText("₹ " + smsList.get(position).getMsgAmt());
        holder.smsDate.setText(smsList.get(position).getFormatDate());
//        if (Double.parseDouble(smsList.get(position).getMsgBal()) < 0.0)
//            holder.smsBal.setTextColor(Color.RED);
//        else
//            holder.smsBal.setTextColor(Color.parseColor("#b79be344"));
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override
    public int getItemCount() {
        return smsList.size();
    }

}
