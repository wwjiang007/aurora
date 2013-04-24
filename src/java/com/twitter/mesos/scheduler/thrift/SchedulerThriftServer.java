package com.twitter.mesos.scheduler.thrift;

import com.twitter.common.thrift.ThriftServer;

/**
 * Thin implementation of ThriftServer.
 */
class SchedulerThriftServer extends ThriftServer {

  SchedulerThriftServer() {
    super("TwitterMesosScheduler", "1");
  }
}
