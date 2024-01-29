package com.highcom.passwordmemo.database

import android.content.ContentValues
import android.content.Context
import com.highcom.passwordmemo.R
import net.sqlcipher.Cursor
import net.sqlcipher.database.SQLiteDatabase
import java.util.Collections

class ListDataManager private constructor(private val context: Context) {
    private var groupId: Long = 1
    val rdb: SQLiteDatabase
    val wdb: SQLiteDatabase
    private lateinit var data: MutableMap<String?, String?>
    val dataList = ArrayList<Map<String?, String?>>()
    private lateinit var groupData: MutableMap<String?, String?>
    val groupList: MutableList<Map<String?, String?>>
    private var sortKey: String?

    init {
        SQLiteDatabase.loadLibs(context)
        sortKey = "id"
        val helper = ListDataOpenHelper(context)
        // onUpgradeを呼び出すために先にWritableDatabaseを先に呼び出す
        wdb = helper.getWritableDatabase(context.getString(R.string.db_secret_key))
        rdb = helper.getReadableDatabase(context.getString(R.string.db_secret_key))
        val cur = cursor
        var mov = cur.moveToFirst()
        while (mov) {
            data = HashMap()
            data["id"] = cur.getString(0)
            data["title"] = cur.getString(1)
            data["account"] = cur.getString(2)
            data["password"] = cur.getString(3)
            data["url"] = cur.getString(4)
            data["group_id"] = cur.getString(5)
            data["memo"] = cur.getString(6)
            data["inputdate"] = cur.getString(7)
            dataList.add(data)
            mov = cur.moveToNext()
        }
        cur.close()
//        sortListData(sortKey)
        groupList = ArrayList()
        val gcur = groupCursor
        var gmov = gcur.moveToFirst()
        while (gmov) {
            groupData = HashMap()
            groupData["group_id"] = gcur.getString(0)
            groupData["group_order"] = gcur.getString(1)
            groupData["name"] = gcur.getString(2)
            groupList.add(groupData)
            gmov = gcur.moveToNext()
        }
        // グループデータがない場合はデフォルトデータとして「すべて」を必ず追加」
        if (groupList.size == 0) {
            groupData = HashMap()
            groupData["group_id"] = "1"
            groupData["group_order"] = "1"
            groupData["name"] = context.getString(R.string.list_title)
            setGroupData(false, groupData)
        }
        gcur.close()
        sortGroupListData()
    }

