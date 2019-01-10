# This is a very simple Makefile that calls 'gradlew' to do the heavy lifting.

default: debug

generate:
	if [ -f jni/Makefile ]; then make -C jni generate; fi
debug: generate
	./gradlew assembleDebug
release: generate
	./gradlew assembleRelease
install: generate
	./gradlew installDebug
lint:
	./gradlew lint
archive: generate
	./gradlew uploadArchives
sync: archive
	rsync -av MAVEN/com/ ghostscript.com:/var/www/maven.ghostscript.com/com/

run: install
	adb shell am start -n com.artifex.mupdf.viewer.app/.LibraryActivity

clean:
	rm -rf .gradle build
	rm -rf jni/.externalNativeBuild jni/.gradle jni/build jni/libmupdf/generated
	rm -rf lib/.gradle lib/build
	rm -rf app/.gradle app/build
