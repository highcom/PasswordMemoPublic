package com.highcom.passwordmemo.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.ColorInt;

import com.highcom.passwordmemo.R;
import com.highcom.passwordmemo.ui.list.BackgroundColorAdapter;
import com.highcom.passwordmemo.ui.list.BackgroundColorItem;

import java.util.ArrayList;
import java.util.List;

public class BackgroundColorUtil {
    private int colors[];
    BackgroundColorListener backgroundColorListener;

    public interface BackgroundColorListener {
        void onSelectColorClicked(int color);
    }

    public BackgroundColorUtil(Context context, BackgroundColorListener listener) {
        colors = new int[8];
        colors[0] = context.getResources().getColor(R.color.white);
        colors[1] = context.getResources().getColor(R.color.lightgray);
        colors[2] = context.getResources().getColor(R.color.lightcyan);
        colors[3] = context.getResources().getColor(R.color.lavender);
        colors[4] = context.getResources().getColor(R.color.bisque);
        colors[5] = context.getResources().getColor(R.color.pink);
        colors[6] = context.getResources().getColor(R.color.palegoldenrod);
        colors[7] = context.getResources().getColor(R.color.palegreen);

        backgroundColorListener = listener;
    }

    public boolean isColorExists(@ColorInt int color) {
        for (int i = 0; i < 8; i++) {
            if (colors[i] == color) return true;
        }
        return false;
    }

    @SuppressLint("ResourceType")
    public AlertDialog createBackgroundColorDialog(Activity activity) {
        final AlertDialog alertDialog = new AlertDialog.Builder(activity)
            .setView(activity.getLayoutInflater().inflate(R.layout.alert_background_color, null))
            .create();
        alertDialog.show();
        ListView listView = alertDialog.findViewById(R.id.backgroundColorListView);

        List<BackgroundColorItem> colorItems = new ArrayList<>();
        colorItems.add(new BackgroundColorItem(activity.getString(R.string.color_white), colors[0]));
        colorItems.add(new BackgroundColorItem(activity.getString(R.string.color_lightgray), colors[1]));
        colorItems.add(new BackgroundColorItem(activity.getString(R.string.color_lightcyan), colors[2]));
        colorItems.add(new BackgroundColorItem(activity.getString(R.string.color_lavender), colors[3]));
        colorItems.add(new BackgroundColorItem(activity.getString(R.string.color_bisque), colors[4]));
        colorItems.add(new BackgroundColorItem(activity.getString(R.string.color_pink), colors[5]));
        colorItems.add(new BackgroundColorItem(activity.getString(R.string.color_palegoldenrod), colors[6]));
        colorItems.add(new BackgroundColorItem(activity.getString(R.string.color_palegreen), colors[7]));
        BackgroundColorAdapter adapter = new BackgroundColorAdapter(activity, R.layout.row_background_color, colorItems);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                backgroundColorListener.onSelectColorClicked(colors[i]);
                alertDialog.dismiss();
            }
        });

        return alertDialog;
    }
}
