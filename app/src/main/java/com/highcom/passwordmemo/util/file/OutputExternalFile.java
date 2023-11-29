package com.highcom.passwordmemo.util.file;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

import com.highcom.passwordmemo.database.ListDataManager;
import com.highcom.passwordmemo.R;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class OutputExternalFile {
    private Context context;

    public OutputExternalFile(Context context) {
        this.context = context;
    }

    public void outputSelectFolder(final Uri uri) {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.output_csv))
                .setMessage(context.getString(R.string.output_message_front) + uri.getPath().replace(":", "/") + System.getProperty("line.separator") + context.getString(R.string.output_message_rear))
                .setPositiveButton(R.string.output_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        exportDatabase(uri);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private boolean exportDatabase(final Uri uri) {
        File file;
        PrintWriter printWriter = null;
        OutputStream outputStream = null;
        try {
            outputStream = context.getContentResolver().openOutputStream(uri);

            SQLiteDatabase db = ListDataManager.getInstance(context).getRdb();

            Cursor curCSV = db.rawQuery("select * from passworddata", null);
            List<Map<String, String>> groupList = ListDataManager.getInstance(context).getGroupList();
            //Write the name of the table and the name of the columns (comma separated values) in the .csv file.
            String header = "TITLE,ACCOUNT,PASSWORD,URL,GROUP,MEMO,INPUTDATE" + System.getProperty("line.separator");
            outputStream.write(header.getBytes());
            while (curCSV.moveToNext()) {
                String title = curCSV.getString(curCSV.getColumnIndex("title"));
                String account = curCSV.getString(curCSV.getColumnIndex("account"));
                String password = curCSV.getString(curCSV.getColumnIndex("password"));
                String url = curCSV.getString(curCSV.getColumnIndex("url"));
                String groupId = curCSV.getString(curCSV.getColumnIndex("group_id"));
                String group = context.getString(R.string.list_title);
                for (Map<String, String> data : groupList) {
                    String id = data.get("group_id");
                    if (groupId.equals(id)) {
                        group = data.get("name");
                        break;
                    }
                }
                String memo = curCSV.getString(curCSV.getColumnIndex("memo")).replace("\n", "  ");
                String inputdate = curCSV.getString(curCSV.getColumnIndex("inputdate"));

                String record = title + "," + account + "," + password + "," + url + "," + group + "," + memo + "," + inputdate + System.getProperty("line.separator");
                outputStream.write(record.getBytes());
            }

            curCSV.close();
        } catch (FileNotFoundException exc) {
            new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.output_csv))
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
            Toast ts = Toast.makeText(context, context.getString(R.string.csv_output_failed_message), Toast.LENGTH_SHORT);
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
                .setTitle(context.getString(R.string.output_csv))
                .setMessage(context.getString(R.string.csv_output_complete_message) + System.getProperty("line.separator") + uri.getPath().replace(":", "/"))
                .setPositiveButton(R.string.ok, null)
                .show();
        return true;
    }
}
