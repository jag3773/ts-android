package com.door43.translationstudio.targettranslations;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.TabLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Chapter;
import com.door43.translationstudio.core.ChapterTranslation;
import com.door43.translationstudio.core.FrameTranslation;
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.core.TranslationFormat;
import com.door43.translationstudio.core.Frame;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.core.Typography;
import com.door43.translationstudio.rendering.DefaultRenderer;
import com.door43.translationstudio.rendering.RenderingGroup;
import com.door43.translationstudio.rendering.USXRenderer;
import com.door43.translationstudio.util.AppContext;
import com.door43.widget.ViewUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by joel on 9/9/2015.
 */
public class ChunkModeAdapter extends ViewModeAdapter<ChunkModeAdapter.ViewHolder> {

    private CharSequence[] mRenderedTargetBody;
    private SourceLanguage mSourceLanguage;
    private final TargetLanguage mTargetLanguage;
    private boolean[] mTargetStateOpen;
    private CharSequence[] mRenderedSourceBody;
    private final Context mContext;
    private static final int BOTTOM_ELEVATION = 2;
    private static final int TOP_ELEVATION = 3;
    private final TargetTranslation mTargetTranslation;
    private SourceTranslation mSourceTranslation;
    private final Library mLibrary;
    private final Translator mTranslator;
    private Frame[] mFrames;
    private int mLayoutBuildNumber = 0;

    public ChunkModeAdapter(Context context, String targetTranslationId, String sourceTranslationId) {
        mLibrary = AppContext.getLibrary();
        mTranslator = AppContext.getTranslator();
        mContext = context;
        mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
        mSourceTranslation = mLibrary.getSourceTranslation(sourceTranslationId);
        mSourceLanguage = mLibrary.getSourceLanguage(mSourceTranslation.projectId, mSourceTranslation.sourceLanguageId);
        mTargetLanguage = mLibrary.getTargetLanguage(mTargetTranslation.getTargetLanguageId());

        Chapter[] chapters = mLibrary.getChapters(mSourceTranslation);
        List<Frame> frames = new ArrayList<>();
        for(Chapter c:chapters) {
            Frame[] chapterFrames = mLibrary.getFrames(mSourceTranslation, c.getId());
            frames.addAll(Arrays.asList(chapterFrames));
        }
        mFrames = frames.toArray(new Frame[frames.size()]);
        mTargetStateOpen = new boolean[mFrames.length];
        mRenderedSourceBody = new CharSequence[mFrames.length];
        mRenderedTargetBody = new CharSequence[mFrames.length];
    }

    /**
     * Updates the source translation displayed
     * @param sourceTranslationId
     */
    public void setSourceTranslation(String sourceTranslationId) {
        mSourceTranslation = mLibrary.getSourceTranslation(sourceTranslationId);
        mSourceLanguage = mLibrary.getSourceLanguage(mSourceTranslation.projectId, mSourceTranslation.sourceLanguageId);

        Chapter[] chapters = mLibrary.getChapters(mSourceTranslation);
        List<Frame> frames = new ArrayList<>();
        for(Chapter c:chapters) {
            Frame[] chapterFrames = mLibrary.getFrames(mSourceTranslation, c.getId());
            frames.addAll(Arrays.asList(chapterFrames));
        }
        mFrames = frames.toArray(new Frame[frames.size()]);
        mTargetStateOpen = new boolean[mFrames.length];
        mRenderedSourceBody = new CharSequence[mFrames.length];
        mRenderedTargetBody = new CharSequence[mFrames.length];

        notifyDataSetChanged();
    }

