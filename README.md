<img src="https://cloud.githubusercontent.com/assets/3826929/18364970/e3bd4b54-7610-11e6-92c5-bcbe5fa7524b.png" title="Radio" align="right" height="60" />

# Radio

A simple web radio player, which uses a lot of libraries:
+ **Appcompat**: Downward compatibility to version 16 (Android 4.1 - JELLY_BEAN) 
+ **Design**: Snackbar + FloatingActionButton
+ **[MultiSelect](https://github.com/bignerdranch/recyclerview-multiselect) RecyclerView**: Efficient viewholder pattern + nice animation/layouting capabilities
+ **[MaterialFavoriteButton](https://github.com/IvBaranov/MaterialFavoriteButton)**: Animated favorite button
+ **ActiveAndroid**: Data persistence through object relation mapping (ORM) of Java objects to SQL rows
+ **Otto**: Heavy use of [events](/app/src/main/java/de/winterrettich/ninaradio/event) to decouple fragments, services, etc
+ **Retrofit + OkHttp**: Querying of the [RadioTime API](http://opml.radiotime.com/) to discover new stations
+ **JUnit + Hamcrest + Espresso**: Automated UI tests
+ **Mockito + Retrofit-Mock**: Mocking of dependencies for decoupled testing

The app supports system callbacks, like audiofocus gain or loss (e.g. when receiving a call), headphone disconnects or media button clicks.  
It also uses a [service](/app/src/main/java/de/winterrettich/ninaradio/service/RadioPlayerService.java) and notification for background playback and allows searching, adding and removing of stations.  
The current song's title and artist is [decoded](/app/src/main/java/de/winterrettich/ninaradio/metadata/MetaDataDecoder.java) from the stream's shoutcast metadata.  
<br>

<p align="center">
<img src="https://cloud.githubusercontent.com/assets/3826929/23171480/e1513762-f852-11e6-9609-9f3c60e041d4.png" title="Buffering" height="350" />
&nbsp;
<img src="https://cloud.githubusercontent.com/assets/3826929/23171316/6c8e02fc-f852-11e6-937b-278abd99029c.png" title="Playback &amp; Notification" height="350" />
&nbsp;
<img src="https://cloud.githubusercontent.com/assets/3826929/23171653/66df6b24-f853-11e6-855c-719f5a4197fd.png" title="Undo" height="350" />
&nbsp;
<img src="https://cloud.githubusercontent.com/assets/3826929/23171320/6f25bc76-f852-11e6-9c1a-5d5cbeaeac8f.png" title="Discovering new stations" height="350" />
</p>
