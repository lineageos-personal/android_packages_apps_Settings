package com.oplus.settings.privacy;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public class PrivacyStateProvider extends ContentProvider {
    private static final String AUTHORITY = "oplus.provider.settings.PrivacyStateProvider";
    private static final String PATH_STATE_RESULT = "state_result";
    private static final int MATCH_STATE_RESULT = 1;
    private static final String[] STATE_RESULT_COLUMNS = {PATH_STATE_RESULT};

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URI_MATCHER.addURI(AUTHORITY, PATH_STATE_RESULT, MATCH_STATE_RESULT);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        if (URI_MATCHER.match(uri) != MATCH_STATE_RESULT) {
            return null;
        }

        MatrixCursor cursor = new MatrixCursor(STATE_RESULT_COLUMNS);
        cursor.addRow(new Object[] {PrivacyPasswordState.getPasswordQuality(getContext())});
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        if (URI_MATCHER.match(uri) != MATCH_STATE_RESULT) {
            return null;
        }
        return "vnd.android.cursor.dir/state_result";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
