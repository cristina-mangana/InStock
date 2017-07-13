package com.example.android.instock;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.instock.data.ProductContract.ProductEntry;
import com.squareup.picasso.Picasso;

import java.text.NumberFormat;

public class DetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Identifier for the data loader
     */
    private static final int PRODUCT_LOADER = 0;

    /**
     * Content URI for the existing product
     */
    private Uri mProductUri;

    // Relevant views to modify in the activity
    private ImageView mProductImageView;
    private TextView mNameTextView;
    private TextView mReferenceTextView;
    private TextView mPriceTextView;
    private TextView mDiscountTextView;
    private TextView mStockTextView;
    private SwitchCompat mOrderedSwitch;
    private TextView mCommentsTextView;
    private TextView mSalesNumberTextView;
    private TextView mSalesMoneyTextView;

    // Current stock of the product
    private int stock = 0;

    // Current state of the order
    private int isOrdered = ProductEntry.IS_ORDERED_FALSE;

    private boolean mProductHasChanged = false;

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Use getIntent() and getData() to get the associated URI
        mProductUri = getIntent().getData();

        // Toolbar settings
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        // No title
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        // Add back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Find all relevant views
        mProductImageView = (ImageView) findViewById(R.id.product_image);
        mNameTextView = (TextView) findViewById(R.id.name);
        mReferenceTextView = (TextView) findViewById(R.id.reference);
        mPriceTextView = (TextView) findViewById(R.id.price);
        mDiscountTextView = (TextView) findViewById(R.id.discount);
        mStockTextView = (TextView) findViewById(R.id.stock);
        mOrderedSwitch = (SwitchCompat) findViewById(R.id.order_switch);
        mCommentsTextView = (TextView) findViewById(R.id.comments);
        mSalesNumberTextView = (TextView) findViewById(R.id.total_sales_number);
        mSalesMoneyTextView = (TextView) findViewById(R.id.total_sales_money);

        // Set OnClickListeners to the stock buttons
        Button mMinusStockButton = (Button) findViewById(R.id.minus_button);
        mMinusStockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (stock == 0) {
                    // Avoid invalid stocks
                    Toast.makeText(getApplicationContext(), getString(R.string.stock_toast),
                            Toast.LENGTH_SHORT).show();
                } else {
                    // Reduce stock by 1
                    stock--;
                    // Display the new stock
                    mStockTextView.setText(String.valueOf(stock));
                }
            }
        });
        Button mPlusStockButton = (Button) findViewById(R.id.plus_button);
        mPlusStockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Increase stock by 1
                stock++;
                // Display the new stock
                mStockTextView.setText(String.valueOf(stock));
            }
        });

        // Setup the switch that allows the user to select the ordered state of the product
        mOrderedSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mOrderedSwitch.isChecked()) {
                    isOrdered = ProductEntry.IS_ORDERED_TRUE;
                } else {
                    isOrdered = ProductEntry.IS_ORDERED_FALSE;
                }
            }
        });

        // Add an OnTouchListener to the views to notify if stock or ordered state have changed
        mMinusStockButton.setOnTouchListener(mTouchListener);
        mPlusStockButton.setOnTouchListener(mTouchListener);
        mOrderedSwitch.setOnTouchListener(mTouchListener);

        // Initialize a loader to read the data from the database and display the current
        // values in the activity
        getLoaderManager().initLoader(PRODUCT_LOADER, null, this);
    }

    // Fires when a configuration change occurs and fragment needs to save state
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // If the product has changed, update it in database
        if (mProductHasChanged) {
            updateProduct();
        }
        super.onSaveInstanceState(savedInstanceState);
    }

    // Hook up the back button
    @Override
    public void onBackPressed() {
        // If the product has changed, update it in database
        if (mProductHasChanged) {
            updateProduct();
        }
        super.onBackPressed();
    }

    // Handle back button on toolbar
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // Inflate toolbar menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail_toolbar_menu, menu);
        return true;
    }

    // Handle clicks on toolbar menu buttons
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_edit:
                // If the product has changed, update it in database before going out the current
                // activity
                if (mProductHasChanged) {
                    updateProduct();
                }
                // Open Editor Activity with the current product uri as data
                Intent intent = new Intent(DetailActivity.this, EditorActivity.class);
                intent.setData(mProductUri);
                startActivity(intent);
                return true;
            case R.id.action_order:
                // If the product has changed, update it in database before going out the current
                // activity
                if (mProductHasChanged) {
                    updateProduct();
                }
                // Send an email to the provider
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:")); // only email apps should handle this
                // Intent is only executed if there's an available mail app in the device
                if (emailIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(emailIntent);
                }
                return true;
            case R.id.action_delete:
                // Show delete confirmation dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.delete_confirmation);
                builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked the "Delete" button, so delete the product
                        deleteProduct();
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked the "Cancel" button, so dismiss the dialog
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    }
                });
                // Create and show the AlertDialog
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Perform the deletion of the product in the database.
     */
    private void deleteProduct() {
            int rowsDeleted = getContentResolver().delete(
                    mProductUri,        // the content URI
                    null,               // the column to select on
                    null                // the value to compare to
            );

            // Show a toast message depending on whether or not the deletion was successful
            if (rowsDeleted == 0) {
                // If there are no rows deleted, then there was an error with the deletion.
                Toast.makeText(this, getString(R.string.delete_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the deletion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.delete_successful),
                        Toast.LENGTH_SHORT).show();
            }

            // Close the activity
            finish();
        }

    /**
     * Update stock and/or ordered state of the product in the database.
     */
    private void updateProduct() {
        // Create a new map of values, where column names are the keys to update (stock
        // and ordered state)
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_STOCK, stock);
        values.put(ProductEntry.COLUMN_PRODUCT_ORDERED, isOrdered);
        int rowsUpdated = getContentResolver().update(
                mProductUri,        // the content URI
                values,             // the columns to update
                null,               // the column to select on
                null                // the value to compare to
        );
        // Show a toast message if the update was unsuccessful
        if (rowsUpdated == 0) {
            // If there are no rows updated, then there was an error with the update.
            Toast.makeText(this, getString(R.string.update_failed),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Define a projection that specifies which columns from the database we care about.
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_REFERENCE,
                ProductEntry.COLUMN_PRODUCT_STOCK,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_DISCOUNT,
                ProductEntry.COLUMN_PRODUCT_IMAGE,
                ProductEntry.COLUMN_PRODUCT_COMMENTS,
                ProductEntry.COLUMN_PRODUCT_ORDERED,
                ProductEntry.COLUMN_PRODUCT_SALES_NUMBER
        };

        // This loader will execute the ContentProvider's query method on a background thread.
        return new CursorLoader(this,           // Parent activity context
                mProductUri,                    // The content URI
                projection,                     // The columns to return
                null,                           // The columns for the WHERE clause
                null,                           // The values for the WHERE clause
                null);                          // The sort order)
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        if (cursor.moveToFirst()) {
            // Set the available data to the views
            mNameTextView.setText(cursor.getString(cursor.getColumnIndex(ProductEntry
                    .COLUMN_PRODUCT_NAME)));
            String referenceString = cursor.getString(cursor.getColumnIndex(ProductEntry
                    .COLUMN_PRODUCT_REFERENCE));
            if (!TextUtils.isEmpty(referenceString)) {
                mReferenceTextView.setText(referenceString);
            } else {
                mReferenceTextView.setText(getString(R.string.no_reference));
            }
            stock = cursor.getInt(cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_STOCK));
            mStockTextView.setText(String.valueOf(cursor.getInt(cursor.getColumnIndex(ProductEntry
                    .COLUMN_PRODUCT_STOCK))));
            double price = (double) cursor.getInt(cursor.getColumnIndex(ProductEntry
                            .COLUMN_PRODUCT_PRICE));
            // Transform from cents
            mPriceTextView.setText(NumberFormat.getCurrencyInstance().format(price / 100));
            String discount = String.valueOf(cursor.getInt(cursor
                    .getColumnIndex(ProductEntry.COLUMN_PRODUCT_DISCOUNT))) + "%";
            mDiscountTextView.setText(String.valueOf(discount));
            String productImageString = cursor.getString(cursor.getColumnIndex(ProductEntry
                    .COLUMN_PRODUCT_IMAGE));
            if (TextUtils.isEmpty(productImageString)) {
                mProductImageView.setImageResource(R.drawable.default_image);
            } else {
                Uri productImageUri = Uri.parse(productImageString);
                Picasso.with(this).load(productImageUri).into(mProductImageView);
                Picasso.with(this).load(productImageUri).into(mProductImageView);
            }
            isOrdered = cursor.getInt(cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_ORDERED));
            if (isOrdered == ProductEntry.IS_ORDERED_TRUE) {
                mOrderedSwitch.setChecked(true);
            } else {
                mOrderedSwitch.setChecked(false);
            }
            String commentsString = cursor.getString(cursor.getColumnIndex(ProductEntry
                    .COLUMN_PRODUCT_COMMENTS));
            if (!TextUtils.isEmpty(commentsString)) {
                mCommentsTextView.setText(commentsString);
            } else {
                mCommentsTextView.setText(getString(R.string.no_comments));
            }
            int salesNumber = cursor.getInt(cursor.getColumnIndex(ProductEntry
                    .COLUMN_PRODUCT_SALES_NUMBER));
            String salesNumberString = String.valueOf(salesNumber) + " " +
                    getString(R.string.sales_label);
            mSalesNumberTextView.setText(salesNumberString);
            double salesMoney = salesNumber * price;
            mSalesMoneyTextView.setText(NumberFormat.getCurrencyInstance().format(salesMoney / 100));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Clear all views
        mNameTextView.setText("");
        mReferenceTextView.setText("");
        mStockTextView.setText(getString(R.string.stock_hint));
        mPriceTextView.setText("");
        mDiscountTextView.setText("");
        mProductImageView.setImageResource(R.drawable.default_image);
        mOrderedSwitch.setChecked(false);
        mCommentsTextView.setText("");
        mSalesNumberTextView.setText("");
        mSalesMoneyTextView.setText("");
    }
}
