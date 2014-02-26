package no.leinstrandil.creole;

import static no.leinstrandil.creole.Utils.*;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

public class SourceParser extends WikiParser {

    public SourceParser(String wikiText) {
        super();
        HEADING_LEVEL_SHIFT = 0;
        parse(wikiText);
    }

    public static String renderXHTML(String wikiText) {
        return new SourceParser(wikiText).toString();
    }

    @Override
    protected void appendImage(String text) {
        super.appendImage(text);
    }

    @Override
    protected void appendLink(String text) {
        String[] link = split(text, '|');
        URI uri = null;
        try { // validate URI
            uri = new URI(link[0].trim());
        } catch (URISyntaxException e) {
        }
        if (uri != null && uri.isAbsolute()) {
            sb.append("<a href=\"" + escapeHTML(uri.toString()) + "\" rel=\"nofollow\">");
            sb.append(escapeHTML(unescapeHTML(link.length >= 2 && !isEmpty(link[1].trim()) ? link[1] : link[0])));
            sb.append("</a>");
        } else {
            sb.append("<a href=\"http://en.wikipedia.org/wiki/" + escapeHTML(escapeURL(link[0]))
                    + "\" title=\"Wikipedia link\">");
            sb.append(escapeHTML(unescapeHTML(link.length >= 2 && !isEmpty(link[1].trim()) ? link[1] : link[0])));
            sb.append("</a>");
        }
    }

    @Override
    protected void appendMacro(String text) {
        if ("TOC".equals(text)) {
            super.appendMacro(text); // use default
        } else if ("My-macro".equals(text)) {
            sb.append("{{ My macro output }}");
        } else {
            super.appendMacro(text);
        }
    }

    public static String escapeURL(String s) {
        try {
            return URLEncoder.encode(s, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

}
