package com.highcom.passwordmemo.util.file

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.core.os.HandlerCompat
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.database.ListDataManager
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PrintWriter
import java.util.Objects
import java.util.concurrent.Executors

class InputExternalFile(private val activity: Activity, listener: InputExternalFileListener) {
    private val context: Context
    private var dataList: MutableList<Map<String?, String?>>? = null
    private var groupList: MutableList<Map<String?, String?>>? = null
    private var id = 0
    private var uri: Uri? = null
    private var progressAlertDialog: AlertDialog? = null
    private var progressBar: ProgressBar? = null
    private val listener: InputExternalFileListener

    interface InputExternalFileListener {
        fun importComplete()
    }

    private inner class BackgroundTask(private val _handler: Handler) : Runnable {
        @WorkerThread
        override fun run() {
            // 既存のデータは全て削除する
            ListDataManager.Companion.getInstance(context)!!.deleteAllData()
            val countUnit = dataList!!.size / 20
            var progressCount = 1
            // 結果が全て取り出せたらデータを登録していく
            for (data in dataList!!) {
                ListDataManager.Companion.getInstance(context)!!.setData(false, data)
                if (countUnit > 0) {
                    // 5パーセントずつ表示を更新する
                    if (progressCount % countUnit == 0) {
                        progressBar!!.progress = progressCount / countUnit * 5
                    }
                    progressCount++
                }
            }
            // 最後にグループデータを登録する
            for (data in groupList!!) {
                if (data["group_id"] == "1") continue
                ListDataManager.Companion.getInstance(context)!!.setGroupData(false, data)
            }
            progressBar!!.progress = 100
            val postExecutor: PostExecutor = PostExecutor()
            _handler.post(postExecutor)
        }
    }

