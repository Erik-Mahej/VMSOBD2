package com.example.vmsobd2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "OBD2CODES.db";
    private static final int DATABASE_VERSION = 3;

    private static final String FAULT_CODES_TABLE = "FaultCodes";
    private static final String FORMULAS_TABLE = "Formulas";
    private static final String CODE_COL = "code";
    private static final String RESPONSE_CODE_COL = "respcodes";
    private static final String DESCRIPTION_COL = "description";
    private static final String FORMULAS = "formulas";
    private static final String HEXCOUNT = "hexcount";

    private static final String SQL_CREATE_FAULT_CODES_TABLE =
            "CREATE TABLE " + FAULT_CODES_TABLE + " (" +
                    CODE_COL + " INTEGER PRIMARY KEY," +
                    DESCRIPTION_COL + " TEXT NOT NULL)";



    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_FAULT_CODES_TABLE);

        //DASHBOARD
        String createTable = "CREATE TABLE obd_formulas (" +
                "pid TEXT PRIMARY KEY," +
                "hex_count INTEGER," +
                "formula TEXT)";
        db.execSQL(createTable);

        db.execSQL("INSERT INTO obd_formulas (pid, hex_count, formula) VALUES ('410C', 2, '((A * 256) + B) / 4')");
        db.execSQL("INSERT INTO obd_formulas (pid, hex_count, formula) VALUES ('4105', 1, 'A - 40')");



        insertInitialFaultCodes(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + FAULT_CODES_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + FORMULAS_TABLE);
        onCreate(db);
    }

    private void insertInitialFaultCodes(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + FAULT_CODES_TABLE, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int count = cursor.getInt(0);
            cursor.close();

            if (count == 0) {
                ContentValues values = new ContentValues();
                values.put(CODE_COL, 300);
                values.put(DESCRIPTION_COL, "Fault Code: P0300 - Random/multiple cylinder misfire detected");
                db.insert(FAULT_CODES_TABLE, null, values);

                values.clear();
                values.put(CODE_COL, 420);
                values.put(DESCRIPTION_COL, "Fault Code: P0420 - Catalytic converter efficiency below threshold");
                db.insert(FAULT_CODES_TABLE, null, values);

                values.clear();
                values.put(CODE_COL, 171);
                values.put(DESCRIPTION_COL, "Fault Code: P0171 - System too lean (Bank 1)");
                db.insert(FAULT_CODES_TABLE, null, values);
            }
        }
    }

    private void insertInitialFormulas(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + FORMULAS_TABLE, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int count = cursor.getInt(0);
            cursor.close();

            if (count == 0) {
                ContentValues values = new ContentValues();
                values.put(RESPONSE_CODE_COL, "410C");
                values.put(HEXCOUNT, 2);
                values.put(FORMULAS, "(256*A+B)/4");
                db.insert(FORMULAS_TABLE, null, values);
            }
        }
    }

    public String getFaultDescription(int responseCode) {
        SQLiteDatabase db = this.getReadableDatabase();
        String description = "Unknown Fault Code: " + responseCode;

        Cursor cursor = null;
        try {
            cursor = db.query(FAULT_CODES_TABLE, new String[]{DESCRIPTION_COL}, CODE_COL + "=?",
                    new String[]{String.valueOf(responseCode)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(DESCRIPTION_COL);
                if (index != -1) {
                    description = cursor.getString(index);
                } else {
                    Log.e("DatabaseError", "Column not found: " + DESCRIPTION_COL);
                }
            } else {
                Log.w("DatabaseWarning", "No rows found for response code: " + responseCode);
            }
        } catch (Exception e) {
            Log.e("DatabaseError", "Error retrieving fault description", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return description;
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