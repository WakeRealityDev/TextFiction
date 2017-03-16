package com.wakereality.textfiction;

import android.support.v4.app.FragmentActivity;

import java.io.File;
import java.util.ArrayList;

import de.onyxbits.textfiction.ImportTask;
import de.onyxbits.textfiction.LibraryAdapter;

/**
 * Created by Stephen A. Gutknecht on 2/19/17.
 */

public class SwitchEngineProviders {

    public static ImportTask getImportTask() {
        ImportTask importTask;
        importTask = new ImportTask();
        return importTask;
    }

    public static LibraryAdapter getLibraryAdapter(FragmentActivity activity, int i, ArrayList<File> games) {
        LibraryAdapter libraryAdapter;
        libraryAdapter = new LibraryAdapter(activity, 0, games);
        return libraryAdapter;
    }
}
