package com.majeur.psclient.util.html;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;

import com.majeur.psclient.util.Utils;

import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is an improved version of {@link android.text.Html}. It supports
 * html with depth higher than one, more html elements, rgb() CSS functions and
 * handles images better.
 * It also implements few features specific to PS such as 'psicon' elements.
 */
class HtmlToSpannedConverter implements ContentHandler {

    private static final float[] HEADING_SIZES = {
            1.5f, 1.4f, 1.3f, 1.2f, 1.1f, 1f,
    };
    private String mSource;
    private XMLReader mReader;
    private int mDepth;
    private SpannableStringBuilder mSpannableStringBuilder;
    private Html.ImageGetter mImageGetter;
    private int mFlags;
    private static Pattern sTextAlignPattern;
    private static Pattern sForegroundColorPattern;
    private static Pattern sBackgroundColorPattern;
    private static Pattern sTextDecorationPattern;

    private static final Map<String, Integer> sColorMap;

    static {
        sColorMap = new HashMap<>();
        sColorMap.put("black", Color.BLACK);
        sColorMap.put("darkgray", Color.DKGRAY);
        sColorMap.put("gray", Color.GRAY);
        sColorMap.put("lightgray", Color.LTGRAY);
        sColorMap.put("white", 0xFFFFFF);
        sColorMap.put("red", Color.RED);
        sColorMap.put("green", Color.GREEN);
        sColorMap.put("blue", Color.BLUE);
        sColorMap.put("yellow", Color.YELLOW);
        sColorMap.put("cyan", Color.CYAN);
        sColorMap.put("magenta", Color.MAGENTA);
        sColorMap.put("aqua", 0x00FFFF);
        sColorMap.put("fuchsia", 0xFF00FF);
        sColorMap.put("darkgrey", Color.DKGRAY);
        sColorMap.put("grey", Color.GRAY);
        sColorMap.put("lightgrey", Color.LTGRAY);
        sColorMap.put("lime", 0x00FF00);
        sColorMap.put("maroon", 0x800000);
        sColorMap.put("navy", 0x000080);
        sColorMap.put("olive", 0x808000);
        sColorMap.put("purple", 0x800080);
        sColorMap.put("silver", 0xC0C0C0);
        sColorMap.put("teal", 0x008080);
    }

    private static Pattern getTextAlignPattern() {
        if (sTextAlignPattern == null) {
            sTextAlignPattern = Pattern.compile("(?:\\s+|\\A)text-align\\s*:\\s*(\\S*)\\b");
        }
        return sTextAlignPattern;
    }

    private static Pattern getForegroundColorPattern() {
        if (sForegroundColorPattern == null) {
            sForegroundColorPattern = Pattern.compile(
                    "(?:\\s+|\\A)color\\s*:\\s*(.[(,.\\w\\s]*)\\b");
        }
        return sForegroundColorPattern;
    }

    private static Pattern getBackgroundColorPattern() {
        if (sBackgroundColorPattern == null) {
            sBackgroundColorPattern = Pattern.compile(
                    "(?:\\s+|\\A)background(?:-color)?\\s*:\\s*(.[(,.\\w\\s]*)\\b");
        }
        return sBackgroundColorPattern;
    }

    private static Pattern getTextDecorationPattern() {
        if (sTextDecorationPattern == null) {
            sTextDecorationPattern = Pattern.compile(
                    "(?:\\s+|\\A)text-decoration\\s*:\\s*(\\S*)\\b");
        }
        return sTextDecorationPattern;
    }

    /* package */ HtmlToSpannedConverter(String source, Html.ImageGetter imageGetter, Parser parser, int flags) {
        mSource = source;
        mSpannableStringBuilder = new SpannableStringBuilder();
        mImageGetter = imageGetter;
        mReader = parser;
        mFlags = flags;
    }

    /* package */ Spannable convert() {
        mReader.setContentHandler(this);
        try {
            mReader.parse(new InputSource(new StringReader(mSource)));
        } catch (IOException e) {
            // We are reading from a string. There should not be IO problems.
            throw new RuntimeException(e);
        } catch (SAXException e) {
            // TagSoup doesn't throw parse exceptions.
            throw new RuntimeException(e);
        }

        // Make sure there is no lb alone at the end
        int l = mSpannableStringBuilder.length();
        if (mSpannableStringBuilder.charAt(l - 1) == '\n')
            mSpannableStringBuilder.delete(l - 1, l);
        return mSpannableStringBuilder;
    }

