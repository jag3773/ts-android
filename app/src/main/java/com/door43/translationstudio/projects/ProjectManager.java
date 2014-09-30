package com.door43.translationstudio.projects;

import android.util.Log;

import com.door43.delegate.DelegateListener;
import com.door43.delegate.DelegateResponse;
import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.data.DataStore;
import com.door43.translationstudio.projects.data.DataStoreDelegateResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The project manager handles all of the projects within the app.
 * TODO: need to provide progress information so we can display appropriate information while the user is waiting for the app to load. e.g. a loading screen while projects are parsed.
 * TODO: parsing tasks need to be ran asyncronously
 * Created by joel on 8/29/2014.
 */
public class ProjectManager implements DelegateListener {
    private static DataStore mDataStore;

    // so we can look up by index
    private static List<Project> mProjects = new ArrayList<Project>();
    // so we can look up by id
    private static Map<String, Project> mProjectMap = new HashMap<String, Project>();

    // so we can look up by index
    private static List<Language> mLanguages = new ArrayList<Language>();
    // so we can look up by id
    private static Map<String, Language> mLanguageMap = new HashMap<String, Language>();

    private static String mSelectedProjectId;
    private static MainApplication mContext;
    private static final String TAG = "ProjectManager";

    public ProjectManager(MainApplication context) {
        mContext = context;
        mDataStore = new DataStore(context);
        // register to receive async messages from the datastore
        mDataStore.registerDelegateListener(this);
        // begin loading projects
        mDataStore.fetchProjectCatalog();
    }

    /**
     * Adds a project to the manager
     * @param p the project to add
     */
    private void addProject(Project p) {
        if(!mProjectMap.containsKey(p.getId())) {
            mProjectMap.put(p.getId(), p);
            mProjects.add(p);
        }
    }

    /**
     * Adds a lanuage to the manager
     * @param l the language to add
     */
    private void addLanguage(Language l) {
        if(!mLanguageMap.containsKey(l.getId())) {
            mLanguageMap.put(l.getId(), l);
            mLanguages.add(l);
        }
    }

    /**
     * Returns a project by id
     * @param id the project id a.k.a slug
     * @return null if the project does not exist
     */
    public Project getProject(String id) {
        if(mProjectMap.containsKey(id)) {
            return mProjectMap.get(id);
        } else {
            return null;
        }
    }

    /**
     * Returns a project by index
     * @param index the project index
     * @return null if the project does not exist
     */
    public Project getProject(int index) {
        if(index < mProjects.size() && index >= 0) {
            return mProjects.get(index);
        } else {
            return null;
        }
    }

    /**
     * Returns a language by id
     * @param id the langyage id a.k.a language code
     * @return null if the language does not exist
     */
    public Language getLanguage(String id) {
        if(mLanguageMap.containsKey(id)) {
            return mLanguageMap.get(id);
        } else {
            return null;
        }
    }

    /**
     * Retusn a lanuage
     * @param index the language index
     * @return null if the language does not exist
     */
    public Language getLanguage(int index) {
        if(index < mLanguages.size() && index >= 0) {
            return mLanguages.get(index);
        } else {
            return null;
        }
    }

    /**
     * Sets the selected project in the app by id
     * @param id the project id
     * @return true if the project exists
     */
    public boolean setSelectedProject(String id) {
        Project p = getProject(id);
        if(p != null) {
            mSelectedProjectId = p.getId();
        }
        return p != null;
    }

    /**
     * Sets the selected project in the app by index
     * @param index the project index
     * @return true if the project exists
     */
    public boolean setSelectedProject(int index) {
        Project p = getProject(index);
        if(p != null) {
            mSelectedProjectId = p.getId();
        }
        return p != null;
    }

    /**
     * Returns the currently selected project in the app
     * @return
     */
    public Project getSelectedProject() {
        Project selectedProject = getProject(mSelectedProjectId);;
        if(selectedProject == null) {
            // auto select the first project if no other project has been selected
            int defaultProjectIndex = 0;
            setSelectedProject(defaultProjectIndex);
            return getProject(defaultProjectIndex);
        } else {
            return selectedProject;
        }
    }

    /**
     * Returns the number of projects in the app
     * @return
     */
    public int numProjects() {
        return mProjectMap.size();
    }

