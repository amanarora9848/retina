// code by jph
package ch.ethz.idsc.retina.gui.gokart;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;

import ch.ethz.idsc.retina.dev.linmot.LinmotSocket;
import ch.ethz.idsc.retina.dev.misc.MiscSocket;
import ch.ethz.idsc.retina.dev.rimo.RimoPutTire;
import ch.ethz.idsc.retina.dev.rimo.RimoSocket;
import ch.ethz.idsc.retina.dev.steer.SteerSocket;
import ch.ethz.idsc.retina.dev.zhkart.DriveMode;
import ch.ethz.idsc.retina.lcm.joystick.GenericXboxPadLcmClient;
import ch.ethz.idsc.retina.sys.AbstractModule;
import ch.ethz.idsc.retina.util.gui.SpinnerLabel;
import ch.ethz.idsc.tensor.RationalScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Subdivide;

public class AutoboxJoystickModule extends AbstractModule {
  private final AutoboxGenericXboxPadJoystick instance = new AutoboxGenericXboxPadJoystick();
  private final JFrame jFrame = new JFrame("joystick");
  private final Tensor TORQUE = Subdivide.of(RealScalar.ZERO, RationalScalar.of(1, 2), 5);

  @Override
  protected void first() throws Exception {
    GenericXboxPadLcmClient.INSTANCE.addListener(instance);
    // ---
    RimoSocket.INSTANCE.addPutProvider(instance.rimoPutProvider);
    LinmotSocket.INSTANCE.addPutProvider(instance.linmotPutProvider);
    SteerSocket.INSTANCE.addPutProvider(instance.steerPutProvider);
    SteerSocket.INSTANCE.addGetListener(instance);
    MiscSocket.INSTANCE.addPutProvider(instance.miscPutProvider);
    // ---
    JPanel jPanel = new JPanel(new BorderLayout());
    {
      JToolBar jToolBar = new JToolBar();
      jToolBar.setFloatable(false);
      jToolBar.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 0));
      {
        SpinnerLabel<DriveMode> spinnerLabel = new SpinnerLabel<>();
        spinnerLabel.setArray(DriveMode.values());
        spinnerLabel.setValue(instance.driveMode);
        spinnerLabel.addSpinnerListener(i -> instance.driveMode = i);
        spinnerLabel.addToComponentReduced(jToolBar, new Dimension(120, 28), "drive mode");
      }
      {
        SpinnerLabel<Integer> spinnerLabel = new SpinnerLabel<>();
        spinnerLabel.setArray(0, 500, 1000, 2000, 4000, (int) RimoPutTire.MAX_SPEED);
        spinnerLabel.setValueSafe(instance.speedLimit);
        spinnerLabel.addSpinnerListener(i -> instance.speedLimit = i);
        spinnerLabel.addToComponentReduced(jToolBar, new Dimension(70, 28), "max speed limit");
      }
      {
        SpinnerLabel<Object> spinnerLabel = new SpinnerLabel<>();
        Object[] values = TORQUE.stream().map(Scalar.class::cast).toArray();
        spinnerLabel.setArray(values);
        spinnerLabel.setValueSafe(RationalScalar.of(1, 5));
        spinnerLabel.addSpinnerListener(i -> instance.torqueAmp = (Scalar) i);
        spinnerLabel.addToComponentReduced(jToolBar, new Dimension(60, 28), "max torque limit");
      }
      jPanel.add(jToolBar, BorderLayout.NORTH);
    }
    // ---
    jFrame.setContentPane(jPanel);
    jFrame.setBounds(200, 200, 280, 70);
    jFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    jFrame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(WindowEvent windowEvent) {
        RimoSocket.INSTANCE.removePutProvider(instance.rimoPutProvider);
        LinmotSocket.INSTANCE.removePutProvider(instance.linmotPutProvider);
        SteerSocket.INSTANCE.removePutProvider(instance.steerPutProvider);
        SteerSocket.INSTANCE.removeGetListener(instance);
        MiscSocket.INSTANCE.removePutProvider(instance.miscPutProvider);
        // ---
        System.out.println("removed listeners and providers");
        GenericXboxPadLcmClient.INSTANCE.removeListener(instance);
      }
    });
    jFrame.setVisible(true);
  }

  @Override
  protected void last() {
    jFrame.setVisible(false);
    jFrame.dispose();
  }
}
