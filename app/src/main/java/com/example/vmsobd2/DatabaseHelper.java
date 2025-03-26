package com.example.vmsobd2;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.List;
import java.util.ArrayList;


public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "OBD2CODES.db";
    private static final int DATABASE_VERSION = 30;

    private static final String createTable1 = "CREATE TABLE faultCodes (" +
                                                "code INTEGER PRIMARY KEY," +
                                                "description TEXT NOT NULL)";
    //DASHBOARD
    /*
    private static final String createTable2 = "CREATE TABLE obd_formulas (" +
                                                "pid TEXT PRIMARY KEY," +
                                                "hex_count INTEGER," +
                                                "formula TEXT)";

    private static final String createTable3 ="CREATE TABLE gauge_settings (" +
                                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                                "metric_name TEXT UNIQUE," +
                                                "unit TEXT," +
                                                "max_speed INTEGER)";

     */
    private static final String createTable4 ="CREATE TABLE obd_data (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "pid TEXT UNIQUE," +
            "hex_count INTEGER," +
            "formula TEXT," +
            "metric_name TEXT UNIQUE," +
            "unit TEXT," +
            "max_speed INTEGER)";
    private static final String createTablePIDs = "CREATE TABLE pids (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "pid TEXT UNIQUE NOT NULL)";




    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(createTable1);
        db.execSQL(createTable4);
        db.execSQL(createTablePIDs);


        db.execSQL("INSERT INTO faultCodes (code, description) VALUES (300, 'Fault Code: P0300 - Random/multiple cylinder misfire detected')");
        db.execSQL("INSERT INTO faultCodes (code, description) VALUES (420, 'Fault Code: P0420 - Catalytic converter efficiency below threshold')");
        db.execSQL("INSERT INTO faultCodes (code, description) VALUES (171, 'Fault Code: P0171 - System too lean (Bank 1)')");

        db.execSQL("INSERT INTO obd_data (id,pid, hex_count, formula,metric_name, unit, max_speed) VALUES (1,'410C', 2, '((A * 256) + B) / 4','RPM', 'RPM', 6000)");
        db.execSQL("INSERT INTO obd_data (id,pid, hex_count, formula,metric_name, unit, max_speed) VALUES (2,'410D', 1, 'A','SPEED', 'km/h', 240)");
        db.execSQL("INSERT INTO obd_data (id,pid, hex_count, formula,metric_name, unit, max_speed) VALUES (3,'4163', 2, 'A * 256 + B','ENGINE_REFERENCE_TORQUE', 'Nm', 600)");

        db.execSQL("INSERT INTO pids (pid) VALUES ('410C')");
        db.execSQL("INSERT INTO pids (pid) VALUES ('4163')");
        db.execSQL("INSERT INTO pids (pid) VALUES ('410D')");


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS faultCodes");
        db.execSQL("DROP TABLE IF EXISTS obd_data");
        db.execSQL("DROP TABLE IF EXISTS pids");

        onCreate(db);
    }


    public String getFaultDescription(int responseCode) {
        SQLiteDatabase db = this.getReadableDatabase();
        String description= "Unknown Fault Code: " + responseCode;
        Cursor cursor = db.rawQuery("SELECT description FROM faultCodes WHERE code = ?" , new String[]{String.valueOf(responseCode)});

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
        Cursor cursor = db.rawQuery("SELECT pid, hex_count, formula FROM obd_data WHERE pid = ?", new String[]{pid});

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

    public GaugeSetting getGaugeSetting(String metricName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT unit, max_speed FROM obd_data WHERE metric_name = ?", new String[]{metricName});
        if (cursor.moveToFirst()) {
            //String metricname2 = cursor.getString(0);
            String unit = cursor.getString(0);
            int maxspeed = cursor.getInt(1);
            cursor.close();
            return new GaugeSetting(metricName, unit, maxspeed);
        } else {
            cursor.close();
            return null;
        }
    }
    public List<String> getAllPIDs() {
        List<String> pidList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT pid FROM pids", null);

        if (cursor.moveToFirst()) {
            do {
                pidList.add(cursor.getString(0)); // Ensure adding as String
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return pidList;
    }

    public List<String> getAllMetricNamesSortByID() {
        List<String> pidList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT id,metric_name FROM obd_data ORDER BY id", null);

        if (cursor.moveToFirst()) {
            do {
                pidList.add(cursor.getString(1));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return pidList;
    }






}