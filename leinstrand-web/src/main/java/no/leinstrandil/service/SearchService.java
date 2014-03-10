package no.leinstrandil.service;

import no.leinstrandil.database.model.web.Node;
import no.leinstrandil.database.model.web.Page;
import no.leinstrandil.database.model.web.TextNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.TypedQuery;
import no.leinstrandil.database.Storage;
import org.jsoup.Jsoup;
import org.jsoup.examples.HtmlToPlainText;

public class SearchService {

    private Storage storage;

    public SearchService(Storage storage) {
        this.storage = storage;
    }

    public List<SearchResult> search(String q) {
        List<SearchResult> list = new ArrayList<>();

        TypedQuery<TextNode> query = storage.createQuery("from TextNode t where t.source like '%" + q + "%'",
                TextNode.class);
        List<TextNode> resultList = query.getResultList();

        for (TextNode textNode : resultList) {
            if (textNode.getId() == textNode.getNode().getTextNodeVersions().get(0).getId()) {
                SearchResult sr = new SearchResult();
                sr.page = textNode.getNode().getPage();
                sr.node = textNode.getNode();
                sr.created = textNode.getCreated();
                sr.textNode = textNode;

                String preview = new HtmlToPlainText().getPlainText(Jsoup.parse(textNode.getSource()));
                String previewLower = preview.toLowerCase();
                String qLower = q.toLowerCase();

                List<Integer> hitPositions = new ArrayList<>();
                int hit = -1;
                int position = 0;
                do {
                    hit = previewLower.indexOf(qLower, position);
                    if (hit >= 0) {
                        hitPositions.add(hit);
                    }
                    position = hit + qLower.length();
                } while (hit >=0 );
                sr.hits = hitPositions.size();

                int count = 0;
                Set<String> variants = new HashSet<>();
                StringBuilder snippet = new StringBuilder("..");
                for (Integer hitPos : hitPositions) {
                    int start = hitPos - 64;
                    if (start < 0) start = 0;
                    int end = hitPos + qLower.length() + 64;
                    if (end >= previewLower.length()) {
                        end = previewLower.length();
                    }
                    snippet.append(preview.substring(start, end));
                    snippet.append("..");
                    variants.add(preview.substring(hitPos, hitPos + q.length()));
                    if ((count++) > 3) break;
                }
                String snippetStr = snippet.toString();
                snippetStr = snippetStr.replace(">", "&gt;");
                snippetStr = snippetStr.replace("<", "&lt;");
                for(String variant : variants) {
                    snippetStr = snippetStr.replace(variant, "<strong>" + variant + "</strong>");
                }
                sr.preview = snippetStr;
                list.add(sr);
            }
        }

        Collections.sort(list, new Comparator<SearchResult>() {
            @Override
            public int compare(SearchResult o1, SearchResult o2) {
                return o1.created.compareTo(o2.created) * -1;
            }
        });

        return list;
    }

    public static class SearchResult {
        public Page page;
        public Node node;
        public TextNode textNode;
        public Date created;
        public int hits;
        public String preview;

        public Date getCreated() {
            return created;
        }

        public int getHits() {
            return hits;
        }

        public Node getNode() {
            return node;
        }

        public Page getPage() {
            return page;
        }

        public String getPreview() {
            return preview;
        }

        public TextNode getTextNode() {
            return textNode;
        }
    }

    public static void main(String[] args) {
        SearchService ss = new SearchService(new Storage());
        List<SearchResult> list = ss.search("v");
        System.out.println(list);
    }

}
