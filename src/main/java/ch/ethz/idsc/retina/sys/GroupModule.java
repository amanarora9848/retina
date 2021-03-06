// code by jph
package ch.ethz.idsc.retina.sys;

import java.util.List;

public abstract class GroupModule extends AbstractModule {
  @Override
  protected final void first() throws Exception {
    ModuleAuto.INSTANCE.runAll(modules());
  }

  @Override
  protected final void last() {
    modules().forEach(ModuleAuto.INSTANCE::terminateOne);
  }

  /** @return list of classes that constitute group */
  protected abstract List<Class<?>> modules();
}