    @Override
    void coordinateChild(Context context, View view) {

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_chunk_list_item, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        int cardMargin = mContext.getResources().getDimensionPixelSize(R.dimen.card_margin);
        int stackedCardMargin = mContext.getResources().getDimensionPixelSize(R.dimen.stacked_card_margin);
        if(mTargetStateOpen[position]) {
            // target on top
            // elevation takes precedence for API 21+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                holder.mSourceCard.setElevation(BOTTOM_ELEVATION);
                holder.mTargetCard.setElevation(TOP_ELEVATION);
            }
            holder.mTargetCard.bringToFront();
            CardView.LayoutParams targetParams = (CardView.LayoutParams)holder.mTargetCard.getLayoutParams();
            targetParams.setMargins(cardMargin, cardMargin, stackedCardMargin, stackedCardMargin);
            holder.mTargetCard.setLayoutParams(targetParams);
            CardView.LayoutParams sourceParams = (CardView.LayoutParams)holder.mSourceCard.getLayoutParams();
            sourceParams.setMargins(stackedCardMargin, stackedCardMargin, cardMargin, cardMargin);
            holder.mSourceCard.setLayoutParams(sourceParams);
            ((View) holder.mTargetCard.getParent()).requestLayout();
            ((View) holder.mTargetCard.getParent()).invalidate();
        } else {
            // source on top
            // elevation takes precedence for API 21+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                holder.mTargetCard.setElevation(BOTTOM_ELEVATION);
                holder.mSourceCard.setElevation(TOP_ELEVATION);
            }
            holder.mSourceCard.bringToFront();
            CardView.LayoutParams sourceParams = (CardView.LayoutParams)holder.mSourceCard.getLayoutParams();
            sourceParams.setMargins(cardMargin, cardMargin, stackedCardMargin, stackedCardMargin);
            holder.mSourceCard.setLayoutParams(sourceParams);
            CardView.LayoutParams targetParams = (CardView.LayoutParams)holder.mTargetCard.getLayoutParams();
            targetParams.setMargins(stackedCardMargin, stackedCardMargin, cardMargin, cardMargin);
            holder.mTargetCard.setLayoutParams(targetParams);
            ((View) holder.mSourceCard.getParent()).requestLayout();
            ((View) holder.mSourceCard.getParent()).invalidate();
        }

        holder.mTargetCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mTargetStateOpen[position]) {
                    mTargetStateOpen[position] = true;
                    ViewUtil.animateSwapCards(holder.mSourceCard, holder.mTargetCard, TOP_ELEVATION, BOTTOM_ELEVATION, false);
                }
            }
        });
        holder.mSourceCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mTargetStateOpen[position]) {
                    mTargetStateOpen[position] = false;
                    ViewUtil.animateSwapCards(holder.mSourceCard, holder.mTargetCard, TOP_ELEVATION, BOTTOM_ELEVATION, true);
                }
            }
        });

        Frame frame = mFrames[position];

        // render the source frame body
        if(mRenderedSourceBody[position] == null) {
            mRenderedSourceBody[position] = renderText(frame.body, frame.getFormat());
        }

        holder.mSourceBody.setText(mRenderedSourceBody[position]);

        // render source frame title
        Chapter chapter = mLibrary.getChapter(mSourceTranslation, frame.getChapterId());
        String sourceChapterTitle = chapter.title;
        if(chapter.title.isEmpty()) {
            sourceChapterTitle = mSourceTranslation.getProjectTitle() + " " + Integer.parseInt(chapter.getId());
        }
        sourceChapterTitle += ":" + frame.getTitle();
        holder.mSourceTitle.setText(sourceChapterTitle);

        // render the target frame body
        if(mRenderedTargetBody[position] == null) {
            FrameTranslation frameTranslation = mTargetTranslation.getFrameTranslation(frame);
            mRenderedTargetBody[position] = renderText(frameTranslation.body, frameTranslation.getFormat());
        }
        if(holder.mTextWatcher != null) {
            holder.mTargetBody.removeTextChangedListener(holder.mTextWatcher);
        }
        holder.mTargetBody.setText(TextUtils.concat(mRenderedTargetBody[position], "\n"));

        // render target frame title
        ChapterTranslation chapterTranslation = mTargetTranslation.getChapterTranslation(chapter);
        String targetChapterTitle = chapterTranslation.title;
        final FrameTranslation frameTranslation = mTargetTranslation.getFrameTranslation(frame);
        if(!targetChapterTitle.isEmpty()) {
            targetChapterTitle += ":" + frameTranslation.getTitle();
        } else {
            targetChapterTitle = sourceChapterTitle;
        }
        holder.mTargetTitle.setText(targetChapterTitle + " - " + mTargetLanguage.name);

        // load tabs
        holder.mTabLayout.setOnTabSelectedListener(null);
        holder.mTabLayout.removeAllTabs();
        String[] sourceTranslationIds = mTranslator.getSourceTranslations(mTargetTranslation.getId());
        for(String id:sourceTranslationIds) {
            SourceTranslation sourceTranslation = mLibrary.getSourceTranslation(id);
            if(sourceTranslation != null) {
                TabLayout.Tab tab = holder.mTabLayout.newTab();
                // include the resource id if there are more than one
                if(mLibrary.getResources(sourceTranslation.projectId, sourceTranslation.sourceLanguageId).length > 1) {
                    tab.setText(sourceTranslation.getSourceLanguageTitle() + " " + sourceTranslation.resourceId.toUpperCase());
                } else {
                    tab.setText(sourceTranslation.getSourceLanguageTitle());
                }
                tab.setTag(sourceTranslation.getId());
                holder.mTabLayout.addTab(tab);
            }
        }

        // select correct tab
        for(int i = 0; i < holder.mTabLayout.getTabCount(); i ++) {
            TabLayout.Tab tab = holder.mTabLayout.getTabAt(i);
            if(tab.getTag().equals(mSourceTranslation.getId())) {
                tab.select();
                break;
            }
        }

        // hook up listener
        holder.mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                final String sourceTranslationId = (String) tab.getTag();
                if (getListener() != null) {
                    Handler hand = new Handler(Looper.getMainLooper());
                    hand.post(new Runnable() {
                        @Override
                        public void run() {
                            getListener().onTabClick(sourceTranslationId);
                        }
                    });
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        holder.mNewTabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getListener() != null) {
                    getListener().onNewTabClick();
                }
            }
        });

        holder.mTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // save
