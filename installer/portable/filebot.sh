#!/bin/sh
PRG="$0"

# resolve relative symlinks
while [ -h "$PRG" ] ; do
	ls=`ls -ld "$PRG"`
	link=`expr "$ls" : '.*-> \(.*\)$'`
	if expr "$link" : '/.*' > /dev/null; then
		PRG="$link"
	else
		PRG="`dirname "$PRG"`/$link"
	fi
done

# get canonical path
WORKING_DIR=`pwd`
PRG_DIR=`dirname "$PRG"`
APP_ROOT=`cd "$PRG_DIR" && pwd`

# add package lib folder to library path
PACKAGE_LIBRARY_PATH="$APP_ROOT/lib/$(uname -m)"
if [ -e /usr/lib/libmediainfo.so ] || [ -e /usr/lib64/libmediainfo.so ]; then
	LIBRARY_PATH="/usr/lib:/usr/lib64:$PACKAGE_LIBRARY_PATH:$LD_LIBRARY_PATH"
else
	LIBRARY_PATH="$PACKAGE_LIBRARY_PATH:$LD_LIBRARY_PATH"
fi

# restore original working dir
cd "$WORKING_DIR"

# make sure required environment variables are set
if [ -z "$USER" ]; then
	export USER=`whoami`
fi

# force JVM language and encoding settings
export LANG="en_US.UTF-8"
export LC_ALL="en_US.UTF-8"

# choose extractor
EXTRACTOR="ApacheVFS"                   # use Apache Commons VFS2 with junrar plugin
# EXTRACTOR="SevenZipExecutable"        # use the 7z executable
# EXTRACTOR="SevenZipNativeBindings"    # use the lib7-Zip-JBinding.so native library

# select application data folder
if [ -w "$APP_ROOT/data" ] || [ ! -e "$APP_ROOT/data" -a -w "$APP_ROOT" ]; then
	APP_DATA="$APP_ROOT/data"
	USER_HOME="$APP_DATA"
else
	APP_DATA="${XDG_DATA_HOME:-$HOME/.local/share}/filebot"
	USER_HOME="$HOME"
fi
mkdir -p "$APP_DATA/tmp"

# resolve java executable (ensure Java 25+)
java_major() {
	"$1" -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1 | {
		read -r major
		if [ "$major" = "1" ]; then
			"$1" -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f2
		else
			echo "$major"
		fi
	}
}

java_ok() {
	[ -x "$1" ] || return 1
	major=$(java_major "$1")
	[ -n "$major" ] && [ "$major" -ge 25 ] 2>/dev/null
}

JAVA=""
for candidate in \
	${JAVA_HOME:+"$JAVA_HOME/bin/java"} \
	"$(command -v java 2>/dev/null)" \
	"$HOME/.sdkman/candidates/java/current/bin/java" \
	"$HOME/.sdkman/candidates/java/25.0.3-tem/bin/java"
do
	if java_ok "$candidate"; then
		JAVA="$candidate"
		break
	fi
done

if [ -z "$JAVA" ] && [ -d "$HOME/.sdkman/candidates/java" ]; then
	for candidate in $(find "$HOME/.sdkman/candidates/java" -maxdepth 3 -type f -path "*/bin/java" 2>/dev/null); do
		if java_ok "$candidate"; then
			JAVA="$candidate"
			break
		fi
	done
fi

if [ -z "$JAVA" ]; then
	echo "Error: Java 25+ is required but was not found" >&2
	exit 1
fi

# auto-detect headless mode: enable only when no display server is present
HEADLESS_OPT=""
if [ -z "$DISPLAY" ] && [ -z "$WAYLAND_DISPLAY" ]; then
	HEADLESS_OPT="-Djava.awt.headless=true"
fi

# resolve fpcalc binary
FPCALC_BIN="fpcalc"
if [ -x "$PACKAGE_LIBRARY_PATH/fpcalc" ]; then
	FPCALC_BIN="$PACKAGE_LIBRARY_PATH/fpcalc"
fi

# start filebot
exec "$JAVA" @{java.application.options} \
	-Dapplication.deployment=portable \
	$HEADLESS_OPT \
	-Dfile.encoding="UTF-8" \
	-Dsun.jnu.encoding="UTF-8" \
	-Dnet.filebot.Archive.extractor="$EXTRACTOR" \
	-Djna.boot.library.path="$LIBRARY_PATH" \
	-Djna.library.path="$LIBRARY_PATH" \
	-Djava.library.path="$LIBRARY_PATH" \
	-Dnet.filebot.AcoustID.fpcalc="$FPCALC_BIN" \
	-Dapplication.dir="$APP_DATA" \
	-Duser.home="$USER_HOME" \
	-Djava.io.tmpdir="$APP_DATA/tmp" \
	-Djava.util.prefs.PreferencesFactory=net.filebot.util.prefs.FilePreferencesFactory \
	-Dnet.filebot.util.prefs.file="$APP_DATA/prefs.properties" \
	$JAVA_OPTS \
	-classpath "$APP_ROOT/*" \
	@{main.class} "$@"
