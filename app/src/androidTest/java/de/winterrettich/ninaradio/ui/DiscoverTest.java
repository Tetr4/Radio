package de.winterrettich.ninaradio.ui;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.widget.EditText;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.winterrettich.ninaradio.MockApplication;
import de.winterrettich.ninaradio.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;

@RunWith(AndroidJUnit4.class)
public class DiscoverTest {
    static final String SEARCH_TEXT = "Der Barde";
    static final String STATION_NAME = SEARCH_TEXT;

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(MainActivity.class);

    @Before
    public void clearDatabase() {
        Context context = InstrumentationRegistry.getTargetContext().getApplicationContext();
        MockApplication app = (MockApplication) context;
        app.clearDatabase();
    }

    @Test
    public void testSwipeToTab() throws Exception {
        // given
        onView(withId(R.id.search_view)).check(matches(not(isDisplayed())));

        // when
        swipeToDiscoverTab();

        // then
        onView(withId(R.id.search_view)).check(matches(isDisplayed()));
    }

    @Test
    public void testSearchResult() throws Exception {
        // given
        swipeToDiscoverTab();

        // when
        performSearch();

        // then
        onView(withId(R.id.result_list)).check(matches(hasDescendant(withText(STATION_NAME))));
    }

    static void swipeToDiscoverTab() throws InterruptedException {
        Thread.sleep(100);
        onView(withId(R.id.viewpager)).perform(swipeLeft());
    }

    static void swipeToFavoriteTab() throws InterruptedException {
        Thread.sleep(100);
        onView(withId(R.id.viewpager)).perform(swipeRight());
    }

    static void performSearch() {
        onView(withId(R.id.search_view)).perform(click());
        onView(isAssignableFrom(EditText.class)).perform(
                typeText(SEARCH_TEXT),
                pressImeActionButton(),
                closeSoftKeyboard());
    }
}
