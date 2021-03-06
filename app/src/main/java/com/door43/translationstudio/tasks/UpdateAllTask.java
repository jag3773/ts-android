package com.door43.translationstudio.tasks;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.ui.SettingsActivity;

import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.ManagedTask;

/**
 * Indexes all of the content in the api
 */

public class UpdateAllTask extends ManagedTask {
    public static final String TASK_ID = "update-all-task";
    private int maxProgress = 0;
    private boolean success = false;
    private boolean overallSuccess = true;

    @Override
    public void start() {
        publishProgress(-1, "");
        try {
            String server = App.getPref(SettingsActivity.KEY_PREF_MEDIA_SERVER, App.getRes(R.string.pref_default_media_server));
            String rootApiUrl = server + App.getRes(R.string.root_catalog_api);
            App.getLibrary().updateSources(rootApiUrl, new org.unfoldingword.door43client.OnProgressListener() {
                @Override
                public boolean onProgress(String tag, long max, long complete) {
                    maxProgress = (int)max;
                    publishProgress((float)complete/(float)max, tag);

                    if(UpdateAllTask.this.isCanceled()) {
                        Logger.i(this.getClass().getSimpleName(), "Download Cancelled");
                        return false;
                    }
                    return true;
                }
            });
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(isCanceled()) {
            return;
        }

        overallSuccess &= success;
        success = false;

        publishProgress(-1, "");
        try {
            App.getLibrary().updateCatalogs(new org.unfoldingword.door43client.OnProgressListener() {
                @Override
                public boolean onProgress(String tag, long max, long complete) {
                    maxProgress = (int)max;
                    publishProgress((float)complete/(float)max, tag);

                    if(UpdateAllTask.this.isCanceled()) {
                        Logger.i(this.getClass().getSimpleName(), "Download Cancelled");
                        return false;
                    }
                    return true;
                }
            });
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(isCanceled()) {
            return;
        }

        overallSuccess &= success;
        success = false;

        publishProgress(-1, "");
        try {
            App.getLibrary().updateChunks(new org.unfoldingword.door43client.OnProgressListener() {
                @Override
                public boolean onProgress(String tag, long max, long complete) {
                    maxProgress = (int)max;
                    publishProgress((float)complete/(float)max, tag);

                    if(UpdateAllTask.this.isCanceled()) {
                        Logger.i(this.getClass().getSimpleName(), "Download Cancelled");
                        return false;
                    }
                    return true;
                }
            });
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        overallSuccess &= success;
    }

    @Override
    public int maxProgress() {
        return maxProgress;
    }

    public boolean isSuccess() {
        return overallSuccess;
    }
}
