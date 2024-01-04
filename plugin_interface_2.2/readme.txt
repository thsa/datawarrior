DataWarrior Plugin-SDK 2.2 (requires DataWarrior v6.6.0 or dev build 12-June-2023 or newer)
===========================================================================================

This file describes the concept and all steps needed to create a DataWarrior plugin.
A plugin, if put in the plugin directory of a DataWarrior installation, adds one or more new
menu item(s) to the DataWarrior menu. Later, when one of these menu items is selected by the user,
the plugin is asked to perform a corresponding action. For instance, the plugin could calculate
properties for all structures of an open DataWarrior window and add them to a new column.
Or it could retrieve data from an external database and display that in a new DataWarrior window.
Or it could do about anything you can develop in Java with or without accessing DataWarrior
functionality or data.

The plugin SDK provides Java interfaces with methods to interact with DataWarrior
- to add menu items that give access to specific plugin functionality
- to access data of an open DataWarrior window
- to add new columns with data to an open DataWarrior window
- to create a new DataWarrior window and populate it with data
- to pass chemical structures from/to DataWarrior windows in SMILES and Molfile format
- to display an editor in a dialog for (sub-)structure search

Developing a plugin is easy for even an unexperienced Java developer. It does not require
any DataWarrior source code nor any knowledge about it. Everything that is needed is a
properly installed JDK and the files that come with the datawarriorPluginSDK.zip file.


Content
-------

readme.txt                              : this file
com/mycompany/ExamplePluginTask1.java   : simple plugin task example source code; used by 'buildAll'
com/mycompany/ExamplePluginTask2.java   : plugin task example using structure editor; used by 'buildAll'
com/mycompany/ExamplePluginTask1.java   : plugin task example adding columns to DataWarrior table; used by 'buildAll'
org/openmolecules/datawarrior/plugin/I* : plugin related Java interfaces needed to build the plugin
org/openmolecules/datawarrior/plugin/PluginStarter.java : Root class that instantiates example tasks and assigns menu items
org/openmolecules/datawarrior/plugin/PluginInfo.java : Class to display message if plugin jar file is double-clicked
buildAll                                : short Linux/Mac script to build examplePlugin.jar from the source
examplePlugin.jar                       : built example plugin file that can be placed into the DataWarrior 'plugin' folder


Note
----
This documentation refers to Plugin-SDK 2.2, which requires DataWarrior v6.0.0 or newer. If you need
compatibility to older DataWarrior version, use Plugin-SDK 1.0, which is supported from DataWarrior v4.5.3.


Concept
-------

When DataWarrior is launched, it processes all '*.jar' files of the 'plugin' folder and adds new menu items
together with associated external functionality to the DataWarrior application.
All jar files in the plugin folder are processed in alphabetical order. The plugin folder may contain a config
file that defines an additional plugin folder, e.g. to circumvent access right restrictions of plugin files.
- If the plugin jar file contains a PluginStarter class, then the class is loaded and its initialize()
  method is called. This method is supposed to define menu items and associate Java classes with them.
  This method may also perform actions not visible to the user, e.g. it could update other plugin files.
- If a Java class is associated with a menu item, then this class is assumed to implement the interface
  org.openmolecules.datawarrior.plugin.IPluginTask. This interface defines methods that all plugin tasks
  must provide, e.g. to show a configuration dialog, when the user chooses its menu item.
- DataWarrior tries to instantiate an object of each task class and, if successful, adds a menu item for
  the task to the menu or sub-menu defined by the PluginStarter class.
- When later such a task's menu item is selected by the user, the task is asked to show its configuration dialog.
- When the user clicks the dialog's 'OK' button, the task is asked to extract the dialog's settings as key-value pairs.
- Then, the task is asked to check, whether the task configuration, i.e. all its key-value pairs, is valid.
- If valid, the task is asked to execute itself with the given configuration and it is passed a call-back object.
- The call-back object provides methods to interact with DataWarrior such as to read data from an open DataWarrior
  window, or to open a new document window and to populate it with data, to add new columns and data to an open
  DataWarrior window, or to execute a DataWarrior macro.
- Plugin tasks are compatible with DataWarrior macros and, thus, can be recorded and replayed to gether with
  other DataWarrior functionality.


Example 1
---------

The ExamplePluginTask1.java contains the Java source code for three little examples combined in one PluginTask:

- Simple:   Transfers a small 2D-String array into a new DataWarrior document.
            DataWarrior creates default views and filters.
