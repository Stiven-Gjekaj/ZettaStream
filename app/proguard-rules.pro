# Rules for the release build.

# libtorrent4j calls its Java classes from native code through JNI.
-keep class org.libtorrent4j.swig.** { *; }
-keep class org.libtorrent4j.** { *; }
