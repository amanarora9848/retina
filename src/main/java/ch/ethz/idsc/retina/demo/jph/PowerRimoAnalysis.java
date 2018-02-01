// code by jph
package ch.ethz.idsc.retina.demo.jph;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

import ch.ethz.idsc.retina.dev.rimo.RimoGetEvent;
import ch.ethz.idsc.retina.dev.rimo.RimoPutEvent;
import ch.ethz.idsc.retina.dev.rimo.RimoPutHelper;
import ch.ethz.idsc.retina.dev.rimo.RimoPutTire;
import ch.ethz.idsc.retina.lcm.autobox.RimoLcmServer;
import ch.ethz.idsc.retina.util.math.Magnitude;
import ch.ethz.idsc.retina.util.math.SI;
import ch.ethz.idsc.retina.util.math.TensorBuilder;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.qty.Quantity;

class PowerRimoAnalysis implements OfflineTableSupplier {
  private final Scalar delta;
  // ---
  private Scalar time_next = Quantity.of(0, SI.SECOND);
  private RimoGetEvent rge;
  private RimoPutEvent rpe;
  final TensorBuilder tensorBuilder = new TensorBuilder();

  public PowerRimoAnalysis(Scalar delta) {
    this.delta = delta;
  }

  @Override
  public void event(Scalar time, String channel, ByteBuffer byteBuffer) {
    if (channel.equals(RimoLcmServer.CHANNEL_GET)) {
      rge = new RimoGetEvent(byteBuffer);
    } else //
    if (channel.equals(RimoLcmServer.CHANNEL_PUT)) {
      rpe = RimoPutHelper.from(byteBuffer);
    }
    if (Scalars.lessThan(time_next, time)) {
      if (Objects.nonNull(rge) && Objects.nonNull(rpe)) {
        // System.out.println("export " + time.number().doubleValue());
        time_next = time.add(delta);
        tensorBuilder.flatten( //
            time.map(Magnitude.SECOND), //
            rge.getTireL.vector_raw(), //
            rge.getTireR.vector_raw(), //
            rpe.getTorque_Y_pair().map(RimoPutTire.MAGNITUDE_ARMS) // ARMS
        );
      }
    }
  }

  @Override
  public Tensor getTable() {
    return tensorBuilder.getTensor();
  }

  public static void main(String[] args) throws IOException {
    OfflineProcessing.INSTANCE.handle(() -> new PowerRimoAnalysis(Quantity.of(0.1, SI.SECOND)));
  }
}
