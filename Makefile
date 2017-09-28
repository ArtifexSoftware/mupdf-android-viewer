# This is a very simple Makefile that calls 'gradlew' to do the heavy lifting.
#
# The tool 'adb' must be on the path, so that we can find the Android SDK.

ANDROID_HOME := $(shell which adb | sed 's,/platform-tools/adb,,')

debug: assembleDebug
release: assembleRelease
install: installDebug

assembleDebug:
	ANDROID_HOME=$(ANDROID_HOME) ./gradlew assembleDebug
assembleRelease:
	ANDROID_HOME=$(ANDROID_HOME) ./gradlew assembleRelease
installDebug:
	ANDROID_HOME=$(ANDROID_HOME) ./gradlew installDebug
lint:
	ANDROID_HOME=$(ANDROID_HOME) ./gradlew lint
archive:
	ANDROID_HOME=$(ANDROID_HOME) ./gradlew uploadArchives
sync: archive
	rsync -av MAVEN/com/ ghostscript.com:/var/www/maven.ghostscript.com/com/

run: installDebug ;
	adb shell am start -n com.artifex.mupdf.demo/.LibraryActivity

clean:
	rm -rf .gradle build
	rm -rf jni/.externalNativeBuild jni/.gradle jni/build
	rm -rf lib/.gradle lib/build
	rm -rf app/.gradle app/build