    private inner class PostExecutor : Runnable {
        @UiThread
        override fun run() {
            progressAlertDialog!!.dismiss()
            listener.importComplete()
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.input_csv))
                .setMessage(
                    context.getString(R.string.csv_input_complete_message) + System.getProperty(
                        "line.separator"
                    ) + getFileNameByUri(context, uri)
                )
                .setPositiveButton(R.string.ok, null)
                .show()
        }
    }

    init {
        context = activity
        this.listener = listener
    }

    fun inputSelectFolder(uri: Uri?) {
        this.uri = uri
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.input_csv))
            .setMessage(
                context.getString(R.string.input_message_front) + getFileNameByUri(
                    context,
                    uri
                ) + System.getProperty("line.separator") + context.getString(R.string.input_message_rear)
            )
            .setPositiveButton(R.string.input_button) { dialog, which ->
                if (importDatabase(uri)) {
                    execImportDatabase()
                } else {
                    failedImportDatabase()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun importDatabase(uri: Uri?): Boolean {
        var file: File
        val printWriter: PrintWriter? = null
        var inputStream: InputStream? = null
        try {
            // 文字コードを判定し、判定できなければデフォルトをutf8とする
            val fd = FileCharDetector(context, uri)
            var encType = fd.detect()
            if (encType == null) encType = "UTF-8"

            // 判定された文字コードを指定してファイル読み込みを行う
            inputStream = context.contentResolver.openInputStream(uri!!)
            val reader =
                BufferedReader(InputStreamReader(Objects.requireNonNull(inputStream), encType))
            var line: String
            var isHeaderCorrect = false
            var columnCount = 0
            dataList = ArrayList()
            groupList = ArrayList()
            val group: MutableMap<String?, String?> = HashMap()
            group["group_id"] = "1"
            group["group_order"] = "1"
            group["name"] = activity.getString(R.string.list_title)
            groupList?.add(group)
            id = HEADER_RECORD
            while (reader.readLine().also { line = it } != null) {
                val result = line.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                // カラム数が合っていなかった場合終了
                if (result.size != 6 && result.size != 7) return false
                // 入力最大レコード数を30000件とする
                if (id > MAX_RECORD) return false
                // ヘッダが正しく設定されているか
                if (!isHeaderCorrect && result[0] == "TITLE" && result[1] == "ACCOUNT" && result[2] == "PASSWORD" && result[3] == "URL" && result[4] == "MEMO" && result[5] == "INPUTDATE") {
                    // バージョン2系の場合
                    isHeaderCorrect = true
                    columnCount = 6
                    continue
                } else if (!isHeaderCorrect && result[0] == "TITLE" && result[1] == "ACCOUNT" && result[2] == "PASSWORD" && result[3] == "URL" && result[4] == "GROUP" && result[5] == "MEMO" && result[6] == "INPUTDATE") {
                    // バージョン3系の場合
                    isHeaderCorrect = true
                    columnCount = 7
                    continue
                }
                if (isHeaderCorrect && columnCount == 6) {
                    val data: MutableMap<String?, String?> = HashMap()
                    data["id"] = id.toString()
                    data["title"] = result[0]
                    data["account"] = result[1]
                    data["password"] = result[2]
                    data["url"] = result[3]
                    data["group_id"] = "1"
                    data["memo"] = result[4]
                    data["inputdate"] = result[5]
                    dataList?.add(data)
                } else if (isHeaderCorrect && columnCount == 7) {
                    var groupId = "0"
                    for (data in groupList!!) {
                        if (data["name"] == result[4]) {
                            groupId = data["group_id"]!!
                        }
                    }
                    if (groupId == "0") {
                        val data: MutableMap<String?, String?> = HashMap()
                        data["group_id"] = (groupList?.size?.plus(1)).toString()
                        data["group_order"] = (groupList?.size?.plus(1)).toString()
                        data["name"] = result[4]
                        groupId = (groupList?.size?.plus(1)).toString()
                        groupList?.add(data)
                    }
                    val data: MutableMap<String?, String?> = HashMap()
                    data["id"] = id.toString()
                    data["title"] = result[0]
                    data["account"] = result[1]
                    data["password"] = result[2]
                    data["url"] = result[3]
                    data["group_id"] = groupId
                    data["memo"] = result[5].replace("  ", "\n")
                    data["inputdate"] = result[6]
                    dataList?.add(data)
                }
                id++
            }

            // ヘッダが正しく設定されていなければ取り込みを行わない
            if (!isHeaderCorrect) return false
        } catch (exc: Exception) {
            val ts = Toast.makeText(
                context,
                context.getString(R.string.csv_input_failed_message),
                Toast.LENGTH_SHORT
            )
            ts.show()
            return false
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return true
    }

    private fun execImportDatabase() {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.input_csv))
            .setMessage(context.getString(R.string.csv_input_confirm_message))
            .setPositiveButton(R.string.execute) { dialog, which -> // 取込み中のプログレスバーを表示する
                progressAlertDialog = AlertDialog.Builder(context)
                    .setTitle(R.string.csv_input_processing)
                    .setView(activity.layoutInflater.inflate(R.layout.alert_progressbar, null))
                    .create()
                progressAlertDialog?.show()
                progressAlertDialog?.setCancelable(false)
                progressAlertDialog?.setCanceledOnTouchOutside(false)
                progressBar = progressAlertDialog?.findViewById(R.id.ProgressBarHorizontal)

                // ワーカースレッドで取込みを開始する
                val mainLooper = Looper.getMainLooper()
                val handler = HandlerCompat.createAsync(mainLooper)
                val backgroundTask: BackgroundTask = BackgroundTask(handler)
                val executorService = Executors.newSingleThreadExecutor()
                executorService.submit(backgroundTask)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun failedImportDatabase() {
        if (id == HEADER_RECORD) {
            // ヘッダが正しくないエラーを表示する
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.input_csv))
                .setMessage(context.getString(R.string.csv_input_failed_header_message))
                .setPositiveButton(R.string.ok, null)
                .show()
        } else if (id > MAX_RECORD) {
            // 入力上限を超えたエラーを表示する
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.input_csv))
                .setMessage(context.getString(R.string.csv_input_failed_counts_message))
                .setPositiveButton(R.string.ok, null)
                .show()
        } else {
            // 指定行がエラーであるエラーを表示する
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.input_csv))
                .setMessage(context.getString(R.string.csv_input_failed_body_message) + id)
                .setPositiveButton(R.string.ok, null)
                .show()
        }
    }

    private fun getFileNameByUri(context: Context, uri: Uri?): String {
        var fileName = ""
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        val cursor = context.contentResolver
            .query(uri!!, projection, null, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                fileName = cursor.getString(
                    cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                )
            }
            cursor.close()
        }
        return fileName
    }

    companion object {
        private const val HEADER_RECORD = 1
        private const val MAX_RECORD = 10000
    }
}