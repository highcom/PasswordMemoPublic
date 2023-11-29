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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RestoreDbFile {
    private Context context;
    private Activity activity;
    private RestoreDbFileListener listener;
    private Uri uri;
    private AlertDialog progressAlertDialog;
    private ProgressBar progressBar;

    public interface RestoreDbFileListener {
        void restoreComplete();
    }

    private class BackgroundTask implements Runnable {
        private final android.os.Handler _handler;

        public BackgroundTask(android.os.Handler handler) {
            _handler = handler;
        }

        @WorkerThread
        @Override
        public void run() {
            String destPath = context.getDatabasePath("PasswordMemoDB").getPath();
            File destFile = new File (destPath);
            destFile.delete();
            String srcPath = context.getDatabasePath("PasswordMemoDB_tmp").getPath();
            File srcFile = new File (srcPath);
            srcFile.renameTo(destFile);

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
            new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.restore_db))
                    .setMessage(context.getString(R.string.db_restore_complete_message) + System.getProperty("line.separator") + getFileNameByUri(context, uri))
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            listener.restoreComplete();
                        }
                    })
                    .show();
        }
    }

    public RestoreDbFile(Activity activity, RestoreDbFileListener listener) {
        this.activity = activity;
        this.context = activity;
        this.listener = listener;
    }

    public void restoreSelectFolder(final Uri uri) {
        this.uri = uri;
        String fileName = getFileNameByUri(context, uri);
        if (fileName.contains(".db")) {
            new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.restore_db))
                    .setMessage(context.getString(R.string.restore_message_front) + getFileNameByUri(context, uri) + System.getProperty("line.separator") + context.getString(R.string.restore_message_rear))
                    .setPositiveButton(R.string.restore_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (restoreDatabase(uri)) {
                                execRestoreDatabase();
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String path = context.getDatabasePath("PasswordMemoDB_tmp").getPath();
                            File file = new File(path);
                            file.delete();
                        }
                    })
                    .show();
        } else {
            new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.restore_db))
                    .setMessage(context.getString(R.string.restore_message_error_file))
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String path = context.getDatabasePath("PasswordMemoDB_tmp").getPath();
                            File file = new File(path);
                            file.delete();
                        }
                    })
                    .show();
        }
    }

    private boolean restoreDatabase(final Uri uri) {
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(uri);

            String path = context.getDatabasePath("PasswordMemoDB_tmp").getPath();
            File file = new File (path);
            OutputStream outputStream = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
            inputStream.close();
        } catch (Exception exc) {
            Toast ts = Toast.makeText(context, context.getString(R.string.db_restore_failed_message), Toast.LENGTH_SHORT);
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

    private void execRestoreDatabase()
    {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.restore_db))
                .setMessage(context.getString(R.string.db_restore_confirm_message))
                .setPositiveButton(R.string.execute, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 取込み中のプログレスバーを表示する
                        progressAlertDialog = new AlertDialog.Builder(context)
                                .setTitle(R.string.db_restore_processing)
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
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String path = context.getDatabasePath("PasswordMemoDB_tmp").getPath();
                        File file = new File (path);
                        file.delete();
                    }
                })
                .show();
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
