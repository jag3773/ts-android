package com.door43.translationstudio.ui.home;

import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.MergeConflictsHandler;
import com.door43.translationstudio.core.Profile;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TargetTranslationMigrator;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.ui.translate.TargetTranslationActivity;
import com.door43.translationstudio.tasks.AdvancedGogsRepoSearchTask;
import com.door43.translationstudio.tasks.CloneRepositoryTask;
import com.door43.translationstudio.tasks.RegisterSSHKeysTask;
import com.door43.translationstudio.tasks.SearchGogsRepositoriesTask;
import org.unfoldingword.tools.taskmanager.SimpleTaskWatcher;
import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;

import com.door43.util.FileUtilities;
import com.door43.widget.ViewUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.unfoldingword.gogsclient.Repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 5/10/16.
 */
public class ImportFromDoor43Dialog extends DialogFragment implements SimpleTaskWatcher.OnFinishedListener {
    public static final String TAG = ImportFromDoor43Dialog.class.getSimpleName();

    private static final String STATE_REPOSITORIES = "state_repositories";
    private static final String STATE_DIALOG_SHOWN = "state_dialog_shown";
    public static final String STATE_CLONE_URL = "state_clone_url";
    public static final String STATE_TARGET_TRANSLATION = "state_target_translation";
    public static final String STATE_MERGE_SELECTION = "state_merge_selection";
    public static final String STATE_MERGE_CONFLICT = "state_merge_conflict";

