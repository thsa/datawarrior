This describes the complete process to make DataWarrior.dmg with embedded universal JRE21 for OSX.

NOTE: In order to natively support Intel and Apple Silicon based architectures we need to use a recent appbundler.
      Unfortunately, it seems that binary launchers created by recent appbundler crash on MacOS Sonoma,
      when used with aa JRE 1.8 (both x86_64 or aarch64; tested with liberica 1.8-392 and Zulu 1.8-392):
      "References to Carbon menus are disallowed with AppKit menu system (see rdar://101002625).
       Use instances of NSMenu and NSMenuItem directly instead."

NOTE: We use the latest theinfinitekind/appbundler (GitHub) to build the DataWarrior.app that goes into the dmg.
      The needed compiled appbundler-1.0ea.jar is part of the project, but if you want to build it yourself,
      you may follow this procedure:
        - Download Ant from https://ant.apache.org and install as
            > sudo mv apache-ant-1.10.14 /usr/local/
            > cd /usr/local
            > sudo ln -s apache-ant-1.10.14 ant
          add these lines to /etc/zshrc:
            > export ANT_HOME=/usr/local/ant
            > export PATH=${PATH}:${ANT_HOME}/bin
        - Download the project from github.com
            > git clone https://github.com/TheInfiniteKind/appbundler.git
                (if you don't have the Xcode command line tools yet, you are promted to install them)
        - build the project and create appbundler-1.0ea.jar
            > cd appbundler
            > ant

1. Verify and possibly update version numbers in build.xml

2. Create a universal JRE (arm64 and x64) from separate Liberica ARM and Intel versions and add that to the DataWarrior.app
   - Download pkg full versions of newest JRE for MacOS for both architectures, place them in macos/jre folder
   - update two path settings in jre/makeUniversalJRE script and run it
   > cd jre
   > ./makeUniversalJRE
   > cd ..

3. build this project to create the DataWarrior.app without sample/reference/macro files in macosx/dist
   > ant

4. create dw_master.dmg, if not done before or if JRE was updated (see howToCreateMasterDMG.txt)

5. Verify that folder 'external_data' contains all examples, macros, plugins, etc, that shall be included in dmg.

6. Run script to create datawarrior.dmg from dw_master.dmg by adding all needed files
   > ./makedmg

