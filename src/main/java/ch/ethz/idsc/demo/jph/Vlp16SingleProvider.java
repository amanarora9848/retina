// code by jph
package ch.ethz.idsc.demo.jph;

import java.nio.ByteBuffer;

import ch.ethz.idsc.retina.dev.lidar.LidarSpacialEvent;
import ch.ethz.idsc.retina.dev.lidar.VelodyneSpacialProvider;
import ch.ethz.idsc.retina.dev.lidar.VelodyneStatics;

/* package */ class Vlp16SingleProvider extends VelodyneSpacialProvider {
  private final int laser;

  /** @param angle_offset
   * @param laser 0 for -15[deg], 1 for 1[deg], 2 for -13[deg], ... */
  public Vlp16SingleProvider(double angle_offset, int laser) {
    this.laser = laser;
  }

  @Override // from LidarRayDataListener
  public void scan(int azimuth, ByteBuffer byteBuffer) {
    float[] coords = new float[2];
    int position = byteBuffer.position();
    byteBuffer.position(position + laser * 3);
    int distance = byteBuffer.getShort() & 0xffff;
    byte intensity = byteBuffer.get();
    // float radius = IR * distance;
    coords[0] = azimuth;
    coords[1] = distance * VelodyneStatics.TO_METER_FLOAT;
    LidarSpacialEvent lidarSpacialEvent = new LidarSpacialEvent(usec, coords, intensity);
    listeners.forEach(listener -> listener.lidarSpacial(lidarSpacialEvent));
  }
}