    fun setSelectGroupId(id: Long) {
        groupId = id
        remakeListData()
    }

//    fun setData(isEdit: Boolean, data: Map<String?, String?>) {
//        // データベースに追加or編集する
//        val values = ContentValues()
//        values.put("id", java.lang.Long.valueOf(data["id"]))
//        values.put("title", data["title"])
//        values.put("account", data["account"])
//        values.put("password", data["password"])
//        values.put("url", data["url"])
//        values.put("group_id", java.lang.Long.valueOf(data["group_id"]))
//        values.put("memo", data["memo"])
//        values.put("inputdate", data["inputdate"])
//        if (isEdit) {
//            // 編集の場合
//            wdb.update("passworddata", values, "id=?", arrayOf(data["id"]))
//            remakeListData()
//        } else {
//            // 新規作成の場合
//            wdb.insert("passworddata", data["id"], values)
//            dataList.add(data)
//        }
//    }

//    fun deleteData(id: String) {
//        // データベースから削除する
//        wdb.delete("passworddata", "id=?", arrayOf(id))
//        remakeListData()
//    }

//    fun deleteAllData() {
//        wdb.delete("passworddata", null, null)
//        dataList.clear()
//        wdb.delete("groupdata", null, null)
//        groupList.clear()
//        // グループデータがない場合はデフォルトデータとして「すべて」を必ず追加」
//        groupList.size
//        groupData = HashMap()
//        groupData["group_id"] = "1"
//        groupData["group_order"] = "1"
//        groupData["name"] = context.getString(R.string.list_title)
//        setGroupData(false, groupData)
//    }

//    fun getDataList(): List<Map<String?, String?>> {
//        return dataList
//    }

//    fun rearrangeData(fromPos: Int, toPos: Int) {
//        var mov: Boolean
//        var values: ContentValues
//        val cur = cursor
//        mov = cur.moveToPosition(fromPos)
//        if (!mov) return
//        val fromId = cur.getLong(0)
//        mov = cur.moveToPosition(toPos)
//        if (!mov) return
//        val toId = cur.getLong(0)
//        values = ContentValues()
//        values.put("id", -1)
//        wdb.update("passworddata", values, "id=?", arrayOf(java.lang.Long.toString(fromId)))
//        values = ContentValues()
//        values.put("id", fromId)
//        wdb.update("passworddata", values, "id=?", arrayOf(java.lang.Long.toString(toId)))
//        values = ContentValues()
//        values.put("id", toId)
//        wdb.update("passworddata", values, "id=?", arrayOf(java.lang.Long.toString(-1)))
//        cur.close()
//        remakeListData()
//    }

//    val newId: Long
//        get() {
//            var newId: Long = 0
//            val cur = rdb.query(
//                "passworddata",
//                arrayOf(
//                    "id",
//                    "title",
//                    "account",
//                    "password",
//                    "url",
//                    "group_id",
//                    "memo",
//                    "inputdate"
//                ),
//                null,
//                null,
//                null,
//                null,
//                "id ASC",
//                null
//            )
//            var mov = cur.moveToFirst()
//            var curId: Long
//            while (mov) {
//                curId = java.lang.Long.valueOf(cur.getString(0))
//                if (newId < curId) {
//                    newId = curId
//                }
//                mov = cur.moveToNext()
//            }
//            cur.close()
//            return newId + 1
//        }

//    fun closeData() {
//        rdb.close()
//        wdb.close()
//        manager = null
//    }

    private fun remakeListData() {
//        dataList.clear()
//        val cur = cursor
//        var mov = cur.moveToFirst()
//        while (mov) {
//            data = HashMap()
//            data["id"] = cur.getString(0)
//            data["title"] = cur.getString(1)
//            data["account"] = cur.getString(2)
//            data["password"] = cur.getString(3)
//            data["url"] = cur.getString(4)
//            data["group_id"] = cur.getString(5)
//            data["memo"] = cur.getString(6)
//            data["inputdate"] = cur.getString(7)
//            dataList.add(data)
//            mov = cur.moveToNext()
//        }
//        cur.close()
//        sortListData(sortKey)
    }

//    fun sortListData(key: String?) {
//        sortKey = key
//        Collections.sort(
//            dataList,
//            Comparator { stringStringMap, t1 ->
//                var result: Int
//                result = if (sortKey == SORT_TITLE) {
//                    stringStringMap[sortKey]!!.compareTo(t1[sortKey]!!)
//                } else if (sortKey == SORT_INPUTDATE) {
//                    t1[sortKey]!!.compareTo(stringStringMap[sortKey]!!)
//                } else {
//                    Integer.valueOf(stringStringMap["id"]).compareTo(
//                        Integer.valueOf(
//                            t1["id"]
//                        )
//                    )
//                }
//                if (result == 0) {
//                    result = Integer.valueOf(stringStringMap["id"]).compareTo(
//                        Integer.valueOf(
//                            t1["id"]
//                        )
//                    )
//                }
//                result
//            }
//        )
//    }

    fun resetGroupIdData(groupId: Long?) {
        val values = ContentValues()
        values.put("group_id", 1L)
        wdb.update("passworddata", values, "group_id=?", arrayOf(groupId.toString()))
        remakeListData()
    }

