// code by jph
package ch.ethz.idsc.demo.jph.sys;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import ch.ethz.idsc.demo.GokartLogFiles;
import ch.ethz.idsc.gokart.offline.api.LogFile;
import ch.ethz.idsc.gokart.offline.api.LogFileLocator;
import ch.ethz.idsc.subare.util.UserHome;

public enum DatahakiLogFileLocator implements LogFileLocator {
  INSTANCE;
  // ---
  /** the archive of all log files is kept on an external hard-drive */
  private static final File ARCHIVE = new File("/media/datahaki/backup/gokartlogs");
  private static final List<File> LOG_ROOT = Arrays.asList( //
      new File("/media/datahaki/media/ethz/gokartlogs"), //
      ARCHIVE, //
      UserHome.file("gokartlogs"));

  @Override
  public File getAbsoluteFile(LogFile logFile) {
    // TODO string input would be sufficient
    String title = logFile.getFilename();
    String date = title.substring(0, 8);
    for (File dir : LOG_ROOT) {
      File file = new File(new File(dir, date), title);
      if (file.isFile())
        return file;
    }
    return null;
  }

  /** @param logFile
   * @return
   * @throws Exception if file cannot be located */
  public static File file(LogFile logFile) {
    return INSTANCE.getAbsoluteFile(logFile);
  }

  /** @return */
  public static Collection<LogFile> all() {
    return GokartLogFiles.all(ARCHIVE);
  }
}
