## Media Stream

Stream your favorite Videos from your Android Device over Network.

---
### Installation
Step 1. 
Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://www.jitpack.io' }
		}
	}
Step 2. Add the dependency

	dependencies {
            implementation 'com.github.xeinebiu:android_media_stream:1.1.0'
	}

---
#### Register Service
````xml
<service
    android:name="com.xeinebiu.media_stream.MediaStreamService"
    android:enabled="true"
    android:label="Media Stream" />
````
---
#### Permissions
Before you use the service, make sure your application is granted Overlay access.
```xml
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

---
### Starting the service
```kotlin
       val streamUrl = MediaStreamService.stream(
            this,
            "Demo Video",
            Uri.parse("https://thepaciellogroup.github.io/AT-browser-tests/video/ElephantsDream.mp4"),
            HashMap()
        )
        AlertDialog.Builder(this)
            .setTitle("Stream")
            .setMessage(streamUrl)
            .show()
```

### Change logs
    1.0.1
	    - Rename module
    1.0.0
        - Initial Release
