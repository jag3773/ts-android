package com.door43.translationstudio.projects.data;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.util.FileUtilities;
import com.door43.translationstudio.util.Logger;
import com.door43.translationstudio.util.ServerUtilities;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * The data store handles all of the text and media resources within the app.
 * This class will look for data locally as well as on the server.
 * The api can be found at https://door43.org/en/dev/api/unfoldingword
 */
public class DataStore {
    private static MainApplication mContext;
    private static String SOURCE_TRANSLATIONS_DIR = "sourceTranslations/";
    private static final int API_VERSION = 2;

    public DataStore(MainApplication context) {
        mContext = context;
    }

    /**
     * Downloads a file and places it in the cached assets directory
     * @param path the relative path within the cached assets directory
     * @param urlString the url from which the file will be downloaded
     */
    private void downloadToAssets(String path, String urlString) {
        try {
            URL url = new URL(urlString);
            File file = new File(mContext.getCacheDir(), "assets/" + path);
            ServerUtilities.downloadFile(url, file);
        } catch (MalformedURLException e) {
            Logger.e(this.getClass().getName(), "malformed url", e);
        }
    }

    /**
     * Returns the projects
     * This should not be ran on the main thread when checking the server
     * @param checkServer indicates an updated list of projects should be downloaded from the server
     * @return
     */
    public String fetchProjectCatalog(boolean checkServer) {
        String path = SOURCE_TRANSLATIONS_DIR + "projects_catalog.json";

        if(checkServer) {
            downloadToAssets(path, "https://api.unfoldingword.org/ts/txt/"+API_VERSION+"/catalog.json");
        }
        return loadJSONAsset(path);
    }

    /**
     * Returns the source languages for a specific project
     * This should not be ran on the main thread when checking the server
     * @param projectId the slug of the project for which languages will be returned
     * @param checkServer indicates an updated list of projects should be downloaded from the server
     * @return
     */
    public String fetchSourceLanguageCatalog(String projectId, boolean checkServer) {
        String path = SOURCE_TRANSLATIONS_DIR + projectId + "/languages_catalog.json";

        if(checkServer) {
            downloadToAssets(path, "https://api.unfoldingword.org/ts/txt/"+API_VERSION+"/"+projectId+"/languages.json");
        }
        return loadJSONAsset(path);
    }


    /**
     * Returns the resources for a specific language
     * This should not be ran on the main thread when checking the server
     * @param projectId the id of the project that contains the language
     * @param languageId the id of the language for which resources will be returned
     * @param checkServer indicates an updated list of projects should be downloaded from the server
     * @return
     */
    public String fetchResourceCatalog(String projectId, String languageId, boolean checkServer) {
        String path = SOURCE_TRANSLATIONS_DIR + projectId + "/" + languageId + "/resources_catalog.json";

        if(checkServer) {
            downloadToAssets(path, "https://api.unfoldingword.org/ts/txt/"+API_VERSION+"/"+projectId+"/"+languageId+"/resources.json");
        }
        return loadJSONAsset(path);
    }

    /**
     * Returns the target languages
     */
    public String fetchTargetLanguageCatalog() {
        // TODO: check for updates on the server
        // https://api.unfoldingword.org/ts/txt/1/langnames.json
        String path = "target_languages.json";
        return loadJSONAsset(path);
    }

    /**
     * Returns the key terms.
     * @param projectId
     * @param languageId
     * @param resourceId
     * @param checkServer indicates an updated list of terms should be downloaded from the server
     * @return
     */
    public String fetchTerms(String projectId, String languageId, String resourceId, boolean checkServer) {
        String path = SOURCE_TRANSLATIONS_DIR + projectId + "/" + languageId + "/" + resourceId + "/terms.json";

        if(checkServer) {
            downloadToAssets(path, "https://api.unfoldingword.org/ts/txt/"+API_VERSION+"/"+projectId+"/"+languageId+"/"+resourceId+"/terms.json");
        }
        return loadJSONAsset(path);
    }

