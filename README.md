# AndroidCustomView

Custom View with Android Studio
=================================
Slide to Select view , grid view ,CustomView

custom slide to select view 

Usage
-----
Here is how to use this project.
* add lintOptions and dexOptions in gradle
```
    lintOptions {
        checkReleaseBuilds false
        // Or, if you prefer, you can continue to check for errors in release builds,
        // but continue the build even when errors are found:
        abortOnError false
        disable "ResourceType"

    }

    dexOptions {
        javaMaxHeapSize "4g"
    }
 ```
* add multiDexEnabled in gradle
```
   defaultConfig {
        applicationId ".........."
        minSdkVersion 23
       ....
       ....
       ....
        multiDexEnabled true
    }
 ```

preivew
-----
￼￼￼![alt text](https://serving.photos.photobox.com/47577444b4e98c1b836779e3c16fbe1fcabfad5e9711a1473a79d36de2756c2ca3058b42.jpg)
