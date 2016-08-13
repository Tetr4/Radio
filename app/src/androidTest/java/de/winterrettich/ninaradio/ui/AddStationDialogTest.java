package de.winterrettich.ninaradio.ui;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.winterrettich.ninaradio.MockApplication;
import de.winterrettich.ninaradio.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.PositionAssertions.isAbove;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.hasErrorText;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isSelected;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.isEmptyOrNullString;

@RunWith(AndroidJUnit4.class)
public class AddStationDialogTest {

    private static final String STATION_NAME = "Test Station";
    private static final String STATION_URL = "http://player.ffn.de/ffnstream.mp3";

    @Before
    public void setup() {
        Context context = InstrumentationRegistry.getTargetContext().getApplicationContext();
        MockApplication app = (MockApplication) context;
        app.clearDatabase();
    }

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(MainActivity.class);

    public void testLayout() {
        openAddStationDialog();

        // Check if edittext is selected
        onView(withId(R.id.station_name)).check(matches(isSelected()));

        // Check edittext positions
        onView(withId(R.id.station_name)).check(isAbove(withId(R.id.station_url)));
    }

    @Test
    public void testAddStation_valid() {
        openAddStationDialog();

        // Add a station via the dialog
        onView(withId(R.id.station_name)).perform(typeText(STATION_NAME));
        onView(withId(R.id.station_url)).perform(typeText(STATION_URL), closeSoftKeyboard());
        onView(withId(android.R.id.button1)).perform(click()); // OK button

        // Check that the station is added
        onView(withId(R.id.recycler_view)).check(matches(hasDescendant(withText(STATION_NAME))));
    }

    @Test
    public void testAddStation_invalid() {
        openAddStationDialog();

        // Click OK without entering a name
        onView(withId(android.R.id.button1)).perform(click());

        // Check that an error is displayed
        onView(withId(R.id.station_name)).check(matches(hasErrorText(not(isEmptyOrNullString()))));

        // Enter a valid name but not url and click ok
        onView(withId(R.id.station_name)).perform(typeText(STATION_NAME));
        onView(withId(android.R.id.button1)).perform(click());

        // Check that an error is displayed
        onView(withId(R.id.station_url)).check(matches(hasErrorText(not(isEmptyOrNullString()))));
    }

    @Test
    public void testAddStation_cancel() {
        openAddStationDialog();

        // Enter station data but click cancel
        onView(withId(R.id.station_name)).perform(typeText(STATION_NAME));
        onView(withId(R.id.station_url)).perform(typeText(STATION_URL), closeSoftKeyboard());
        onView(withId(android.R.id.button2)).perform(click()); // cancel button

        // Check that no station is added
        onView(withId(R.id.recycler_view)).check(matches(not(hasDescendant(any(View.class)))));
    }

    private void openAddStationDialog() {
        // Click on add icon in menu
        onView(ViewMatchers.withId(R.id.action_add_station)).perform(click());

        // Check if dialog (actually edittext in dialog) is displayed
        onView(withId(R.id.station_name)).inRoot(isDialog()).check(matches(isDisplayed()));
    }

}
