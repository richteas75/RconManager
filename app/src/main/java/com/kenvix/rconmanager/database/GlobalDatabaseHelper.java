package com.kenvix.rconmanager.database;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
//import android.support.annotation.Nullable;
import androidx.annotation.Nullable;
import android.util.Log;

public final class GlobalDatabaseHelper extends SQLiteOpenHelper {
    static final int DatabaseVersion = 3;
    static final String DatabaseFileName = "database.sqlite3";
    public Context context;

    public GlobalDatabaseHelper(@Nullable Context context) {
        super(context, DatabaseFileName, null, DatabaseVersion);
        this.context = context;
    }

    public GlobalDatabaseHelper(@Nullable Context context, @Nullable SQLiteDatabase.CursorFactory factory) {
        super(context, DatabaseFileName, factory, DatabaseVersion);
        this.context = context;
    }

    public GlobalDatabaseHelper(@Nullable Context context, @Nullable DatabaseErrorHandler errorHandler) {
        super(context, DatabaseFileName, null, DatabaseVersion, errorHandler);
        this.context = context;
    }

    public GlobalDatabaseHelper(@Nullable Context context, @Nullable SQLiteDatabase.CursorFactory factory, @Nullable DatabaseErrorHandler errorHandler) {
        super(context, DatabaseFileName, factory, DatabaseVersion, errorHandler);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i("Global Database Helper", "Received Database Create Request");
        db.beginTransaction();

        try {
            db.execSQL(IntegratedSQLCommands.getCreateServerListTableSQL());
            db.execSQL(IntegratedSQLCommands.getCreateQuickCommandsTableSQL());
            db.execSQL(IntegratedSQLCommands.getCreateConnectionTableSQL());

            db.setTransactionSuccessful();
        } catch (Exception ex) {
            Log.e("Global Database Helper", "Create cirtial table failed. Operation canceled ...");
            throw ex;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.beginTransaction();
        try {
            switch(oldVersion) {
                // incremental upgrades, therefore missing break statements
                case 1: //upgrade logic from version 1 to 2
                case 2: //upgrade logic from version 2 to 3
                    db.execSQL(IntegratedSQLCommands.getCreateConnectionTableSQL());
                    db.setTransactionSuccessful();
                    break;
            }
        } catch (Exception ex) {
            Log.e("Global Database Helper", "Adding critical table failed. Operation canceled ...");
            throw ex;
        } finally {
            db.endTransaction();
        }
    }
}
