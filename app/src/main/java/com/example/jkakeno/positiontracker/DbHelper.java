package com.example.jkakeno.positiontracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;
import java.util.ArrayList;

/*This class handles the writting and queueing of the data base.
The database contains records of timestamp and coordinates where the user has been.*/

public class DbHelper extends SQLiteOpenHelper {

    private static final String TAG = DbHelper.class.getSimpleName();
    private static final String DB_NAME="MyDataBase";
    private static final int DB_VER = 1;
    public static final String DB_TABLE="Location";
    public static final String DB_TIMESTAMP = "Timestamp";
    public static final String DB_LATITUDE = "Latitude";
    public static final String DB_LONGITUDE = "Longitude";


    public DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VER);
    }


//Create the db
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate db");
        String query = "CREATE TABLE " + DB_TABLE + "(" + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + DB_TIMESTAMP + " INT," + DB_LATITUDE + " REAL," + DB_LONGITUDE + " REAL);";
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpdate db");
        String query = "DELETE TABLE IF EXISTS " + DB_TABLE;
        db.execSQL(query);
        onCreate(db);
    }

//Insert position to db
    public void insertPosition(long timeStamp, double latitude, double longitude){
        Log.d(TAG, "insertPosition db");
        SQLiteDatabase db= this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DB_TIMESTAMP, timeStamp);
        values.put(DB_LATITUDE, latitude);
        values.put(DB_LONGITUDE, longitude);
        db.insertWithOnConflict(DB_TABLE,null,values,SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }


//Get the period from LocationService and query the db to get the list of positions in the db
    public ArrayList<Position> getPositionList(){
        Log.d(TAG, "getPositionList db");
        ArrayList<Position> positionList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
//        String query = "SELECT * FROM " + DB_TABLE + " WHERE " + DB_TIMESTAMP + " BETWEEN " + timemin + " AND " + timemax;
        String query = "SELECT * FROM " + DB_TABLE;
        Cursor cursor = db.rawQuery(query,null);
        while(cursor.moveToNext()){
            long timestamp = cursor.getLong(cursor.getColumnIndex(DB_TIMESTAMP));
            double latitude = cursor.getDouble(cursor.getColumnIndex(DB_LATITUDE));
            double longitude = cursor.getDouble(cursor.getColumnIndex(DB_LONGITUDE));
            positionList.add(new Position(timestamp,latitude,longitude));
        }
        cursor.close();
        db.close();
        return positionList;
    }
}
