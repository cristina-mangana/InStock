package com.example.android.instock.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.android.instock.data.ProductContract.ProductEntry;

/**
 * Created by Cristina on 12/07/2017.
 * Database helper for InStock app. Manages database creation and version management.
 */

public class ProductDbHelper extends SQLiteOpenHelper {

    // Database version
    private static final int DATABASE_VERSION = 1;

    // Name of the database file
    private static final String DATABASE_NAME = "inventory.db";

    // SQL statement to create the products table
    private static final String SQL_CREATE_PRODUCTS_TABLE =
            "CREATE TABLE " + ProductEntry.TABLE_NAME + " (" +
                    ProductEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    ProductEntry.COLUMN_PRODUCT_NAME + " TEXT NOT NULL," +
                    ProductEntry.COLUMN_PRODUCT_REFERENCE + " TEXT," +
                    ProductEntry.COLUMN_PRODUCT_STOCK + " INTEGER NOT NULL DEFAULT 0," +
                    ProductEntry.COLUMN_PRODUCT_PRICE + " INTEGER NOT NULL DEFAULT 0," +
                    ProductEntry.COLUMN_PRODUCT_DISCOUNT + " INTEGER NOT NULL DEFAULT 0," +
                    ProductEntry.COLUMN_PRODUCT_IMAGE + " TEXT," +
                    ProductEntry.COLUMN_PRODUCT_COMMENTS + " TEXT," +
                    ProductEntry.COLUMN_PRODUCT_ORDERED + " INTEGER NOT NULL DEFAULT 0," +
                    ProductEntry.COLUMN_PRODUCT_SALES_NUMBER + " INTEGER);";

    /**
     * Constructs a new instance of {@link ProductDbHelper}.
     * @param context of the app
     */
    public ProductDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * This is called when the database is created for the first time.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Execute the SQL statement
        db.execSQL(SQL_CREATE_PRODUCTS_TABLE);
    }

    /**
     * This method is called when the database needs to be upgraded.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // The database is still at version 1, so there's nothing to do here.
    }
}
