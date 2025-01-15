package com.kenvix.rconmanager.database.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;

public final class ConnectionsModel extends BaseModel {
    public static final String FieldServerID   = "sid";
    public static final String FieldPreFilledCommand  = "prefilledcommand";
    public static final String FieldResult  = "result";
    public static final String FieldHistory = "history";

    private static final String delimiter = ",";

    public ConnectionsModel(Context context) {
        super(context);
    }

    public String buildStringFromArray(String[] stringArray) {
        // Convert string array to a single string (e.g., using a delimiter)
        String arrayAsString = TextUtils.join(delimiter, stringArray);
        return arrayAsString;
    }

    public ArrayList<String> decodeToStringArray(String arrayAsString) {
         // Split the string back into an array
         String[] stringArray = arrayAsString.split(delimiter);
         ArrayList<String> returnArrayList  = new ArrayList<String>(Arrays.asList(stringArray));
         return returnArrayList;
    }

    public void add(int sid, String command, String result, String[] history) throws SQLException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(FieldServerID, sid);
        contentValues.put(FieldPreFilledCommand, command);
        contentValues.put(FieldResult, result);
        contentValues.put(FieldHistory, buildStringFromArray(history));
        insert(contentValues);
    }

    public int updatePreFilledCommandBySid(int sid, String command) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(FieldPreFilledCommand, command);
        return update(contentValues, FieldServerID + " = ?", getSignleWhereValue(sid));
    }

    public int updateResultBySid(int sid, String result) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(FieldResult, result);
        return update(contentValues, FieldServerID + " = ?", getSignleWhereValue(sid));
    }

    public int updateHistoryBySid(int sid, String[] history) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(FieldHistory, buildStringFromArray(history));
        return update(contentValues, FieldServerID + " = ?", getSignleWhereValue(sid));
    }

    public int updateBySid(int sid, String command, String result, String[] history) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(FieldPreFilledCommand, command);
        contentValues.put(FieldResult, result);
        contentValues.put(FieldHistory, buildStringFromArray(history));
        return update(contentValues, FieldServerID + " = ?", getSignleWhereValue(sid));
    }

    public Cursor getBySid(int sid) {
        return selectOne(FieldServerID + " = ?", getSignleWhereValue(sid));
    }

    public void deleteBySid(int sid) throws SQLException {
        delete("sid = ?", getSignleWhereValue(sid));
    }


}
