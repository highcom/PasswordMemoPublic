package com.highcom.passwordmemo.util.file

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import com.highcom.passwordmemo.R

class SelectInputOutputFileDialog(
    private val context: Context,
    private val operation: Operation,
    private val inputOutputFileDialogListener: InputOutputFileDialogListener
) {
    private var checkedItem = -1
    private lateinit var items: Array<String?>

    enum class Operation {
        DB_RESTORE_BACKUP, CSV_INPUT_OUTPUT
    }

    interface InputOutputFileDialogListener {
        fun onSelectOperationClicked(path: String?)
    }

    init {
        init()
    }

    fun init() {
        items = arrayOfNulls(2)
        when (operation) {
            Operation.DB_RESTORE_BACKUP -> {
                items[0] = context.getString(R.string.restore_db)
                items[1] = context.getString(R.string.backup_db)
            }

            Operation.CSV_INPUT_OUTPUT -> {
                items[0] = context.getString(R.string.input_csv)
                items[1] = context.getString(R.string.output_csv)
            }
        }
    }

    fun createOpenFileDialog(): AlertDialog.Builder {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.getString(R.string.select_operation))
            .setSingleChoiceItems(items, checkedItem) { _, which -> checkedItem = which }
            .setPositiveButton(R.string.next) { _, _ ->
                if (checkedItem == 0 || checkedItem == 1) {
                    inputOutputFileDialogListener.onSelectOperationClicked(items[checkedItem])
                } else {
                    val ts = Toast.makeText(
                        context,
                        context.getString(R.string.select_operation_err_message),
                        Toast.LENGTH_SHORT
                    )
                    ts.show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
        return builder
    }
}