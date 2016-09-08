<img src="https://cloud.githubusercontent.com/assets/3826929/18364970/e3bd4b54-7610-11e6-92c5-bcbe5fa7524b.png" title="Radio" align="right" height="60" />
# Radio

A simple web radio player, which uses a lot of libraries:
+ **Appcompat**: Downward compatibility to version 16 (Android 4.1 - JELLY_BEAN) 
+ **Design**: Snackbar + FloatingActionButton
+ **[MultiSelect](https://github.com/bignerdranch/recyclerview-multiselect) RecyclerView**: Efficient viewholder pattern + nice animation/layouting capabilities
+ **ActiveAndroid**: Data persistence through object relation mapping (ORM) of Java objects to SQL rows
+ **Otto**: Heavy use of [events](/app/src/main/java/de/winterrettich/ninaradio/event) to decouple fragments, services, etc
+ **Retrofit + OkHttp**: Querying of the [RadioTime API](http://opml.radiotime.com/) to discover new stations
+ **JUnit + Hamcrest + Espresso**: Automated UI tests
+ **Mockito + Retrofit-Mock**: Mocking of dependencies for decoupled testing

The app supports system callbacks, like audiofocus gain or loss (e.g. when receiving a call), headphone disconnects or media button clicks.
It also uses a [service](/app/src/main/java/de/winterrettich/ninaradio/service/RadioPlayerService.java) and notification for background playback
 and allows searching, adding and removing of stations.
