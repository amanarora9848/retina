// code by swisstrolley+
package ch.ethz.idsc.retina.sys;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** ModuleWatchdog runs every PERIOD_S and checks if all the modules that should
 * be running are running as given by their threads. If not it will try to
 * restart the module.
 *
 * The initialisation of this INSTANCE starts the watchdog. checkAlive is called
 * by the main method and will restart the module if the timeOut is exceeded.
 * This should be reliable as if the main fails... (it will never happen). */
public enum ModuleAuto {
  INSTANCE;
  // Maps for holding the module list
  private static Map<Class<?>, AbstractModule> moduleMap = new LinkedHashMap<>();
  private static Map<Class<?>, AbstractModule> watchList = new LinkedHashMap<>();

  /** Methods for launching the modules */
  public static void runAll(List<Class<?>> modules) {
    System.out.println(new Date() + " Module Auto: Launch all");
    for (Class<?> module : modules)
      runOne(module);
    System.out.println(new Date() + " Module Auto: Launch all done");
  }

  public static void terminateAll() {
    System.out.println(new Date() + " Module Auto: Terminate all");
    moduleMap.values().stream().parallel() //
        .forEach(AbstractModule::terminate);
    moduleMap.clear();
    System.out.println(new Date() + " Module Auto: Terminate all done");
  }

  public static void runOne(Class<?> module) {
    if (moduleMap.containsKey(module)) {
      System.out.println(new Date() + " Module Auto: Already launched: " + module);
      return;
    }
    try {
      AbstractModule instance = (AbstractModule) module.newInstance();
      System.out.println(new Date() + " Module Auto: Launching: " + module);
      instance.launch();
      moduleMap.put(module, instance);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void terminateOne(Class<?> module) {
    if (watchList.containsKey(module))
      watchList.remove(module);
    if (moduleMap.containsKey(module)) {
      AbstractModule abstractModule = moduleMap.get(module);
      moduleMap.remove(module);
      System.out.println(new Date() + " Module Auto: Terminating: " + module);
      abstractModule.terminate();
    }
  }

  public static void watch(Class<?> module, AbstractModule abstractModule) {
    if (watchList.containsKey(module)) {
      System.out.println(new Date() + " Module Auto: Already watching: " + module);
      return;
    }
    watchList.put(module, abstractModule);
  }
}
