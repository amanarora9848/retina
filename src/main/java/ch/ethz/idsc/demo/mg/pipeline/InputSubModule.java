// code by mg
package ch.ethz.idsc.demo.mg.pipeline;

import java.io.IOException;
import java.nio.ByteBuffer;

import ch.ethz.idsc.demo.mg.HandLabelFileLocations;
import ch.ethz.idsc.demo.mg.gui.PipelineFrame;
import ch.ethz.idsc.demo.mg.gui.PipelineVisualization;
import ch.ethz.idsc.retina.dev.davis.DavisDvsListener;
import ch.ethz.idsc.retina.dev.davis._240c.DavisDvsEvent;
import ch.ethz.idsc.retina.dev.davis.data.DavisDvsDatagramDecoder;
import ch.ethz.idsc.retina.lcm.OfflineLogListener;
import ch.ethz.idsc.tensor.Scalar;

// this module distributes the event stream to the visualization and control pipeline
public class InputSubModule implements OfflineLogListener, DavisDvsListener {
  // parameters
  private final DavisDvsDatagramDecoder davisDvsDatagramDecoder = new DavisDvsDatagramDecoder(); // for event processing
  private final DavisSurfaceOfActiveEvents surface = new DavisSurfaceOfActiveEvents(); // for filtering of event stream
  private final BlobFeatureFilter featureFilter = new BlobFeatureFilter(); // receive tracked cones from that module
  private final DavisBlobTracker track = new DavisBlobTracker(featureFilter); // next module in pipeline
  private final PipelineVisualization viz = new PipelineVisualization(); // for visualization
  private final PipelineFrame[] frames = new PipelineFrame[3]; // for visualization
  private final String pathToFile = HandLabelFileLocations.Labels + "labeledFeatures.dat";
  private final TrackingEvaluator evaluator = new TrackingEvaluator(pathToFile, track);
  private final int maxDuration = 2000; // [ms]
  private final int backgroundActivityFilterTime = 2000; // [us] the shorter the more is filtered
  private final int imageInterval = 50000; // [us] visualization interval
  private final boolean useFilter = true;
  // fields for testing
  private float eventCount = 0;
  private float filteredEventCount;
  private int lastTimestamp;
  private int begin, end;
  private long startTime, endTime;
  private boolean saveImages = false;
  private String imagePrefix = "dubi8a";

  public InputSubModule() {
    davisDvsDatagramDecoder.addDvsListener(this);
    frames[0] = new PipelineFrame();
    frames[1] = new PipelineFrame();
    frames[2] = new PipelineFrame();
  }

  @Override
  public void event(Scalar time, String channel, ByteBuffer byteBuffer) {
    if (channel.equals("davis240c.overview.dvs")) {
      davisDvsDatagramDecoder.decode(byteBuffer);
    }
  }

  @Override
  public void davisDvs(DavisDvsEvent davisDvsEvent) {
    // start timer
    if (eventCount == 0) {
      begin = davisDvsEvent.time;
      lastTimestamp = davisDvsEvent.time;
      startTime = System.currentTimeMillis();
    }
    ++eventCount;
    // send raw event stream to visualization
    frames[0].receiveEvent(davisDvsEvent);
    // send filtered events to visualization, tracker and evaluator
    if (surface.backgroundActivityFilter(davisDvsEvent, backgroundActivityFilterTime) && useFilter) {
      track.receiveNewEvent(davisDvsEvent);
      evaluator.receiveEvent(davisDvsEvent);
      frames[1].receiveEvent(davisDvsEvent);
      frames[2].receiveEvent(davisDvsEvent);
      ++filteredEventCount;
      // the events are accumulated for the interval time and then displayed in a single frame
      if ((davisDvsEvent.time - lastTimestamp) > imageInterval) {
        viz.setImage(frames[0].getAccumulatedEvents(), 0);
        // active blobs color coded by featurefilter
        viz.setImage(frames[1].trackOverlay(featureFilter.getTrackedBlobs()), 1);
        // hidden blobs
        viz.setImage(frames[2].trackOverlay(track.getBlobList(0)), 2);
        if (saveImages) {
          try {
            viz.saveImage(imagePrefix, davisDvsEvent.time);
            // track.printStatusUpdate(davisDvsEvent);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        frames[0].clearImage();
        frames[1].clearImage();
        frames[2].clearImage();
        lastTimestamp = davisDvsEvent.time;
      }
    }
    if (davisDvsEvent.time - begin > maxDuration * 1000) {
      end = davisDvsEvent.time;
      endTime = System.currentTimeMillis();
      int diff = end - begin;
      System.out.println("Percentage hit by active blobs: " + track.hitthreshold / eventCount * 100);
      System.out.println("Elapsed time in the eventstream [ms]: " + diff / 1000 + " with " + eventCount + " events");
      long elapsedTime = endTime - startTime;
      System.out.println("Computation time: " + elapsedTime + "[ms]");
      System.out.format("%.2f%% of the events were processed after filtering.", getFilteredPercentage());
      System.exit(0);
    }
  }

  public float getFilteredPercentage() {
    return 100 * filteredEventCount / eventCount;
  }
}