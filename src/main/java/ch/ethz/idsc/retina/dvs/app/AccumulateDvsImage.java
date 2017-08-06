// code by jpg
package ch.ethz.idsc.retina.dvs.app;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

import ch.ethz.idsc.retina.dev.davis.DavisDevice;
import ch.ethz.idsc.retina.dev.davis.DvsDavisEventListener;
import ch.ethz.idsc.retina.dev.davis.TimedImageListener;
import ch.ethz.idsc.retina.dev.davis._240c.DvsDavisEvent;
import ch.ethz.idsc.retina.util.data.GlobalAssert;

public class AccumulateDvsImage implements DvsDavisEventListener {
  private static final byte CLEAR_BYTE = (byte) 128;
  // ---
  private final int WIDTH;
  private final int HEIGHT;
  private final List<TimedImageListener> timedImageListeners = new LinkedList<>();
  private final BufferedImage bufferedImage;
  private final byte[] bytes;
  private final int interval;
  private int last = -1;
  // private final Tensor clear = Array.of(l -> RealScalar.of(128), WIDTH, HEIGHT);
  // private Tensor image = Array.zeros(WIDTH, HEIGHT);

  /** @param interval [us] */
  public AccumulateDvsImage(DavisDevice davisDevice, int interval) {
    WIDTH = davisDevice.getWidth();
    HEIGHT = davisDevice.getHeight();
    bufferedImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
    DataBufferByte dataBufferByte = (DataBufferByte) bufferedImage.getRaster().getDataBuffer();
    bytes = dataBufferByte.getData();
    GlobalAssert.that(bytes.length == WIDTH * HEIGHT);
    this.interval = interval;
    // ---
    clearImage();
  }

  public void addListener(TimedImageListener timedImageListener) {
    timedImageListeners.add(timedImageListener);
  }

  @Override
  public void dvs(DvsDavisEvent dvsDavisEvent) {
    if (last == -1) // magic const
      last = dvsDavisEvent.time;
    if (last + interval < dvsDavisEvent.time) {
      // BufferedImage bi = ImageFormat.of(image);
      // timedImageListeners.forEach(listener -> listener.image(last, bi));
      timedImageListeners.forEach(listener -> listener.image(last, bufferedImage));
      clearImage();
      last += interval;
    }
    int polarity = dvsDavisEvent.i == 0 ? 0 : 255;
    // image.set(RealScalar.of(polarity), dvsDavisEvent.x, dvsDavisEvent.y);
    int index = dvsDavisEvent.x + (dvsDavisEvent.y) * WIDTH;
    bytes[index] = (byte) polarity;
  }

  private void clearImage() {
    IntStream.range(0, bytes.length).forEach(i -> bytes[i] = CLEAR_BYTE);
    // image = clear.copy();
  }
}