- SMILES:   One source column contains chemical structures encoded as SMILES strings.
            The source table is transferred into a new DataWarrior document and a new column is created
            that contains chemical structures, which are automatically created from the SMILES codes.
            DataWarrior creates default views and filters.
- Molfiles: Transfers a small 2D-String array into a new DataWarrior document.
            An additional column is created that is populated with chemical structures, which are
            automatically produced from provided molfiles.
            Custom views with custom settings and custom filters are created by providing a template string.

The plugin creates a small dialog to choose one of the three options and then causes DataWarrior to create
a new frame with the data taken from the chosen example.


Example 2
---------

The ExamplePluginTask2.java contains the Java source code demonstrating how to build a query dialog that
allows the user to draw a chemical structure as part of the database query. It also shows how to toggle
the editor's mode between structure and sub-structure depending on whether the search mode is a similarity
or a sub-structure search. When the user presses OK, the plugin causes DataWarrior to create a fake result
window with some data rows. One of the columns shows chemical structures. The example code demonstrates how
to create the new window, how to define the result columns and how to populate the result rows.


Example 3
---------

The ExamplePluginTask3.java is different from the first two examples. It does not create a new DataWarrior
window filled with data from a database query. Instead, it demonstrates how to add one or more new columns
to an existing window and how to fill them with data. Typically, the data would be related the data of an
already existing column, e.g. it could be newly calculated properties from existing chemical structures.
This example offers three possible properties to be calculated from an existing chemical structure column.
In a dialog, the user may decide which of the properties to calculate. Then, the plugin generates the
respective number of new columns. In a loop over all rows, the plugin then gets the chemical structures from
the currently open DataWarrior table, calculates the chosen properties, and writes them into the new columns.
Finally, DataWarrior analyses all new columns and creates new default filters for them.
The menu item that triggers the plugin is in the 'Chemistry->From Chemical Structure' sub-menu.


PluginStarter
-------------

Plugins should contain a PluginStarter class, which must then implement the IPluginStarter interface.
The IPluginStarter defines one method only, which is called by DataWarrior directly, when the plugin is loaded.
With this initialize() method DataWarrior passes a PluginStarterHelper object to the plugin, which should be
used to register the user accessible plugin functionality with DataWarrior. The plugin should call the helper
method addMenuItem() once for every task it provides. This will associate a new menu item with that plugin class,
which is called when the user selects the menu item.

The initialize() method may be used for other purposes, which need to be done at plugin load time.
For instance, it could just go through the plugin folder and check, whether any of the other plugin files
are outdated. It could then retrieve updated plugin files from a server and replace the outdated ones,
before plugin loading in DataWarrior continues. For this to work, the updating plugin must be loaded first,
which can achieved if its file is alphabetical lower than all other plugins, because plugins are loaded by
DataWarrior in alphabetical order.


Steps to create a new plugin
----------------------------

- Copy buildAll, and the 'src' folder to a new directory.
- Create one or more new SomeUniquePluginTask.java file(s) implementing the IPluginTask interface.
- Study the comments and implement all methods of the interface. (See ExamplePluginTaskX.java)
- For the implementation of the run() method study the method descriptions of the IPluginHelper interface.
- Make sure you have a PluginStarter class that calls addMenuItem() for everyone of your plugin classes.
- Optionally, choose a different jar-file name in the buildAll script
- Run buildAll and copy the created plugin jar file into the 'plugin' folder of your 'datawarrior'
  installation folder. Launch DataWarrior and look for the new menu item(s) in the intended menu(s).

Note: Later, when the plugin runs in the DataWarrior context, it has full access to all DataWarrior
classes, e.g. openchemlib and json.org. If you need such classes, don't add them to the plugin.
Just add them to the classpath, when building the plugin (see comment in the buildAll script).


Debugging
---------

If your created plugins gain in complexity, you may want to more efficiently debug them than just
adding System.out.println() statements into the source code. You may use the debuger of your development
environment, e.g. IntelliJ or Eclipse, to set break points, single step through the code, watch variables, etc.
For that to work, however, you need to do some extra work:
- Download the DataWarrior source code from github.com and create a runnable project within your IDE.
- Add your plugin's source code directory to the DataWarrior's project source code. Exclude the
  org/openmolecules/datawarrior/plugin directory, because the DataWarrior source code contains these interfaces.
- When DataWarrior is now launched by the IDE, it will find, load and instantiate the PluginStarter class
  from the source code rather than from a plugin jar file making your source code availabe for full debugging.
