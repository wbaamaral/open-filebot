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
JAVA="java"
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
	JAVA="$JAVA_HOME/bin/java"
fi
JAVA_VER=$("$JAVA" -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VER" = "1" ]; then
	JAVA_VER=$("$JAVA" -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f2)
fi
if [ -z "$JAVA_VER" ] || [ "$JAVA_VER" -lt 25 ] 2>/dev/null; then
	if [ -x "$HOME/.sdkman/candidates/java/current/bin/java" ]; then
		JAVA="$HOME/.sdkman/candidates/java/current/bin/java"
	elif [ -x "$HOME/.sdkman/candidates/java/25.0.3-tem/bin/java" ]; then
		JAVA="$HOME/.sdkman/candidates/java/25.0.3-tem/bin/java"
	elif [ -d "$HOME/.sdkman/candidates/java" ]; then
		SDKMAN_JAVA=$(find "$HOME/.sdkman/candidates/java" -maxdepth 2 -name "java" -path "*/bin/java" 2>/dev/null | grep -E "25\." | head -n 1)
		if [ -n "$SDKMAN_JAVA" ] && [ -x "$SDKMAN_JAVA" ]; then
			JAVA="$SDKMAN_JAVA"
		fi
	fi
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
	-Djna.boot.library.path="$PACKAGE_LIBRARY_PATH" \
	-Djna.library.path="$PACKAGE_LIBRARY_PATH:$LD_LIBRARY_PATH" \
	-Djava.library.path="$PACKAGE_LIBRARY_PATH:$LD_LIBRARY_PATH" \
	-Dnet.filebot.AcoustID.fpcalc="$FPCALC_BIN" \
	-Dapplication.dir="$APP_DATA" \
	-Duser.home="$USER_HOME" \
	-Djava.io.tmpdir="$APP_DATA/tmp" \
	-Djava.util.prefs.PreferencesFactory=net.filebot.util.prefs.FilePreferencesFactory \
	-Dnet.filebot.util.prefs.file="$APP_DATA/prefs.properties" \
	$JAVA_OPTS \
	-classpath "$APP_ROOT/*" \
	@{main.class} "$@"
