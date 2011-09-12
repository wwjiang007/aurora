package com.twitter.mesos.executor;

import java.io.File;

import com.google.common.base.Preconditions;

import org.apache.commons.io.FileUtils;

import com.twitter.common.quantity.Amount;
import com.twitter.common.quantity.Data;
import com.twitter.mesos.Tasks;
import com.twitter.mesos.gen.AssignedTask;
import com.twitter.mesos.gen.ResourceConsumption;
import com.twitter.mesos.gen.ScheduleStatus;

/**
 * A task that has completed execution.
 *
 * @author William Farner
 */
public class DeadTask extends TaskOnDisk {

  private final AssignedTask task;
  private final ScheduleStatus state;

  // Lazy-loaded fields.
  private Amount<Long, Data> diskConsumed = null;

  /**
   * Creates a new dead task.
   *
   * @param taskRoot Root directory for this task's recorded state.
   * @throws TaskStorageException If the task could not be restored from the task root.
   */
  DeadTask(File taskRoot) throws TaskStorageException {
    super(taskRoot);

    Preconditions.checkNotNull(taskRoot);
    Preconditions.checkArgument(taskRoot.exists(), "Sandbox does not exist: " + taskRoot);
    Preconditions.checkArgument(taskRoot.isDirectory(), "Not a directory: " + taskRoot);

    this.task = restoreTask();
    this.state = restoreStatus();
    Preconditions.checkState(!Tasks.isActive(state),
        "A dead task may not be assigned an active state.");
  }

  @Override
  public String getId() {
    return task.getTaskId();
  }

  @Override
  public boolean isRunning() {
    return false;
  }

  @Override
  public void stage() throws TaskRunException {
    throw new UnsupportedOperationException("A dead task cannot be staged.");
  }

  @Override
  public ScheduleStatus blockUntilTerminated() {
    throw new UnsupportedOperationException("Should not attempt to block on a dead task.");
  }

  @Override
  public void terminate(ScheduleStatus terminalState) {
    throw new UnsupportedOperationException("The state of a dead task cannot be changed.");
  }

  @Override
  public AssignedTask getAssignedTask() {
    return task;
  }

  @Override
  public ScheduleStatus getScheduleStatus() {
    return state;
  }

  @Override
  public void run() {
    throw new UnsupportedOperationException("A dead task cannot be run.");
  }

  @Override
  public String toString() {
    return String.format("DeadTask(s: %s, t: %s)", state, task);
  }
}
