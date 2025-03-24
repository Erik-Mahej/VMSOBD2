package com.example.vmsobd2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "OBD2CODES.db";
    private static final int DATABASE_VERSION = 1;

    private static final String FIRST_TABLE_NAME = "FaultCodes";
    private static final String SECOND_TABLE_NAME = "FaultCodes";
    private static final String CODE_COL = "code";
    private static final String RESPONSE_CODE_COL = "respcodes";
    private static final String DESCRIPTION_COL = "description";
    private static final String FORMULAS = "formulas";
    private static final String HEXCOUNT = "hexcount";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + FIRST_TABLE_NAME + " (" +
                    CODE_COL + " INTEGER PRIMARY KEY," +
                    DESCRIPTION_COL + " TEXT NOT NULL)";
    private static final String SQL_CREATE_SECOND_ENTRIES =
            "CREATE TABLE " + SECOND_TABLE_NAME + " (" +
                    RESPONSE_CODE_COL + " TEXT PRIMARY KEY," +
                    HEXCOUNT + " INTEGER NOT NULL," +
                    FORMULAS + " TEXT NOT NULL)";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Vytvoreni tabulek
        db.execSQL(SQL_CREATE_ENTRIES);
        db.execSQL(SQL_CREATE_SECOND_ENTRIES);

        // Insert prvnich hodnot
        insertInitialFaultCodes(db);
        insertInitialFormulas(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + FIRST_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + SECOND_TABLE_NAME);
        onCreate(db);
    }

    private void insertInitialFaultCodes(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + FIRST_TABLE_NAME, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int count = cursor.getInt(0);
            cursor.close();

            // Vlozeni dat jen pokud je tabulka prazdna
            if (count == 0) {
                ContentValues values = new ContentValues();

                // Insert P0300
                values.put(CODE_COL, 300);
                values.put(DESCRIPTION_COL, "Fault Code: P0300 - Random/multiple cylinder misfire detected");
                db.insert(FIRST_TABLE_NAME, null, values);

                // Insert P0420
                values.clear();
                values.put(CODE_COL, 420);
                values.put(DESCRIPTION_COL, "Fault Code: P0420 - Catalytic converter efficiency below threshold");
                db.insert(FIRST_TABLE_NAME, null, values);

                // Insert P0171
                values.clear();
                values.put(CODE_COL, 171);
                values.put(DESCRIPTION_COL, "Fault Code: P0171 - System too lean (Bank 1)");
                db.insert(FIRST_TABLE_NAME, null, values);
            }
        }
    }
        private void insertInitialFormulas(SQLiteDatabase db){
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + SECOND_TABLE_NAME, null);
            if (cursor != null) {
                cursor.moveToFirst();
                int count = cursor.getInt(0);
                cursor.close();

                // Vlozeni dat jen pokud je tabulka prazdna
                if (count == 0) {
                    ContentValues values = new ContentValues();

                    // Insert 410C
                    values.put(RESPONSE_CODE_COL, "410C");//kod odpovedi
                    values.put(HEXCOUNT, 2);//pocet hex cisel ktere dostavame
                    values.put(FORMULAS, "(256*A+B)/4"); //vzorec
                    db.insert(FIRST_TABLE_NAME, null, values);

                }
            }
        }


    public String getFaultDescription(int responseCode) {
        SQLiteDatabase db = this.getReadableDatabase();
        String description = "Unknown Fault Code: " + Integer.toHexString(responseCode).toUpperCase();

        Cursor cursor = db.query(FIRST_TABLE_NAME, new String[]{DESCRIPTION_COL}, CODE_COL + "=?",
                new String[]{String.valueOf(responseCode)}, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(DESCRIPTION_COL);
                if (index != -1) {
                    description = cursor.getString(index);
                } else {
                    Log.e("DatabaseError", "Column not found: " + DESCRIPTION_COL);
                }
            }
            cursor.close();
        }

        return description;
    }
    public int getCodeHexCount(String responseCode) {
        SQLiteDatabase db = this.getReadableDatabase();
        int RESPHEXCOUNT=0;
        Cursor cursor = db.query(SECOND_TABLE_NAME, new String[]{HEXCOUNT}, RESPONSE_CODE_COL + "=?",
                new String[]{String.valueOf(responseCode)}, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(HEXCOUNT);
                if (index != -1) {
                    RESPHEXCOUNT = cursor.getInt(index);
                } else {
                    Log.e("DatabaseError", "Column not found: " + HEXCOUNT);
                }
            }
            cursor.close();
        }

        return RESPHEXCOUNT;
    }
    public String getCodeFormula(String responseCode) {
        SQLiteDatabase db = this.getReadableDatabase();
        String formula = "0";

        Cursor cursor = db.query(SECOND_TABLE_NAME, new String[]{FORMULAS}, RESPONSE_CODE_COL + "=?",
                new String[]{String.valueOf(responseCode)}, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(FORMULAS);
                if (index != -1) {
                    formula = cursor.getString(index);
                } else {
                    Log.e("DatabaseError", "Column not found: " + FORMULAS);
                }
            }
            cursor.close();
        }

        return formula;
    }
}