//                TODO: do this so we don't have to wait for compiling
//                Translator.applyFrameTranslation(frameTranslation, (Editable)s);

                String translation = Translator.compileTranslation((Editable)s);
                mTargetTranslation.applyFrameTranslation(frameTranslation, translation);

                // TODO: we either need to force the translation to save when the view leaves the window (so we have it if they come back before it was saved)
                // or just always save immediately

                mRenderedTargetBody[position] = renderText(translation, frameTranslation.getFormat());

                // update view
                // TRICKY: anything worth updating will need to change by at least 7 characters
                // <a></a> <-- at least 7 characters are required to create a tag for rendering.
                int minDeviation = 7;
                if(count - before > minDeviation) {
                    int scrollX = holder.mTargetBody.getScrollX();
                    int scrollY = holder.mTargetBody.getScrollX();
                    int selection = holder.mTargetBody.getSelectionStart();

                    holder.mTargetBody.removeTextChangedListener(holder.mTextWatcher);
                    holder.mTargetBody.setText(TextUtils.concat(mRenderedTargetBody[position], "\n"));
                    holder.mTargetBody.addTextChangedListener(holder.mTextWatcher);

                    holder.mTargetBody.scrollTo(scrollX, scrollY);
                    if(selection > holder.mTargetBody.length()) {
                        selection = holder.mTargetBody.length();
                    }
                    holder.mTargetBody.setSelection(selection);
                    holder.mTargetBody.clearFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
        holder.mTargetBody.addTextChangedListener(holder.mTextWatcher);

        // set up fonts
        if(holder.mLayoutBuildNumber != mLayoutBuildNumber) {
            holder.mLayoutBuildNumber = mLayoutBuildNumber;
            Typography.formatSub(mContext, holder.mSourceTitle, mSourceLanguage.getId(), mSourceLanguage.getDirection());
            Typography.format(mContext, holder.mSourceBody, mSourceLanguage.getId(), mSourceLanguage.getDirection());
            Typography.formatSub(mContext, holder.mTargetTitle, mTargetLanguage.getId(), mTargetLanguage.getDirection());
            Typography.format(mContext, holder.mTargetBody, mTargetLanguage.getId(), mTargetLanguage.getDirection());
        }
    }

    /**
     * Trigers some aspects of the children views to be rebuilt
     */
    public void rebuild() {
        mLayoutBuildNumber ++;
        notifyDataSetChanged();
    }

    private CharSequence renderText(String text, TranslationFormat format) {
        RenderingGroup renderingGroup = new RenderingGroup();
        if (format == TranslationFormat.USX) {
            // TODO: add click listeners for verses and notes
            renderingGroup.addEngine(new USXRenderer(null, null));
        } else {
            // TODO: add note click listener
            renderingGroup.addEngine(new DefaultRenderer(null));
        }
        renderingGroup.init(text);
        return renderingGroup.start();
    }

    @Override
    public int getItemCount() {
        return mFrames.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public int mLayoutBuildNumber = -1;
        public TextWatcher mTextWatcher;
        public final TextView mTargetTitle;
        public final EditText mTargetBody;
        public final CardView mTargetCard;
        public final CardView mSourceCard;
        public final TabLayout mTabLayout;
        public final ImageButton mNewTabButton;
        public TextView mSourceTitle;
        public TextView mSourceBody;
        public ViewHolder(View v) {
            super(v);
            mSourceCard = (CardView)v.findViewById(R.id.source_translation_card);
            mSourceTitle = (TextView)v.findViewById(R.id.source_translation_title);
            mSourceBody = (TextView)v.findViewById(R.id.source_translation_body);
            mTargetCard = (CardView)v.findViewById(R.id.target_translation_card);
            mTargetTitle = (TextView)v.findViewById(R.id.target_translation_title);
            mTargetBody = (EditText)v.findViewById(R.id.target_translation_body);
            mTabLayout = (TabLayout)v.findViewById(R.id.source_translation_tabs);
            mTabLayout.setTabTextColors(R.color.dark_disabled_text, R.color.dark_secondary_text);
            mNewTabButton = (ImageButton) v.findViewById(R.id.new_tab_button);
        }
    }
}