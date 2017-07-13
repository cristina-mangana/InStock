package com.example.android.instock.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.instock.data.ProductContract.ProductEntry;

/**
 * Created by Cristina on 12/07/2017.
 * {@link ContentProvider} for InStock app.
 */

public class ProductProvider extends ContentProvider {

    /**
     * Tag for the log messages
     */
    public static final String LOG_TAG = ProductProvider.class.getSimpleName();

    /** URI matcher code for the content URI for the products table */
    public static final int PRODUCTS = 100;

    /** URI matcher code for the content URI for a single product in the products table */
    public static final int PRODUCT_ID = 101;

    /** URI matcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The content URI of the form "content://com.example.android.instock/products" will map to
        // the integer code {@link #PRODUCTS}. This URI is used to provide access to MULTIPLE rows
        // of the products table.
        sUriMatcher.addURI(ProductContract.CONTENT_AUTHORITY, ProductContract.PATH_PRODUCTS,
                PRODUCTS);

        // The content URI of the form "content://com.example.android.instock/products/#" will map
        // to the integer code {@link #PRODUCT_ID}. This URI is used to provide access to ONE row
        // of the products table.
        sUriMatcher.addURI(ProductContract.CONTENT_AUTHORITY, ProductContract.PATH_PRODUCTS + "/#",
                PRODUCT_ID);
    }

    /**
     * Database helper that will provide access to the database
     */
    private ProductDbHelper mDbHelper;

    /**
     * Initialize the provider and the database helper object.
     */
    @Override
    public boolean onCreate() {
        // Create and initialize a ProductDbHelper object to gain access to the database.
        mDbHelper = new ProductDbHelper(getContext());
        return true;
    }

    /**
     * Perform the query for the given URI. Use the given projection, selection, selection
     * arguments, and sort order.
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                // Query the whole products table with the given projection, selection, selection
                // arguments, and sort order
                cursor = database.query(ProductEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case PRODUCT_ID:
                // Extract out the ID from the URI
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                // Perform a query on the products table to return a cursor containing a single row
                // of the table.
                cursor = database.query(ProductEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI on the Cursor to know the need to update the cursor when modified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return the cursor
        return cursor;
    }

    /**
     * Returns the MIME type of data for the content URI.
     */
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return ProductEntry.CONTENT_LIST_TYPE;
            case PRODUCT_ID:
                return ProductEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    /**
     * Insert new data into the provider with the given ContentValues.
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return insertProduct(uri, values);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert a product into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    @Nullable
    private Uri insertProduct(Uri uri, ContentValues values) {
        // Check that the name is not null or empty string
        String name = values.getAsString(ProductEntry.COLUMN_PRODUCT_NAME);
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("A name is required");
        }

        // If the stock is provided, check that it's greater than or equal to 0
        Integer stock = values.getAsInteger(ProductEntry.COLUMN_PRODUCT_STOCK);
        if (stock != null && stock < 0) {
            throw new IllegalArgumentException("Invalid stock");
        }

        // If the price is provided, check that it's greater than or equal to 0
        Integer price = values.getAsInteger(ProductEntry.COLUMN_PRODUCT_PRICE);
        if (price != null && price < 0) {
            throw new IllegalArgumentException("Invalid price");
        }

        // If the discount is provided, check that it's greater than or equal to 0
        Integer discount = values.getAsInteger(ProductEntry.COLUMN_PRODUCT_DISCOUNT);
        if (discount != null && discount < 0) {
            throw new IllegalArgumentException("Invalid discount");
        }

        // Get the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Insert the new row, returning the ID of the new row
        long id = db.insert(ProductEntry.TABLE_NAME, null, values);

        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that the data has changed for the content URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Return the URI with the ID of the inserted row appended to its end
        return ContentUris.withAppendedId(uri, id);
    }

    /**
     * Delete the data at the given selection and selection arguments.
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        final int match = sUriMatcher.match(uri);
        // Track the number of rows that were deleted
        int rowsDeleted = 0;
        switch (match) {
            case PRODUCTS:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(ProductEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case PRODUCT_ID:
                // Delete a single row given by the ID in the URI
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(ProductEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows deleted
        return rowsDeleted;
    }

    /**
     * Updates the data at the given selection and selection arguments, with the new ContentValues.
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return updateProduct(uri, values, selection, selectionArgs);
            case PRODUCT_ID:
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updateProduct(uri, values, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update products in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more products).
     * Return the number of rows that were successfully updated.
     */
    private int updateProduct(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }

        // If the key is present, check that the name is not null or empty string
        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_NAME)) {
            String name = values.getAsString(ProductEntry.COLUMN_PRODUCT_NAME);
            if (name == null || name.length() == 0) {
                throw new IllegalArgumentException("A name is required");
            }
        }

        // If the key is present and the stock is provided, check that it's greater than or equal
        // to 0
        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_STOCK)) {
            Integer stock = values.getAsInteger(ProductEntry.COLUMN_PRODUCT_STOCK);
            if (stock != null && stock < 0) {
                throw new IllegalArgumentException("Invalid stock");
            }
        }

        // If the key is present and the price is provided, check that it's greater than or equal
        // to 0
        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_PRICE)) {
            Integer price = values.getAsInteger(ProductEntry.COLUMN_PRODUCT_PRICE);
            if (price != null && price < 0) {
                throw new IllegalArgumentException("Invalid price");
            }
        }

        // If the key is present and the discount is provided, check that it's greater than or equal
        // to 0
        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_DISCOUNT)) {
            Integer discount = values.getAsInteger(ProductEntry.COLUMN_PRODUCT_DISCOUNT);
            if (discount != null && discount < 0) {
                throw new IllegalArgumentException("Invalid discount");
            }
        }

        // Get the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = db.update(ProductEntry.TABLE_NAME, values, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Returns the number of database rows affected by the update statement
        return rowsUpdated;
    }
}
