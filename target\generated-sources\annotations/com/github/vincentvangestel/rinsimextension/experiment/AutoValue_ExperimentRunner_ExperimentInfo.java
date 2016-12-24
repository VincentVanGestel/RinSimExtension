
package com.github.vincentvangestel.rinsimextension.experiment;

import com.github.rinde.rinsim.core.model.time.RealtimeClockLogger.LogEntry;
import com.github.rinde.rinsim.core.model.time.RealtimeTickInfo;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
 final class AutoValue_ExperimentRunner_ExperimentInfo extends ExperimentRunner.ExperimentInfo {

  private final List<LogEntry> log;
  private final long rtCount;
  private final long stCount;
  private final StatisticsDTO stats;
  private final ImmutableList<RealtimeTickInfo> tickInfoList;
  private final Optional<ExperimentRunner.AuctionStats> auctionStats;

  AutoValue_ExperimentRunner_ExperimentInfo(
      List<LogEntry> log,
      long rtCount,
      long stCount,
      StatisticsDTO stats,
      ImmutableList<RealtimeTickInfo> tickInfoList,
      Optional<ExperimentRunner.AuctionStats> auctionStats) {
    if (log == null) {
      throw new NullPointerException("Null log");
    }
    this.log = log;
    this.rtCount = rtCount;
    this.stCount = stCount;
    if (stats == null) {
      throw new NullPointerException("Null stats");
    }
    this.stats = stats;
    if (tickInfoList == null) {
      throw new NullPointerException("Null tickInfoList");
    }
    this.tickInfoList = tickInfoList;
    if (auctionStats == null) {
      throw new NullPointerException("Null auctionStats");
    }
    this.auctionStats = auctionStats;
  }

  @Override
  List<LogEntry> getLog() {
    return log;
  }

  @Override
  long getRtCount() {
    return rtCount;
  }

  @Override
  long getStCount() {
    return stCount;
  }

  @Override
  StatisticsDTO getStats() {
    return stats;
  }

  @Override
  ImmutableList<RealtimeTickInfo> getTickInfoList() {
    return tickInfoList;
  }

  @Override
  Optional<ExperimentRunner.AuctionStats> getAuctionStats() {
    return auctionStats;
  }

  @Override
  public String toString() {
    return "ExperimentInfo{"
        + "log=" + log + ", "
        + "rtCount=" + rtCount + ", "
        + "stCount=" + stCount + ", "
        + "stats=" + stats + ", "
        + "tickInfoList=" + tickInfoList + ", "
        + "auctionStats=" + auctionStats
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ExperimentRunner.ExperimentInfo) {
      ExperimentRunner.ExperimentInfo that = (ExperimentRunner.ExperimentInfo) o;
      return (this.log.equals(that.getLog()))
           && (this.rtCount == that.getRtCount())
           && (this.stCount == that.getStCount())
           && (this.stats.equals(that.getStats()))
           && (this.tickInfoList.equals(that.getTickInfoList()))
           && (this.auctionStats.equals(that.getAuctionStats()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = 1;
    h *= 1000003;
    h ^= this.log.hashCode();
    h *= 1000003;
    h ^= (this.rtCount >>> 32) ^ this.rtCount;
    h *= 1000003;
    h ^= (this.stCount >>> 32) ^ this.stCount;
    h *= 1000003;
    h ^= this.stats.hashCode();
    h *= 1000003;
    h ^= this.tickInfoList.hashCode();
    h *= 1000003;
    h ^= this.auctionStats.hashCode();
    return h;
  }

  private static final long serialVersionUID = 6324066851233398736L;

}
