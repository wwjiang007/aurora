/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aurora.scheduler.storage.durability;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.aurora.GuavaUtils;
import org.apache.aurora.gen.BatchJobUpdateStrategy;
import org.apache.aurora.gen.JobConfiguration;
import org.apache.aurora.gen.JobUpdate;
import org.apache.aurora.gen.JobUpdateInstructions;
import org.apache.aurora.gen.JobUpdateSettings;
import org.apache.aurora.gen.JobUpdateStrategy;
import org.apache.aurora.gen.QueueJobUpdateStrategy;
import org.apache.aurora.gen.ResourceAggregate;
import org.apache.aurora.gen.ScheduledTask;
import org.apache.aurora.gen.TaskConfig;
import org.apache.aurora.scheduler.quota.QuotaManager;
import org.apache.aurora.scheduler.resources.ResourceType;
import org.apache.aurora.scheduler.storage.entities.IJobConfiguration;
import org.apache.aurora.scheduler.storage.entities.IJobUpdate;
import org.apache.aurora.scheduler.storage.entities.IJobUpdateSettings;
import org.apache.aurora.scheduler.storage.entities.IResource;
import org.apache.aurora.scheduler.storage.entities.IResourceAggregate;
import org.apache.aurora.scheduler.storage.entities.IScheduledTask;

/**
 * Helps migrating thrift schema by populating deprecated and/or replacement fields.
 */
public final class ThriftBackfill {

  /**
   * Ensures TaskConfig.resources and correspondent task-level fields are all populated.
   *
   * @param config TaskConfig to backfill.
   * @return Backfilled TaskConfig.
   */
  public TaskConfig backfillTask(TaskConfig config) {
    return config;
  }

  /**
   * Backfills JobConfiguration. See {@link #backfillTask(TaskConfig)}.
   *
   * @param jobConfig JobConfiguration to backfill.
   * @return Backfilled JobConfiguration.
   */
  public IJobConfiguration backfillJobConfiguration(JobConfiguration jobConfig) {
    backfillTask(jobConfig.getTaskConfig());
    return IJobConfiguration.build(jobConfig);
  }

  /**
   * Backfills set of tasks. See {@link #backfillTask(TaskConfig)}.
   *
   * @param tasks Set of tasks to backfill.
   * @return Backfilled set of tasks.
   */
  public Set<IScheduledTask> backfillTasks(Set<ScheduledTask> tasks) {
    return tasks.stream()
        .map(t -> backfillScheduledTask(t))
        .map(IScheduledTask::build)
        .collect(GuavaUtils.toImmutableSet());
  }

  /**
   * Ensures ResourceAggregate.resources and correspondent deprecated fields are all populated.
   *
   * @param aggregate ResourceAggregate to backfill.
   * @return Backfilled IResourceAggregate.
   */
  public static IResourceAggregate backfillResourceAggregate(ResourceAggregate aggregate) {
    EnumSet<ResourceType> quotaResources = QuotaManager.QUOTA_RESOURCE_TYPES;
    if (aggregate.getResources().size() > quotaResources.size()) {
      throw new IllegalArgumentException("Too many resource values in quota.");
    }

    if (!quotaResources.equals(aggregate.getResources().stream()
        .map(e -> ResourceType.fromResource(IResource.build(e)))
        .collect(Collectors.toSet()))) {

      throw new IllegalArgumentException("Quota resources must be exactly: " + quotaResources);
    }
    return IResourceAggregate.build(aggregate);
  }

  private ScheduledTask backfillScheduledTask(ScheduledTask task) {
    backfillTask(task.getAssignedTask().getTask());
    return task;
  }

  /**
   * Backfills JobUpdate. See {@link #backfillTask(TaskConfig)}.
   *
   * @param update JobUpdate to backfill.
   * @return Backfilled job update.
   */
  public IJobUpdate backFillJobUpdate(JobUpdate update) {
    JobUpdateInstructions instructions = update.getInstructions();
    if (instructions.isSetDesiredState()) {
      backfillTask(instructions.getDesiredState().getTask());
    }

    backfillUpdateStrategy(instructions.getSettings());

    instructions.getInitialState().forEach(e -> backfillTask(e.getTask()));

    return IJobUpdate.build(update);
  }

  public static void backfillUpdateStrategy(JobUpdateSettings settings) {
    IJobUpdateSettings updateSettings = IJobUpdateSettings.build(settings);

    // Convert old job update schema to have an update strategy
    if (!updateSettings.isSetUpdateStrategy()) {
      if (updateSettings.isWaitForBatchCompletion()) {
        settings.setUpdateStrategy(
            JobUpdateStrategy.batchStrategy(
                new BatchJobUpdateStrategy().setGroupSize(updateSettings.getUpdateGroupSize())));
      } else {
        settings.setUpdateStrategy(
            JobUpdateStrategy.queueStrategy(
                new QueueJobUpdateStrategy().setGroupSize(updateSettings.getUpdateGroupSize())));
      }
    }
  }
}
