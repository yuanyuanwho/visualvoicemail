package au.com.wallaceit.voicemail.mailstore.migrations;


import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import au.com.wallaceit.voicemail.VisualVoicemail;


class MigrationTo40 {
    public static void addMimeTypeColumn(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE messages ADD mime_type TEXT");
        } catch (SQLiteException e) {
            Log.e(VisualVoicemail.LOG_TAG, "Unable to add mime_type column to messages");
        }
    }
}
