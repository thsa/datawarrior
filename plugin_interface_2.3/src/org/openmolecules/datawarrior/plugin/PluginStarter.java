package org.openmolecules.datawarrior.plugin;

import java.io.File;
import java.util.Properties;

public class PluginStarter implements IPluginStarter {
	@Override
	public boolean initialize(IPluginStartHelper helper, File pluginDir, Properties config) {
		try {
			IPluginTask task1 = new com.mycompany.ExamplePluginTask1();
			IPluginTask task2 = new com.mycompany.ExamplePluginTask2();
			IPluginTask task3 = new com.mycompany.ExamplePluginTask3();
			IPluginTask task4 = new com.mycompany.ExamplePluginTask4();
			IPluginTask task5 = new com.mycompany.ExamplePluginTask5();

			helper.registerTask(task1, "My-Company");
			helper.registerTask(task2, "My-Company");
			helper.registerTask(task3, "My-Company");
			helper.registerTask(task4, "My-Company");
			helper.registerTask(task5, "My-Company");

			helper.addTaskToMenu("My-Company;New Table", "Create Simple Table...", task1);
			helper.addMenuSeparator("My-Company;New Table");
			helper.addTaskToMenu("My-Company;New Table", "Simulate Structure Query...", task2);
			helper.addMenuSeparator("My-Company");
			helper.addTaskToMenu("My-Company", "Calculate Chemical Properties...", task3);
			helper.addTaskToMenu("My-Company", "Extract Scaffold...", task4);
			helper.addTaskToMenu("My-Company", "Simulate A Virtual Screening...", task5);
			return true;
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}
