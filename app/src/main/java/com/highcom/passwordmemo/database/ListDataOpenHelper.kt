package com.highcom.passwordmemo.database

import android.content.Context
import net.sqlcipher.Cursor
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteOpenHelper
import java.util.Arrays

/**
 * Created by koichi on 2018/05/30.
 */
class ListDataOpenHelper(context: Context?) : SQLiteOpenHelper(context, "PasswordMemoDB", null, 3) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "create table passworddata("
                    + "id long not null,"
                    + "title text default '',"
                    + "account text default '',"
                    + "password text default '',"
                    + "url text default '',"
                    + "group_id long default 1,"
                    + "memo text default '',"
                    + "inputdate text default ''"
                    + ");"
        )
        db.execSQL(
            "create table groupdata("
                    + "group_id long not null,"
                    + "group_order int,"
                    + "name text default ''"
                    + ");"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        val targetTable = "passworddata"
        db.beginTransaction()
        try {
            // 初期化
            db.execSQL("ALTER TABLE $targetTable RENAME TO temp_$targetTable")
            // 元カラム一覧
            val columns = getColumns(db, "temp_$targetTable")
            onCreate(db)
            // 新カラム一覧
            val newColumns: List<String>? = getColumns(db, targetTable)

            // 変化しないカラムのみ抽出
            columns!!.retainAll(newColumns!!)

            // 共通データを移す。(OLDにしか存在しないものは捨てられ, NEWにしか存在しないものはNULLになる)
            val cols = join(columns, ",")
            db.execSQL(
                String.format(
                    "INSERT INTO %s (%s) SELECT %s from temp_%s", targetTable,
                    cols, cols, targetTable
                )
            )
            // 終了処理
            db.execSQL("DROP TABLE temp_$targetTable")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    companion object {
        private fun getColumns(db: SQLiteDatabase, tableName: String): MutableList<String>? {
            var ar: MutableList<String>? = null
            var c: Cursor? = null
            try {
                c = db.rawQuery("SELECT * FROM $tableName LIMIT 1", null)
                if (c != null) {
                    ar = ArrayList(Arrays.asList(*c.columnNames))
                }
            } finally {
                c?.close()
            }
            return ar
        }

        private fun join(list: List<String>?, delim: String): String {
            val buf = StringBuilder()
            val num = list!!.size
            for (i in 0 until num) {
                if (i != 0) buf.append(delim)
                buf.append(list[i])
            }
            return buf.toString()
        }
    }
}