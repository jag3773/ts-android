package com.door43.translationstudio.rendering;

import android.graphics.Typeface;
import android.os.Build;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.StyleSpan;

import com.door43.translationstudio.spannables.USXChar;
import com.door43.translationstudio.spannables.USXNoteSpan;
import com.door43.translationstudio.spannables.Span;
import com.door43.translationstudio.spannables.USXVersePinSpan;
import com.door43.translationstudio.spannables.USXVerseSpan;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is the USX rendering engine. This handles all of the rendering for USX formatted source and translation
 * NOTE: when rendering large chunks of text it is important to always keep things as a CharSequence and not string
 * so that spans generated by prior rendering methods are not lost.
 */
public class USXRenderer extends ClickableRenderingEngine {

    private Span.OnClickListener mNoteListener;
    private Span.OnClickListener mVerseListener;
    private boolean mRenderLinebreaks = false;
    private boolean mRenderVerses = true;
    private String mSearch;
    private int mHighlightColor = 0;
    private int[] mExpectedVerseRange = new int[0];
    private boolean mSuppressLeadingMajorSectionHeadings = false;
    public static String beginParagraphStyle = "<para\\s+style=\"[\\w\\d]*\"\\s*>";
    public static Pattern beginParagraphPattern =  Pattern.compile(beginParagraphStyle);
    public static String endParagraphStyle = "<\\/para>";
    public static Pattern endParagraphPattern =  Pattern.compile(endParagraphStyle);
    public static String beginCharacterStyle = "<char\\s+style=\"[\\w\\d]*\"\\s*>";
    public static Pattern beginCharacterPattern =  Pattern.compile(beginCharacterStyle);
    public static String endCharacterStyle = "<\\/char>";
    public static Pattern endCharacterPattern =  Pattern.compile(endCharacterStyle);


    /**
     * Creates a new usx rendering engine without any listeners
     */
    public USXRenderer() {

    }

    /**
     * Creates a new usx rendering engine with some custom click listeners
     * @param verseListener
     */
    public USXRenderer(Span.OnClickListener verseListener, Span.OnClickListener noteListener) {
        mVerseListener = verseListener;
        mNoteListener = noteListener;
    }

    /**
     * if set to false verses will not be displayed in the output.
     *
     * @param enable default is true
     */
    public void setVersesEnabled(boolean enable) {
        mRenderVerses = enable;
    }

    /**
     * if set to true, then line breaks will be shown in the output.
     *
     * @param enable default is false
     */
    public void setLinebreaksEnabled(boolean enable) {
        mRenderLinebreaks = enable;
    }

    /**
     * If set to not null matched strings will be highlighted.
     *
     * @param searchString - null is disable
     * @param highlightColor
     */
    public void setSearchString(CharSequence searchString, int highlightColor) {
        mHighlightColor = highlightColor;
        if((searchString != null) && (searchString.length() > 0) ) {
            mSearch = searchString.toString().toLowerCase();
        } else {
            mSearch = null;
        }
    }

    /**
     * Specifies an inclusive range of verses expected in the input.
     * If a verse is not found it will be inserted at the front of the input.
     * @param verseRange
     */
    public void setPopulateVerseMarkers(int[] verseRange) {
        mExpectedVerseRange = verseRange;
    }

    /**
     * Set whether to suppress display of major section headers.
     *
     * <p>The intent behind this is that major section headers prior to chapter markers will be
     * displayed above chapter markers, but only in read mode.</p>
     *
     * @param suppressLeadingMajorSectionHeadings The value to set
     */
    public void setSuppressLeadingMajorSectionHeadings(boolean suppressLeadingMajorSectionHeadings) {
        mSuppressLeadingMajorSectionHeadings = suppressLeadingMajorSectionHeadings;
    }

