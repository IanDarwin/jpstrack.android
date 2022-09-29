package jpstrack.android;

import android.app.Application;

import java.io.File;

/**
 * This singleton extends regular Application class just to hold onto
 * the File object when we have a file open.
 */
public class ApplicationSingleton extends Application {
    protected File fileInProgress;
}
