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
	rsync -av --chmod=g+w --chown=:gs-web \
		$(HOME)/MAVEN/com/artifex/mupdf/viewer/$(shell git describe --tags)/ \
		ghostscript.com:/var/www/maven.ghostscript.com/com/artifex/mupdf/viewer/$(shell git describe --tags)/
	rsync -av --chmod=g+w --chown=:gs-web \
		$(HOME)/MAVEN/com/artifex/mupdf/viewer/maven-metadata.xml* \
		ghostscript.com:/var/www/maven.ghostscript.com/com/artifex/mupdf/viewer/

tarball: release
	cp app/build/outputs/apk/release/app-universal-release.apk \
		mupdf-$(shell git describe --tags)-android-viewer.apk
	cp app/build/outputs/bundle/release/app-release.aab \
		mupdf-$(shell git describe --tags)-android-viewer-app-release.aab
synctarball: tarball
	rsync -av --chmod=g+w --chown=:gs-web \
		mupdf-$(shell git describe --tags)-android-viewer.apk \
		ghostscript.com:/var/www/mupdf.com/downloads/archive/mupdf-$(shell git describe --tags)-android-viewer.apk
	rsync -av --chmod=g+w --chown=:gs-web \
		mupdf-$(shell git describe --tags)-android-viewer-app-release.aab \
		ghostscript.com:/var/www/mupdf.com/downloads/archive/mupdf-$(shell git describe --tags)-android-viewer-app-release.aab

run: install
	adb shell am start -n com.artifex.mupdf.viewer.app/.LibraryActivity
run-release: install-release
	adb shell am start -n com.artifex.mupdf.viewer.app/.LibraryActivity

clean:
	rm -rf .gradle build
	rm -rf jni/.cxx jni/.externalNativeBuild jni/.gradle jni/build
	rm -rf lib/.gradle lib/build
	rm -rf app/.gradle app/build/generated app/build/intermediates app/build/tmp app/build/outputs/logs app/build/outputs/bundle

nuke: clean
	rm -rf app/build
