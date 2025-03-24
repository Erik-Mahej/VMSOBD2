package com.example.vmsobd2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "OBD2CODES.db";
    private static final int DATABASE_VERSION = 6;

    private static final String createTable1 = "CREATE TABLE faultCodes (" +
                                                "code INTEGER PRIMARY KEY," +
                                                "description TEXT NOT NULL)";
    //DASHBOARD
    private static final String createTable2 = "CREATE TABLE obd_formulas (" +
                                                "pid TEXT PRIMARY KEY," +
                                                "hex_count INTEGER," +
                                                "formula TEXT)";

    private static final String createTable3 ="CREATE TABLE gauge_settings (" +
                                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                                "metric_name TEXT UNIQUE," +
                                                "unit TEXT," +
                                                "max_speed INTEGER)";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(createTable1);
        db.execSQL(createTable2);
        db.execSQL(createTable3);


        db.execSQL("INSERT INTO faultCodes (code, description) VALUES (300, 'Fault Code: P0300 - Random/multiple cylinder misfire detected')");
        db.execSQL("INSERT INTO faultCodes (code, description) VALUES (420, 'Fault Code: P0420 - Catalytic converter efficiency below threshold')");
        db.execSQL("INSERT INTO faultCodes (code, description) VALUES (171, 'Fault Code: P0171 - System too lean (Bank 1)')");

        db.execSQL("INSERT INTO obd_formulas (pid, hex_count, formula) VALUES ('410C', 2, '((A * 256) + B) / 4')");
        db.execSQL("INSERT INTO obd_formulas (pid, hex_count, formula) VALUES ('4105', 1, 'A - 40')");

        db.execSQL("INSERT INTO gauge_settings (metric_name, unit, max_speed) VALUES ('RPM', 'RPM', 6000)");
        db.execSQL("INSERT INTO gauge_settings (metric_name, unit, max_speed) VALUES ('Speed', 'km/h', 240)");
        db.execSQL("INSERT INTO gauge_settings (metric_name, unit, max_speed) VALUES ('Fuel Level', '%', 100)");
        db.execSQL("INSERT INTO gauge_settings (metric_name, unit, max_speed) VALUES ('Avg Consumption', 'L/100km', 20)");
        db.execSQL("INSERT INTO gauge_settings (metric_name, unit, max_speed) VALUES ('Current Consumption', 'L/100km', 20)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS faultCodes");
        db.execSQL("DROP TABLE IF EXISTS obd_formulas");
        db.execSQL("DROP TABLE IF EXISTS gauge_settings");
        onCreate(db);
    }


    public String getFaultDescription(int responseCode) {
        SQLiteDatabase db = this.getReadableDatabase();
        String description= "Unknown Fault Code: " + responseCode;
        Cursor cursor = db.rawQuery("SELECT description FROM faultCodes WHERE code = ?", new String[]{String.valueOf(responseCode)});

        if (cursor.moveToFirst()) {
            description = cursor.getString(0);
            cursor.close();
            return description;
        } else {
            cursor.close();
            return description;
        }
    }


    public ObdFormula getFormulaByPid(String pid) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT pid, hex_count, formula FROM obd_formulas WHERE pid = ?", new String[]{pid});

        if (cursor.moveToFirst()) {
            String code = cursor.getString(0);
            int hexCount = cursor.getInt(1);
            String formula = cursor.getString(2);
            cursor.close();
            return new ObdFormula(code, hexCount, formula);
        } else {
            cursor.close();
            return null;
        }
    }



}