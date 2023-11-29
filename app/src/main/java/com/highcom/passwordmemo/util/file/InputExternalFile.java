package com.highcom.passwordmemo.util.file;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.core.os.HandlerCompat;

import com.highcom.passwordmemo.R;
import com.highcom.passwordmemo.database.ListDataManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InputExternalFile {
    private Context context;
    private Activity activity;
    private List<Map<String, String>> dataList;
    private List<Map<String, String>> groupList;
    private final static int HEADER_RECORD = 1;
    private final static int MAX_RECORD = 10000;
    private Integer id = 0;
    private Uri uri;
    private AlertDialog progressAlertDialog;
    private ProgressBar progressBar;
    private InputExternalFileListener listener;

    public interface InputExternalFileListener {
        void importComplete();
    }

    private class BackgroundTask implements Runnable {
        private final android.os.Handler _handler;

        public BackgroundTask(android.os.Handler handler) {
            _handler = handler;
        }

        @WorkerThread
        @Override
        public void run() {
            // 既存のデータは全て削除する
            ListDataManager.getInstance(context).deleteAllData();

            int countUnit = dataList.size() / 20;
            int progressCount = 1;
            // 結果が全て取り出せたらデータを登録していく
            for (Map<String, String> data : dataList) {
                ListDataManager.getInstance(context).setData(false, data);
                if (countUnit > 0) {
                    // 5パーセントずつ表示を更新する
                    if (progressCount % countUnit == 0) {
                        progressBar.setProgress((progressCount / countUnit) * 5);
                    }
                    progressCount++;
                }
            }
            // 最後にグループデータを登録する
            for (Map<String, String> data : groupList) {
                if (data.get("group_id").equals("1")) continue;
                ListDataManager.getInstance(context).setGroupData(false, data);
            }
            progressBar.setProgress(100);
            PostExecutor postExecutor = new PostExecutor();
            _handler.post(postExecutor);
        }
    }

    private class PostExecutor implements Runnable {
        @UiThread
        @Override
        public void run() {
            progressAlertDialog.dismiss();
            listener.importComplete();
            new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.input_csv))
                    .setMessage(context.getString(R.string.csv_input_complete_message) + System.getProperty("line.separator") + getFileNameByUri(context, uri))
                    .setPositiveButton(R.string.ok, null)
                    .show();
        }
    }


    public InputExternalFile(Activity activity, InputExternalFileListener listener) {
        this.activity = activity;
        this.context = activity;
        this.listener = listener;
    }

    public void inputSelectFolder(final Uri uri) {
        this.uri = uri;
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.input_csv))
                .setMessage(context.getString(R.string.input_message_front) + getFileNameByUri(context, uri) + System.getProperty("line.separator") + context.getString(R.string.input_message_rear))
                .setPositiveButton(R.string.input_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (importDatabase(uri)) {
                            execImportDatabase();
                        } else {
                            failedImportDatabase();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private boolean importDatabase(final Uri uri) {
        File file;
        PrintWriter printWriter = null;
        InputStream inputStream = null;
        try {
            // 文字コードを判定し、判定できなければデフォルトをutf8とする
            FileCharDetector fd = new FileCharDetector(context, uri);
            String encType = fd.detect();
            if (encType == null) encType = "UTF-8";

            // 判定された文字コードを指定してファイル読み込みを行う
            inputStream = context.getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream), encType));
            String line;
            boolean isHeaderCorrect = false;
            int columnCount = 0;
            dataList = new ArrayList<>();
            groupList = new ArrayList<>();
            Map<String, String> group = new HashMap<String, String>();
            group.put("group_id", "1");
            group.put("group_order", "1");
            group.put("name", activity.getString(R.string.list_title));
            groupList.add(group);
            id = HEADER_RECORD;
            while ((line = reader.readLine()) != null) {
                String[] result = line.split(",");
                // カラム数が合っていなかった場合終了
                if (result.length != 6 && result.length != 7) return false;
                // 入力最大レコード数を30000件とする
                if (id > MAX_RECORD) return false;
                // ヘッダが正しく設定されているか
                if (!isHeaderCorrect &&
                    result[0].equals("TITLE") &&
                    result[1].equals("ACCOUNT") &&
                    result[2].equals("PASSWORD") &&
                    result[3].equals("URL") &&
                    result[4].equals("MEMO") &&
                    result[5].equals("INPUTDATE")) {
                    // バージョン2系の場合
                    isHeaderCorrect = true;
                    columnCount = 6;
                    continue;
                } else if (!isHeaderCorrect &&
                    result[0].equals("TITLE") &&
                    result[1].equals("ACCOUNT") &&
                    result[2].equals("PASSWORD") &&
                    result[3].equals("URL") &&
                    result[4].equals("GROUP") &&
                    result[5].equals("MEMO") &&
                    result[6].equals("INPUTDATE")) {
                    // バージョン3系の場合
                    isHeaderCorrect = true;
                    columnCount = 7;
                    continue;
                }

                if (isHeaderCorrect && columnCount == 6) {
                    Map<String, String> data = new HashMap<String, String>();
                    data.put("id", id.toString());
                    data.put("title", result[0]);
                    data.put("account", result[1]);
                    data.put("password", result[2]);
                    data.put("url", result[3]);
                    data.put("group_id", "1");
                    data.put("memo", result[4]);
                    data.put("inputdate", result[5]);
                    dataList.add(data);
                } else if (isHeaderCorrect && columnCount == 7) {
                    String groupId = "0";
                    for (Map<String, String> data : groupList) {
                        if (data.get("name").equals(result[4])) {
                            groupId = data.get("group_id");
                        }
                    }

                    if (groupId.equals("0")) {
                        Map<String, String> data = new HashMap<String, String>();
                        data.put("group_id", String.valueOf(groupList.size() + 1));
                        data.put("group_order", String.valueOf(groupList.size() + 1));
                        data.put("name", result[4]);
                        groupId = String.valueOf(groupList.size() + 1);
                        groupList.add(data);
                    }
                    Map<String, String> data = new HashMap<String, String>();
                    data.put("id", id.toString());
                    data.put("title", result[0]);
                    data.put("account", result[1]);
                    data.put("password", result[2]);
                    data.put("url", result[3]);
                    data.put("group_id", groupId);
                    data.put("memo", result[5].replace("  ", "\n"));
                    data.put("inputdate", result[6]);
                    dataList.add(data);
                }
                id++;
            }

            // ヘッダが正しく設定されていなければ取り込みを行わない
            if (!isHeaderCorrect) return false;

        } catch (Exception exc) {
            Toast ts = Toast.makeText(context, context.getString(R.string.csv_input_failed_message), Toast.LENGTH_SHORT);
            ts.show();
            return false;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return true;
    }

    private void execImportDatabase()
    {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.input_csv))
                .setMessage(context.getString(R.string.csv_input_confirm_message))
                .setPositiveButton(R.string.execute, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 取込み中のプログレスバーを表示する
                        progressAlertDialog = new AlertDialog.Builder(context)
                                .setTitle(R.string.csv_input_processing)
                                .setView(activity.getLayoutInflater().inflate(R.layout.alert_progressbar, null))
                                .create();
                        progressAlertDialog.show();
                        progressAlertDialog.setCancelable(false);
                        progressAlertDialog.setCanceledOnTouchOutside(false);
                        progressBar = progressAlertDialog.findViewById(R.id.ProgressBarHorizontal);

                        // ワーカースレッドで取込みを開始する
                        Looper mainLooper = Looper.getMainLooper();
                        Handler handler = HandlerCompat.createAsync(mainLooper);
                        BackgroundTask backgroundTask = new BackgroundTask(handler);
                        ExecutorService executorService  = Executors.newSingleThreadExecutor();
                        executorService.submit(backgroundTask);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void failedImportDatabase()
    {
        if (id == HEADER_RECORD) {
            // ヘッダが正しくないエラーを表示する
            new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.input_csv))
                    .setMessage(context.getString(R.string.csv_input_failed_header_message))
                    .setPositiveButton(R.string.ok, null)
                    .show();
        } else if (id > MAX_RECORD){
            // 入力上限を超えたエラーを表示する
            new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.input_csv))
                    .setMessage(context.getString(R.string.csv_input_failed_counts_message))
                    .setPositiveButton(R.string.ok, null)
                    .show();
        } else {
            // 指定行がエラーであるエラーを表示する
            new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.input_csv))
                    .setMessage(context.getString(R.string.csv_input_failed_body_message) + id)
                    .setPositiveButton(R.string.ok, null)
                    .show();
        }
    }

    private String getFileNameByUri(Context context, Uri uri) {
        String fileName = "";
        String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
        Cursor cursor = context.getContentResolver()
                .query(uri, projection, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                fileName = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME));
            }
            cursor.close();
        }

        return fileName;
    }
}
