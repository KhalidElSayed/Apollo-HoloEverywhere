/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo.menu;

import java.util.ArrayList;

import org.holoeverywhere.ArrayAdapter;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.app.Dialog;
import org.holoeverywhere.app.DialogFragment;
import org.holoeverywhere.app.Fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ListAdapter;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.ui.activities.ProfileActivity;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.Lists;

/**
 * Used when the user touches the image in the header in {@link ProfileActivity}
 * . It provides an easy interface for them to choose a new image, use the old
 * image, or search Google for one.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class PhotoSelectionDialog extends DialogFragment {

    /**
     * Easily detect the MIME type
     */
    public enum ProfileType {
        ALBUM, ARTIST, OTHER
    }

    private static final int FETCH_IMAGE = 3;

    private static final int GOOGLE_SEARCH = 2;

    private static ProfileType mProfileType;

    private static final int NEW_PHOTO = 0;

    private static final int OLD_PHOTO = 1;

    /**
     * @param title The dialog title.
     * @param value The MIME type
     * @return A new instance of the dialog.
     */
    public static PhotoSelectionDialog newInstance(final String title, final ProfileType type) {
        final PhotoSelectionDialog frag = new PhotoSelectionDialog();
        final Bundle args = new Bundle();
        args.putString(Config.NAME, title);
        frag.setArguments(args);
        mProfileType = type;
        return frag;
    }

    private final ArrayList<String> mChoices = Lists.newArrayList();

    /**
     * Empty constructor as per the {@link Fragment} documentation
     */
    public PhotoSelectionDialog() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final String title = getArguments().getString(Config.NAME);
        switch (mProfileType) {
            case ARTIST:
                setArtistChoices();
                break;
            case ALBUM:
                setAlbumChoices();
                break;
            case OTHER:
                setOtherChoices();
                break;
            default:
                break;
        }
        // Dialog item Adapter
        final ListAdapter adapter = new ArrayAdapter<String>(getSupportActivity(),
                android.R.layout.select_dialog_item, mChoices);
        return new AlertDialog.Builder(getSupportActivity()).setTitle(title)
                .setAdapter(adapter, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        switch (which) {
                            case NEW_PHOTO:
                                ((ProfileActivity) getSupportActivity()).selectNewPhoto();
                                break;
                            case OLD_PHOTO:
                                ((ProfileActivity) getSupportActivity()).selectOldPhoto();
                                break;
                            case FETCH_IMAGE:
                                ((ProfileActivity) getSupportActivity()).fetchAlbumArt();
                                break;
                            case GOOGLE_SEARCH:
                                ((ProfileActivity) getSupportActivity()).googleSearch();
                                break;
                            default:
                                break;
                        }
                    }
                }).create();
    }

    /**
     * Adds the choices for the album profile image.
     */
    private void setAlbumChoices() {
        // Select a photo from the gallery
        mChoices.add(NEW_PHOTO, getString(R.string.new_photo));
        // Option to fetch the old album image
        mChoices.add(OLD_PHOTO, getString(R.string.old_photo));
        if (ApolloUtils.isOnline(getSupportActivity())) {
            // Search Google for the album name
            mChoices.add(GOOGLE_SEARCH, getString(R.string.google_search));
            // Option to fetch the album image
            mChoices.add(FETCH_IMAGE, getString(R.string.context_menu_fetch_album_art));
        }
    }

    /**
     * Adds the choices for the artist profile image.
     */
    private void setArtistChoices() {
        // Select a photo from the gallery
        mChoices.add(NEW_PHOTO, getString(R.string.new_photo));
        if (ApolloUtils.isOnline(getSupportActivity())) {
            // Option to fetch the old artist image
            mChoices.add(OLD_PHOTO, getString(R.string.context_menu_fetch_artist_image));
            // Search Google for the artist name
            mChoices.add(GOOGLE_SEARCH, getString(R.string.google_search));
        }
    }

    /**
     * Adds the choices for the genre and playlist images.
     */
    private void setOtherChoices() {
        // Select a photo from the gallery
        mChoices.add(NEW_PHOTO, getString(R.string.new_photo));
        // Option to use the default image
        mChoices.add(OLD_PHOTO, getString(R.string.use_default));
    }
}
