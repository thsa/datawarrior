This describes the complete process to make DataWarrior.dmg with embedded universal JRE21 for OSX

1. If not done before, create project infinitekind-appbundler-9d6ca23b3bee from GitHub
- Install XCode from AppStore
- Download Ant from https://ant.apache.org and install as
    sudo mv apache-ant-1.10.14 /usr/local/
    cd /usr/local
    sudo ln -s apache-ant-1.10.14 ant
  add these lines to /etc/zshrc:
    export ANT_HOME=/usr/local/ant
    export PATH=${PATH}:${ANT_HOME}/bin
- build the project and create appbundler-1.0ea.jar

2. Install the latest JDK and verify that running the following command gives the correct path
> /usr/libexec/java_home

3. Update version, needed jars and copyright notice in build.xml

4. Create a universal JRE (arm64 and x64) from separate Liberica ARM and Intel versions and add that to the DataWarrior.app
   - Download pkg full versions of newest JRE for MacOS for both architectures, place them in macos/jre folder
   - update two path settings in jre/makeUniversalJRE script and run it
   > cd jre
   > ./makeUniversalJRE
   > cd ..

5. build this project to create the DataWarrior.app without sample/reference/macro files in macosx/dist
> ant

6. create dw_master.dmg, if not done before or if JRE was updated (see howToCreateMasterDMG.txt)

7. Run script to create datawarrior.dmg from dw_master.dmg by adding all needed files
> ./makedmg

