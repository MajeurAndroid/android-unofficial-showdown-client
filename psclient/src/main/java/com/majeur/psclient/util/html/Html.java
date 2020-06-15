package com.majeur.psclient.util.html;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.Spanned;
import com.majeur.psclient.util.Callback;
import org.ccil.cowan.tagsoup.HTMLSchema;
import org.ccil.cowan.tagsoup.Parser;


/**
 * This class is an improved version of {@link android.text.Html}. It supports
 * html with depth higher than one, more html elements, rgb() CSS functions and
 * handles images better.
 * It also implements few features specific to PS such as 'psicon' elements.
 */
public class Html {

    /**
     * Retrieves images for HTML &lt;img&gt; tags.
     */
    public static interface ImageGetter {
        /**
         * This method is called when the HTML parser encounters an
         * &lt;img&gt; tag.  The <code>source</code> argument is the
         * string from the "src" attribute; the return value should be
         * a Drawable representation of the image or <code>null</code>
         * for a generic replacement image.  Make sure you call
         * setBounds() on your Drawable if it doesn't already have
         * its bounds set.
         */
        public Drawable getDrawable(String source, int reqW, int reqH);
    }

    /**
     * Flag indicating that texts inside &lt;p&gt; elements will be separated from other texts with
     * one newline character by default.
     */
    public static final int FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH = 0x00000001;

    /**
     * Flag indicating that texts inside &lt;h1&gt;~&lt;h6&gt; elements will be separated from
     * other texts with one newline character by default.
     */
    public static final int FROM_HTML_SEPARATOR_LINE_BREAK_HEADING = 0x00000002;

    /**
     * Flag indicating that texts inside &lt;li&gt; elements will be separated from other texts
     * with one newline character by default.
     */
    public static final int FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM = 0x00000004;

    /**
     * Flag indicating that texts inside &lt;ul&gt; elements will be separated from other texts
     * with one newline character by default.
     */
    public static final int FROM_HTML_SEPARATOR_LINE_BREAK_LIST = 0x00000008;
    /**
     * Flag indicating that texts inside &lt;div&gt; elements will be separated from other texts
     * with one newline character by default.
     */
    public static final int FROM_HTML_SEPARATOR_LINE_BREAK_DIV = 0x00000010;

    /**
     * Flag indicating that texts inside &lt;blockquote&gt; elements will be separated from other
     * texts with one newline character by default.
     */
    public static final int FROM_HTML_SEPARATOR_LINE_BREAK_BLOCKQUOTE = 0x00000020;

    /**
     * Flags for {@link #fromHtml(String, int, ImageGetter, Callback)}: Separate block-level
     * elements with line breaks (single newline character) in between. This inverts the
     * {@link Spanned} to HTML string conversion done with the option
     * TO_HTML_PARAGRAPH_LINES_INDIVIDUAL.
     */
    public static final int FROM_HTML_MODE_COMPACT =
            FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH
                    | FROM_HTML_SEPARATOR_LINE_BREAK_HEADING
                    | FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM
                    | FROM_HTML_SEPARATOR_LINE_BREAK_LIST
                    | FROM_HTML_SEPARATOR_LINE_BREAK_DIV
                    | FROM_HTML_SEPARATOR_LINE_BREAK_BLOCKQUOTE;

    private Html() {
    }

    /**
     * Lazy initialization holder for HTML parser. This class will
     * a) be preloaded by the zygote, or b) not loaded until absolutely
     * necessary.
     */
    private static class HtmlParser {
        private static final HTMLSchema schema = new HTMLSchema();
    }

    public static void fromHtml(final String source, final int flags, final ImageGetter imageGetter, final Callback<Spanned> callback) {
        new AsyncTask<Void, Void, Spanned>() {
            @Override
            protected Spanned doInBackground(Void[] objects) {
                return fromHtmlInternal(source, flags, imageGetter);
            }

            @Override
            protected void onPostExecute(Spanned sp) {
                callback.callback(sp);
            }
        }.execute();
    }

    private static Spanned fromHtmlInternal(String source, int flags, ImageGetter imageGetter) {
        Parser parser = new Parser();
        try {
            parser.setProperty(Parser.schemaProperty, HtmlParser.schema);
        } catch (org.xml.sax.SAXNotRecognizedException | org.xml.sax.SAXNotSupportedException e) {
            // Should not happen.
            throw new RuntimeException(e);
        }
        HtmlToSpannedConverter converter =
                new HtmlToSpannedConverter(source, imageGetter, parser, flags);
        return converter.convert();
    }
}

