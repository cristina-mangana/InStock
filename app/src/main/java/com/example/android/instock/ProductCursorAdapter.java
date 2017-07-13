package com.example.android.instock;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.instock.data.ProductContract.ProductEntry;
import com.squareup.picasso.Picasso;

import java.text.NumberFormat;

/**
 * Created by Cristina on 12/07/2017.
 * {@link ProductCursorAdapter} is an adapter for a list that uses a {@link Cursor} of product data
 * as its data source.
 */

public class ProductCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new {@link ProductCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public ProductCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Inflate a list item view using the layout specified in product_list_item.xml
        return LayoutInflater.from(context).inflate(R.layout.product_list_item, parent, false);
    }

    /**
     * This method binds the data (in the current row pointed to by cursor) to the given
     * list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        // Find the individual views to modify
        ImageView productImageView = (ImageView) view.findViewById(R.id.product_image);
        TextView stockTextView = (TextView) view.findViewById(R.id.product_stock);
        TextView nameTextView = (TextView) view.findViewById(R.id.product_name);
        TextView priceTextView = (TextView) view.findViewById(R.id.product_price);
        TextView priceWithDiscountTextView =
                (TextView) view.findViewById(R.id.product_discount_price);
        ImageView salesButton = (ImageView) view.findViewById(R.id.sales_button);

        // Find the columns of attributes that we're interested in
        int idColumnIndex = cursor.getColumnIndex(ProductEntry._ID);
        int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
        int stockColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_STOCK);
        int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
        int discountColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_DISCOUNT);
        int imageColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_IMAGE);

        // Read the attributes from the Cursor for the current product
        String productName = cursor.getString(nameColumnIndex);
        final int productStock = cursor.getInt(stockColumnIndex);
        double productPrice = (double) cursor.getInt(priceColumnIndex);
        productPrice = productPrice / 100;
        double productDiscount = (double) cursor.getInt(discountColumnIndex);
        String productImageString = cursor.getString(imageColumnIndex);

        // Form the content URI that represents the current product by appending the id
        final Uri productUri = ContentUris.withAppendedId (ProductEntry.CONTENT_URI,
                cursor.getInt(idColumnIndex));

        // Update the views with the attributes of the current product
        if (TextUtils.isEmpty(productImageString)) {
            productImageView.setImageResource(R.drawable.default_image);
        } else {
            Uri productImageUri = Uri.parse(productImageString);
            Picasso.with(context).load(productImageUri).into(productImageView);
        }
        nameTextView.setText(productName);
        stockTextView.setText(String.valueOf(productStock));
        priceTextView.setText(NumberFormat.getCurrencyInstance().format(productPrice));
        // Calculate the product price with discount
        if (productDiscount != 0) {
            double priceWithDiscount = (1 - (productDiscount / 100)) * productPrice;
            priceWithDiscountTextView.setVisibility(View.VISIBLE);
            priceWithDiscountTextView.setText(NumberFormat.getCurrencyInstance()
                    .format(priceWithDiscount));
            // Cross out the old text
            // https://stackoverflow.com/questions/3881553/is-there-an-easy-way-to-strike-through-text-in-an-app-widget
            priceTextView.setPaintFlags(priceTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            priceWithDiscountTextView.setVisibility(View.GONE);
        }

        // Set the background color of the stock circle
        // Fetch the background from the TextView, which is a GradientDrawable.
        GradientDrawable stockBackgroundCircle = (GradientDrawable) stockTextView.getBackground();
        // Get the appropriate background color based on the current stock
        int stockColor;
        if (productStock <= 5) {
            stockColor = ContextCompat.getColor(context, R.color.colorOutOfStock);
        } else {
            stockColor = ContextCompat.getColor(context, R.color.colorPrimary);
        }
        // Set the color on the background circle
        stockBackgroundCircle.setColor(stockColor);

        // Set onClickListener on the sales button
        salesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Update stock and sales number
                if (productStock == 0) {
                    // Avoid invalid stocks
                    Toast.makeText(context, context.getString(R.string.stock_toast),
                            Toast.LENGTH_SHORT).show();
                } else {
                    // Create a new map of values, where column names are the keys to update (stock
                    // and sales number)
                    ContentValues values = new ContentValues();
                    values.put(ProductEntry.COLUMN_PRODUCT_STOCK, productStock - 1);
                    int salesNumber = cursor.getInt(cursor.getColumnIndex(ProductEntry
                            .COLUMN_PRODUCT_SALES_NUMBER));
                    values.put(ProductEntry.COLUMN_PRODUCT_SALES_NUMBER, salesNumber + 1);
                    int rowsUpdated = context.getContentResolver().update(
                            productUri,         // the content URI
                            values,             // the columns to update
                            null,               // the column to select on
                            null                // the value to compare to
                    );
                    // Show a toast message if the update was unsuccessful
                    if (rowsUpdated == 0) {
                        // If there are no rows updated, then there was an error with the update.
                        Toast.makeText(context, context.getString(R.string.update_failed),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
}
