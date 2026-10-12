#!/usr/bin/env bash
# Instala a build portable no sistema e remove a instalação anterior.
set -euo pipefail

PROJ="${1:-/auxiliar/desenvolvimento/projetos/open-filebot}"
PORTABLE="$PROJ/dist/FileBot_4.9.1-portable"

if [ ! -x "$PORTABLE/filebot" ]; then
	echo "ERRO: build portable não encontrada em $PORTABLE" >&2
	exit 1
fi

echo "==> Removendo instalação anterior"
rm -f /usr/bin/filebot
rm -rf /usr/share/filebot
rm -f /usr/share/applications/filebot.desktop /usr/share/applications/filebot-license.desktop

echo "==> Instalando em /opt/filebot"
mkdir -p /opt/filebot
rm -rf /opt/filebot/*
cp -a "$PORTABLE/." /opt/filebot/
chmod 755 /opt/filebot/filebot /opt/filebot/filebot.sh
find /opt/filebot/lib -type f -exec chmod 755 {} +
ln -sf /opt/filebot/filebot /usr/local/bin/filebot

echo "==> Instalando ícones"
mkdir -p /usr/share/icons/hicolor/scalable/apps /usr/share/pixmaps
cp "$PROJ/installer/icons/filebot.svg" /usr/share/icons/hicolor/scalable/apps/filebot.svg
cp "$PROJ/installer/icons/filebot.svg" /usr/share/pixmaps/filebot.svg
if [ -f "$PROJ/installer/icons/icon256.png" ]; then
	cp "$PROJ/installer/icons/icon256.png" /usr/share/pixmaps/filebot.png
else
	cp "$PROJ/installer/icons/filebot-round.png" /usr/share/pixmaps/filebot.png
fi

echo "==> Instalando atalhos de menu"
cat > /usr/share/applications/filebot.desktop <<'EOF'
[Desktop Entry]
Type=Application
Name=FileBot
Comment=The ultimate TV and Movie Renamer
MimeType=inode/directory;video/*;audio/*;text/*;application/*;
Exec=filebot %F
Icon=filebot
Categories=AudioVideo;Video;Utility;FileTools;
StartupNotify=true
StartupWMClass=net-filebot-Main
EOF

cat > /usr/share/applications/filebot-license.desktop <<'EOF'
[Desktop Entry]
Type=Application
Name=FileBot
Comment=Import FileBot License File
MimeType=application/x-psm;application/x-filebot-license
Exec=filebot --license %F
Icon=filebot
NoDisplay=true
StartupNotify=true
StartupWMClass=net-filebot-Main
EOF

echo "==> Atualizando caches"
update-desktop-database /usr/share/applications 2>/dev/null || true
gtk-update-icon-cache -f -t /usr/share/icons/hicolor 2>/dev/null || true

echo "==> Verificação"
ls -la /opt/filebot/ | head -20
echo
ls -la /usr/local/bin/filebot
echo "which: $(command -v filebot)"
echo
filebot -version
