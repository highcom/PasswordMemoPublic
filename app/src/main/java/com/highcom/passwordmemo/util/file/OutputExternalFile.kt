package com.highcom.passwordmemo.util.file

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.database.ListDataManager
import net.sqlcipher.database.SQLiteDatabase
import java.io.File
import java.io.FileNotFoundException
import java.io.OutputStream
import java.io.PrintWriter

class OutputExternalFile(private val context: Context) {
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
            .setPositiveButton(R.string.output_button) { dialog, which -> exportDatabase(uri) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun exportDatabase(uri: Uri?): Boolean {
        var file: File
        val printWriter: PrintWriter? = null
        var outputStream: OutputStream? = null
        try {
            outputStream = context.contentResolver.openOutputStream(uri!!)
            val db: SQLiteDatabase = ListDataManager.Companion.getInstance(
                context
            ).getRdb()
            val curCSV: Cursor = db.rawQuery("select * from passworddata", null)
            val groupList: List<Map<String?, String?>> = ListDataManager.Companion.getInstance(
                context
            )!!.getGroupList()
            //Write the name of the table and the name of the columns (comma separated values) in the .csv file.
            val header =
                "TITLE,ACCOUNT,PASSWORD,URL,GROUP,MEMO,INPUTDATE" + System.getProperty("line.separator")
            outputStream!!.write(header.toByteArray())
            while (curCSV.moveToNext()) {
                val title = curCSV.getString(curCSV.getColumnIndex("title"))
                val account = curCSV.getString(curCSV.getColumnIndex("account"))
                val password = curCSV.getString(curCSV.getColumnIndex("password"))
                val url = curCSV.getString(curCSV.getColumnIndex("url"))
                val groupId = curCSV.getString(curCSV.getColumnIndex("group_id"))
                var group = context.getString(R.string.list_title)
                for (data in groupList) {
                    val id = data["group_id"]
                    if (groupId == id) {
                        group = data["name"]
                        break
                    }
                }
                val memo = curCSV.getString(curCSV.getColumnIndex("memo")).replace("\n", "  ")
                val inputdate = curCSV.getString(curCSV.getColumnIndex("inputdate"))
                val record =
                    title + "," + account + "," + password + "," + url + "," + group + "," + memo + "," + inputdate + System.getProperty(
                        "line.separator"
                    )
                outputStream.write(record.toByteArray())
            }
            curCSV.close()
        } catch (exc: FileNotFoundException) {
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
            return false
        } catch (exc: Exception) {
            val ts = Toast.makeText(
                context,
                context.getString(R.string.csv_output_failed_message),
                Toast.LENGTH_SHORT
            )
            ts.show()
            return false
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.output_csv))
            .setMessage(
                context.getString(R.string.csv_output_complete_message) + System.getProperty("line.separator") + uri!!.path!!
                    .replace(":", "/")
            )
            .setPositiveButton(R.string.ok, null)
            .show()
        return true
    }
}