    /**
     * Renders the usx input into a readable form
     * @param in the raw input string
     * @return
     */
    @Override
    public CharSequence render(CharSequence in) {
        CharSequence out = in;

        out = trimWhitespace(out);
        if(!mRenderLinebreaks) {
            out = renderLineBreaks(out);  // TODO: Eventually we may want to convert these to paragraphs.
        }
//        out = renderWhiteSpace(out);
        out = renderMajorSectionHeading(out);
        out = renderSectionHeading(out);
        out = renderParagraph(out);
        out = renderBlankLine(out);
        out = renderPoeticLine(out);
        out = renderRightAlignedPoeticLine(out);
        out = renderVerse(out);
        out = renderNote(out);
        out = renderChapterLabel(out);
        out = renderSelah(out);
        out = renderBrokenMarkers(out);
        out = renderHighlightSearch(out);

        return out;
    }

    /**
     * Renders all the Selah tags
     * @param in
     * @return
     */
    private CharSequence renderSelah(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = USXChar.getPattern(USXChar.STYLE_SELAH);
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            SpannableStringBuilder span = new SpannableStringBuilder(matcher.group(1));
            span.setSpan(new StyleSpan(Typeface.ITALIC), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), "\n", span);
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Strips out new lines and replaces them with a single space
     * @param in
     * @return
     */
    public CharSequence trimWhitespace(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = Pattern.compile("(^\\s*|\\s*$)");
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), "");
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Renders section headings.
     * @param in
     * @return
     */
    public CharSequence renderSectionHeading(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = paraPattern("s");
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;

        while(matcher.find()) {
            if(isStopped()) return in;
            SpannableStringBuilder span = new SpannableStringBuilder(matcher.group(1));
            span.setSpan(new StyleSpan(Typeface.BOLD), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), span, "\n");
            lastIndex = matcher.end();
        }

        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Renders section headings.
     * @param in
     * @return
     */
    public CharSequence renderHighlightSearch(CharSequence in) {
        if(mSearch == null) {
            return in;
        }

        CharSequence out = "";
        String lowerCaseText = in.toString().toLowerCase();
        int lastIndex = 0;

        while(lastIndex < in.length()) {
            if(isStopped()) return in;

            int pos = lowerCaseText.indexOf(mSearch, lastIndex);
            if(pos < 0) {
                break;
            }

            SpannableStringBuilder span = new SpannableStringBuilder(in.subSequence(pos, pos + mSearch.length()));
            span.setSpan(new BackgroundColorSpan(mHighlightColor), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            out = TextUtils.concat(out, in.subSequence(lastIndex, pos), span);

            lastIndex = pos + mSearch.length();
        }

        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Renders major section headings.
     * @param in
     * @return
     */
    public CharSequence renderMajorSectionHeading(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = paraPattern("ms");
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;

        while(matcher.find()) {
            if(isStopped()) return in;

            if (mSuppressLeadingMajorSectionHeadings && 0 == matcher.start()) {
                out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()));
            } else {
                SpannableStringBuilder span = new SpannableStringBuilder(matcher.group(1).toUpperCase());
                span.setSpan(new StyleSpan(Typeface.BOLD), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                span.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), span, "\n");
            }
            lastIndex = matcher.end();
        }

        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Strips out extra whitespace from the text
     * @param in
     * @return
     */
    public CharSequence renderWhiteSpace(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = Pattern.compile("(\\s+)");
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), " ");
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }
    
    /**
     * Strips out new lines and replaces them with a single space
     * @param in
     * @return
     */
    public CharSequence renderLineBreaks(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = Pattern.compile("(\\s*\\n+\\s*)");
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), " ");
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Renders all note tags
     * @param in
     * @return
     */
    public CharSequence renderNote(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = Pattern.compile(USXNoteSpan.PATTERN);
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            USXNoteSpan note = USXNoteSpan.parseNote(matcher.group());
            if(note != null) {
                note.setOnClickListener(mNoteListener);
                out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), note.toCharSequence());
            } else {
                // failed to parse the note
                out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.end()));
            }

            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Renders all verse tags
     * @param in
     * @return
     */
    public CharSequence renderVerse(CharSequence in) {
        CharSequence out = "";

        CharSequence insert = "";
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
            insert = "\n"; // this is a hack to get around bug in JellyBean in rendering multiple
                            // verses on a long line.  This hack messes up the paragraph formatting,
                            // but at least JellyBean becomes usable and doesn't crash.
        }

        Pattern pattern = Pattern.compile(USXVerseSpan.PATTERN);
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        List<Integer> foundVerses = new ArrayList<>();
        while(matcher.find()) {
            if(isStopped()) return in;
            if(mRenderVerses) {
                Span verse;
                if(mVerseListener == null) {
                    verse = new USXVerseSpan(matcher.group(1));
                } else {
                    verse = new USXVersePinSpan(matcher.group(1));
                }

                if (verse != null) {
                    // record found verses
                    int startVerse = ((USXVerseSpan)verse).getStartVerseNumber();
                    int endVerse = ((USXVerseSpan)verse).getEndVerseNumber();
                    boolean alreadyRendered = false;
                    if(endVerse > startVerse) {
                        // range of verses
                        for(int i = startVerse; i <= endVerse; i ++) {
                            if(!foundVerses.contains(i)) {
                                foundVerses.add(i);
                            } else {
                                alreadyRendered = true;
                            }
                        }
                    } else {
                        if(!foundVerses.contains(startVerse)) {
                            foundVerses.add(startVerse);
                        } else {
                            alreadyRendered = true;
                        }
                    }
                    // render verses not already found
                    if(!alreadyRendered) {
                        // exclude verses not within the range
                        boolean invalidVerse = false;
                        if(mExpectedVerseRange.length > 0) {
                            int minVerse = mExpectedVerseRange[0];
                            int maxVerse = (mExpectedVerseRange.length > 1) ? mExpectedVerseRange[1] : 0;
                            if(maxVerse == 0) maxVerse = minVerse;

                            int verseNumStart = ((USXVerseSpan) verse).getStartVerseNumber();
                            int verseNumEnd = ((USXVerseSpan) verse).getEndVerseNumber();
                            if(verseNumEnd == 0) verseNumEnd = verseNumStart;
                            invalidVerse = verseNumStart < minVerse || verseNumStart > maxVerse || verseNumEnd < minVerse || verseNumEnd > maxVerse;
                        }
                        if(!invalidVerse) {
                            verse.setOnClickListener(mVerseListener);
                            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), insert, verse.toCharSequence());
                        } else {
                            // exclude invalid verse
                            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()));
                        }
                    } else {
                        // exclude duplicate verse
                        out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()));
                    }
                } else {
                    // failed to parse the verse
                    out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.end()));
                }
            } else {
                // exclude verse from display
                out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()));
            }
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));

        if(mRenderVerses) {
            // populate missing verses
            if (mExpectedVerseRange.length == 1) {
                if (!foundVerses.contains(mExpectedVerseRange[0])) {
                    // generate missing verse
                    Span verse;
                    if (mVerseListener == null) {
                        verse = new USXVerseSpan(mExpectedVerseRange[0]);
                    } else {
                        verse = new USXVersePinSpan(mExpectedVerseRange[0]);
                    }
                    verse.setOnClickListener(mVerseListener);
                    out = TextUtils.concat(verse.toCharSequence(), out);
                }
            } else if (mExpectedVerseRange.length == 2) {
                for (int i = mExpectedVerseRange[1]; i >= mExpectedVerseRange[0]; i--) {
                    if (!foundVerses.contains(i)) {
                        // generate missing verse
                        Span verse;
                        if (mVerseListener == null) {
                            verse = new USXVerseSpan(i);
                        } else {
                            verse = new USXVersePinSpan(i);
                        }
                        verse.setOnClickListener(mVerseListener);
                        out = TextUtils.concat(verse.toCharSequence(), out);
                    }
                }
            }
        }
        return out;
    }

    /**
     * Renders all paragraph tags
     * @param in
     * @return
     */
    public CharSequence renderBrokenMarkers(CharSequence in) {
        CharSequence out = "";
        out = removePattern( in, beginParagraphPattern);
        out = removePattern( out, endParagraphPattern);
        out = removePattern( out, beginCharacterPattern);
        out = removePattern( out, endCharacterPattern);
        return out;
    }

    /**
     * Renders all paragraph tags
     * @param in
     * @return
     */
    public CharSequence removePattern(CharSequence in, Pattern pattern) {
        Matcher matcher = pattern.matcher(in);
        CharSequence out = "";
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;

            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()));
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Renders all paragraph tags
     * @param in
     * @return
     */
    public CharSequence renderParagraph(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = paraPattern("p");
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            String lineBreak = "";
            if(matcher.start() > 0) {
                lineBreak = "\n";
            }
            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), lineBreak, "    ", in.subSequence(matcher.start(1), matcher.end(1)), "\n");
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Renders all blank line tags
     * @param in
     * @return
     */
    public CharSequence renderBlankLine(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = paraShortPattern("b");
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), "\n\n");
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Renders a chapter label
     * @param in
     * @return
     */
    public CharSequence renderChapterLabel(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = paraPattern("cl");
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while (matcher.find()) {
            if(isStopped()) return in;

            SpannableString span = new SpannableString(in.subSequence(matcher.start(1), matcher.end(1)));
            span.setSpan(new StyleSpan(Typeface.BOLD), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            out = TextUtils.concat(out,  in.subSequence(lastIndex, matcher.start()), span);
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Renders all poetic line tags
     * @param in
     * @return
     */
    public CharSequence renderPoeticLine(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = paraPattern("q(\\d+)");
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            int level = Integer.parseInt(matcher.group(1));
            SpannableString span = new SpannableString(in.subSequence(matcher.start(2), matcher.end(2)));
            span.setSpan(new StyleSpan(Typeface.NORMAL), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            String padding = "";
            for(int i = 0; i < level; i ++) {
                padding += "    ";
            }

            // outdent for verse markers
            if (level > 0 && span.toString().indexOf("<verse number") == 0) {
                padding = padding.substring(0, padding.length() - 2);
            }

            // don't stack new lines
            String leadingLineBreak = "";
            String trailingLineBreak = "";

            // leading
            if(in.subSequence(0, matcher.start()) != null) {
                String previous = in.subSequence(0, matcher.start()).toString().replace(" ", "");
                int lastLineBreak = previous.lastIndexOf("\n");
                if (lastLineBreak < previous.length() - 1) {
                    leadingLineBreak = "\n";
                }
            }

            // trailing
            if(in.subSequence(matcher.end(), in.length()) != null) {
                String next = in.subSequence(matcher.end(), in.length()).toString().replace(" ", "");
                int nextLineBreak = next.indexOf("\n");
                int nextParagraph = next.indexOf("<para");
                if (nextLineBreak > 0 && nextParagraph > 0) {
                    trailingLineBreak = "\n";
                }
            }

            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), leadingLineBreak, padding, span, trailingLineBreak);
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Renders all right-aligned poetic line tags
     * @param in
     * @return
     */
    public CharSequence renderRightAlignedPoeticLine(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = paraPattern("qr");
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;

        while(matcher.find()) {
            if(isStopped()) return in;
            SpannableStringBuilder span = new SpannableStringBuilder(matcher.group(1));
            span.setSpan(new StyleSpan(Typeface.ITALIC), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), "\n", span);
            lastIndex = matcher.end();
        }

        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Return the leading section heading, if any. Non-leading major section headings, and leading
     * headings of other types, are not included.
     *
     * @see http://digitalbiblelibrary.org/static/docs/usx/parastyles.html
     * @param in The string to examine for a leading major section heading.
     * @return The leading major section heading; or the empty string if there is none.
     */
    public CharSequence getLeadingMajorSectionHeading(CharSequence in) {
        Pattern pattern = paraPattern("ms");
        Matcher matcher = pattern.matcher(in);

        if(matcher.find() && 0 == matcher.start()) {
            return matcher.group(1);
        } else {
            return "";
        }
    }

    /**
     * Returns a pattern that matches a para tag pair e.g. <para style=""></para>
     * @param style a string or regular expression to identify the style
     * @return
     */
    private static Pattern paraPattern(String style) {
        return Pattern.compile("<para\\s+style=\""+style+"\"\\s*>\\s*(((?!</para>).)*)</para>", Pattern.DOTALL);
    }

    /**
     * Returns a pattern that matches a single para tag e.g. <para style=""/>
     * @param style a string or regular expression to identify the style
     * @return
     */
    private static Pattern paraShortPattern(String style) {
        return Pattern.compile("<para\\s+style=\""+style+"\"\\s*/>", Pattern.DOTALL);
    }
}
