package com.highcom.passwordmemo.domain.file

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.ui.viewmodel.SettingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.OutputStream

/**
 * 外部ファイル出力処理クラス
 * * DBデータをCSV形式のファイルとして出力する
 *
 * @property context コンテキスト
 * @property settingViewModel 設定画面ビューモデル
 */
class OutputExternalFile(private val context: Context,
                         private val settingViewModel: SettingViewModel
    ) {
    /**
     * CSVファイル出力先確認ダイアログ表示処理
     *
     * @param uri ファイル出力先URI
     * @param isChromeCsv ChromeCSVか
     */
    fun outputSelectFolder(uri: Uri?, isChromeCsv: Boolean) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.output_csv))
            .setMessage(
                context.getString(R.string.output_message_front) + uri!!.path!!
                    .replace(
                        ":",
                        "/"
                    ) + System.getProperty("line.separator") + context.getString(R.string.output_message_rear)
            )
            .setPositiveButton(R.string.output_button) { _, _ ->
                settingViewModel.viewModelScope.launch(Dispatchers.IO) {
                    exportDatabase(uri, isChromeCsv)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * CSVファイル出力実行処理
     * * DBデータをCSV形式に変換してファイル出力する
     *
     * @param uri 出力先ファイルURI
     * @param isChromeCsv ChromeCSVか
     * @return 出力完了可否
     */
    private suspend fun exportDatabase(uri: Uri?, isChromeCsv: Boolean): Boolean {
        var result = true
        var outputStream: OutputStream? = null

        val combineFlow = combine(settingViewModel.passwordList, settingViewModel.groupList) { passwordList, groupList ->
            passwordList to groupList
        }

        val (passwordList, groupList) = combineFlow.first()
        try {
            outputStream = context.contentResolver.openOutputStream(uri!!)
            //Write the name of the table and the name of the columns (comma separated values) in the .csv file.
            val header = if (isChromeCsv) {
                "name,url,username,password,note" + System.getProperty("line.separator")
            } else {
                "TITLE,ACCOUNT,PASSWORD,URL,GROUP,MEMO,INPUTDATE,GCOLOR,PCOLOR" + System.getProperty("line.separator")
            }
            withContext(Dispatchers.IO) {
                outputStream!!.write(header.toByteArray())
            }
            for (passwordEntity in passwordList) {
                val record = if (isChromeCsv) {
                    val name = passwordEntity.title
                    val url = passwordEntity.url
                    val username = passwordEntity.account
                    val password = passwordEntity.password
                    val note = passwordEntity.memo.replace("\n", "  ")
                    "$name,$url,$username,$password,$note" + System.getProperty("line.separator")
                } else {
                    val title = passwordEntity.title
                    val account = passwordEntity.account
                    val password = passwordEntity.password
                    val url = passwordEntity.url
                    val groupId = passwordEntity.groupId
                    var group = context.getString(R.string.list_title)
                    val memo = passwordEntity.memo.replace("\n", "  ")
                    val inputdate = passwordEntity.inputDate
                    var groupColor = 0
                    val passwordColor = passwordEntity.color
                    for (groupEntity in groupList) {
                        if (groupId == groupEntity.groupId) {
                            group = groupEntity.name
                            groupColor = groupEntity.color
                            break
                        }
                    }
                    "$title,$account,$password,$url,$group,$memo,$inputdate,$groupColor,$passwordColor" + System.getProperty("line.separator")
                }
                withContext(Dispatchers.IO) {
                    outputStream?.write(record.toByteArray())
                }
            }
        } catch (exc: FileNotFoundException) {
            withContext(Dispatchers.Main) {
                AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.output_csv))
                    .setMessage(context.getString(R.string.no_access_message))
                    .setPositiveButton(R.string.move) { _, _ ->
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        intent.data = Uri.parse("package:" + context.packageName)
                        context.startActivity(intent)
                    }
                    .setNegativeButton(R.string.end, null)
                    .show()
            }
            result = false
        } catch (exc: Exception) {
            withContext(Dispatchers.Main) {
                val ts = Toast.makeText(
                    context,
                    context.getString(R.string.csv_output_failed_message),
                    Toast.LENGTH_SHORT
                )
                ts.show()
            }
            result = false
        } finally {
            if (outputStream != null) {
                try {
                    withContext(Dispatchers.IO) {
                        outputStream.close()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            withContext(Dispatchers.Main) {
                AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.output_csv))
                    .setMessage(
                        context.getString(R.string.csv_output_complete_message) + System.getProperty("line.separator") + uri!!.path!!
                            .replace(":", "/")
                    )
                    .setPositiveButton(R.string.ok, null)
                    .show()
            }
        }

        return result
    }
}