    private void handleStartTag(String tag, Attributes attributes) {
        if (tag.equalsIgnoreCase("br")) {
            // We don't need to handle this. TagSoup will ensure that there's a </br> for each <br>
            // so we can safely emit the linebreaks when we handle the close tag.
        } else if (tag.equalsIgnoreCase("p")) {
            startBlockElement(mSpannableStringBuilder, mDepth, attributes, getMarginParagraph());
        } else if (tag.equalsIgnoreCase("ul")) {
            startBlockElement(mSpannableStringBuilder, mDepth, attributes, getMarginList());
        } else if (tag.equalsIgnoreCase("li")) {
            startLi(mSpannableStringBuilder, mDepth, attributes);
        } else if (tag.equalsIgnoreCase("div")) {
            startBlockElement(mSpannableStringBuilder, mDepth, attributes, getMarginDiv());
        } else if (tag.equalsIgnoreCase("span")) {
            startSpan(mSpannableStringBuilder, mDepth, attributes);
            startCssStyle(mSpannableStringBuilder, mDepth, attributes, true);
        } else if (tag.equalsIgnoreCase("strong")) {
            start(mSpannableStringBuilder, new Bold(mDepth));
        } else if (tag.equalsIgnoreCase("b")) {
            start(mSpannableStringBuilder, new Bold(mDepth));
        } else if (tag.equalsIgnoreCase("em")) {
            start(mSpannableStringBuilder, new Italic(mDepth));
        } else if (tag.equalsIgnoreCase("cite")) {
            start(mSpannableStringBuilder, new Italic(mDepth));
        } else if (tag.equalsIgnoreCase("dfn")) {
            start(mSpannableStringBuilder, new Italic(mDepth));
        } else if (tag.equalsIgnoreCase("i")) {
            start(mSpannableStringBuilder, new Italic(mDepth));
        } else if (tag.equalsIgnoreCase("big")) {
            start(mSpannableStringBuilder, new Big(mDepth));
        } else if (tag.equalsIgnoreCase("small")) {
            start(mSpannableStringBuilder, new Small(mDepth));
        } else if (tag.equalsIgnoreCase("font")) {
            startFont(mSpannableStringBuilder, attributes);
        } else if (tag.equalsIgnoreCase("blockquote")) {
            startBlockquote(mSpannableStringBuilder, mDepth, attributes);
        } else if (tag.equalsIgnoreCase("tt")) {
            start(mSpannableStringBuilder, new Monospace(mDepth));
        } else if (tag.equalsIgnoreCase("a")) {
            startCssStyle(mSpannableStringBuilder, mDepth, attributes, true);
            startA(mSpannableStringBuilder, mDepth, attributes);
        } else if (tag.equalsIgnoreCase("u")) {
            start(mSpannableStringBuilder, new Underline(mDepth));
        } else if (tag.equalsIgnoreCase("del")) {
            start(mSpannableStringBuilder, new Strikethrough(mDepth));
        } else if (tag.equalsIgnoreCase("s")) {
            start(mSpannableStringBuilder, new Strikethrough(mDepth));
        } else if (tag.equalsIgnoreCase("strike")) {
            start(mSpannableStringBuilder, new Strikethrough(mDepth));
        } else if (tag.equalsIgnoreCase("sup")) {
            start(mSpannableStringBuilder, new Super(mDepth));
        } else if (tag.equalsIgnoreCase("sub")) {
            start(mSpannableStringBuilder, new Sub(mDepth));
        } else if (tag.length() == 2 &&
                Character.toLowerCase(tag.charAt(0)) == 'h' &&
                tag.charAt(1) >= '1' && tag.charAt(1) <= '6') {
            startHeading(mSpannableStringBuilder, attributes, tag.charAt(1) - '1');
        } else if (tag.equalsIgnoreCase("img")) {
            startImg(mSpannableStringBuilder, attributes, mImageGetter);
        } else if (tag.equalsIgnoreCase("table")) {
            startTable(mSpannableStringBuilder, mDepth, attributes);
        } else if (tag.equalsIgnoreCase("td") || tag.equalsIgnoreCase("th")) {
            startTd(mSpannableStringBuilder, mDepth, attributes);
        } else if (tag.equalsIgnoreCase("tr")) {
            startBlockElement(mSpannableStringBuilder, mDepth, attributes, 1);
        } else if (tag.equalsIgnoreCase("psicon")) {
            startPsIcon(mSpannableStringBuilder, attributes, mImageGetter);
        } else if (tag.equalsIgnoreCase("marquee")) {
            startCssStyle(mSpannableStringBuilder, mDepth, attributes, true);
        } else if (tag.equalsIgnoreCase("details")) {
            startDetails(mSpannableStringBuilder, mDepth);
        } else if (tag.equalsIgnoreCase("summary")) {
            startSummary(mSpannableStringBuilder, mDepth);
        } else if (tag.equalsIgnoreCase("button")) {
            startButton(mSpannableStringBuilder, mDepth, attributes);
            startCssStyle(mSpannableStringBuilder, mDepth, attributes, true);
        } else if (tag.equalsIgnoreCase("center")) {
            start(mSpannableStringBuilder, new Alignment(mDepth, Layout.Alignment.ALIGN_CENTER));
        }
    }