    /**
     * Downloads and returns the key terms.
     * Rather than using the standard download url this method allows you to specify from which url
     * to download the terms. This is especially helpful when cross referencing api's.
     * @param projectId
     * @param languageId
     * @param resourceId
     * @param urlString the url from which the terms will be downloaded
     * @return
     */
    public String fetchTerms(String projectId, String languageId, String resourceId, String urlString) {
        String path = SOURCE_TRANSLATIONS_DIR + projectId + "/" + languageId + "/" + resourceId + "/terms.json";

        downloadToAssets(path, urlString);

        return loadJSONAsset(path);
    }

    /**
     * Returns the notes
     * @param projectId
     * @param languageId
     * @param resourceId
     * @param checkServer indicates an updated list of notes should be downloaded from the server
     * @return
     */
    public String fetchNotes(String projectId, String languageId, String resourceId, boolean checkServer) {
        String path = SOURCE_TRANSLATIONS_DIR + projectId + "/" + languageId + "/" + resourceId + "/notes.json";

        if(checkServer) {
            downloadToAssets(path, "https://api.unfoldingword.org/ts/txt/"+API_VERSION+"/"+projectId+"/"+languageId+"/"+resourceId+"/notes.json");
        }
        return loadJSONAsset(path);
    }

    /**
     * Downloads and returns the notes.
     * Rather than using the standard download url this method allows you to specify from which url
     * to download the notes. This is especially helpful when cross referencing api's.
     * @param projectId
     * @param languageId
     * @param resourceId
     * @param urlString the url from which the notes will be downloaded
     * @return
     */
    public String fetchNotes(String projectId, String languageId, String resourceId, String urlString) {
        String path = SOURCE_TRANSLATIONS_DIR + projectId + "/" + languageId + "/" + resourceId + "/notes.json";

        downloadToAssets(path, urlString);

        return loadJSONAsset(path);
    }

    /**
     * Returns the source text
     * @param projectId
     * @param languageId
     * @param resourceId
     * @param checkServer indicates an updated version of the source should be downloaded from the server
     * @return
     */
    public String fetchSource(String projectId, String languageId, String resourceId, boolean checkServer) {
        String path = SOURCE_TRANSLATIONS_DIR + projectId + "/" + languageId + "/" + resourceId + "/source.json";

        if(checkServer) {
            downloadToAssets(path, "https://api.unfoldingword.org/ts/txt/"+API_VERSION+"/"+projectId+"/"+languageId+"/"+resourceId+"/source.json");
        }
        return loadJSONAsset(path);
    }

    /**
     * Downloads and returns the source.
     * Rather than using the standard download url this method allows you to specify from which url
     * to download the source. This is especially helpful when cross referencing api's.
     * @param projectId
     * @param languageId
     * @param resourceId
     * @param urlString the url from which the source will be downloaded
     * @return
     */
    public String fetchSource(String projectId, String languageId, String resourceId, String urlString) {
        String path = SOURCE_TRANSLATIONS_DIR + projectId + "/" + languageId + "/" + resourceId + "/source.json";

        downloadToAssets(path, urlString);

        return loadJSONAsset(path);
    }

    /**
     * Load json from the local assets
     * @param path path to the json file within the assets directory
     * @return the string contents of the json file
     */
    private String loadJSONAsset(String path) {
        File cacheAsset = new File(mContext.getCacheDir(), "assets/" + path);
        if(cacheAsset.exists()) {
            // attempt to load from the cached assets first. These will be the most up to date
            try {
                return FileUtilities.getStringFromFile(cacheAsset);
            } catch (IOException e) {
                Logger.e(this.getClass().getName(), "failed to load cached assets", e);
                return loadPackagedJSONAsset(path);
            }
        } else {
            return loadPackagedJSONAsset(path);
        }
    }

    /**
     * Loads json from the packaged assets (those distributed with the app)
     * @param path
     * @return
     */
    private String loadPackagedJSONAsset(String path) {
        try {
            InputStream is = mContext.getAssets().open(path);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer, "UTF-8");
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "failed to load the packaged assets", e);
            return null;
        }
    }
}
