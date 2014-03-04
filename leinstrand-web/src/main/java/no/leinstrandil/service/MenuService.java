package no.leinstrandil.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import no.leinstrandil.database.Storage;
import no.leinstrandil.database.model.MenuEntry;
import no.leinstrandil.database.model.Page;

public class MenuService {

    private Storage storage;

    public MenuService(Storage storage) {
        this.storage = storage;
    }

    public List<MenuEntry> getRootEntries() {
        try {
            TypedQuery<MenuEntry> query = storage.createQuery("from MenuEntry where parent = null", MenuEntry.class);
            List<MenuEntry> menuEntries = query.getResultList();
            return sortListOfEntries(menuEntries);
        } catch (NoResultException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<MenuEntry> sortEntries(Set<MenuEntry> entries) {
        return sortListOfEntries(new ArrayList<MenuEntry>(entries));
    }

    public List<MenuEntry> sortListOfEntries(List<MenuEntry> entries) {
        Collections.sort(entries, new Comparator<MenuEntry>() {
            @Override
            public int compare(MenuEntry o1, MenuEntry o2) {
                Integer p1 = o1.getPriority();
                Integer p2 = o2.getPriority();
                if (p1 == null)
                    p1 = Integer.MAX_VALUE;
                if (p2 == null)
                    p2 = Integer.MAX_VALUE;
                return p1 - p2;
            }
        });
        return entries;
    }

    public boolean isPageInThisTopMenu(MenuEntry topMenu, Page page) {
        // Check if we are in the top menu
        if (topMenu.getPage() != null) {
            if (page.getUrlName().equals(topMenu.getPage().getUrlName())) {
                return true;
            }
        }
        // Check if we are in any of the child menues
        for(MenuEntry childMenu : topMenu.getSubMenuEntries()) {
            Page childMenuPage = childMenu.getPage();
            if(childMenuPage != null) {
                if(page.getUrlName().equals(childMenu.getPage().getUrlName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<MenuEntry> getFavoritesForPage(Page page) {
        List<MenuEntry> favList = new ArrayList<>();

        Set<MenuEntry> menuEntrySet = page.getMenuEntries();
        if (menuEntrySet == null || menuEntrySet.isEmpty()) {
            return favList;
        }

        for (MenuEntry me : menuEntrySet) {
            MenuEntry parentEntry = me.getParent();
            if (parentEntry != null) {
                Set<MenuEntry> candidates = parentEntry.getSubMenuEntries();
                for (MenuEntry candidate : candidates) {
                    if (candidate.getFavorite() != null && candidate.getFavorite() > 0) {
                        favList.add(candidate);
                    }
                }
            }
        }

        Collections.sort(favList, new Comparator<MenuEntry>() {
            @Override
            public int compare(MenuEntry o1, MenuEntry o2) {
                if (o1.getFavorite() == null || o2.getFavorite() == null) {
                    return 0;
                }
                if (o1.getFavorite() == o2.getFavorite()) {
                    return 0;
                }
                if (o1.getFavorite() > o2.getFavorite()) {
                    return 1;
                }
                return -1;
            }
        });

        return favList;
    }

}
