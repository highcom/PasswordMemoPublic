package com.highcom.passwordmemo.util.file;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.mozilla.universalchardet.UniversalDetector;

import java.io.InputStream;

public class FileCharDetector {

    private Context mContext;
    private Uri mUri;

    public FileCharDetector(Context context, Uri uri) {
        this.mContext = context;
        this.mUri = uri;
    }

    public String detect() throws java.io.IOException {
        byte[] buf = new byte[4096];
        InputStream inputStream = mContext.getContentResolver().openInputStream(mUri);

        // 文字コード判定ライブラリの実装
        UniversalDetector detector = new UniversalDetector(null);

        // 判定開始
        int nread;
        while ((nread = inputStream.read(buf)) > 0 && !detector.isDone()) {
            detector.handleData(buf, 0, nread);
        }
        // 判定終了
        detector.dataEnd();

        // 文字コード判定
        String encType = detector.getDetectedCharset();
        if (encType != null) {
            Log.d("DETECTOR", "文字コード = " + encType);
        } else {
            Log.d("DETECTOR", "文字コードを判定できませんでした");
        }

        // 判定の初期化
        detector.reset();

        return encType;
    }
}
