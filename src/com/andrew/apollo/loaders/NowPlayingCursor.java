
package com.andrew.apollo.loaders;

import static com.andrew.apollo.utils.MusicUtils.mService;

import java.util.Arrays;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AudioColumns;

import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;

/**
 * A custom {@link Cursor} used to return the queue and allow for easy dragging
 * and dropping of the items in it.
 */
@SuppressLint("NewApi")
public class NowPlayingCursor extends AbstractCursor {

    private static final String[] PROJECTION = new String[] {
            /* 0 */
            BaseColumns._ID,
            /* 1 */
            AudioColumns.TITLE,
            /* 2 */
            AudioColumns.ARTIST,
            /* 3 */
            AudioColumns.ALBUM
    };

    private final Context mContext;

    private int mCurPos;

    private long[] mCursorIndexes;

    private long[] mNowPlaying;

    private Cursor mQueueCursor;

    private int mSize;

    /**
     * Constructor of <code>NowPlayingCursor</code>
     * 
     * @param context The {@link Context} to use
     */
    public NowPlayingCursor(final Context context) {
        mContext = context;
        makeNowPlayingCursor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        try {
            if (mQueueCursor != null) {
                mQueueCursor.close();
                mQueueCursor = null;
            }
        } catch (final Exception close) {
        }
        super.close();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("deprecation")
    @Override
    public void deactivate() {
        if (mQueueCursor != null) {
            mQueueCursor.deactivate();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getColumnNames() {
        return PROJECTION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCount() {
        return mSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDouble(final int column) {
        return mQueueCursor.getDouble(column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getFloat(final int column) {
        return mQueueCursor.getFloat(column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getInt(final int column) {
        try {
            return mQueueCursor.getInt(column);
        } catch (final Exception ignored) {
            onChange(true);
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLong(final int column) {
        try {
            return mQueueCursor.getLong(column);
        } catch (final Exception ignored) {
            onChange(true);
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short getShort(final int column) {
        return mQueueCursor.getShort(column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getString(final int column) {
        try {
            return mQueueCursor.getString(column);
        } catch (final Exception ignored) {
            onChange(true);
            return "";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getType(final int column) {
        if (ApolloUtils.hasHoneycomb()) {
            return mQueueCursor.getType(column);
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNull(final int column) {
        return mQueueCursor.isNull(column);
    }

    /**
     * Actually makes the queue
     */
    private void makeNowPlayingCursor() {
        mQueueCursor = null;
        mNowPlaying = MusicUtils.getQueue();
        mSize = mNowPlaying.length;
        if (mSize == 0) {
            return;
        }

        final StringBuilder selection = new StringBuilder();
        selection.append(BaseColumns._ID + " IN (");
        for (int i = 0; i < mSize; i++) {
            selection.append(mNowPlaying[i]);
            if (i < mSize - 1) {
                selection.append(",");
            }
        }
        selection.append(")");

        mQueueCursor = mContext.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, PROJECTION, selection.toString(),
                null, BaseColumns._ID);

        if (mQueueCursor == null) {
            mSize = 0;
            return;
        }

        final int playlistSize = mQueueCursor.getCount();
        mCursorIndexes = new long[playlistSize];
        mQueueCursor.moveToFirst();
        final int columnIndex = mQueueCursor.getColumnIndexOrThrow(BaseColumns._ID);
        for (int i = 0; i < playlistSize; i++) {
            mCursorIndexes[i] = mQueueCursor.getLong(columnIndex);
            mQueueCursor.moveToNext();
        }
        mQueueCursor.moveToFirst();
        mCurPos = -1;

        int removed = 0;
        for (int i = mNowPlaying.length - 1; i >= 0; i--) {
            final long trackId = mNowPlaying[i];
            final int cursorIndex = Arrays.binarySearch(mCursorIndexes, trackId);
            if (cursorIndex < 0) {
                removed += MusicUtils.removeTrack(trackId);
            }
        }
        if (removed > 0) {
            mNowPlaying = MusicUtils.getQueue();
            mSize = mNowPlaying.length;
            if (mSize == 0) {
                mCursorIndexes = null;
                return;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onMove(final int oldPosition, final int newPosition) {
        if (oldPosition == newPosition) {
            return true;
        }

        if (mNowPlaying == null || mCursorIndexes == null || newPosition >= mNowPlaying.length) {
            return false;
        }

        final long id = mNowPlaying[newPosition];
        final int cursorIndex = Arrays.binarySearch(mCursorIndexes, id);
        mQueueCursor.moveToPosition(cursorIndex);
        mCurPos = newPosition;
        return true;
    };

    /**
     * @param which The position to remove
     * @return True if sucessfull, false othersise
     */
    public boolean removeItem(final int which) {
        try {
            if (mService.removeTracks(which, which) == 0) {
                return false;
            }
            int i = which;
            mSize--;
            while (i < mSize) {
                mNowPlaying[i] = mNowPlaying[i + 1];
                i++;
            }
            onMove(-1, mCurPos);
        } catch (final RemoteException ignored) {
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean requery() {
        makeNowPlayingCursor();
        return true;
    }
}
