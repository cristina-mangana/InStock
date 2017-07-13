package com.example.android.instock.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by Cristina on 11/07/2017.
 * Contract for the app
 */

public final class ProductContract {

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    private ProductContract() {}

    /**
     * Content authority string for the content provider.
     */
    public static final String CONTENT_AUTHORITY = "com.example.android.instock";

    /**
     * Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
     * the content provider.
     */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /**
     * Possible path (appended to base content URI for possible URI's).
     */
    public static final String PATH_PRODUCTS = "products";

    /**
     * Inner class that defines constant values for the products database table.
     * Each entry in the table represents a single product.
     */
    public static final class ProductEntry implements BaseColumns {

        /**
         * The content URI to access the pet data in the provider
         */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PRODUCTS);

        /**
         * The MIME type of the {@link #CONTENT_URI} for a list of products.
         */
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/"
                        + PATH_PRODUCTS;

        /**
         * The MIME type of the {@link #CONTENT_URI} for a single product.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/"
                        + PATH_PRODUCTS;

        /**
         * Name of the database table for product
         */
        public final static String TABLE_NAME = "products";

        /**
         * Unique ID number for the product (only for use in the database table).
         * Type: INTEGER
         */
        public final static String _ID = BaseColumns._ID;

        /**
         * Name of the product.
         * Type: TEXT
         */
        public final static String COLUMN_PRODUCT_NAME = "name";

        /**
         * Reference code for the product
         * Type: TEXT
         */
        public final static String COLUMN_PRODUCT_REFERENCE = "reference";

        /**
         * Current stock of the product.
         * Type: INTEGER
         */
        public final static String COLUMN_PRODUCT_STOCK = "stock";

        /**
         * Product price in cents.
         * Type: INTEGER
         */
        public final static String COLUMN_PRODUCT_PRICE = "price";

        /**
         * Product discount for sales.
         * Type: INTEGER
         */
        public final static String COLUMN_PRODUCT_DISCOUNT = "product_discount";

        /**
         * Url of the product image stored in the device.
         * Type: TEXT
         */
        public final static String COLUMN_PRODUCT_IMAGE = "image_url";

        /**
         * Product comments.
         * Type: TEXT
         */
        public final static String COLUMN_PRODUCT_COMMENTS = "comments";

        /**
         * Boolean to know whether the product is ordered to the supplier or not. The only possible
         * values are {@link #IS_ORDERED_FALSE} or {@link #IS_ORDERED_TRUE},
         * Type: INTEGER
         */
        public final static String COLUMN_PRODUCT_ORDERED = "is_ordered";

        /**
         * Product sales number.
         * Type: INTEGER
         */
        public final static String COLUMN_PRODUCT_SALES_NUMBER = "sales_number";

        /**
         * Possible values for the ordered state of the product.
         */
        public static final int IS_ORDERED_FALSE = 0;
        public static final int IS_ORDERED_TRUE = 1;
    }
}
