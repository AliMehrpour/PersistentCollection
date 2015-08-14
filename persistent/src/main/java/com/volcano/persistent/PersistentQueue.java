/*
 * Copyright 2015 Volcano
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.volcano.persistent;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.Closeable;
import java.util.NoSuchElementException;

/**
 * A persistent SQLite-based, FIFO queue. All operations are atomic, synchronized and thread-safe.
 * The underlying database is structured to survive process and even system crashes. If a Database
 * exception is thrown during a change, the change is aborted.<p>
 *
 * This queue is singleton. In order to access the queue, use {@link #getInstance(Context)}.
 */
public final class PersistentQueue implements XQueue, Closeable {
    private static PersistentQueue sInstance = null;

    private static final String COLUMN_POSITION = "position";
    private static final String COLUMN_VALUE    = "value";
    private static final String DATABASE_NAME   = "queue.db";
    private static final int DATABASE_VERSION   = 1;
    private static final String INDEX_QUEUE     = "index_queue";
    private static final String TABLE_QUEUE     = "queue";

    private DbQueue mDbQueue;
    private int mFirstPosition;
    private int mLastPosition;

    /**
     * @return Singleton {@link PersistentQueue} instance
     */
    public synchronized static PersistentQueue getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new PersistentQueue(context);
        }

        return sInstance;
    }

    private PersistentQueue(Context context) {
        mDbQueue = new DbQueue(context, DATABASE_NAME, null, DATABASE_VERSION);
        initialize();
    }

    private void initialize() {
        final SQLiteDatabase db = getReadableDatabase();
        final Cursor cursor = db.rawQuery("SELECT MIN(" + COLUMN_POSITION + "), MAX(" + COLUMN_POSITION + ") FROM " + TABLE_QUEUE, null);
        mFirstPosition = 0;
        mLastPosition = 0;
        if (cursor.moveToFirst()) {
            mFirstPosition = cursor.getInt(0);
            mLastPosition = cursor.getInt(1);
            cursor.close();
        }
    }

    @Override
    public synchronized void enqueue(String item) {
        if (isEmpty()) {
            mFirstPosition = mLastPosition = 1;
        }
        else { // Move last index
            mLastPosition++;
        }

        final SQLiteDatabase db = getWritableDatabase();
        insertItem(db, mLastPosition, item);
        close();
    }

    @Override
    public synchronized String dequeue() {
        if (isEmpty()) {
            return null;
        }

        final String firstItem = peek();

        final SQLiteDatabase db = getWritableDatabase();
        final int deleted = db.delete(TABLE_QUEUE, COLUMN_POSITION + " = " + mFirstPosition, null);
        if (deleted > 0) {
            if (mFirstPosition == mLastPosition) { // Delete last item in queue, So reset indexes
                mFirstPosition = mLastPosition = 0;
            }
            else { // Move first index
                mFirstPosition++;
            }
        }

        return firstItem;
    }

    @Override
    public synchronized String peek() {
        if (isEmpty()) {
            return null;
        }

        return getItemAt(mFirstPosition);
    }

    @Override
    public synchronized String peekAt(int position) {
        if (isEmpty()) {
            return null;
        }
        else if (position < 1 || position > size()) {
            throw new IndexOutOfBoundsException();
        }

        return getItemAt(position + mFirstPosition - 1);
    }

    @Override
    public synchronized void insertAt(int position, String item) {
        if (position < 1 || position > size() + 1) {
            throw new IndexOutOfBoundsException();
        }

        final SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            int realPosition = position + mFirstPosition - 1;
            realPosition = realPosition == 0 ? 1 : realPosition;
            final int diffPosition = mLastPosition - realPosition + 1;

            // Shift below items
            db.execSQL("UPDATE " + TABLE_QUEUE + " SET " + COLUMN_POSITION + " = " + COLUMN_POSITION +
                    " + " + diffPosition + " WHERE " + COLUMN_POSITION + " >= ?", new Integer[] { realPosition });

            // Insert new item
            insertItem(db, realPosition, item);
            if (isEmpty()) {
                mFirstPosition = mLastPosition = 1;
            }
            else {
                mLastPosition++;
            }

            // Shift back items
            db.execSQL("UPDATE " + TABLE_QUEUE + " SET " + COLUMN_POSITION + " = " + COLUMN_POSITION +
                    " - " + (diffPosition - 1) + " WHERE " + COLUMN_POSITION + " > ?", new Integer[] { realPosition });

            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
            close();
        }
    }

    @Override
    public synchronized void removeAt(int position) {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        else if (position < 1 || position > size()) {
            throw new IndexOutOfBoundsException();
        }

        final SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            final int realPosition = position + mFirstPosition - 1;
            final int diffPosition = mLastPosition - realPosition;

            // Shift below items
            db.execSQL("UPDATE " + TABLE_QUEUE + " SET " + COLUMN_POSITION + " = " + COLUMN_POSITION +
                    " + " + diffPosition + " WHERE " + COLUMN_POSITION + " > ?", new Integer[] { realPosition });

            // Delete item
            db.execSQL("DELETE FROM " + TABLE_QUEUE + " WHERE " + COLUMN_POSITION + " = " + realPosition);
            if (mFirstPosition == mLastPosition) { // Delete last item in queue, So reset indexes
                mFirstPosition = mLastPosition = 0;
            }
            else { // Queue is not empty
                mLastPosition--;
            }

            // Shift back items
            db.execSQL("UPDATE " + TABLE_QUEUE + " SET " + COLUMN_POSITION + " = " + COLUMN_POSITION +
                    " - " + (diffPosition + 1) + " WHERE " + COLUMN_POSITION + " > ?", new Integer[] { realPosition });

            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
            close();
        }
    }

    @Override
    public synchronized int size() {
        return (mLastPosition == 0 ? 0 : mLastPosition - mFirstPosition + 1);
    }

    @Override
    public synchronized boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public void close() {
        mDbQueue.close();
    }

    private SQLiteDatabase getWritableDatabase() {
        return mDbQueue.getWritableDatabase();
    }

    private SQLiteDatabase getReadableDatabase() {
        return mDbQueue.getReadableDatabase();
    }

    private String getItemAt(int index) {
        final SQLiteDatabase db = getReadableDatabase();
        final Cursor cursor = db.rawQuery("SELECT " + COLUMN_VALUE + " FROM " + TABLE_QUEUE + " WHERE " + COLUMN_POSITION + " = " + index, null);

        String value = null;
        if (cursor.moveToFirst()) {
            value = cursor.getString(0);
            cursor.close();
        }
        close();

        return value;
    }

    private void insertItem(SQLiteDatabase db, int position, String value) {
        final ContentValues values = new ContentValues();
        values.put(COLUMN_POSITION, position);
        values.put(COLUMN_VALUE, value);
        db.insert(TABLE_QUEUE, null, values);
    }

    /**
     * A database helper class for handling queue
     */
    private final static class DbQueue extends SQLiteOpenHelper {

        public DbQueue(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // Create table
            db.execSQL("CREATE TABLE " + TABLE_QUEUE + "(" + COLUMN_POSITION + " INTEGER PRIMARY KEY NOT NULL, " + COLUMN_VALUE + " TEXT NOT NULL)");
            // Create index
            db.execSQL("CREATE INDEX " + INDEX_QUEUE + " ON " + TABLE_QUEUE + "(" + COLUMN_POSITION + ")");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Do nothing
        }
    }
}

