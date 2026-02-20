package com.highcom.passwordmemo.domain.file

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import com.highcom.passwordmemo.R

/**
 * ファイル入出力選択ダイアログ
 * * DBファイル・CSVファイルの入出力の選択操作をするためのダイアログ
 *
 * @property context コンテキスト
 * @property operation 選択操作
 * @property inputOutputFileDialogListener ファイル入出力選択通知リスナー
 */
class SelectInputOutputFileDialog(
    private val context: Context,
    private val operation: Operation,
    private val inputOutputFileDialogListener: InputOutputFileDialogListener
) {
    /** 選択した操作項目 */
    private var checkedItem = -1
    /** 選択操作項目一覧 */
    private lateinit var items: Array<String?>

    /**
     * 選択操作項目定義
     *
     */
    enum class Operation {
        DB_RESTORE_BACKUP, CSV_INPUT_OUTPUT, CHROME_CSV_INPUT_OUTPUT
    }

    /**
     * ファイル入出力選択通知リスナークラス
     *
     */
    interface InputOutputFileDialogListener {
        /**
         * ファイル入出力選択通知処理
         *
         * @param path 選択操作文字列
         */
        fun onSelectOperationClicked(path: String?, isChromeCsv: Boolean)
    }

    init {
        init()
    }

    fun init() {
        when (operation) {
            Operation.DB_RESTORE_BACKUP -> {
                items = arrayOfNulls(2)
                items[0] = context.getString(R.string.restore_db)
                items[1] = context.getString(R.string.backup_db)
            }

            Operation.CSV_INPUT_OUTPUT, Operation.CHROME_CSV_INPUT_OUTPUT -> {
                items = arrayOfNulls(3)
                items[0] = context.getString(R.string.input_csv_override)
                items[1] = context.getString(R.string.input_csv_add)
                items[2] = context.getString(R.string.output_csv)
            }
        }
    }

    /**
     * ファイル入出力選択ダイアログ生成処理
     *
     * @return ダイアログビルダー
     */
    fun createOpenFileDialog(): AlertDialog.Builder {
        val builder = AlertDialog.Builder(context)
        val isChromeCsv = operation == Operation.CHROME_CSV_INPUT_OUTPUT
        val title = if (isChromeCsv) {
            context.getString(R.string.select_chrome_data_operation)
        } else {
            context.getString(R.string.select_operation)
        }
        builder.setTitle(title)
            .setSingleChoiceItems(items, checkedItem) { _, which -> checkedItem = which }
            .setPositiveButton(R.string.next) { _, _ ->
                if (checkedItem >= 0 && checkedItem < items.size) {
                    inputOutputFileDialogListener.onSelectOperationClicked(items[checkedItem], isChromeCsv)
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