    @Override
    public void onDelegateResponse(String id, DelegateResponse response) {
        DataStoreDelegateResponse message = (DataStoreDelegateResponse)response;
        if(message.getType() == DataStoreDelegateResponse.MessageType.PROJECT) {
            // parse the message
            JSONArray json;
            try {
                json = new JSONArray(message.getJSON());
            } catch (JSONException e) {
                Log.w(TAG, e.getMessage());
                return;
            }

            // load the data
            for(int i=0; i<json.length(); i++) {
                try {
                    JSONObject jsonProject = json.getJSONObject(i);
                    if(jsonProject.has("title") && jsonProject.has("slug") && jsonProject.has("desc")) {
                        Project p = new Project(jsonProject.get("title").toString(), jsonProject.get("slug").toString(), jsonProject.get("desc").toString());
                        addProject(p);
                        mDataStore.fetchLanguageCatalog(p.getId());
                    } else {
                        Log.w(TAG, "missing required parameters in the project catalog");
                    }
                } catch (JSONException e) {
                    Log.w(TAG, e.getMessage());
                    continue;
                }
            }
        } else if(message.getType() == DataStoreDelegateResponse.MessageType.LANGUAGE) {
            // parse the message
            JSONArray json;
            try {
                json = new JSONArray(message.getJSON());
            } catch (JSONException e) {
                Log.w(TAG, e.getMessage());
                return;
            }

            // load the data
            for(int i=0; i<json.length(); i++) {
                try {
                    JSONObject jsonLanguage = json.getJSONObject(i);
                    if(jsonLanguage.has("language") && jsonLanguage.has("status") && jsonLanguage.has("string") && jsonLanguage.has("direction")) {
                        JSONObject jsonStatus = jsonLanguage.getJSONObject("status");
                        if(jsonStatus.has("checking_level")) {
                            // require minimum language checking level
                            if(Integer.parseInt(jsonStatus.get("checking_level").toString()) >= mContext.getResources().getInteger(R.integer.min_source_lang_checking_level)) {
                                // add the language
                                Language.Direction langDir = jsonLanguage.get("direction").toString() == "ltr" ? Language.Direction.LeftToRight : Language.Direction.RightToLeft;
                                Language l = new Language(jsonLanguage.get("language").toString(), jsonLanguage.get("string").toString(), langDir);
                                addLanguage(l);

                                // fetch source text
                                Project p = getProject(message.getProjectSlug());
                                if(p != null) {
                                    mDataStore.fetchSourceText(p.getId(), l.getId());
                                } else {
                                    Log.w(TAG, "project not found");
                                }
                            }
                        } else {
                            Log.w(TAG, "missing required parameters in the project catalog");
                        }
                    } else {
                        Log.w(TAG, "missing required parameters in the project catalog");
                    }
                } catch (JSONException e) {
                    Log.w(TAG, e.getMessage());
                    continue;
                }
            }
        } else if(message.getType() == DataStoreDelegateResponse.MessageType.SOURCE) {
            // TODO: this will break once we have multiple source languages. It will just load all the source into a single project mixing up all the languages. We need a way to manager different languges for each project.

            // parse the message
            JSONArray jsonChapters;
            try {
                JSONObject json = new JSONObject(message.getJSON());
                jsonChapters = json.getJSONArray("chapters");
            } catch (JSONException e) {
                Log.w(TAG, e.getMessage());
                return;
            }

            // load the data
            for(int i=0; i<jsonChapters.length(); i++) {
                try {
                    JSONObject jsonChapter = jsonChapters.getJSONObject(i);
                    if(jsonChapter.has("ref") && jsonChapter.has("frames") && jsonChapter.has("title") && jsonChapter.has("number")) {
                        // load chapter
                        String num = jsonChapter.get("number").toString();
                        String chapterNumber = num.substring(0, num.indexOf("."));
                        Chapter c = new Chapter(chapterNumber, jsonChapter.get("title").toString(), jsonChapter.get("ref").toString());

                        // add chapter to the project
                        Project p = getProject(message.getProjectSlug());
                        if(p != null) {
                            p.addChapter(c);

                            // load frames
                            JSONArray jsonFrames = jsonChapter.getJSONArray("frames");
                            for(int j=0; j<jsonFrames.length(); j++) {
                                JSONObject jsonFrame = jsonFrames.getJSONObject(j);
                                if(jsonFrame.has("id") && jsonFrame.has("text")) {
                                    c.addFrame(new Frame(jsonFrame.get("id").toString(), jsonFrame.get("text").toString()));
                                    // TODO: load image assets for the frame
                                } else {
                                    Log.w(TAG, "missing required parameters in the source frames");
                                }
                            }
                        } else {
                            Log.w(TAG, "could not locate project");
                        }
                    } else {
                        Log.w(TAG, "missing required parameters in the source chapters");
                    }
                } catch (JSONException e) {
                    Log.w(TAG, e.getMessage());
                    continue;
                }
            }

        } else if(message.getType() == DataStoreDelegateResponse.MessageType.IMAGES) {
            // TODO: handle loading image assets for frames. Care should be taken to avoid memory leaks or slow load times. We may want to do this on demand instead of up front (except for locally stored assets).
        } else if(message.getType() == DataStoreDelegateResponse.MessageType.AUDIO) {
            // TODO: handle loading audio assets
        } else {
            // Unknown message type
            Log.w("ProjectManager", "Unknown delegate message type "+message.getType());
        }
    }

}