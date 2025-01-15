package com.kenvix.rconmanager.database;

final class IntegratedSQLCommands {
    static String getCreateServerListTableSQL() {
        return "CREATE TABLE IF NOT EXISTS \"main\".\"server\" (\n" +
                "  \"sid\" INTEGER NOT NULL ON CONFLICT FAIL PRIMARY KEY AUTOINCREMENT,\n" +
                "  \"name\" TEXT NOT NULL ON CONFLICT FAIL,\n" +
                "  \"host\" TEXT NOT NULL ON CONFLICT FAIL,\n" +
                "  \"port\" integer NOT NULL ON CONFLICT FAIL,\n" +
                "  \"password\" TEXT NOT NULL\n" +
                ");";
    }

    static String getCreateQuickCommandsTableSQL() {
        return "CREATE TABLE IF NOT EXISTS \"main\".\"quick_command\" (\n" +
                "  \"cid\" INTEGER NOT NULL ON CONFLICT FAIL PRIMARY KEY AUTOINCREMENT,\n" +
                "  \"name\" TEXT NOT NULL ON CONFLICT FAIL,\n" +
                "  \"value\" TEXT NOT NULL ON CONFLICT FAIL\n" +
                ");";
    }

    static String getCreateConnectionTableSQL() {
        return "CREATE TABLE IF NOT EXISTS \"main\".\"connections\" (\n" +
                "  \"sid\" INTEGER NOT NULL ON CONFLICT FAIL PRIMARY KEY,\n" +
                "  \"prefilledcommand\" TEXT NOT NULL ON CONFLICT FAIL,\n" +
                "  \"result\" TEXT NOT NULL ON CONFLICT FAIL,\n" +
                "  \"history\" TEXT NOT NULL ON CONFLICT FAIL\n" +
                ");";
    }

}
