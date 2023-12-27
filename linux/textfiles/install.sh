#!/bin/sh
if [ `id -u` != 0 ] ; then
   echo "This script must be run as root" 
   exit 1
fi

# copy the application and sample files to /opt
cp -r datawarrior /opt/
cp uninstall.sh /opt/datawarrior/
cp resources/openmolecules-datawarrior.xml /opt/datawarrior/
mkdir /opt/datawarrior/update
chmod 755 /opt/datawarrior
chmod 755 /opt/datawarrior/datawarrior
chmod 777 /opt/datawarrior/update

# Use the freedesktop.org tools to install application and document icons,
# register mime- and file-types and install launchers
# in a generic way that is supposed to cover major X desktops like KDE and Gnome.
xdg-desktop-icon install resources/openmolecules-datawarrior.desktop
xdg-desktop-menu install --mode system resources/openmolecules-datawarrior.desktop
xdg-mime install resources/openmolecules-datawarrior.xml
xdg-icon-resource install --size 48 --mode system resources/48x48/openmolecules-datawarrior.png
xdg-icon-resource install --size 64 --mode system resources/64x64/openmolecules-datawarrior.png
xdg-icon-resource install --size 96 --mode system resources/96x96/openmolecules-datawarrior.png
xdg-icon-resource install --size 128 --mode system resources/128x128/openmolecules-datawarrior.png
xdg-icon-resource install --context mimetypes --size 48 --mode system --noupdate resources/48x48/application-vnd.openmolecules.dwam.png application-vnd.openmolecules.dwam
xdg-icon-resource install --context mimetypes --size 48 --mode system --noupdate resources/48x48/application-vnd.openmolecules.dwaq.png application-vnd.openmolecules.dwaq
xdg-icon-resource install --context mimetypes --size 48 --mode system --noupdate resources/48x48/application-vnd.openmolecules.dwar.png application-vnd.openmolecules.dwar
xdg-icon-resource install --context mimetypes --size 48 --mode system --noupdate resources/48x48/application-vnd.openmolecules.dwas.png application-vnd.openmolecules.dwas
xdg-icon-resource install --context mimetypes --size 48 --mode system --noupdate resources/48x48/application-vnd.openmolecules.dwat.png application-vnd.openmolecules.dwat
xdg-icon-resource install --context mimetypes --size 48 --mode system --noupdate resources/48x48/application-vnd.openmolecules.sdf.png application-vnd.openmolecules.sdf
xdg-icon-resource install --context mimetypes --size 64 --mode system --noupdate resources/64x64/application-vnd.openmolecules.dwam.png application-vnd.openmolecules.dwam
xdg-icon-resource install --context mimetypes --size 64 --mode system --noupdate resources/64x64/application-vnd.openmolecules.dwaq.png application-vnd.openmolecules.dwaq
xdg-icon-resource install --context mimetypes --size 64 --mode system --noupdate resources/64x64/application-vnd.openmolecules.dwar.png application-vnd.openmolecules.dwar
xdg-icon-resource install --context mimetypes --size 64 --mode system --noupdate resources/64x64/application-vnd.openmolecules.dwas.png application-vnd.openmolecules.dwas
xdg-icon-resource install --context mimetypes --size 64 --mode system --noupdate resources/64x64/application-vnd.openmolecules.dwat.png application-vnd.openmolecules.dwat
xdg-icon-resource install --context mimetypes --size 64 --mode system --noupdate resources/64x64/application-vnd.openmolecules.sdf.png application-vnd.openmolecules.sdf
xdg-icon-resource install --context mimetypes --size 96 --mode system --noupdate resources/96x96/application-vnd.openmolecules.dwam.png application-vnd.openmolecules.dwam
xdg-icon-resource install --context mimetypes --size 96 --mode system --noupdate resources/96x96/application-vnd.openmolecules.dwaq.png application-vnd.openmolecules.dwaq
xdg-icon-resource install --context mimetypes --size 96 --mode system --noupdate resources/96x96/application-vnd.openmolecules.dwar.png application-vnd.openmolecules.dwar
xdg-icon-resource install --context mimetypes --size 96 --mode system --noupdate resources/96x96/application-vnd.openmolecules.dwas.png application-vnd.openmolecules.dwas
xdg-icon-resource install --context mimetypes --size 96 --mode system --noupdate resources/96x96/application-vnd.openmolecules.dwat.png application-vnd.openmolecules.dwat
xdg-icon-resource install --context mimetypes --size 96 --mode system --noupdate resources/96x96/application-vnd.openmolecules.sdf.png application-vnd.openmolecules.sdf
xdg-icon-resource install --context mimetypes --size 128 --mode system --noupdate resources/128x128/application-vnd.openmolecules.dwam.png application-vnd.openmolecules.dwam
xdg-icon-resource install --context mimetypes --size 128 --mode system --noupdate resources/128x128/application-vnd.openmolecules.dwaq.png application-vnd.openmolecules.dwaq
xdg-icon-resource install --context mimetypes --size 128 --mode system --noupdate resources/128x128/application-vnd.openmolecules.dwar.png application-vnd.openmolecules.dwar
xdg-icon-resource install --context mimetypes --size 128 --mode system --noupdate resources/128x128/application-vnd.openmolecules.dwas.png application-vnd.openmolecules.dwas
xdg-icon-resource install --context mimetypes --size 128 --mode system --noupdate resources/128x128/application-vnd.openmolecules.dwat.png application-vnd.openmolecules.dwat
xdg-icon-resource install --context mimetypes --size 128 --mode system --noupdate resources/128x128/application-vnd.openmolecules.sdf.png application-vnd.openmolecules.sdf
xdg-icon-resource forceupdate --mode system

# xdg-desktop-icon seems to leave the file with 700 protection
chmod 777 ~/Desktop/openmolecules-datawarrior.desktop

