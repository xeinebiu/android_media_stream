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
            implementation 'com.github.xeinebiu:android_media_stream:1.6.0'
	}

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
    1.6.0
        - Update dependencies
    1.5.0
        - Update dependencies
    1.4.0
        - Update dependencies
    1.3.0
        - Use Ktor as embeded server
        - Fix video not playing
        - Move port to resources for overwrite possibilities
        - Add headers for subtitles
    1.2.0
        - Lazy load subtitles
    1.1.0
        - Support subtitles
    1.0.1
	    - Rename module
    1.0.0
        - Initial Release
