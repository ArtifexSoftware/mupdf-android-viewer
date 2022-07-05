# This is a very simple Makefile that calls 'gradlew' to do the heavy lifting.

default: debug

debug:
	./gradlew --warning-mode=all assembleDebug bundleDebug
release:
	./gradlew --warning-mode=all assembleRelease bundleRelease
install:
	./gradlew --warning-mode=all installDebug
install-release:
	./gradlew --warning-mode=all installRelease
lint:
	./gradlew --warning-mode=all lint
archive:
	./gradlew --warning-mode=all publishReleasePublicationToLocalRepository
sync: archive
	rsync -av --chmod=g+w --chown=:gs-priv $(HOME)/MAVEN/com/ ghostscript.com:/var/www/maven.ghostscript.com/com/

run: install
	adb shell am start -n com.artifex.mupdf.viewer.app/.LibraryActivity
run-release: install-release
	adb shell am start -n com.artifex.mupdf.viewer.app/.LibraryActivity

tarball: release
	cp app/build/outputs/apk/release/app-universal-release.apk \
		mupdf-$(shell git describe --tags)-android-viewer.apk

clean:
	rm -rf .gradle build
	rm -rf jni/.cxx jni/.externalNativeBuild jni/.gradle jni/build
	rm -rf lib/.gradle lib/build
	rm -rf app/.gradle app/build
