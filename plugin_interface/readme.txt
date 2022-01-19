DataWarrior Plugin-SDK
======================

This file describes the concept and all steps needed to create a DataWarrior plugin.
A plugin, if put in the plugin directory of a DataWarrior installation, adds one or more new
menu item(s) to the DataWarrior 'Database' menu. Later, when a menu item is selected by the user,
the plugin is asked to perform a respective action. Typically, this would be the retrieval of
data from an external database, which is then added to current DataWarrior window or shown
in a new DataWarrior document window.

Developing a plugin is easy for even an unexperienced Java developer. It does not require
any DataWarrior source code nor any knowledge about it. Everything that is needed is a
properly installed JDK and the files that come with the datawarriorPluginSDK.zip file.


Content
-------

readme.txt                           : this file
ExamplePluginTask1.java              : complete example plugin source code; used by 'buildAll'
ExamplePluginTask2.java              : example plugin code using structure editor; used by 'buildAll'
org/openmolecules/datawarrior/plugin : package containing two java interface files; used by 'buildAll'
tasknames                            : contains plugin task names; used by 'buildAll'
buildAll                             : short Linux/Mac script to build examplePlugin.jar from the source
examplePlugin.jar                    : plugin file that can be placed into the DataWarrior 'plugin' folder


Concept
-------

When DataWarrior (4.5.3 or higher) is launched, it processes all '*.jar' files of the 'plugin' folder
in alphabetical order in the following way:
- If the plugin contains an PluginInitializer class, then the class is loaded and its initialize()
  method is called.
- Plugins are supposed to contain a file called 'tasknames' on the top level. It should contain the name(s)
  of one of more classes providing supplementary task functionality (full class name with package hierarchy,
  one class per line). DataWarrior tries to open this file and extracts the class name(s) from it.
  Optionally, a line may also contain, comma separated, the menu name where the task's menu item should be
  shown. If no menu name is specified, then the task's menu item will be shown in the 'Database' menu.
- These classes are assumed to implement the interface org.openmolecules.datawarrior.plugin.IPluginTask,
  which defines methods to name the task, to show a configuration dialog and to execute the task.
- DataWarrior tries to instantiate each task class and, if successful, adds a menu item for every task
  to the 'Database' menu unless another menu is specified.
- When such a task's menu item is later selected by the user, the task is asked to show its configuration dialog.
- When the user clicks 'OK' in the dialog, the task is asked to extract the dialog settings as key-value pairs.
- Then, the task is asked to check, whether the configuration, i.e. the key-value pairs, is valid.
- If valid, the task is asked to execute the configuration and is passed a call-back object.
- The call-back object provides methods to allocate a new document window and to populate it with data.
  Alternatively, the task may add new columns to the active window and fill them with data.


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


PluginInitializer Example
-------------------------

Plugins may contain a PluginInitializer class, which must then implement the IPluginInitializer interface.
An IPluginInitializer defines one method only, which is called by DataWarrior directly, when the plugin is loaded.
In fact, a plugin's purpose may just be to run some code at load time and not to provide any tasks at all.
For instance such plugin could just go through the plugin folder and check, whether any of the other
plugin files should be updated.


Steps to create a new plugin
----------------------------

- Copy tasknames, buildAll, and the org folder to a new directory.
- Create one or more new SomeUniquePluginTask.java file(s) implementing the IPluginTask interface.
- Study the comments and implement all methods of the interface. (See ExamplePluginTask?.java)
- For the implementation of the run() method study the method descriptions of the IPluginHelper interface.
- Ensure that 'tasknames' contains the correct class name(s) and, if used, package name(s)
- Correct plugin class name(s) and jar-file name in the buildAll script
- Run buildAll and copy the created plugin jar file into the 'plugin' folder within the 'datawarrior'
  installation folder. Launch DataWarrior and look for the new menu item in the 'Database' menu.

Note: Later, when the plugin runs in the DataWarrior context, it has full access to all DataWarrior
classes, e.g. openchemlib and json.org. If you need such classes, don't add them to the plugin.
Just add them to the classpath, when building the plugin (see comment in the buildAll script).

