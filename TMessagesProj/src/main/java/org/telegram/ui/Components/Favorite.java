package org.telegram.ui.Components;

import org.telegram.messenger.ApplicationLoader;
import java.util.ArrayList;

public class Favorite
{
    private static final String TAG = "Favorite";
    private static Favorite Instance = null;

    private ArrayList<Long> list;

    public static Favorite getInstance()
    {
        Favorite localInstance = Instance;
        if (localInstance == null)
        {
            Instance = localInstance = new Favorite();
        }
        return localInstance;
    }

    public Favorite()
    {
        list = ApplicationLoader.databaseHandler.getList();
    }

    public ArrayList<Long> getList()
    {
        return list;
    }

    public void addFavorite(Long id)
    {
        list.add(id);
        ApplicationLoader.databaseHandler.addFavorite(id);
    }

    public void deleteFavorite(Long id)
    {
        list.remove(id);
        ApplicationLoader.databaseHandler.deleteFavorite(id);
    }

    public boolean isFavorite(Long id)
    {
        return list.contains(id);
    }

    public int getCount()
    {
        return list.size();
    }

}

