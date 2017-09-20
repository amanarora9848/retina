// code by jph
package ch.ethz.idsc.retina.dev.linmot;

import java.io.Serializable;
import java.nio.ByteBuffer;

/** information received from micro-autobox about linear motor that controls the
 * break of the gokart */
public class LinmotGetEvent implements Serializable {
  /** 16 bytes */
  public static final int LENGTH = 16;
  // ---
  public final short status_word;
  public final short state_variable;
  public final int actual_position;
  public final int demand_position;
  private final short winding_temp1;
  private final short winding_temp2;

  public LinmotGetEvent(ByteBuffer byteBuffer) {
    status_word = byteBuffer.getShort();
    state_variable = byteBuffer.getShort();
    actual_position = byteBuffer.getInt();
    demand_position = byteBuffer.getInt();
    winding_temp1 = byteBuffer.getShort();
    winding_temp2 = byteBuffer.getShort();
  }

  public void encode(ByteBuffer byteBuffer) {
    byteBuffer.putShort(status_word);
    byteBuffer.putShort(state_variable);
    byteBuffer.putInt(actual_position);
    byteBuffer.putInt(demand_position);
    byteBuffer.putShort(winding_temp1);
    byteBuffer.putShort(winding_temp2);
  }

  /** @return temperature of winding 1 in Celsius */
  public double windingTemperature1() {
    // TODO NRJ document conversion factor
    return winding_temp1 * 0.1;
  }

  /** @return temperature of winding 1 in Celsius */
  public double windingTemperature2() {
    return winding_temp2 * 0.1;
  }

  public String toInfoString() {
    return String.format("%d %d %d %d %d %d", //
        status_word, state_variable, //
        actual_position, demand_position, //
        winding_temp1, winding_temp2);
  }
}