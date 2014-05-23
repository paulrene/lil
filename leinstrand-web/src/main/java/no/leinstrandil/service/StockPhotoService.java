package no.leinstrandil.service;

import java.util.ArrayList;
import java.util.List;

public class StockPhotoService {

    private static final String PHOTO_PATH = "images/stock/";
    private List<StockPhoto> photoList;
    private StockPhoto defaultPhoto;

    public StockPhotoService() {
        photoList = new ArrayList<>();
        photoList.add(new StockPhoto("lil_stock_01.jpg", "køllebøller", "innebandy", "bygdarom", "bygdarommet"));
        photoList.add(new StockPhoto("lil_stock_02.jpg", "rønningen", "marka", "påske", "ski", "langrenn"));
        photoList.add(new StockPhoto("lil_stock_03.jpg", "målsetning"));
        photoList.add(new StockPhoto("lil_stock_04.jpg", "langrenn", "ski", "turrenn", "poengrenn", "marka", "løyper", "skiføret", "skiføre"));
        photoList.add(new StockPhoto("lil_stock_05.jpg", "sol"));
        photoList.add(new StockPhoto("lil_stock_06.jpg", "aking", "skileik"));
        photoList.add(new StockPhoto("lil_stock_07.jpg", "allidrett"));
        photoList.add(new StockPhoto("lil_stock_08.jpg", "allidrett", "golf"));
        photoList.add(new StockPhoto("lil_stock_09.jpg", "tur"));
        photoList.add(new StockPhoto("lil_stock_10.jpg", "barnefotball"));
        photoList.add(new StockPhoto("lil_stock_11.jpg", "gressmatte", "fotballbanen", "kunstgressbanen", "fotball"));
        photoList.add(new StockPhoto("lil_stock_12.jpg", "fairplay"));
        photoList.add(new StockPhoto("lil_stock_13.jpg", "tilskuer", "supporter", "a-lag", "kunstgressbanen"));
        photoList.add(new StockPhoto("lil_stock_14.jpg", "friidrett", "idrettsuka"));
        photoList.add(new StockPhoto("lil_stock_15.jpg", "parkour", "idrettsuka"));
        photoList.add(new StockPhoto("lil_stock_16.jpg", "parkour", "idrettsuka"));
        photoList.add(new StockPhoto("lil_stock_17.jpg", "frukt", "idrettsuka"));
        photoList.add(new StockPhoto("lil_stock_18.jpg", "sykkel", "sykling", "idrettsuka"));
        photoList.add(new StockPhoto("lil_stock_20.jpg", "ringvål", "skistadion", "ballbinge", "lysløypa", "lysløype", "skogly"));
        photoList.add(new StockPhoto("lil_stock_21.jpg", "diplom", "ferdighetsmerke", "merkeprøver", "klubbkveld"));
        photoList.add(new StockPhoto("lil_stock_23.jpg", "årsmøte", "styremøte"));
        photoList.add(new StockPhoto("lil_stock_24.jpg", "loppemarked", "loppis", "lopper"));
        photoList.add(new StockPhoto("lil_stock_25.jpg", "treningstider", "treningstid", "treningstiden"));
        photoList.add(new StockPhoto("lil_stock_26.jpg", "kurs", "klubbdommerkurs", "aktivitetslederkurs"));
        photoList.add(new StockPhoto("lil_stock_27.jpg", "granåsen", "olympiatoppen"));
        photoList.add(new StockPhoto("lil_stock_28.jpg", "dalgård", "friidrett"));

        defaultPhoto = new StockPhoto("lil_stock_19.jpg");
    }

    public String getStockPhoto(String text) {
        if (text == null) {
            return defaultPhoto.getPhotoUrl();
        }

        text = text.replace(".", " ");
        text = text.replace(",", " ");
        text = text.replace(":", " ");
        text = text.replace(";", " ");
        text = text.replace("!", " ");
        text = text.replace("?", " ");
        text = text.replace(")", " ");
        text = text.replace("(", " ");
        text = text.replace("\"", " ");
        text = text.replace("/", " ");
        text = text.replace("+", " ");
        text = text.replace("&", " ");
        text = text.replace("#", " ");

        String[] words = text.split("\\s");
        int[] score = new int[photoList.size()];
        for (int n = 0; n < photoList.size(); n++) {
            StockPhoto photo = photoList.get(n);
            for (int i = 0; i < words.length; i++) {
                String word = words[i].trim();
                for (String keyword : photo.keywordList) {
                    if (word.equalsIgnoreCase(keyword)) {
                        score[n]++;
                    }
                }
            }
        }

        int highestScore = -1;
        for (int n = 0; n < score.length; n++) {
            if (score[n] > highestScore) {
                highestScore = score[n];
            }
        }

        if (highestScore <= 0) {
            return defaultPhoto.getPhotoUrl();
        }

        List<StockPhoto> matchList = new ArrayList<>();
        for (int n = 0; n < score.length; n++) {
            if (score[n] == highestScore) {
                matchList.add(photoList.get(n));
            }
        }

        return matchList.get(Math.abs(text.hashCode()) % matchList.size()).getPhotoUrl();
    }

    private static class StockPhoto {
        public String filename;
        public List<String> keywordList;

        public StockPhoto(String filename, String... keywords) {
            this.filename = filename;
            keywordList = new ArrayList<>();
            for (String keyword : keywords) {
                keywordList.add(keyword);
            }
        }

        public String getPhotoUrl() {
            return PHOTO_PATH + filename;
        }
    }

}
