// code by mg
package ch.ethz.idsc.demo.mg.slam;

import org.bytedeco.javacpp.opencv_core.Mat;

import ch.ethz.idsc.demo.mg.pipeline.EventFiltering;
import ch.ethz.idsc.demo.mg.util.GokartToImageInterface;
import ch.ethz.idsc.demo.mg.util.ImageToGokartInterface;
import ch.ethz.idsc.gokart.core.pos.GokartPoseInterface;
import ch.ethz.idsc.retina.dev.davis.DavisDvsListener;
import ch.ethz.idsc.retina.dev.davis._240c.DavisDvsEvent;
import ch.ethz.idsc.tensor.Tensor;

/** implements the slam algorithm
 * "simultaneous localization and mapping for event-based vision systems" */
class SlamProvider implements DavisDvsListener {
  private final ImageToGokartInterface imageToGokartLookup;
  private final GokartToImageInterface gokartToImageUtil;
  private final GokartPoseInterface gokartOdometryPose;
  private final GokartPoseInterface gokartLidarPose;
  private final EventFiltering eventFiltering;
  private final SlamParticle[] slamParticles;
  private final SlamLocalizationStep slamLocalizationStep;
  private final SlamMappingStep slamMappingStep;
  private final SlamWayPointExtraction slamWayPoints;
  private final boolean lidarMappingMode;
  private final int numOfPart;
  private boolean isInitialized;

  SlamProvider(SlamConfig slamConfig, GokartPoseInterface gokartOdometryPose, GokartPoseInterface gokartLidarPose) {
    imageToGokartLookup = slamConfig.davisConfig.createImageToGokartUtilLookup();
    gokartToImageUtil = slamConfig.davisConfig.createGokartToImageUtil();
    this.gokartOdometryPose = gokartOdometryPose;
    this.gokartLidarPose = gokartLidarPose;
    eventFiltering = new EventFiltering(slamConfig.davisConfig);
    slamLocalizationStep = new SlamLocalizationStep(slamConfig);
    slamMappingStep = new SlamMappingStep(slamConfig);
    slamWayPoints = new SlamWayPointExtraction(slamConfig, slamLocalizationStep.getPoseInterface());
    lidarMappingMode = slamConfig.lidarMappingMode;
    numOfPart = slamConfig.numberOfParticles.number().intValue();
    slamParticles = new SlamParticle[numOfPart];
    for (int i = 0; i < numOfPart; i++)
      slamParticles[i] = new SlamParticle();
  }

  public void initialize(Tensor pose, double timeStamp) {
    slamLocalizationStep.initialize(slamParticles, pose, timeStamp);
    slamMappingStep.initialize(timeStamp);
    slamWayPoints.initialize(timeStamp);
  }

  @Override
  public void davisDvs(DavisDvsEvent davisDvsEvent) {
    if (!isInitialized) {
      initialize(gokartLidarPose.getPose(), davisDvsEvent.time / 1000000.0);
      isInitialized = true;
    }
    if (eventFiltering.filterPipeline(davisDvsEvent)) {
      double currentTimeStamp = davisDvsEvent.time / 1000000.0;
      double[] eventGokartFrame = imageToGokartLookup.imageToGokart(davisDvsEvent.x, davisDvsEvent.y);
      if (lidarMappingMode) {
        slamLocalizationStep.setPose(gokartLidarPose.getPose());
        slamMappingStep.mappingStepWithLidar(gokartLidarPose.getPose(), eventGokartFrame, currentTimeStamp);
      } else {
        slamLocalizationStep.localizationStep(slamParticles, slamMappingStep.getMap(0), eventGokartFrame, currentTimeStamp);
        slamMappingStep.mappingStep(slamParticles, eventGokartFrame, currentTimeStamp);
        slamWayPoints.mapPostProcessing(slamMappingStep.getMap(0), currentTimeStamp);
      }
    }
  }

  public GokartPoseInterface getPoseInterface() {
    return slamLocalizationStep.getPoseInterface();
  }

  public SlamParticle[] getParticles() {
    return slamParticles;
  }

  public Mat getProcessedMat() {
    return slamWayPoints.getProcessedMat();
  }

  // mapID: 0 == occurrence map, 1 == normalization map, 2 == likelihood map
  public MapProvider getMap(int mapID) {
    return slamMappingStep.getMap(mapID);
  }
}
