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

public class TextSizeAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final int layoutID;
    private final String[] names;
    private final int[] sizes;
    private int mColor;

    static class TextSizeViewHolder {
        TextView textView;
    }

    public TextSizeAdapter(Context context,
                    int itemLayoutId,
                    String[] spinnerItems,
                    int[] spinnerSizes ){

        inflater = LayoutInflater.from(context);
        layoutID = itemLayoutId;
        names = spinnerItems;
        sizes = spinnerSizes;
        Resources res = context.getResources();
    }

    @Override
    public int getCount() {
        return names.length;
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
            convertView = inflater.inflate(layoutID, null);
            holder = new TextSizeViewHolder();

            holder.textView = convertView.findViewById(R.id.select_text_size_view);
            convertView.setTag(holder);
        } else {
            holder = (TextSizeViewHolder) convertView.getTag();
        }

        holder.textView.setText(names[position]);
        holder.textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizes[position]);

        return convertView;
    }
}
