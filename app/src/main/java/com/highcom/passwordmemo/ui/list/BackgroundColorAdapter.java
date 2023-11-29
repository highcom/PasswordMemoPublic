package com.highcom.passwordmemo.ui.list;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.highcom.passwordmemo.R;

import java.util.List;

public class BackgroundColorAdapter extends ArrayAdapter<BackgroundColorItem> {

    private int mResource;
    private List<BackgroundColorItem> mItems;
    private LayoutInflater mInflater;

    public BackgroundColorAdapter(@NonNull Context context, int resource, @NonNull List<BackgroundColorItem> objects) {
        super(context, resource, objects);

        mResource = resource;
        mItems = objects;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view;

        if (convertView != null) {
            view = convertView;
        }
        else {
            view = mInflater.inflate(mResource, null);
        }

        // リストビューに表示する要素を取得
        BackgroundColorItem item = mItems.get(position);

        // 背景色と名前を設定
        TextView title = view.findViewById(R.id.backgroundColorName);
        title.setText(item.getColorName());
        LinearLayout linearLayout = view.findViewById(R.id.backgroundColorRow);
        linearLayout.setBackgroundColor(item.getColorCode());

        return view;
    }
}
