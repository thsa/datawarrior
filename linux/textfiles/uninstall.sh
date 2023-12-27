#!/bin/sh
if [ `id -u` != 0 ] ; then
   echo "This script must be run as root" 
   exit 1
fi

xdg-desktop-icon uninstall openmolecules-datawarrior.desktop
xdg-desktop-menu uninstall --mode system openmolecules-datawarrior.desktop
xdg-mime uninstall /opt/datawarrior/openmolecules-datawarrior.xml
xdg-icon-resource uninstall --size 48 --mode system openmolecules-datawarrior
xdg-icon-resource uninstall --size 64 --mode system openmolecules-datawarrior
xdg-icon-resource uninstall --size 96 --mode system openmolecules-datawarrior
xdg-icon-resource uninstall --size 128 --mode system openmolecules-datawarrior
xdg-icon-resource uninstall --context mimetypes --size 48 --mode system --noupdate application-vnd.openmolecules.dwam
xdg-icon-resource uninstall --context mimetypes --size 48 --mode system --noupdate application-vnd.openmolecules.dwaq
xdg-icon-resource uninstall --context mimetypes --size 48 --mode system --noupdate application-vnd.openmolecules.dwar
xdg-icon-resource uninstall --context mimetypes --size 48 --mode system --noupdate application-vnd.openmolecules.dwas
xdg-icon-resource uninstall --context mimetypes --size 48 --mode system --noupdate application-vnd.openmolecules.dwat
xdg-icon-resource uninstall --context mimetypes --size 48 --mode system --noupdate application-vnd.openmolecules.sdf
xdg-icon-resource uninstall --context mimetypes --size 64 --mode system --noupdate application-vnd.openmolecules.dwam
xdg-icon-resource uninstall --context mimetypes --size 64 --mode system --noupdate application-vnd.openmolecules.dwaq
xdg-icon-resource uninstall --context mimetypes --size 64 --mode system --noupdate application-vnd.openmolecules.dwar
xdg-icon-resource uninstall --context mimetypes --size 64 --mode system --noupdate application-vnd.openmolecules.dwas
xdg-icon-resource uninstall --context mimetypes --size 64 --mode system --noupdate application-vnd.openmolecules.dwat
xdg-icon-resource uninstall --context mimetypes --size 64 --mode system --noupdate application-vnd.openmolecules.sdf
xdg-icon-resource uninstall --context mimetypes --size 96 --mode system --noupdate application-vnd.openmolecules.dwam
xdg-icon-resource uninstall --context mimetypes --size 96 --mode system --noupdate application-vnd.openmolecules.dwaq
xdg-icon-resource uninstall --context mimetypes --size 96 --mode system --noupdate application-vnd.openmolecules.dwar
xdg-icon-resource uninstall --context mimetypes --size 96 --mode system --noupdate application-vnd.openmolecules.dwas
xdg-icon-resource uninstall --context mimetypes --size 96 --mode system --noupdate application-vnd.openmolecules.dwat
xdg-icon-resource uninstall --context mimetypes --size 96 --mode system --noupdate application-vnd.openmolecules.sdf
xdg-icon-resource uninstall --context mimetypes --size 128 --mode system --noupdate application-vnd.openmolecules.dwam
xdg-icon-resource uninstall --context mimetypes --size 128 --mode system --noupdate application-vnd.openmolecules.dwaq
xdg-icon-resource uninstall --context mimetypes --size 128 --mode system --noupdate application-vnd.openmolecules.dwar
xdg-icon-resource uninstall --context mimetypes --size 128 --mode system --noupdate application-vnd.openmolecules.dwas
xdg-icon-resource uninstall --context mimetypes --size 128 --mode system --noupdate application-vnd.openmolecules.dwat
xdg-icon-resource uninstall --context mimetypes --size 128 --mode system --noupdate application-vnd.openmolecules.sdf
xdg-icon-resource forceupdate --mode system

rm -rf /opt/datawarrior

