package com.highcom.passwordmemo.database;

import android.content.ContentValues;
import android.content.Context;

import com.highcom.passwordmemo.R;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ListDataManager {
    public static final String SORT_ID = "id";
    public static final String SORT_TITLE = "title";
    public static final String SORT_INPUTDATE = "inputdate";

    private static ListDataManager manager;
    private Context context;

    private long groupId = 1;
    private SQLiteDatabase rdb;
    private SQLiteDatabase wdb;
    private Map<String, String> data;
    private List<Map<String, String>> dataList;
    private Map<String, String> groupData;
    private List<Map<String, String>> groupList;
    private String sortKey;

    private ListDataManager(Context context) {
        this.context = context;
        SQLiteDatabase.loadLibs(context);
        sortKey = "id";
        ListDataOpenHelper helper = new ListDataOpenHelper(context);
        // onUpgradeを呼び出すために先にWritableDatabaseを先に呼び出す
        wdb = helper.getWritableDatabase(context.getString(R.string.db_secret_key));
        rdb = helper.getReadableDatabase(context.getString(R.string.db_secret_key));

        Cursor cur = getCursor();
        dataList = new ArrayList<Map<String, String>>();

        boolean mov = cur.moveToFirst();
        while (mov) {
            data = new HashMap<String, String>();
            data.put("id", cur.getString(0));
            data.put("title", cur.getString(1));
            data.put("account", cur.getString(2));
            data.put("password", cur.getString(3));
            data.put("url", cur.getString(4));
            data.put("group_id", cur.getString(5));
            data.put("memo", cur.getString(6));
            data.put("inputdate", cur.getString(7));
            dataList.add(data);
            mov = cur.moveToNext();
        }
        cur.close();

        sortListData(sortKey);

        groupList = new ArrayList<Map<String, String>>();
        Cursor gcur = getGroupCursor();
        boolean gmov = gcur.moveToFirst();

        while (gmov) {
            groupData = new HashMap<String, String>();
            groupData.put("group_id", gcur.getString(0));
            groupData.put("group_order", gcur.getString(1));
            groupData.put("name", gcur.getString(2));
            groupList.add(groupData);
            gmov = gcur.moveToNext();
        }
        // グループデータがない場合はデフォルトデータとして「すべて」を必ず追加」
        if (groupList.size() == 0) {
            groupData = new HashMap<String, String>();
            groupData.put("group_id", "1");
            groupData.put("group_order", "1");
            groupData.put("name", context.getString(R.string.list_title));
            setGroupData(false, groupData);
        }
        gcur.close();

        sortGroupListData();
    }

    public static ListDataManager getInstance(Context context) {
        if (manager == null) {
            manager = new ListDataManager(context);
        }
        return manager;
    }

    public void setSelectGroupId(long id) {
        groupId = id;
        remakeListData();
    }

    public SQLiteDatabase getRdb() {
        return rdb;
    }

    public SQLiteDatabase getWdb() {
        return wdb;
    }

    public void setData(boolean isEdit, Map<String, String> data) {
        // データベースに追加or編集する
        ContentValues values = new ContentValues();
        values.put("id", Long.valueOf(data.get("id")).longValue());
        values.put("title", data.get("title"));
        values.put("account", data.get("account"));
        values.put("password", data.get("password"));
        values.put("url", data.get("url"));
        values.put("group_id", Long.valueOf(data.get("group_id")).longValue());
        values.put("memo", data.get("memo"));
        values.put("inputdate", data.get("inputdate"));
        if (isEdit) {
            // 編集の場合
            wdb.update("passworddata", values, "id=?", new String[] { data.get("id") });
            remakeListData();
        } else {
            // 新規作成の場合
            wdb.insert("passworddata", data.get("id"), values);
            dataList.add(data);
        }
    }

    public void deleteData(String id) {
        // データベースから削除する
        wdb.delete("passworddata", "id=?", new String[]{id});
        remakeListData();
    }

    public void deleteAllData() {
        wdb.delete("passworddata", null, null);
        dataList.clear();
        wdb.delete("groupdata", null, null);
        groupList.clear();
        // グループデータがない場合はデフォルトデータとして「すべて」を必ず追加」
        groupList.size();
        groupData = new HashMap<String, String>();
        groupData.put("group_id", "1");
        groupData.put("group_order", "1");
        groupData.put("name", context.getString(R.string.list_title));
        setGroupData(false, groupData);
    }

    public List<Map<String, String>> getDataList() {
        return dataList;
    }

    public void rearrangeData(int fromPos, int toPos) {
        boolean mov;
        ContentValues values;

        Cursor cur = getCursor();

        mov = cur.moveToPosition(fromPos);
        if (!mov) return;
        long fromId = cur.getLong(0);

        mov = cur.moveToPosition(toPos);
        if (!mov) return;
        long toId = cur.getLong(0);

        values = new ContentValues();
        values.put("id", -1);
        wdb.update("passworddata", values, "id=?", new String[] { Long.toString(fromId) });

        values = new ContentValues();
        values.put("id", fromId);
        wdb.update("passworddata", values, "id=?", new String[] { Long.toString(toId) });

        values = new ContentValues();
        values.put("id", toId);
        wdb.update("passworddata", values, "id=?", new String[] { Long.toString(-1) });

        cur.close();

        remakeListData();
    }

    public long getNewId() {
        long newId = 0;
        Cursor cur = rdb.query("passworddata", new String[] { "id", "title", "account", "password", "url", "group_id", "memo", "inputdate" }, null, null, null, null, "id ASC", null);

        boolean mov = cur.moveToFirst();
        long curId;
        while (mov) {
            curId = Long.valueOf(cur.getString(0)).longValue();
            if (newId < curId) {
                newId = curId;
            }
            mov = cur.moveToNext();
        }
        cur.close();

        return newId + 1;
    }

    public void closeData() {
        rdb.close();
        wdb.close();
        manager = null;
    }

    private void remakeListData() {
        dataList.clear();

        Cursor cur = getCursor();

        boolean mov = cur.moveToFirst();
        while (mov) {
            data = new HashMap<String, String>();
            data.put("id", cur.getString(0));
            data.put("title", cur.getString(1));
            data.put("account", cur.getString(2));
            data.put("password", cur.getString(3));
            data.put("url", cur.getString(4));
            data.put("group_id", cur.getString(5));
            data.put("memo", cur.getString(6));
            data.put("inputdate", cur.getString(7));
            dataList.add(data);
            mov = cur.moveToNext();
        }
        cur.close();

        sortListData(sortKey);
    }

    public void sortListData(final String key) {
        sortKey = key;
        Collections.sort(dataList, new Comparator<Map<String, String>>() {
            @Override
            public int compare(Map<String, String> stringStringMap, Map<String, String> t1) {
                int result;
                if (Objects.equals(sortKey, SORT_TITLE)) {
                    result = stringStringMap.get(sortKey).compareTo(t1.get(sortKey));
                } else if (Objects.equals(sortKey, SORT_INPUTDATE)) {
                    result = t1.get(sortKey).compareTo(stringStringMap.get(sortKey));
                } else {
                    result = Integer.valueOf(stringStringMap.get("id")).compareTo(Integer.valueOf(t1.get("id")));
                }

                if (result == 0) {
                    result = Integer.valueOf(stringStringMap.get("id")).compareTo(Integer.valueOf(t1.get("id")));
                }
                return result;
            }
        });
    }

    public void resetGroupIdData(Long groupId) {
        ContentValues values = new ContentValues();
        values.put("group_id", 1L);
        wdb.update("passworddata", values, "group_id=?", new String[] { groupId.toString() });
        remakeListData();
    }

    private Cursor getCursor() {
        String selection = null;
        if (groupId != 1) {
            selection = "group_id = " + groupId;
        }
        return rdb.query("passworddata", new String[] { "id", "title", "account", "password", "url", "group_id", "memo", "inputdate" }, selection, null, null, null, "id ASC", null);
    }

    public List<Map<String, String>> getGroupList() {
        return groupList;
    }

    public void rearrangeGroupData(int fromPos, int toPos) {
        boolean mov;
        ContentValues values;

        Cursor cur = getGroupCursor();

        mov = cur.moveToPosition(fromPos);
        if (!mov) return;
        int fromOrder = cur.getInt(1);

        mov = cur.moveToPosition(toPos);
        if (!mov) return;
        int toOrder = cur.getInt(1);

        values = new ContentValues();
        values.put("group_order", -1);
        wdb.update("groupdata", values, "group_order=?", new String[] { Integer.toString(fromOrder) });

        values = new ContentValues();
        values.put("group_order", fromOrder);
        wdb.update("groupdata", values, "group_order=?", new String[] { Integer.toString(toOrder) });

        values = new ContentValues();
        values.put("group_order", toOrder);
        wdb.update("groupdata", values, "group_order=?", new String[] { Integer.toString(-1) });

        cur.close();

        remakeGroupListData();
    }

    public long getNewGroupId() {
        long newId = 0;
        Cursor cur = getGroupCursor();

        boolean mov = cur.moveToFirst();
        long curId;
        while (mov) {
            curId = Long.valueOf(cur.getString(0)).longValue();
            if (newId < curId) {
                newId = curId;
            }
            mov = cur.moveToNext();
        }
        cur.close();

        return newId + 1;
    }

    public void setGroupData(boolean isEdit, Map<String, String> data) {
        // データベースに追加or編集する
        ContentValues values = new ContentValues();
        values.put("group_id", Long.valueOf(data.get("group_id")).longValue());
        values.put("group_order", Integer.valueOf(data.get("group_order")).intValue());
        values.put("name", data.get("name"));
        if (isEdit) {
            // 編集の場合
            wdb.update("groupdata", values, "group_id=?", new String[] { data.get("group_id") });
            remakeGroupListData();
        } else {
            // 新規作成の場合
            wdb.insert("groupdata", data.get("group_id"), values);
            groupList.add(data);
        }
    }

    public void deleteGroupData(String id) {
        // データベースから削除する
        wdb.delete("groupdata", "group_id=?", new String[]{id});
        remakeGroupListData();
    }

    public void sortGroupListData() {
        Collections.sort(groupList, new Comparator<Map<String, String>>() {
            @Override
            public int compare(Map<String, String> stringStringMap, Map<String, String> t1) {
                int result;
                result = Integer.valueOf(stringStringMap.get("group_order")).compareTo(Integer.valueOf(t1.get("group_order")));

                if (result == 0) {
                    result = Integer.valueOf(stringStringMap.get("group_id")).compareTo(Integer.valueOf(t1.get("group_id")));
                }
                return result;
            }
        });
    }

    private Cursor getGroupCursor() {
        return rdb.query("groupdata", new String[] { "group_id", "group_order", "name" }, null, null, null, null, "group_order ASC", null);
    }

    private void remakeGroupListData() {
        groupList.clear();

        Cursor cur = getGroupCursor();

        boolean mov = cur.moveToFirst();
        int order = 1;
        while (mov) {
            data = new HashMap<String, String>();
            data.put("group_id", cur.getString(0));
            data.put("group_order", Integer.toString(order));
            data.put("name", cur.getString(2));
            groupList.add(data);
            mov = cur.moveToNext();
            order++;
        }
        cur.close();

        sortGroupListData();
    }
}
