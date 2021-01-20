## DataWarrior
*DataWarrior* is a versatile, interactive data analysis and visualization applications.
Especially, its cheminformatics functionality makes it a unique tool in academia and industry.
It runs on all major platforms. Pre-built installers exist for Linux, Macintosh and Windows.
Software installers, documentation, sample data, and a support forum are available on
www.openmolecules.org/datawarrior.

*DataWarrior* is built on the open-source projects *OpenChemLib* and *FXMolViewer*. 

### Dependencies
Apart from a working JDK8 with JavaFX, *DataWarrior* needs some open-source dependencies.
All required dependency files are provided as part of this project in the ./lib folder:
* OpenChemLib: Cheminformatics base functionality to handle molecules and reactions
* SunFlow source code and janino.jar: Ray-Tracer to build photo-realistic images of 3-dimensional scenes
* mmtf: Java library to download and parse binary structure files from the PDB-database

### How to download the project
git clone https://github.com/thsa/datawarrior.git

### How to build the project
On Linux or Macintosh just run the 'buildDataWarrior' shell script.

### How to run the project
After building it just run the 'runDataWarrior' shell script.

### Platform Integration
Ideally, *DataWarrior* should be installed in a platform specific way to register its file
extentions and to properly display file icons. The platform integration is not (yet) part of
this project. Thus, if you extend *DataWarrior* using the source code and if you don't do the
platform integration yourself, then it is recommended to use an official installer and just
replace the original datawarrior.jar file with the freshly built one.

Unfortunately, this does not work on Windows platforms, because a proper platform integration on
Windows requires the application to be an .exe file. Therefore, on Windows the datawarrior.jar,
the application icon and all document icons are embedded in the DataWarrior.exe file as resources.

Note: The datawarrior.jar file of the platform specific installers are shrunk, which makes the
datawarrior.jar file significantly smaller, because unused classes and methods are removed
from the byte code.

### How to contribute
Contact the author under the e-mail shown on www.openmolecules.org


### License
*DataWarrior*. Copyright (C) 2021 Idorsia Pharmaceuticals Ltd.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