    private void handleEndTag(String tag) {
        if (tag.equalsIgnoreCase("br")) {
            handleBr(mSpannableStringBuilder);
        } else if (tag.equalsIgnoreCase("p")) {
            endCssStyle(mSpannableStringBuilder, mDepth);
            endBlockElement(mSpannableStringBuilder, mDepth);
        } else if (tag.equalsIgnoreCase("ul")) {
            endBlockElement(mSpannableStringBuilder, mDepth);
        } else if (tag.equalsIgnoreCase("li")) {
            endLi(mSpannableStringBuilder, mDepth);
        } else if (tag.equalsIgnoreCase("div")) {
            endBlockElement(mSpannableStringBuilder, mDepth);
        } else if (tag.equalsIgnoreCase("span")) {
            endSpan(mSpannableStringBuilder, mDepth);
            endCssStyle(mSpannableStringBuilder, mDepth);
        } else if (tag.equalsIgnoreCase("strong")) {
            end(mSpannableStringBuilder, mDepth, Bold.class, new StyleSpan(Typeface.BOLD));
        } else if (tag.equalsIgnoreCase("b")) {
            end(mSpannableStringBuilder, mDepth, Bold.class, new StyleSpan(Typeface.BOLD));
        } else if (tag.equalsIgnoreCase("em")) {
            end(mSpannableStringBuilder, mDepth, Italic.class, new StyleSpan(Typeface.ITALIC));
        } else if (tag.equalsIgnoreCase("cite")) {
            end(mSpannableStringBuilder, mDepth, Italic.class, new StyleSpan(Typeface.ITALIC));
        } else if (tag.equalsIgnoreCase("dfn")) {
            end(mSpannableStringBuilder, mDepth, Italic.class, new StyleSpan(Typeface.ITALIC));
        } else if (tag.equalsIgnoreCase("i")) {
            end(mSpannableStringBuilder, mDepth, Italic.class, new StyleSpan(Typeface.ITALIC));
        } else if (tag.equalsIgnoreCase("big")) {
            end(mSpannableStringBuilder, mDepth, Big.class, new RelativeSizeSpan(1.25f));
        } else if (tag.equalsIgnoreCase("small")) {
            end(mSpannableStringBuilder, mDepth, Small.class, new RelativeSizeSpan(0.8f));
        } else if (tag.equalsIgnoreCase("font")) {
            endFont(mSpannableStringBuilder, mDepth);
        } else if (tag.equalsIgnoreCase("blockquote")) {
            endBlockquote(mSpannableStringBuilder, mDepth);
        } else if (tag.equalsIgnoreCase("tt")) {
            end(mSpannableStringBuilder, mDepth, Monospace.class, new TypefaceSpan("monospace"));
        } else if (tag.equalsIgnoreCase("a")) {
            endCssStyle(mSpannableStringBuilder, mDepth);
            endA(mSpannableStringBuilder, mDepth);
        } else if (tag.equalsIgnoreCase("u")) {
            end(mSpannableStringBuilder, mDepth, Underline.class, new UnderlineSpan());
        } else if (tag.equalsIgnoreCase("del")) {
            end(mSpannableStringBuilder, mDepth, Strikethrough.class, new StrikethroughSpan());
        } else if (tag.equalsIgnoreCase("s")) {
            end(mSpannableStringBuilder, mDepth, Strikethrough.class, new StrikethroughSpan());
        } else if (tag.equalsIgnoreCase("strike")) {
            end(mSpannableStringBuilder, mDepth, Strikethrough.class, new StrikethroughSpan());
        } else if (tag.equalsIgnoreCase("sup")) {
            end(mSpannableStringBuilder, mDepth, Super.class, new SuperscriptSpan());
        } else if (tag.equalsIgnoreCase("sub")) {
            end(mSpannableStringBuilder, mDepth, Sub.class, new SubscriptSpan());
        } else if (tag.length() == 2 &&
                Character.toLowerCase(tag.charAt(0)) == 'h' &&
                tag.charAt(1) >= '1' && tag.charAt(1) <= '6') {
            endHeading(mSpannableStringBuilder, mDepth);
        } else if (tag.equalsIgnoreCase("table")) {
            endTable(mSpannableStringBuilder, mDepth);
        } else if (tag.equalsIgnoreCase("td") || tag.equalsIgnoreCase("th")) {
            endTd(mSpannableStringBuilder, mDepth);
        } else if (tag.equalsIgnoreCase("tr")) {
            endBlockElement(mSpannableStringBuilder, mDepth);
        } else if (tag.equalsIgnoreCase("marquee")) {
            endCssStyle(mSpannableStringBuilder, mDepth);
        } else if (tag.equalsIgnoreCase("details")) {
            endDetails(mSpannableStringBuilder, mDepth);
        } else if (tag.equalsIgnoreCase("summary")) {
            endSummary(mSpannableStringBuilder, mDepth);
        } else if (tag.equalsIgnoreCase("button")) {
            endButton(mSpannableStringBuilder, mDepth);
            endCssStyle(mSpannableStringBuilder, mDepth);
        } else if (tag.equalsIgnoreCase("center")) {
            endCenter(mSpannableStringBuilder, mDepth);
        }
    }

