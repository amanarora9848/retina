// code by jph
package ch.ethz.idsc.retina.dev.rimo;

import java.net.DatagramPacket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import ch.ethz.idsc.gokart.core.AutoboxDevice;
import ch.ethz.idsc.gokart.core.AutoboxSocket;
import ch.ethz.idsc.gokart.core.fuse.EmergencyBrakeProvider;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.idsc.tensor.qty.Unit;
import ch.ethz.idsc.tensor.qty.UnitSystem;

public class RimoSocket extends AutoboxSocket<RimoGetEvent, RimoPutEvent> {
  private static final int LOCAL_PORT = 5000;
  private static final int REMOTE_PORT = 5000;
  private static final Unit HERTZ = Unit.of("Hz");
  // ---
  /** the communication rate affects the torque PI control */
  private static final int SEND_PERIOD_MS = 20; // 50[Hz]
  // ---
  public static final RimoSocket INSTANCE = new RimoSocket();

  private RimoSocket() {
    super(RimoGetEvent.LENGTH, LOCAL_PORT);
    // ---
    addGetListener(EmergencyBrakeProvider.INSTANCE);
    // ---
    addPutProvider(RimoPutFallback.INSTANCE);
  }

  @Override // from AutoboxSocket
  protected RimoGetEvent createGetEvent(ByteBuffer byteBuffer) {
    return new RimoGetEvent(byteBuffer);
  }

  @Override // from AutoboxSocket
  protected long getPutPeriod_ms() {
    return SEND_PERIOD_MS;
  }

  public Scalar getGetPeriod() {
    return UnitSystem.SI().apply(Quantity.of(250, HERTZ)).reciprocal();
  }

  @Override // from AutoboxSocket
  protected DatagramPacket getDatagramPacket(byte[] data) throws UnknownHostException {
    return new DatagramPacket(data, data.length, AutoboxDevice.REMOTE_INET_ADDRESS, REMOTE_PORT);
  }
}
