package com.highcom.passwordmemo.util.file;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

import com.highcom.passwordmemo.R;

public class SelectInputOutputFileDialog {
    private Context context;
    private InputOutputFileDialogListener inputOutputFileDialogListener;
    private int checkedItem = -1;
    private String[] items;
    public enum Operation {
        DB_RESTORE_BACKUP,
        CSV_INPUT_OUTPUT,
    }
    private Operation operation;

    public interface InputOutputFileDialogListener {
        void onSelectOperationClicked(String path);
    }

    public SelectInputOutputFileDialog(Context context, Operation operation, InputOutputFileDialogListener inputOutputFileDialogListener) {
        this.context = context;
        this.operation = operation;
        this.inputOutputFileDialogListener = inputOutputFileDialogListener;
        this.init();
    }

    public void init() {
        items = new String[2];
        switch (operation) {
            case DB_RESTORE_BACKUP:
                items[0] = context.getString(R.string.restore_db);
                items[1] = context.getString(R.string.backup_db);
                break;
            case CSV_INPUT_OUTPUT:
            default:
                items[0] = context.getString(R.string.input_csv);
                items[1] = context.getString(R.string.output_csv);
                break;
        }
    }

    public AlertDialog.Builder createOpenFileDialog() {
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.select_operation))
                .setSingleChoiceItems(items, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        checkedItem = which;
                    }
                })
                .setPositiveButton(R.string.next, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        if (checkedItem == 0 || checkedItem == 1) {
                            inputOutputFileDialogListener.onSelectOperationClicked(items[checkedItem]);
                        } else {
                            Toast ts = Toast.makeText(context, context.getString(R.string.select_operation_err_message), Toast.LENGTH_SHORT);
                            ts.show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null);
        return builder;
    }

}