    private int getMarginParagraph() {
        return getMargin(Html.FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH);
    }

    private int getMarginHeading() {
        return getMargin(Html.FROM_HTML_SEPARATOR_LINE_BREAK_HEADING);
    }

    private int getMarginListItem() {
        return getMargin(Html.FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM);
    }

    private int getMarginList() {
        return getMargin(Html.FROM_HTML_SEPARATOR_LINE_BREAK_LIST);
    }

    private int getMarginDiv() {
        return getMargin(Html.FROM_HTML_SEPARATOR_LINE_BREAK_DIV);
    }

    private int getMarginBlockquote() {
        return getMargin(Html.FROM_HTML_SEPARATOR_LINE_BREAK_BLOCKQUOTE);
    }

    /**
     * Returns the minimum number of newline characters needed before and after a given block-level
     * element.
     * @param flag the corresponding option flag defined in {@link Html} of a block-level element
     */
    private int getMargin(int flag) {
        if ((flag & mFlags) != 0) {
            return 1;
        }
        return 2;
    }

    private static void appendNewlines(Editable text, int minNewline) {
        final int len = text.length();
        if (len == 0) {
            return;
        }
        int existingNewlines = 0;
        for (int i = len - 1; i >= 0 && text.charAt(i) == '\n'; i--) {
            existingNewlines++;
        }
        for (int j = existingNewlines; j < minNewline; j++) {
            text.append("\n");
        }
    }

    private static void startBlockElement(Editable text, int depth, Attributes attributes, int margin) {
        final int len = text.length();
        if (margin > 0) {
            appendNewlines(text, margin);
            start(text, new Newline(depth, margin));
        }
        String htmlClass = attributes.getValue("", "class");
        if (htmlClass != null) {
            if (htmlClass.toLowerCase().contains("infobox"))
                start(text, new Border(depth));
        }
        String align = attributes.getValue("", "align"); // Support for deprecated align attr
        boolean alignSet = false; // Priority to CSS
        String style = attributes.getValue("", "style");
        if (style != null) {
            Matcher m = getTextAlignPattern().matcher(style);
            if (m.find()) {
                String alignment = m.group(1);
                if (alignment.equalsIgnoreCase("start")) {
                    start(text, new Alignment(depth, Layout.Alignment.ALIGN_NORMAL));
                    alignSet = true;
                } else if (alignment.equalsIgnoreCase("center")) {
                    start(text, new Alignment(depth, Layout.Alignment.ALIGN_CENTER));
                    alignSet = true;
                } else if (alignment.equalsIgnoreCase("end")) {
                    start(text, new Alignment(depth, Layout.Alignment.ALIGN_OPPOSITE));
                    alignSet = true;
                } else if (alignment.equalsIgnoreCase("left")) {
                    start(text, new Alignment(depth, compatLeftAlignment()));
                    alignSet = true;
                } else if (alignment.equalsIgnoreCase("right")) {
                    start(text, new Alignment(depth, compatRightAlignment()));
                    alignSet = true;
                }
            }
        }
        if (align != null && !alignSet) {
            if (align.equalsIgnoreCase("middle") || align.equalsIgnoreCase("center")) {
                start(text, new Alignment(depth, Layout.Alignment.ALIGN_CENTER));
            } else if (align.equalsIgnoreCase("start")) {
                start(text, new Alignment(depth, Layout.Alignment.ALIGN_NORMAL));
            } else if (align.equalsIgnoreCase("end")) {
                start(text, new Alignment(depth, Layout.Alignment.ALIGN_OPPOSITE));
            } else if (align.equalsIgnoreCase("left")) {
                start(text, new Alignment(depth, compatLeftAlignment()));
            } else if (align.equalsIgnoreCase("right")) {
                start(text, new Alignment(depth, compatRightAlignment()));
            }
        }
        startCssStyle(text, depth, attributes, false);
    }

    private static void endBlockElement(Editable text, int depth) {
        Newline n = getLast(text, Newline.class, depth);
        if (n != null) {
            appendNewlines(text, n.mNumNewlines);
            text.removeSpan(n);
        }
        Alignment a = getLast(text, Alignment.class, depth);
        if (a != null) {
            setSpanFromMark(text, a, new AlignmentSpan.Standard(a.mAlignment));
        }
        Border b = getLast(text, Border.class, depth);
        if (b != null) {
            int d = (int) Resources.getSystem().getDisplayMetrics().density;
            setSpanFromMark(text, b, new BorderSpan(Color.DKGRAY, d));
        }
        endCssStyle(text, depth);
    }

