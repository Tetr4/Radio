package de.winterrettich.ninaradio.ui;

import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import de.winterrettich.ninaradio.R;

public class RadioPagerAdapter extends FragmentPagerAdapter {
    private static final int PAGE_COUNT = 2;

    private static final int STATION_LIST_FRAGMENT_POSITION = 0;
    private static final int DISCOVER_FRAGMENT_POSITION = 1;

    private final Resources mResources;

    public RadioPagerAdapter(FragmentManager fm, Resources resources) {
        super(fm);
        mResources = resources;
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case STATION_LIST_FRAGMENT_POSITION:
                return new FavoriteStationsFragment();

            case DISCOVER_FRAGMENT_POSITION:
                return new DiscoverFragment();

            default:
                return null;
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case STATION_LIST_FRAGMENT_POSITION:
                // TODO string resource
                return mResources.getString(R.string.my_stations);

            case DISCOVER_FRAGMENT_POSITION:
                // TODO string resource
                return mResources.getString(R.string.discover);

            default:
                return null;
        }
    }

}
