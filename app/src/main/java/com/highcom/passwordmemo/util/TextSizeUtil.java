package com.highcom.passwordmemo.util;

import android.content.Context;
import android.view.View;
import android.webkit.WebSettings;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.highcom.passwordmemo.R;
import com.highcom.passwordmemo.ui.list.TextSizeAdapter;
import com.highcom.passwordmemo.util.login.LoginDataManager;

public class TextSizeUtil {
    public static final int TEXT_SIZE_SMALL = 12;
    public static final int TEXT_SIZE_MEDIUM = 15;
    public static final int TEXT_SIZE_LARGE = 18;
    public static final int TEXT_SIZE_EXTRA_LARGE = 21;


    private String[] textNames;
    private static final int[] textSizes = { TEXT_SIZE_SMALL, TEXT_SIZE_MEDIUM, TEXT_SIZE_LARGE, TEXT_SIZE_EXTRA_LARGE };

    public interface TextSizeListener {
        void onTextSizeSelected(float size);
    }

    private TextSizeAdapter mTextSizeAdapter;
    private TextSizeListener mTextSizeListener;

    public TextSizeUtil(Context context, TextSizeListener listener) {
        textNames = new String[4];
        textNames[0] = context.getString(R.string.size_small);
        textNames[1] = context.getString(R.string.size_medium);
        textNames[2] = context.getString(R.string.size_large);
        textNames[3] = context.getString(R.string.size_extra_large);
        mTextSizeAdapter = new TextSizeAdapter(context, R.layout.row_text_size, textNames, textSizes);
        mTextSizeListener = listener;
    }

    public void createTextSizeSpinner(Spinner spinner) {
        spinner.setAdapter(mTextSizeAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mTextSizeListener.onTextSizeSelected(textSizes[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public int getSpecifiedValuePosition(float size) {
        int position = 1; // デフォルト値である「中」の位置

        for (int i = 0; i < textSizes.length; i++) {
            if (textSizes[i] == size) {
                position = i;
                break;
            }
        }

        return position;
    }
}
