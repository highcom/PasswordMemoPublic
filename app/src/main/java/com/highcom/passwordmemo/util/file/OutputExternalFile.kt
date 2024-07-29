package com.highcom.passwordmemo.util.file

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

class OutputExternalFile(private val context: Context,
                         private val settingViewModel: SettingViewModel
    ) {
    fun outputSelectFolder(uri: Uri?) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.output_csv))
            .setMessage(
                context.getString(R.string.output_message_front) + uri!!.path!!
                    .replace(
                        ":",
                        "/"
                    ) + System.getProperty("line.separator") + context.getString(R.string.output_message_rear)
            )
            .setPositiveButton(R.string.output_button) { dialog, which ->
                settingViewModel.viewModelScope.launch(Dispatchers.IO) {
                    exportDatabase(uri)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private suspend fun exportDatabase(uri: Uri?): Boolean {
        var result = true
        var outputStream: OutputStream? = null

        val combineFlow = combine(settingViewModel.passwordList, settingViewModel.groupList) { passwordList, groupList ->
            passwordList to groupList
        }

        val (passwordList, groupList) = combineFlow.first()
        try {
            outputStream = context.contentResolver.openOutputStream(uri!!)
            //Write the name of the table and the name of the columns (comma separated values) in the .csv file.
            val header =
                "TITLE,ACCOUNT,PASSWORD,URL,GROUP,MEMO,INPUTDATE" + System.getProperty("line.separator")
            outputStream!!.write(header.toByteArray())
            for (passwordEntity in passwordList) {
                val title = passwordEntity.title
                val account = passwordEntity.account
                val password = passwordEntity.password
                val url = passwordEntity.url
                val groupId = passwordEntity.groupId
                var group = context.getString(R.string.list_title)
                for (groupEntity in groupList) {
                    if (groupId == groupEntity.groupId) {
                        group = groupEntity.name
                        break
                    }
                }
                val memo = passwordEntity.memo.replace("\n", "  ")
                val inputdate = passwordEntity.inputDate
                val record =
                    title + "," + account + "," + password + "," + url + "," + group + "," + memo + "," + inputdate + System.getProperty(
                        "line.separator"
                    )
                outputStream?.write(record.toByteArray())
            }
        } catch (exc: FileNotFoundException) {
            withContext(Dispatchers.Main) {
                AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.output_csv))
                    .setMessage(context.getString(R.string.no_access_message))
                    .setPositiveButton(R.string.move) { dialog, which ->
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
                    outputStream.close()
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