    private SimpleTaskWatcher taskWatcher;
    private TranslationRepositoryAdapter adapter;
    private Translator translator;
    private List<Repository> repositories = new ArrayList<>();
    private String mCloneHtmlUrl;
    private File cloneDestDir;
    private EditText repoEditText;
    private EditText userEditText;
    private DialogShown mDialogShown = DialogShown.NONE;
    private TargetTranslation mTargetTranslation;
    private ProgressDialog mProgressDialog = null;
    private ImportDialog.MergeOptions mMergeSelection = ImportDialog.MergeOptions.NONE;
    private boolean mMergeConflicted = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_import_from_door43, container, false);

        this.taskWatcher = new SimpleTaskWatcher(getActivity(), R.string.loading);
        this.taskWatcher.setOnFinishedListener(this);

        this.translator = App.getTranslator();

        Button dismissButton = (Button) v.findViewById(R.id.dismiss_button);
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                taskWatcher.stop();
                SearchGogsRepositoriesTask task = (SearchGogsRepositoriesTask) TaskManager.getTask(SearchGogsRepositoriesTask.TASK_ID);
                if (task != null) {
                    task.stop();
                    TaskManager.cancelTask(task);
                    TaskManager.clearTask(task);
                }
                dismiss();
            }
        });
        userEditText = (EditText)v.findViewById(R.id.username);
        repoEditText = (EditText)v.findViewById(R.id.translation_id);

        v.findViewById(R.id.search_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userQuery = userEditText.getText().toString();
                String repoQuery = repoEditText.getText().toString();

                if (userEditText.hasFocus()) {
                    App.closeKeyboard(getActivity(), userEditText);
                }
                if (repoEditText.hasFocus()) {
                    App.closeKeyboard(getActivity(), repoEditText);
                }

                Profile profile = App.getProfile();
                if(profile != null && profile.gogsUser != null) {
                    AdvancedGogsRepoSearchTask task = new AdvancedGogsRepoSearchTask(profile.gogsUser, userQuery, repoQuery, 50);
                    TaskManager.addTask(task, AdvancedGogsRepoSearchTask.TASK_ID);
                    taskWatcher.watch(task);
                } else {
                    Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), getResources().getString(R.string.login_doo43), Snackbar.LENGTH_LONG);
                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                    snack.show();
                    dismiss();
                }
            }
        });

        ListView list = (ListView) v.findViewById(R.id.list);
        adapter = new TranslationRepositoryAdapter();
        adapter.setTextOnlyResources(true); // only allow importing of text resources
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(adapter != null) {
                    final int final_pos = position;
                    if (adapter.isSupported(position)) {
                        doImportProject(position);
                    } else {
                        String projectName = adapter.getProjectName(position);
                        String message = getActivity().getString(R.string.import_warning, projectName);
                        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                                .setTitle(R.string.import_from_door43)
                                .setMessage(message)
                                .setPositiveButton(R.string.label_import, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        doImportProject(final_pos);
                                    }
                                })
                                .setNegativeButton(R.string.title_cancel, null)
                                .show();
                    }
                }
            }
        });

        // restore state
        if(savedInstanceState != null) {
            mDialogShown = DialogShown.fromInt(savedInstanceState.getInt(STATE_DIALOG_SHOWN, DialogShown.NONE.getValue()));
            mCloneHtmlUrl = savedInstanceState.getString(STATE_CLONE_URL, null);
            mMergeConflicted = savedInstanceState.getBoolean(STATE_MERGE_CONFLICT, false);
            mMergeSelection = ImportDialog.MergeOptions.fromInt(savedInstanceState.getInt(STATE_MERGE_SELECTION, ImportDialog.MergeOptions.NONE.getValue()));
            String targetTranslationId = savedInstanceState.getString(STATE_TARGET_TRANSLATION, null);
            if(targetTranslationId != null) {
                mTargetTranslation = App.getTranslator().getTargetTranslation(targetTranslationId);
            }

            String[] repoJsonArray = savedInstanceState.getStringArray(STATE_REPOSITORIES);
            if(repoJsonArray != null) {
                for (String json : repoJsonArray) {
                    try {
                        Repository repo = Repository.fromJSON(new JSONObject(json));
                        if (json != null) {
                            repositories.add(repo);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                adapter.setRepositories(repositories);
            }
        }

        // connect to existing task
        AdvancedGogsRepoSearchTask searchTask = (AdvancedGogsRepoSearchTask) TaskManager.getTask(AdvancedGogsRepoSearchTask.TASK_ID);
        CloneRepositoryTask cloneTask = (CloneRepositoryTask) TaskManager.getTask(CloneRepositoryTask.TASK_ID);
        if (searchTask != null) {
            taskWatcher.watch(searchTask);
        } else if (cloneTask != null) {
            taskWatcher.watch(cloneTask);
        }

        restoreDialogs();
        return v;
    }

    /**
     * do import of project at position
     * @param position
     */
    private void doImportProject(int position) {
        showProgressDialog();
        Repository repo = adapter.getItem(position);
        String repoName = repo.getFullName().replace("/", "-");
        cloneDestDir = new File(App.context().getCacheDir(), repoName + System.currentTimeMillis() + "/");
        mCloneHtmlUrl = repo.getHtmlUrl();
        cloneRepository(ImportDialog.MergeOptions.NONE);
    }

    /**
     * start a clone task
     */
    private void cloneRepository(ImportDialog.MergeOptions mergeSelection) {
        showProgressDialog();
        mMergeSelection = mergeSelection;
        CloneRepositoryTask task = new CloneRepositoryTask(mCloneHtmlUrl, cloneDestDir);
        taskWatcher.watch(task);
        TaskManager.addTask(task, CloneRepositoryTask.TASK_ID);
    }

    /**
     * restore the dialogs that were displayed before rotation
     */
    private void restoreDialogs() {

        //recreate dialog last shown
        switch(mDialogShown) {
            case IMPORT_FAILED:
                notifyImportFailed();
                break;

            case AUTH_FAILURE:
                showAuthFailure();
                break;

            case MERGE_CONFLICT:
                showMergeOverwritePrompt(mTargetTranslation);
                break;

            case NONE:
                break;

            default:
                Logger.e(TAG,"Unsupported restore dialog: " + mDialogShown.toString());
                break;
        }
    }

    /**
     * creates and displays progress dialog
     */
    private void showProgressDialog() {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                dismissProgressDialog();

                mProgressDialog = new ProgressDialog(getActivity());
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setCanceledOnTouchOutside(true);
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setTitle(R.string.import_project_file);
                mProgressDialog.show();
            }
        });
    }

    /**
     * removes the progress dialog
     */
    protected void dismissProgressDialog() {
        if (null != mProgressDialog) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // widen dialog to accommodate more text
        int desiredWidth = 750;
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int width = displayMetrics.widthPixels;
        float density = displayMetrics.density;
        float correctedWidth = width / density;
        float screenWidthFactor = desiredWidth /correctedWidth;
        screenWidthFactor = Math.min(screenWidthFactor, 1f); // sanity check
        getDialog().getWindow().setLayout((int) (width * screenWidthFactor), WindowManager.LayoutParams.MATCH_PARENT);
    }

    @Override
    public void onFinished(ManagedTask task) {
        taskWatcher.stop();
        TaskManager.clearTask(task);

        if (task instanceof AdvancedGogsRepoSearchTask) {
            this.repositories = ((AdvancedGogsRepoSearchTask) task).getRepositories();
            adapter.setRepositories(repositories);
        } else if (task instanceof CloneRepositoryTask) {
            if (!task.isCanceled()) {
                CloneRepositoryTask.Status status = ((CloneRepositoryTask)task).getStatus();
                File tempPath = ((CloneRepositoryTask) task).getDestDir();
                String cloneUrl = ((CloneRepositoryTask) task).getCloneUrl();
                boolean alreadyExisted = false;

                if(status == CloneRepositoryTask.Status.SUCCESS) {
                    Logger.i(this.getClass().getName(), "Repository cloned from " + cloneUrl);
                    tempPath = TargetTranslationMigrator.migrate(tempPath);
                    TargetTranslation tempTargetTranslation = TargetTranslation.open(tempPath);
                    boolean importFailed = false;
                    mMergeConflicted = false;
                    if (tempTargetTranslation != null) {
                        TargetTranslation existingTargetTranslation = translator.getTargetTranslation(tempTargetTranslation.getId());
                        alreadyExisted = (existingTargetTranslation != null);
                        if( alreadyExisted && (mMergeSelection != ImportDialog.MergeOptions.OVERWRITE)) {
                            // merge target translation
                            try {
                                boolean success = existingTargetTranslation.merge(tempPath);
                                if(!success) {
                                    if(MergeConflictsHandler.isTranslationMergeConflicted(existingTargetTranslation.getId())) {
                                        mMergeConflicted = true;
                                    }
                                }
                                showMergeOverwritePrompt(existingTargetTranslation);
                            } catch (Exception e) {
                                Logger.e(this.getClass().getName(), "Failed to merge the target translation", e);
                                notifyImportFailed();
                                importFailed = true;
                            }
                        } else {
                            // restore the new target translation
                            try {
                                translator.restoreTargetTranslation(tempTargetTranslation);
                            } catch (IOException e) {
                                Logger.e(this.getClass().getName(), "Failed to import the target translation " + tempTargetTranslation.getId(), e);
                                notifyImportFailed();
                                importFailed = true;
                            }
                            alreadyExisted = false;
                        }
                    } else {
                        Logger.e(this.getClass().getName(), "Failed to open the online backup");
                        notifyImportFailed();
                        importFailed = true;
                    }
                    FileUtilities.deleteQuietly(tempPath);

                    if(!importFailed && !alreadyExisted) {
                        // todo: terrible hack. We should instead register a listener with the dialog
                        ((HomeActivity) getActivity()).notifyDatasetChanged();
                        showImportSuccess();
                    }
                } else if(status == CloneRepositoryTask.Status.AUTH_FAILURE) {
                    Logger.i(this.getClass().getName(), "Authentication failed");
                    // if we have already tried ask the user if they would like to try again
                    if(App.hasSSHKeys()) {
                        dismissProgressDialog();
                        showAuthFailure();
                        return;
                    }

                    RegisterSSHKeysTask keyTask = new RegisterSSHKeysTask(false);
                    taskWatcher.watch(keyTask);
                    TaskManager.addTask(keyTask, RegisterSSHKeysTask.TASK_ID);
                } else {
                    notifyImportFailed();
                }
            }

        } else if(task instanceof RegisterSSHKeysTask) {
            if(((RegisterSSHKeysTask)task).isSuccess()) {
                Logger.i(this.getClass().getName(), "SSH keys were registered with the server");
                cloneRepository(mMergeSelection);
            } else {
                notifyImportFailed();
            }
        }
        dismissProgressDialog();
    }

    /**
     * tell user that import was successful
     */
    public void showImportSuccess() {
        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                .setTitle(R.string.import_from_door43)
                .setMessage(R.string.title_import_success)
                .setPositiveButton(R.string.dismiss, null)
                .show();
    }

    /**
     * let user know there was a merge conflict
     * @param targetTranslation
     */
    public void showMergeOverwritePrompt(TargetTranslation targetTranslation) {
        mDialogShown = DialogShown.MERGE_CONFLICT;
        mTargetTranslation = targetTranslation;
        int messageID = mMergeConflicted ? R.string.import_merge_conflict_project_name : R.string.import_project_already_exists;
        String message = getActivity().getString(messageID, targetTranslation.getId());
        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                .setTitle(R.string.merge_conflict_title)
                .setMessage(message)
                .setPositiveButton(R.string.merge_projects_label, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = DialogShown.NONE;
                        if(mMergeConflicted) {
                            doManualMerge();
                        } else {
                            showImportSuccess();
                        }
                    }
                })
                .setNeutralButton(R.string.title_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = DialogShown.NONE;
                        resetToMasterBackup();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.overwrite_projects_label, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = DialogShown.NONE;
                        resetToMasterBackup(); // restore and now overwrite
                        cloneRepository(ImportDialog.MergeOptions.OVERWRITE);
                    }
                }).show();
    }

    /**
     * restore original version
     */
    private void resetToMasterBackup() {
        if(mTargetTranslation != null) {
            mTargetTranslation.resetToMasterBackup();
        }
    }

    /**
     * open review mode to let user resolve conflict
     */
    private void doManualMerge() {
        // ask parent activity to navigate to target translation review mode with merge filter on
        Intent intent = new Intent(getActivity(), TargetTranslationActivity.class);
        Bundle args = new Bundle();
        args.putString(App.EXTRA_TARGET_TRANSLATION_ID, mTargetTranslation.getId());
        args.putBoolean(App.EXTRA_START_WITH_MERGE_FILTER, true);
        args.putInt(App.EXTRA_VIEW_MODE, TranslationViewMode.REVIEW.ordinal());
        intent.putExtras(args);
        startActivity(intent);
        dismiss();
    }

    public void showAuthFailure() {
        mDialogShown = DialogShown.AUTH_FAILURE;
        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                .setTitle(R.string.error).setMessage(R.string.auth_failure_retry)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = DialogShown.NONE;
                        RegisterSSHKeysTask keyTask = new RegisterSSHKeysTask(true);
                        taskWatcher.watch(keyTask);
                        TaskManager.addTask(keyTask, RegisterSSHKeysTask.TASK_ID);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = DialogShown.NONE;
                        notifyImportFailed();
                    }
                }).show();
    }

    public void notifyImportFailed() {
        mDialogShown = DialogShown.IMPORT_FAILED;
        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                .setTitle(R.string.error)
                .setMessage(R.string.restore_failed)
                .setPositiveButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialogShown = DialogShown.NONE;
                    }
                })
                .show();
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        List<String> repoJsonList = new ArrayList<>();
        for (Repository r : repositories) {
            repoJsonList.add(r.toJSON().toString());
        }
        out.putStringArray(STATE_REPOSITORIES, repoJsonList.toArray(new String[repoJsonList.size()]));
        out.putInt(STATE_DIALOG_SHOWN, mDialogShown.getValue());
        out.putInt(STATE_MERGE_SELECTION, mMergeSelection.getValue());
        out.putBoolean(STATE_MERGE_CONFLICT, mMergeConflicted);
        if(mCloneHtmlUrl != null) {
            out.putString(STATE_CLONE_URL, mCloneHtmlUrl);
        }

        if(mTargetTranslation != null) {
            String targetTranslationId = mTargetTranslation.getId();
            out.putString(STATE_TARGET_TRANSLATION, targetTranslationId);
        }

        super.onSaveInstanceState(out);
    }

    @Override
    public void onDestroy() {
        dismissProgressDialog();
        taskWatcher.stop();
        super.onDestroy();
    }

    /**
     * for keeping track which dialog is being shown for orientation changes (not for DialogFragments)
     */
    public enum DialogShown {
        NONE(0),
        IMPORT_FAILED(1),
        AUTH_FAILURE(2),
        MERGE_CONFLICT(3);

        private int _value;

        DialogShown(int Value) {
            this._value = Value;
        }

        public int getValue() {
            return _value;
        }

        public static DialogShown fromInt(int i) {
            for (DialogShown b : DialogShown.values()) {
                if (b.getValue() == i) {
                    return b;
                }
            }
            return null;
        }
    }
}
