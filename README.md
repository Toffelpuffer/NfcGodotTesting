Test implementation of reading NFC Ndef messages in a Godot game. Modifications made directly to the godot game activity ([android/build/src/com/godot/game/GodotApp.java](android/build/src/com/godot/game/GodotApp.java)) to read Ndef data from android intent. 
The application expects tags to contain basic Ndef messages, that will be displayed in the godot game when read.

To make it easy to register android functions and signals emiters to the godot app, the main activity ['dynamically adds a plugin'](https://github.com/Toffelpuffer/NfcGodotTesting/blob/main/android/build/src/com/godot/game/GodotApp.java#L67-L69) to the godot process. 

A method to write Ndef messages to tags is included in the godot game activity, but not used in godot.

To build the project for android either merge the `android/build` folder with the installed/generated godot android build template or just copy the lib folder from the template into the cloned project. (The `godot-lib.template_debug.aar` and `godot-lib.template_release.aar` are not included here due to file size.)

### Possible Other Approach: Puting the reader/writer inside an android plugin
This might be possible in an android plugin by manually pulling the current intent like [this](https://github.com/thimenesup/GodotNFC/blob/master/android/plugins/godot-nfc/src/main/java/org/thimenesup/godotnfc/GodotNFC.java). But having the intent listener from the main activity was more convenient for this proof of concept. And also using the android intent should allow neat stuff like starting the app from scaned tag. I tried it with a plugin [using a broadcast receiver](https://developer.android.com/develop/background-work/background-tasks/broadcasts) for capturing the intent but it did not work, maybe [due to the special handling of NFC intents](https://stackoverflow.com/questions/4853622/android-nfc-tag-received-with-broadcastreceiver).

Extending [`NfcAdapter.ReaderCallback`](https://developer.android.com/reference/android/nfc/NfcAdapter.ReaderCallback) and overriding `onTagDiscovered` like [this](https://github.com/worseproductions/godot-nfc-android-plugin/blob/main/plugin/src/main/java/com/worseproductions/godotnfcandroidplugin/GodotNfcAndroidPlugin.kt) worked, but retrieving the message was less convienient. Altough if your are in the mood for pushing bits around and/or using [other NFC Technologies](https://developer.android.com/reference/android/nfc/tech/TagTechnology) this looks like it should work.

### Resources
- The tag I used (NTAG215): https://www.nxp.com/docs/en/data-sheet/NTAG213_215_216.pdf
- Android NFC Basics: https://developer.android.com/develop/connectivity/nfc/nfc
- Export Godot for Android: https://docs.godotengine.org/en/stable/tutorials/export/exporting_for_android.html
- Good post on writing NDEF Msgs to NFC Tags: https://stackoverflow.com/questions/64920307/how-to-write-ndef-records-to-nfc-tag

### Tips
Thinks that took me a while to figure out and/or helped:

- For deuging on device: Setup android studio to build on your device and open the Logcat output with filter to something like `Godot | process:test | NfcTest`.
- Do not forget to use [JavaJDK 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) as java sdk in Godot and when building with gradlew. On mac (arm) in Godot you set the Java SDK path to `/Library/Java/JavaVirtualMachines/jdk-17.jd` 
