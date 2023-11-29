package com.highcom.passwordmemo.util.file;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

import com.highcom.passwordmemo.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

public class BackupDbFile {
    private Context context;

    public BackupDbFile(Context context) {
        this.context = context;
    }


    public void backupSelectFolder(final Uri uri) {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.backup_db))
                .setMessage(context.getString(R.string.backup_message_front) + uri.getPath().replace(":", "/") + System.getProperty("line.separator") + context.getString(R.string.backup_message_rear))
                .setPositiveButton(R.string.backup_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        backupDatabase(uri);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private boolean backupDatabase(final Uri uri) {
        OutputStream outputStream = null;
        try {
            String path = context.getDatabasePath("PasswordMemoDB").getPath();
            File file = new File(path);
            InputStream inputStream = new FileInputStream(file);
            outputStream = context.getContentResolver().openOutputStream(uri);
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }

        } catch (FileNotFoundException exc) {
            new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.backup_db))
                    .setMessage(context.getString(R.string.no_access_message))
                    .setPositiveButton(R.string.move, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + context.getPackageName()));
                            context.startActivity(intent);
                        }
                    })
                    .setNegativeButton(R.string.end, null)
                    .show();
            return false;
        } catch (Exception exc) {
            Toast ts = Toast.makeText(context, context.getString(R.string.db_backup_failed_message), Toast.LENGTH_SHORT);
            ts.show();
            return false;
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.backup_db))
                .setMessage(context.getString(R.string.db_backup_complete_message) + System.getProperty("line.separator") + uri.getPath().replace(":", "/"))
                .setPositiveButton(R.string.ok, null)
                .show();
        return true;
    }
}
