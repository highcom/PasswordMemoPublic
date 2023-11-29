package com.highcom.passwordmemo.ui.list;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.highcom.passwordmemo.R;

import java.util.List;

public class SetTextSizeAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final List<String> names;
    private final int size;

    static class TextSizeViewHolder {
        TextView textView;
    }

    public SetTextSizeAdapter(Context context, List<String> names, int spinnerSize ){

        inflater = LayoutInflater.from(context);
        this.names = names;
        size = spinnerSize;
        Resources res = context.getResources();
    }

    @Override
    public int getCount() {
        return names.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextSizeViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.row_spinner_set_textsize, null);
            holder = new TextSizeViewHolder();

            holder.textView = convertView.findViewById(R.id.select_spinner_set_textsize_view);
            convertView.setTag(holder);
        } else {
            holder = (TextSizeViewHolder) convertView.getTag();
        }

        holder.textView.setText(names.get(position));
        holder.textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);

        return convertView;
    }
}
