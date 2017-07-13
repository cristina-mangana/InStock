package com.example.android.instock;

import android.Manifest;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.instock.data.ProductContract.ProductEntry;
import com.squareup.picasso.Picasso;

import java.util.Currency;
import java.util.Locale;

/**
 * Allows user to add a new product or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Identifier for the data loader
     */
    private static final int PRODUCT_LOADER = 0;

    /**
     * Identifier for the request image intent
     */
    static final int REQUEST_IMAGE_OPEN = 1;

    /**
     * Content URI for the existing product (null if it's a new product)
     */
    private Uri mProductUri;

    // Relevant views to modify in the activity
    private ImageView mProductImageView;
    private ImageView mAddImageButton;
    private EditText mNameEditText;
    private EditText mReferenceEditText;
    private TextView mStockTextView;
    private EditText mPriceEditText;
    private EditText mDiscountEditText;
    private EditText mCommentsEditText;

    // Current stock of the product
    private int stock = 0;

    // String containing the URI of the image selected by the user
    private String selectedImageString;

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
        setContentView(R.layout.activity_editor);

        // Use getIntent() and getData() to get the associated URI
        mProductUri = getIntent().getData();

        // Toolbar settings
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        // Set title
        if (mProductUri == null) {
            // This is a new product
            getSupportActionBar().setTitle(getString(R.string.add_product));
        } else {
            // This is an existing product
            getSupportActionBar().setTitle(getString(R.string.edit_product));
            // Initialize a loader to read the data from the database and display the current
            // values in the activity
            getLoaderManager().initLoader(PRODUCT_LOADER, null, this);
        }
        // Add back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Find all relevant views to read user input from
        mProductImageView = (ImageView) findViewById(R.id.image);
        mAddImageButton = (ImageView) findViewById(R.id.add_image);
        mNameEditText = (EditText) findViewById(R.id.edit_name);
        mReferenceEditText = (EditText) findViewById(R.id.edit_reference);
        mStockTextView = (TextView) findViewById(R.id.stock);
        mPriceEditText = (EditText) findViewById(R.id.edit_price);
        mDiscountEditText = (EditText) findViewById(R.id.edit_discount);
        mCommentsEditText = (EditText) findViewById(R.id.edit_comments);

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

        // Set onClickListener to the add photo button
        mAddImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if the permission is granted in versions > Marshmallow
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(),
                            Manifest.permission.READ_EXTERNAL_STORAGE);
                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        selectImageFromGallery();
                    } else {
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    selectImageFromGallery();
                }
            }
        });

        // Set the background color of the add photo circle
        GradientDrawable stockBackgroundCircle = (GradientDrawable) mAddImageButton.getBackground();
        stockBackgroundCircle.setColor(ContextCompat.getColor(this, R.color.colorPrimary));

        // Display correct currency
        String currency = Currency.getInstance(Locale.getDefault()).getCurrencyCode();
        TextView currencyLabelTextView = (TextView) findViewById(R.id.currency_label);
        currencyLabelTextView.setText(currency);

        // Add an OnTouchListener to the views to notify if something has changed
        mProductImageView.setOnTouchListener(mTouchListener);
        mAddImageButton.setOnTouchListener(mTouchListener);
        mNameEditText.setOnTouchListener(mTouchListener);
        mReferenceEditText.setOnTouchListener(mTouchListener);
        mMinusStockButton.setOnTouchListener(mTouchListener);
        mPlusStockButton.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mDiscountEditText.setOnTouchListener(mTouchListener);
        mCommentsEditText.setOnTouchListener(mTouchListener);

        // Restore state from saved instance, only in add mode
        if (mProductUri == null && savedInstanceState != null) {
            stock = savedInstanceState.getInt("stockSaved");
            mStockTextView.setText(String.valueOf(stock));
            String selectedImageStringSaved =
                    savedInstanceState.getString("selectedImageStringSaved");
            if (!TextUtils.isEmpty(selectedImageStringSaved)) {
                selectedImageString = selectedImageStringSaved;
                Uri productImageUri = Uri.parse(selectedImageString);
                Picasso.with(this).load(productImageUri).into(mProductImageView);
            }
        }
    }

    // Fires when a configuration change occurs and fragment needs to save state
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Update product if it has changed
        if (mProductHasChanged) {
            if (mProductUri != null) {
                ContentValues values = new ContentValues();
                String nameString = mNameEditText.getText().toString().trim();
                if (!TextUtils.isEmpty(nameString)) {
                    // If name is empty, don't save
                    values.put(ProductEntry.COLUMN_PRODUCT_NAME, nameString);
                }
                values.put(ProductEntry.COLUMN_PRODUCT_REFERENCE,
                        mReferenceEditText.getText().toString().trim());
                values.put(ProductEntry.COLUMN_PRODUCT_STOCK, stock);
                String priceString = mPriceEditText.getText().toString().trim();
                if (!TextUtils.isEmpty(priceString) && Double.parseDouble(priceString) > 0) {
                    // If price is negative, don't save
                    int priceInt = (int) (Double.parseDouble(priceString) * 100);
                    values.put(ProductEntry.COLUMN_PRODUCT_PRICE, priceInt);
                }
                String discountString = mDiscountEditText.getText().toString().trim();
                if (!TextUtils.isEmpty(discountString) &&
                        (Integer.parseInt(discountString) > 0 &&
                                Integer.parseInt(discountString) < 100)) {
                    // If discount is out of valid range (0 - 100), don't save
                    values.put(ProductEntry.COLUMN_PRODUCT_DISCOUNT, discountString);
                }
                if (!TextUtils.isEmpty(selectedImageString)) {
                    //If there's no new image, don't save
                    values.put(ProductEntry.COLUMN_PRODUCT_IMAGE, selectedImageString);
                }
                values.put(ProductEntry.COLUMN_PRODUCT_COMMENTS,
                        mCommentsEditText.getText().toString().trim());
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
            } else {
                savedInstanceState.putInt("stockSaved", stock);
                savedInstanceState.putString("selectedImageStringSaved", selectedImageString);
            }
        }
        super.onSaveInstanceState(savedInstanceState);
    }

    // Hook up the back button
    @Override
    public void onBackPressed() {
        // If the product hasn't changed, continue with handling back button press
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes);
        builder.setPositiveButton(R.string.discard_changes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User clicked "Discard" button, close the current activity.
                finish();
            }
        });
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
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
        getMenuInflater().inflate(R.menu.edit_toolbar_menu, menu);
        return true;
    }

    // Handle clicks on toolbar menu buttons
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_done:
                String nameString = mNameEditText.getText().toString().trim();
                String referenceString = mReferenceEditText.getText().toString().trim();
                String priceString = mPriceEditText.getText().toString().trim();
                String discountString = mDiscountEditText.getText().toString().trim();
                String commentsString = mCommentsEditText.getText().toString().trim();
                if (TextUtils.isEmpty(nameString) || TextUtils.isEmpty(referenceString) ||
                        TextUtils.isEmpty(commentsString) || TextUtils.isEmpty(priceString) ||
                        TextUtils.isEmpty(discountString) || TextUtils.isEmpty(selectedImageString)
                        || stock == 0) {
                    // If some field is empty, show toast and don't save
                    Toast.makeText(this, getString(R.string.no_inputs_toast),
                            Toast.LENGTH_SHORT).show();
                } else if (!TextUtils.isEmpty(priceString) && Double.parseDouble(priceString) < 0) {
                    // If price is negative, show toast and don't save
                    Toast.makeText(this, getString(R.string.negative_price_toast),
                            Toast.LENGTH_SHORT).show();
                } else if (!TextUtils.isEmpty(discountString) &&
                        (Double.parseDouble(discountString) < 0 ||
                                Double.parseDouble(discountString) > 100)) {
                    // If discount is out of valid range (0 - 100), show toast and don't save
                    Toast.makeText(this, getString(R.string.invalid_discount_toast),
                            Toast.LENGTH_SHORT).show();
                } else {
                    // Save product to the database
                    saveProduct();
                    // Exit activity
                    finish();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Helper method to select an image from the gallery
    private void selectImageFromGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_IMAGE_OPEN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_OPEN && resultCode == RESULT_OK) {
            Uri selectedImageUri = data.getData();
            // Set Image resource
            Picasso.with(this).load(selectedImageUri).into(mProductImageView);
            // Transform Uri into string
            selectedImageString = selectedImageUri.toString();
        }
    }

    /**
     * Get user input and save data into the database.
     */
    private void saveProduct() {
        // Get the data from the user input. trim() method eliminates leading or trailing
        // whitespace introduced by the user
        String nameString = mNameEditText.getText().toString().trim();
        String referenceString = mReferenceEditText.getText().toString().trim();
        String commentsString = mCommentsEditText.getText().toString().trim();
        // Parsing the String of an EditText could result in a NumberFormatException when trying to
        // parse no user input ("")
        String priceString = mPriceEditText.getText().toString().trim();
        int priceInt;
        if (!TextUtils.isEmpty(priceString)) {
            // Transform into cents
            priceInt = (int) (Double.parseDouble(mPriceEditText.getText().toString().trim()) * 100);
        } else {
            priceInt = 0; // Set to the default value
        }
        String discountString = mDiscountEditText.getText().toString().trim();
        double discountInt;
        if (!TextUtils.isEmpty(discountString)) {
            discountInt = Double.parseDouble(mDiscountEditText.getText().toString().trim());
        } else {
            discountInt = 0; // Set to the default value
        }

        // Check if this is supposed to be a new product and check if all the fields in the editor
        // are blank
        if (mProductUri == null &&
                TextUtils.isEmpty(nameString) && TextUtils.isEmpty(referenceString) &&
                TextUtils.isEmpty(commentsString) && TextUtils.isEmpty(priceString) &&
                TextUtils.isEmpty(discountString) && TextUtils.isEmpty(selectedImageString) &&
                stock == 0) {
            // Since no fields were modified, we can return early without creating a new product.
            return;
        }

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, nameString);
        values.put(ProductEntry.COLUMN_PRODUCT_REFERENCE, referenceString);
        values.put(ProductEntry.COLUMN_PRODUCT_STOCK, stock);
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, priceInt);
        values.put(ProductEntry.COLUMN_PRODUCT_DISCOUNT, discountInt);
        if (!TextUtils.isEmpty(selectedImageString)) {
            values.put(ProductEntry.COLUMN_PRODUCT_IMAGE, selectedImageString);
        }
        values.put(ProductEntry.COLUMN_PRODUCT_COMMENTS, commentsString);

        // Determine if this is a new or existing product by checking if mProductUri is null or not
        if (mProductUri == null) {
            // Insert a new product into the provider, returning the content URI for the new product
            Uri newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.insert_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.insert_successful),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // This is an existing product, so update the product with content URI
            int rowsUpdated = getContentResolver().update(
                    mProductUri,        // the content URI
                    values,             // the columns to update
                    null,               // the column to select on
                    null                // the value to compare to
            );

            // Show a toast message depending on whether or not the update was successful
            if (rowsUpdated == 0) {
                // If there are no rows updated, then there was an error with the update.
                Toast.makeText(this, getString(R.string.update_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.update_successful),
                        Toast.LENGTH_SHORT).show();
            }
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
                ProductEntry.COLUMN_PRODUCT_COMMENTS
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
            // Update the inputs with the available data
            mNameEditText.setText(cursor.getString(cursor.getColumnIndex(ProductEntry
                    .COLUMN_PRODUCT_NAME)));
            mReferenceEditText.setText(cursor.getString(cursor.getColumnIndex(ProductEntry
                    .COLUMN_PRODUCT_REFERENCE)));
            stock = cursor.getInt(cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_STOCK));
            mStockTextView.setText(String.valueOf(stock));
            // Transform from cents, adding decimals
            double priceDouble = (double) cursor.getInt(cursor.getColumnIndex(ProductEntry
                    .COLUMN_PRODUCT_PRICE));
            mPriceEditText.setText(String.valueOf(priceDouble / 100));
            mDiscountEditText.setText(String.valueOf(cursor.getInt(cursor
                    .getColumnIndex(ProductEntry.COLUMN_PRODUCT_DISCOUNT))));
            String productImageString = cursor.getString(cursor.getColumnIndex(ProductEntry
                    .COLUMN_PRODUCT_IMAGE));
            if (TextUtils.isEmpty(productImageString)) {
                mProductImageView.setImageResource(R.drawable.default_image);
            } else {
                Uri productImageUri = Uri.parse(productImageString);
                Picasso.with(this).load(productImageUri).into(mProductImageView);
            }
            mCommentsEditText.setText(cursor.getString(cursor.getColumnIndex(ProductEntry
                    .COLUMN_PRODUCT_COMMENTS)));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Clear all editor fields
        mNameEditText.setText("");
        mReferenceEditText.setText("");
        mStockTextView.setText(getString(R.string.stock_hint));
        mPriceEditText.setText("");
        mDiscountEditText.setText("");
        mProductImageView.setImageResource(R.drawable.default_image);
        mCommentsEditText.setText("");
    }
}
