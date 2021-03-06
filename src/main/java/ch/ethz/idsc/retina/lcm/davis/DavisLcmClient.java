// code by jph
package ch.ethz.idsc.retina.lcm.davis;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashSet;

import ch.ethz.idsc.retina.dev.davis.DavisApsType;
import ch.ethz.idsc.retina.dev.davis.DavisStatics;
import ch.ethz.idsc.retina.dev.davis.data.DavisApsDatagramDecoder;
import ch.ethz.idsc.retina.dev.davis.data.DavisDvsDatagramDecoder;
import ch.ethz.idsc.retina.lcm.LcmClientInterface;
import idsc.BinaryBlob;
import lcm.lcm.LCM;
import lcm.lcm.LCMDataInputStream;
import lcm.lcm.LCMSubscriber;
import lcm.lcm.SubscriptionRecord;

public class DavisLcmClient implements LcmClientInterface {
  private final String cameraId;
  public final DavisDvsDatagramDecoder davisDvsDatagramDecoder = new DavisDvsDatagramDecoder();
  public final DavisApsDatagramDecoder davisSigDatagramDecoder = new DavisApsDatagramDecoder();
  public final DavisApsDatagramDecoder davisRstDatagramDecoder = new DavisApsDatagramDecoder();
  private final Collection<SubscriptionRecord> subscriptions = new HashSet<>();

  /** @param cameraId for instance "overview" */
  public DavisLcmClient(String cameraId) {
    this.cameraId = cameraId;
  }

  @Override
  public void startSubscriptions() {
    LCM lcm = LCM.getSingleton();
    if (davisDvsDatagramDecoder.hasListeners())
      subscriptions.add(lcm.subscribe(DavisDvsBlockPublisher.channel(cameraId), new LCMSubscriber() {
        @Override
        public void messageReceived(LCM lcm, String channel, LCMDataInputStream ins) {
          try {
            digestDvs(new BinaryBlob(ins));
          } catch (IOException exception) {
            exception.printStackTrace();
          }
        }
      }));
    if (davisSigDatagramDecoder.hasListeners())
      subscriptions.add(lcm.subscribe(DavisApsBlockPublisher.channel(cameraId, DavisApsType.SIG), new LCMSubscriber() {
        @Override
        public void messageReceived(LCM lcm, String channel, LCMDataInputStream ins) {
          try {
            digestSig(new BinaryBlob(ins));
          } catch (IOException exception) {
            exception.printStackTrace();
          }
        }
      }));
    if (davisRstDatagramDecoder.hasListeners())
      subscriptions.add(lcm.subscribe(DavisApsBlockPublisher.channel(cameraId, DavisApsType.RST), new LCMSubscriber() {
        @Override
        public void messageReceived(LCM lcm, String channel, LCMDataInputStream ins) {
          try {
            digestRst(new BinaryBlob(ins));
          } catch (IOException exception) {
            exception.printStackTrace();
          }
        }
      }));
  }

  @Override
  public void stopSubscriptions() {
    LCM.getSingleton().unsubscribeAll(subscriptions);
    subscriptions.clear();
  }

  public void digestDvs(BinaryBlob dvsBinaryBlob) {
    ByteBuffer byteBuffer = ByteBuffer.wrap(dvsBinaryBlob.data);
    byteBuffer.order(DavisStatics.BYTE_ORDER);
    davisDvsDatagramDecoder.decode(byteBuffer);
  }

  public void digestSig(BinaryBlob apsBinaryBlob) {
    ByteBuffer byteBuffer = ByteBuffer.wrap(apsBinaryBlob.data);
    byteBuffer.order(DavisStatics.BYTE_ORDER);
    davisSigDatagramDecoder.decode(byteBuffer);
  }

  public void digestRst(BinaryBlob apsBinaryBlob) {
    ByteBuffer byteBuffer = ByteBuffer.wrap(apsBinaryBlob.data);
    byteBuffer.order(DavisStatics.BYTE_ORDER);
    davisRstDatagramDecoder.decode(byteBuffer);
  }
}