    private static void handleBr(Editable text) {
        text.append('\n');
    }

    private void startLi(Editable text, int depth, Attributes attributes) {
        startBlockElement(text, depth, attributes, getMarginListItem());
        start(text, new Bullet(depth));
    }

    private static void endLi(Editable text, int depth) {
        endBlockElement(text, depth);
        end(text, depth, Bullet.class, new BulletSpan());
    }

    private static void startSpan(Editable text, int depth, Attributes attributes) {
        String htmlClass = attributes.getValue("", "class");
        if (htmlClass != null) {
            if (htmlClass.toLowerCase().contains("statcol") || htmlClass.toLowerCase().contains("bstcol"))
                start(text, new ClearBrAtEnd(depth));
            if (htmlClass.toLowerCase().contains("col"))
                start(text, new SpaceAtEnd(depth));
        }
    }

    private static void endSpan(Editable text, int depth) {
        ClearBrAtEnd scs = getLast(text, ClearBrAtEnd.class, depth);
        if (scs != null) {
            replaceCharFromMark(text, scs, '\n', ':');
        }
        SpaceAtEnd sae = getLast(text, SpaceAtEnd.class, depth);
        if (sae != null) {
            text.append(' ');
        }
    }

    private void startBlockquote(Editable text, int depth, Attributes attributes) {
        startBlockElement(text, depth, attributes, getMarginBlockquote());
        start(text, new Blockquote(depth));
    }

    private static void endBlockquote(Editable text, int depth) {
        endBlockElement(text, depth);
        end(text, depth, Blockquote.class, new QuoteSpan());
    }

    private void startHeading(Editable text, Attributes attributes, int level) {
        startBlockElement(text, mDepth, attributes, getMarginHeading());
        start(text, new Heading(mDepth, level));
    }

    private static void endHeading(Editable text, int depth) {
        // RelativeSizeSpan and StyleSpan are CharacterStyles
        // Their ranges should not include the newlines at the end
        Heading h = getLast(text, Heading.class, depth);
        if (h != null) {
            setSpanFromMark(text, h, new RelativeSizeSpan(HEADING_SIZES[h.mLevel]),
                    new StyleSpan(Typeface.BOLD));
        }
        endBlockElement(text, depth);
    }

    private static void startTable(Editable text, int depth, Attributes attributes) {
        start(text, new Table(depth));
        startBlockElement(text, depth, attributes, 0);
    }

    private static void endTable(Editable text, int depth) {
        Table t = getLast(text, Table.class, depth);
        if (t != null) {
            int st = text.getSpanStart(t);
            int len = text.length();
            CharSequence content = text.subSequence(st, len);
            boolean hasOnlyBr = true;
            for (int i = 0; i < len - st; i++)
                if (content.charAt(i) != '\n' && content.charAt(i) != ' ')
                    hasOnlyBr = false;
            if (hasOnlyBr) text.delete(st, len);
        }
        endBlockElement(text, depth);
    }

    private static void startTd(Editable text, int depth, Attributes attributes) {
        startCssStyle(text, depth, attributes, true);
    }

    private static void endTd(Editable text, int depth) {
        endCssStyle(text, depth);
        text.append(' ');
    }

