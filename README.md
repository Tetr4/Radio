# Radio
A simple web radio player, which uses a lot of libraries:
+ **Appcompat**: Downward compatibility to version 16 (Android 4.1 - JELLY_BEAN) 
+ **Design**: Snackbar + FloatingActionButton
+ **[MultiSelect](https://github.com/bignerdranch/recyclerview-multiselect) RecyclerView**: Efficient viewholder pattern + nice animation/layouting capabilities
+ **ActiveAndroid**: Data persistence through object relation mapping (ORM) of Java objects to SQL rows
+ **Otto**: Heavy use of [events](/app/src/main/java/de/winterrettich/ninaradio/event) to remove spaghetti connections between fragments, services, etc

The app supports system callbacks, like audiofocus gain or loss (e.g. when receiving a call), headphone disconnects or media button clicks.
It also uses a [service](/app/src/main/java/de/winterrettich/ninaradio/service/RadioPlayerService.java) and notification for background playback
 and allows adding/removing of stations.
