# This is a very simple Makefile that calls 'gradlew' to do the heavy lifting.

default: debug

generate:
	if [ -f jni/Makefile ]; then make -C jni generate; fi
debug: generate
	./gradlew --warning-mode=all assembleDebug bundleDebug
release: generate
	./gradlew --warning-mode=all assembleRelease bundleRelease
install: generate
	./gradlew --warning-mode=all installDebug
lint:
	./gradlew --warning-mode=all lint
archive: generate
	./gradlew --warning-mode=all uploadArchives
sync: archive
	rsync -av MAVEN/com/ ghostscript.com:/var/www/maven.ghostscript.com/com/

run: install
	adb shell am start -n com.artifex.mupdf.viewer.app/.LibraryActivity

clean:
	rm -rf .gradle build
	rm -rf jni/.externalNativeBuild jni/.gradle jni/build jni/libmupdf/generated
	rm -rf lib/.gradle lib/build
	rm -rf app/.gradle app/build
