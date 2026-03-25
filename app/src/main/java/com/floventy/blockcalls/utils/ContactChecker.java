package com.floventy.blockcalls.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

/**
 * Utility class to check if a phone number is saved in the device contacts.
 * Used to whitelist saved contacts from being blocked by wildcard rules.
 */
public class ContactChecker {

    private static final String TAG = "ContactChecker";

    /**
     * Check if the given phone number is saved in the device contacts.
     *
     * @param context Application context
     * @param phoneNumber The phone number to look up
     * @return true if the number is found in contacts
     */
    public static boolean isContactSaved(Context context, String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }

        try {
            Uri lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            );

            String[] projection = {ContactsContract.PhoneLookup._ID};

            Cursor cursor = context.getContentResolver().query(
                lookupUri,
                projection,
                null,
                null,
                null
            );

            if (cursor != null) {
                boolean found = cursor.getCount() > 0;
                cursor.close();
                if (found) {
                    Log.d(TAG, "Number " + phoneNumber + " is a saved contact, skipping block");
                }
                return found;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking contacts for: " + phoneNumber, e);
        }

        return false;
    }
}
