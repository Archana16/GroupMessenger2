package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by archana on 2/15/15.
 */
public class MyDataBase extends SQLiteOpenHelper {
   public static String TABLE_NAME;

    MyDataBase(Context context,String name){
        super(context, "PA2", null,3);
        this.TABLE_NAME = name;
    }
    @Override
    public void onCreate(SQLiteDatabase sqlDB)
    {
        sqlDB.execSQL("CREATE TABLE "+TABLE_NAME+"(key TEXT UNIQUE, value TEXT NOT NULL);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion,
                          int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);
        onCreate(db);
    }

}
