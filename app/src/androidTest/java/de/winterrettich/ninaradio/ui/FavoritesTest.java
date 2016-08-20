package de.winterrettich.ninaradio.ui;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import de.winterrettich.ninaradio.MockApplication;
import de.winterrettich.ninaradio.R;
import de.winterrettich.ninaradio.event.DatabaseEvent;
import de.winterrettich.ninaradio.model.Station;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static de.winterrettich.ninaradio.ui.DiscoverTest.performSearch;
import static de.winterrettich.ninaradio.ui.DiscoverTest.swipeToDiscoverTab;
import static de.winterrettich.ninaradio.ui.DiscoverTest.swipeToFavoriteTab;
import static org.hamcrest.CoreMatchers.not;

public class FavoritesTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(MainActivity.class);

    @Before
    public void clearDatabase() {
        Context context = InstrumentationRegistry.getTargetContext().getApplicationContext();
        MockApplication app = (MockApplication) context;
        app.clearDatabase();
    }

    @Test
    public void testAddFavorite() throws Exception {
        // when
        swipeToDiscoverTab();
        performSearch();
        // click favorite button in result element
        onView(withId(R.id.favorite_button)).perform(click());
        swipeToFavoriteTab();

        // then
        // check if it is added to favorites
        onView(withId(R.id.favorites_list)).check(
                matches(hasDescendant(withText(DiscoverTest.STATION_NAME))));
    }

    @Test
    public void testRemoveFavorite() throws Exception {
        // given
        addStation();
        onView(withId(R.id.favorites_list)).check(
                matches(hasDescendant(withText(DiscoverTest.STATION_NAME))));

        // when
        onView(withText(DiscoverTest.STATION_NAME)).perform(longClick());
        onView(withId(R.id.action_delete)).perform(click());

        // then
        onView(withId(R.id.favorites_list)).check(
                matches(not(hasDescendant(withText(DiscoverTest.STATION_NAME)))));
    }

    private void addStation() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                Station station = new Station(DiscoverTest.STATION_NAME, "");
                MockApplication.sBus.post(new DatabaseEvent(DatabaseEvent.Operation.CREATE_STATION, station));
            }
        });
    }

}
