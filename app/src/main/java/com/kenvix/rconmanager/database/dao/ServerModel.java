package com.kenvix.rconmanager.database.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;

public class ServerModel extends BaseModel {
    public static final String FieldSid  = "sid";
    public static final String FieldName = "name";
    public static final String FieldHost = "host";
    public static final String FieldPort = "port";
    public static final String FieldPassword = "password";

    public ServerModel(Context context) {
        super(context);
    }

    public Cursor getAll() {
        return select(null, null);
    }

    public void add(String name, String host, int port, String password) throws SQLException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(FieldName, name);
        contentValues.put(FieldHost, host);
        contentValues.put(FieldPort, port);
        contentValues.put(FieldPassword, password);

        insert(contentValues);
    }

    public Cursor getBySid(int sid) {
        return selectOne("sid = ?", getSignleWhereValue(sid));
    }

    public void deleteBySid(int sid) throws SQLException {
        delete("sid = ?", getSignleWhereValue(sid));
    }

    public void updateBySid(int sid, ContentValues values) throws SQLException {
        update(values, "sid = ?", getSignleWhereValue(sid));
    }
}