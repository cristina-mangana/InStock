package com.example.android.instock;

import android.Manifest;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.android.instock.data.ProductContract.ProductEntry;

public class MainActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Identifier for the data loader
     */
    private static final int PRODUCT_LOADER = 0;

    private ProductCursorAdapter mCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check for permissions at runtime in version > Marshmallow
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {

                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                }
            }
        }

        // Toolbar settings
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        // Set title
        getSupportActionBar().setTitle(getString(R.string.inventory));

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.add_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        // Find the ListView which will be populated with the products data
        ListView productsListView = (ListView) findViewById(R.id.list);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items
        View emptyView = findViewById(R.id.empty_view);
        productsListView.setEmptyView(emptyView);

        // Setup an Adapter to create an empty list of products in the Cursor.
        mCursorAdapter = new ProductCursorAdapter(this, null);

        // Attach the adapter to the ListView.
        productsListView.setAdapter(mCursorAdapter);

        // Setup item click listener
        productsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Create new intent to go to DetailActivity
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);

                // Form the content URI that represents the specific product that was clicked on
                // by appending the id.
                Uri productUri = ContentUris.withAppendedId (ProductEntry.CONTENT_URI, id);

                // Set the URI on the data field of the intent
                intent.setData(productUri);

                // Launch the activity
                startActivity(intent);
            }
        });

        // Kick off the loader
        getLoaderManager().initLoader(PRODUCT_LOADER, null, this);
    }

    // This is called when a new Loader needs to be created.
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Define a projection that specifies which columns from the database we care about.
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_STOCK,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_DISCOUNT,
                ProductEntry.COLUMN_PRODUCT_IMAGE,
                ProductEntry.COLUMN_PRODUCT_SALES_NUMBER
        };

        // This loader will execute the ContentProvider's query method on a background thread.
        return new CursorLoader(this,           // Parent activity context
                ProductEntry.CONTENT_URI,       // The content URI
                projection,                     // The columns to return
                null,                           // The columns for the WHERE clause
                null,                           // The values for the WHERE clause
                null);                          // The sort order)
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Update the adapter with a new cursor containing the updated products data
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Callback called when the data needs to be deleted
        mCursorAdapter.swapCursor(null);
    }
}