    private static void startPsIcon(Editable text, Attributes attributes, Html.ImageGetter img) {
        String species = attributes.getValue("", "pokemon");
        if (species != null) {
            String src = "content://com.majeur.psclient/dex-icon/" + species;
            int len = text.length();
            text.append("\uFFFC");
            Drawable d = null;
            if (img != null) {
                int w = (int) (46 * Resources.getSystem().getDisplayMetrics().density);
                d = img.getDrawable(src, w, 0);
            }
            if (d == null) {
                d = new ColorDrawable(Color.GRAY);
                d.setBounds(0, 0, 48, 48);
            }
            text.setSpan(new ImageSpan(d, src), len, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private static void startDetails(Editable text, int depth) {
        start(text, new Details(depth));
    }

    private static void endDetails(Editable text, int depth) {
        Details d = getLast(text, Details.class, depth);
        if (d == null) return;
        int where = text.getSpanStart(d);
        text.removeSpan(d);
        int len = text.length();
        if (where == len) return;
        DetailsSpan[] s = text.getSpans(where, text.length(), DetailsSpan.class);
        if (s.length == 0) return;
        DetailsSpan span = s[s.length - 1];
        int contentStart = text.getSpanEnd(span);
        int contentEnd = text.length();
        CharSequence content = text.subSequence(contentStart, contentEnd);
        span.setContent(content);
        text.delete(contentStart, contentEnd);
    }

    private static void startSummary(Editable text, int depth) {
        start(text, new Summary(depth));
    }

    private static void endSummary(Editable text, int depth) {
        end(text, depth, Summary.class, new DetailsSpan());
    }

    private static void startButton(Editable text, int depth, Attributes attributes) {
        String name = attributes.getValue("", "name");
        String value = attributes.getValue("", "value");
        if (name == null || value == null || value.length() == 0) return;

        if (name.equalsIgnoreCase("send") || name.equalsIgnoreCase("parseCommand")) {
            start(text, new SendCommand(depth, value));
        }
    }

    private static void endButton(Editable text, int depth) {
        SendCommand sc = getLast(text, SendCommand.class, depth);
        if (sc != null) {
            setSpanFromMark(text, sc, new ChatCommandSpan(sc.mCommand));
        }
    }

    private static <T> T getLast(Spanned text, Class<T> kind, int depth) {
        /*
         * This knows that the last returned object from getSpans()
         * will be the most recently added.
         */
        T[] objs = text.getSpans(0, text.length(), kind);
        if (objs.length == 0) {
            return null;
        } else {
            T t = objs[objs.length - 1];
            if (t instanceof Mark) {
                return ((Mark) t).mDepth == depth ? t : null;
            }
            return t;
        }
    }

    private static void setSpanFromMark(Spannable text, Mark mark, Object... spans) {
        int where = text.getSpanStart(mark);
        text.removeSpan(mark);
        int len = text.length();
        if (where != len) {
            for (Object span : spans) {
                Log.e("HtmlToSpannedConverter", "Set " + span + " from " + where + " to " + len);
                text.setSpan(span, where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private static void removeCharFromMark(Editable text, Mark mark, char c) {
        int where = text.getSpanStart(mark);
        text.removeSpan(mark);
        int len = text.length();
        if (where != len) {
            for (int i = where; i < len; i++) {
                if (text.charAt(i) == c) {
                    text.delete(i, i + 1);
                    len--;
                }
            }
        }
    }

    private static void replaceCharFromMark(Editable text, Mark mark, char o, char n) {
        int where = text.getSpanStart(mark);
        text.removeSpan(mark);
        int len = text.length();
        if (where != len) {
            for (int i = where; i < len; i++) {
                if (text.charAt(i) == o) {
                    text.replace(i, i + 1, Character.toString(n));
                    len--;
                }
            }
        }
    }

    private static void start(Editable text, Mark mark) {
        int len = text.length();
        text.setSpan(mark, len, len, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
    }

    private static void end(Editable text, int depth, Class<? extends Mark> kind, Object repl) {
        int len = text.length();
        Mark mark = getLast(text, kind, depth);
        if (mark != null) {
            setSpanFromMark(text, mark, repl);
        }
    }

    private static void startCssStyle(Editable text, int depth, Attributes attributes, boolean inline) {
        String bgcolor = attributes.getValue("", "bgcolor"); // Support for deprecated bg attr
        boolean bgSet = false; // Priority to CSS
        String style = attributes.getValue("", "style");
        if (style != null) {
            Matcher m = getForegroundColorPattern().matcher(style);
            if (m.find()) {
                int c = getHtmlColor(m.group(1));
                if (c != -1) {
                    start(text, new Foreground(depth, c | 0xFF000000));
                }
            }
            m = getBackgroundColorPattern().matcher(style);
            if (m.find()) {
                int c = getHtmlColor(m.group(1));
                if (c != -1) {
                    start(text, inline ? new Background(depth, c | 0xFF000000) :
                            new ParagraphBackground(depth, c | 0xFF000000));
                    bgSet = true;
                }
            }
            m = getTextDecorationPattern().matcher(style);
            if (m.find()) {
                String textDecoration = m.group(1);
                if (textDecoration.equalsIgnoreCase("line-through")) {
                    start(text, new Strikethrough(depth));
                }
            }
        }
        if (bgcolor != null && !bgSet) {
            int color = getHtmlColor(bgcolor);
            start(text, inline ? new Background(depth, color | 0xFF000000) :
                    new ParagraphBackground(depth, color | 0xFF000000));
        }
    }

    private static void endCssStyle(Editable text, int depth) {
        Strikethrough s = getLast(text, Strikethrough.class, depth);
        if (s != null) {
            setSpanFromMark(text, s, new StrikethroughSpan());
        }
        Background b = getLast(text, Background.class, depth);
        if (b != null) {
            setSpanFromMark(text, b, new BackgroundColorSpan(b.mBackgroundColor));
        }
        ParagraphBackground pb = getLast(text, ParagraphBackground.class, depth);
        if (pb != null) {
            setSpanFromMark(text, pb, new ParagraphBackgroundColorSpan(pb.mBackgroundColor));
        }
        Foreground f = getLast(text, Foreground.class, depth);
        if (f != null) {
            setSpanFromMark(text, f, new ForegroundColorSpan(f.mForegroundColor));
        }
    }

    private static void endCenter(Editable text, int depth) {
        Alignment a = getLast(text, Alignment.class, depth);
        if (a != null) {
            setSpanFromMark(text, a, new AlignmentSpan.Standard(a.mAlignment));
        }
    }

    private static void startImg(Editable text, Attributes attributes, Html.ImageGetter img) {
        String src = attributes.getValue("", "src");
        String rw = attributes.getValue("", "width");
        String rh = attributes.getValue("", "height");
        int w = 0;
        int h = 0;
        float density = Resources.getSystem().getDisplayMetrics().density;
        if (rw != null) {
            try {
                w = Integer.parseInt(rw);
                w *= density;
            } catch (NumberFormatException ignored) {
            }
        }
        if (rh != null) {
            try {
                h = Integer.parseInt(rh);
                h *= density;
            } catch (NumberFormatException ignored) {
            }
        }
        int len = text.length();
        text.append("\uFFFC");
        Drawable d = null;
        if (img != null) {
            d = img.getDrawable(src, w, h);
        }
        if (d == null) {
            d = new ColorDrawable(Color.GRAY);
            d.setBounds(0, 0, 48, 48);
        }
        text.setSpan(new ImageSpan(d, src, ImageSpan.ALIGN_BASELINE), len, text.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void startFont(Editable text, Attributes attributes) {
        String color = attributes.getValue("", "color");
        String face = attributes.getValue("", "face");
        if (!TextUtils.isEmpty(color)) {
            int c = getHtmlColor(color);
            if (c != -1) {
                start(text, new Foreground(mDepth, c | 0xFF000000));
            }
        }
        if (!TextUtils.isEmpty(face)) {
            start(text, new Font(mDepth, face));
        }
    }

    private static void endFont(Editable text, int depth) {
        Font font = getLast(text, Font.class, depth);
        if (font != null) {
            setSpanFromMark(text, font, new TypefaceSpan(font.mFace));
        }
        Foreground foreground = getLast(text, Foreground.class, depth);
        if (foreground != null) {
            setSpanFromMark(text, foreground,
                    new ForegroundColorSpan(foreground.mForegroundColor));
        }
    }

    private static void startA(Editable text, int depth, Attributes attributes) {
        String href = attributes.getValue("", "href");
        start(text, new Href(depth, href));
    }

    private static void endA(Editable text, int depth) {
        Href h = getLast(text, Href.class, depth);
        if (h != null) {
            if (h.mHref != null) {
                setSpanFromMark(text, h, new URLSpan(h.mHref));
            }
        }
    }

    private static int getHtmlColor(String color) {
        if (color.startsWith("rgb")) {
            try {
                int st = color.indexOf('(');
                if (st == -1) return -1;
                String[] vals = color.substring(st + 1).split(",");
                if (vals.length < 3) return -1;
                int r = parseCssColorVal(vals[0].trim());
                int g = parseCssColorVal(vals[1].trim());
                int b = parseCssColorVal(vals[2].trim());
                int a = vals.length > 3 ? parseCssColorVal(vals[3].trim()) : 255;
                return Color.argb(a, r, g, b);
            } catch (NumberFormatException nfe) {
                return -1;
            }
        }
        Integer i = sColorMap.get(color.toLowerCase(Locale.US));
        if (i != null) return i;
        try {
            return Utils.convertXmlValueToInt(color, -1);
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    private static int parseCssColorVal(String val) throws NumberFormatException {
        int color;
        if (val.charAt(val.length() - 1) == '%') {
            int p = Integer.parseInt(val.substring(val.length() - 2));
            color = Math.round((p / 100f) * 255f);
        } else if (val.contains(".")) {
            color = Math.round(Float.parseFloat(val) * 255);
        } else {
            color = Integer.parseInt(val);
        }
        return color > 255 ? 255 : color;
    }

    private static Layout.Alignment compatLeftAlignment() {
//        if (Build.VERSION.SDK_INT >= 28) {
//            return Layout.Alignment.ALIGN_LEFT;
//        } else {
            boolean ltr = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault())
                    == View.LAYOUT_DIRECTION_LTR;
            return ltr ? Layout.Alignment.ALIGN_NORMAL : Layout.Alignment.ALIGN_OPPOSITE;
//        }
    }

    private static Layout.Alignment compatRightAlignment() {
//        if (Build.VERSION.SDK_INT >= 28) {
//            return Layout.Alignment.ALIGN_RIGHT;
//        } else {
            boolean rtl = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault())
                    == View.LAYOUT_DIRECTION_RTL;
            return rtl ? Layout.Alignment.ALIGN_NORMAL : Layout.Alignment.ALIGN_OPPOSITE;
//        }
    }

    public void setDocumentLocator(Locator locator) {
    }

    public void startDocument() throws SAXException {
    }

    public void endDocument() throws SAXException {
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
    }

    public void endPrefixMapping(String prefix) throws SAXException {
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        mDepth++;
        handleStartTag(localName, attributes);
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        handleEndTag(localName);
        mDepth--;
    }

    public void characters(char ch[], int start, int length) throws SAXException {
        StringBuilder sb = new StringBuilder();
        /*
         * Ignore whitespace that immediately follows other whitespace;
         * newlines count as spaces.
         */
        for (int i = 0; i < length; i++) {
            char c = ch[i + start];
            if (c == ' ' || c == '\n') {
                char pred;
                int len = sb.length();
                if (len == 0) {
                    len = mSpannableStringBuilder.length();
                    if (len == 0) {
                        pred = '\n';
                    } else {
                        pred = mSpannableStringBuilder.charAt(len - 1);
                    }
                } else {
                    pred = sb.charAt(len - 1);
                }
                if (pred != ' ' && pred != '\n') {
                    sb.append(' ');
                }
            } else {
                sb.append(c);
            }
        }
        mSpannableStringBuilder.append(sb);
    }

    public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
    }

    public void processingInstruction(String target, String data) throws SAXException {
    }

    public void skippedEntity(String name) throws SAXException {
    }

    private static class Mark {
        int mDepth;

        Mark(int depth) {
            mDepth = depth;
        }
    }

    private static class Bold extends Mark {
        Bold(int depth) {
            super(depth);
        }
    }

    private static class Italic extends Mark {
        Italic(int depth) {
            super(depth);
        }
    }

    private static class Underline extends Mark {
        Underline(int depth) {
            super(depth);
        }
    }

    private static class Strikethrough extends Mark {
        Strikethrough(int depth) {
            super(depth);
        }
    }

    private static class Big extends Mark {
        Big(int depth) {
            super(depth);
        }
    }

    private static class Small extends Mark {
        Small(int depth) {
            super(depth);
        }
    }

    private static class Monospace extends Mark {
        Monospace(int depth) {
            super(depth);
        }
    }

    private static class Blockquote extends Mark {
        Blockquote(int depth) {
            super(depth);
        }
    }

    private static class Super extends Mark {
        Super(int depth) {
            super(depth);
        }
    }

    private static class Sub extends Mark {
        Sub(int depth) {
            super(depth);
        }
    }

    private static class Bullet extends Mark {
        Bullet(int depth) {
            super(depth);
        }
    }

    private static class Font extends Mark {
        String mFace;

        Font(int depth, String face) {
            super(depth);
            mFace = face;
        }
    }

    private static class Href extends Mark {
        String mHref;

        Href(int depth, String href) {
            super(depth);
            mHref = href;
        }
    }

    private static class Foreground extends Mark {
        private int mForegroundColor;

        Foreground(int depth, int foregroundColor) {
            super(depth);
            mForegroundColor = foregroundColor;
        }
    }

    private static class Background extends Mark {
        private int mBackgroundColor;

        Background(int depth, int backgroundColor) {
            super(depth);
            mBackgroundColor = backgroundColor;
        }
    }

    private static class ParagraphBackground extends Mark {
        private int mBackgroundColor;

        ParagraphBackground(int depth, int backgroundColor) {
            super(depth);
            mBackgroundColor = backgroundColor;
        }
    }

    private static class Heading extends Mark {
        private int mLevel;

        Heading(int depth, int level) {
            super(depth);
            mLevel = level;
        }
    }

    private static class Newline extends Mark {
        private int mNumNewlines;

        Newline(int depth, int numNewlines) {
            super(depth);
            mNumNewlines = numNewlines;
        }
    }

    private static class Alignment extends Mark {
        private Layout.Alignment mAlignment;

        Alignment(int depth, Layout.Alignment alignment) {
            super(depth);
            mAlignment = alignment;
        }
    }

    private static class Table extends Mark {
        Table(int depth) {
            super(depth);
        }
    }

    private static class Border extends Mark {
        Border(int depth) {
            super(depth);
        }
    }

    private static class ClearBrAtEnd extends Mark {
        ClearBrAtEnd(int depth) {
            super(depth);
        }
    }

    private static class SpaceAtEnd extends Mark {
        SpaceAtEnd(int depth) {
            super(depth);
        }
    }

    private static class Details extends Mark {
        Details(int depth) {
            super(depth);
        }
    }

    private static class Summary extends Mark {
        Summary(int depth) {
            super(depth);
        }
    }

    private static class SendCommand extends Mark {
        String mCommand;
        SendCommand(int depth, String cmd) {
            super(depth);
            mCommand = cmd;
        }
    }
 }