    private val cursor: Cursor
        private get() {
            var selection: String? = null
            if (groupId != 1L) {
                selection = "group_id = $groupId"
            }
            return rdb.query(
                "passworddata",
                arrayOf(
                    "id",
                    "title",
                    "account",
                    "password",
                    "url",
                    "group_id",
                    "memo",
                    "inputdate"
                ),
                selection,
                null,
                null,
                null,
                "id ASC",
                null
            )
        }

    fun rearrangeGroupData(fromPos: Int, toPos: Int) {
        var mov: Boolean
        var values: ContentValues
        val cur = groupCursor
        mov = cur.moveToPosition(fromPos)
        if (!mov) return
        val fromOrder = cur.getInt(1)
        mov = cur.moveToPosition(toPos)
        if (!mov) return
        val toOrder = cur.getInt(1)
        values = ContentValues()
        values.put("group_order", -1)
        wdb.update("groupdata", values, "group_order=?", arrayOf(Integer.toString(fromOrder)))
        values = ContentValues()
        values.put("group_order", fromOrder)
        wdb.update("groupdata", values, "group_order=?", arrayOf(Integer.toString(toOrder)))
        values = ContentValues()
        values.put("group_order", toOrder)
        wdb.update("groupdata", values, "group_order=?", arrayOf(Integer.toString(-1)))
        cur.close()
        remakeGroupListData()
    }

    val newGroupId: Long
        get() {
            var newId: Long = 0
            val cur = groupCursor
            var mov = cur.moveToFirst()
            var curId: Long
            while (mov) {
                curId = java.lang.Long.valueOf(cur.getString(0))
                if (newId < curId) {
                    newId = curId
                }
                mov = cur.moveToNext()
            }
            cur.close()
            return newId + 1
        }

    fun setGroupData(isEdit: Boolean, data: Map<String?, String?>) {
        // データベースに追加or編集する
        val values = ContentValues()
        values.put("group_id", java.lang.Long.valueOf(data["group_id"]))
        values.put("group_order", Integer.valueOf(data["group_order"]))
        values.put("name", data["name"])
        if (isEdit) {
            // 編集の場合
            wdb.update("groupdata", values, "group_id=?", arrayOf(data["group_id"]))
            remakeGroupListData()
        } else {
            // 新規作成の場合
            wdb.insert("groupdata", data["group_id"], values)
            groupList.add(data)
        }
    }

    fun deleteGroupData(id: String?) {
        // データベースから削除する
        wdb.delete("groupdata", "group_id=?", arrayOf(id))
        remakeGroupListData()
    }

    fun sortGroupListData() {
        Collections.sort(groupList) { stringStringMap, t1 ->
            var result: Int
            result = Integer.valueOf(stringStringMap["group_order"]).compareTo(
                Integer.valueOf(
                    t1["group_order"]
                )
            )
            if (result == 0) {
                result = Integer.valueOf(stringStringMap["group_id"]).compareTo(
                    Integer.valueOf(
                        t1["group_id"]
                    )
                )
            }
            result
        }
    }

    private val groupCursor: Cursor
        private get() = rdb.query(
            "groupdata",
            arrayOf("group_id", "group_order", "name"),
            null,
            null,
            null,
            null,
            "group_order ASC",
            null
        )

    private fun remakeGroupListData() {
        groupList.clear()
        val cur = groupCursor
        var mov = cur.moveToFirst()
        var order = 1
        while (mov) {
            data = HashMap()
            data["group_id"] = cur.getString(0)
            data["group_order"] = Integer.toString(order)
            data["name"] = cur.getString(2)
            groupList.add(data)
            mov = cur.moveToNext()
            order++
        }
        cur.close()
        sortGroupListData()
    }

    companion object {
        const val SORT_ID = "id"
        const val SORT_TITLE = "title"
        const val SORT_INPUTDATE = "inputdate"
        private var manager: ListDataManager? = null
        fun getInstance(context: Context): ListDataManager? {
            if (manager == null) {
                manager = ListDataManager(context)
            }
            return manager
        }
    }
}