
package com.github.vincentvangestel.rinsimextension.experiment;

import javax.annotation.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
 final class AutoValue_ExperimentRunner_AuctionStats extends ExperimentRunner.AuctionStats {

  private final int numParcels;
  private final int numReauctions;
  private final int numUnsuccesfulReauctions;
  private final int numFailedReauctions;

  AutoValue_ExperimentRunner_AuctionStats(
      int numParcels,
      int numReauctions,
      int numUnsuccesfulReauctions,
      int numFailedReauctions) {
    this.numParcels = numParcels;
    this.numReauctions = numReauctions;
    this.numUnsuccesfulReauctions = numUnsuccesfulReauctions;
    this.numFailedReauctions = numFailedReauctions;
  }

  @Override
  int getNumParcels() {
    return numParcels;
  }

  @Override
  int getNumReauctions() {
    return numReauctions;
  }

  @Override
  int getNumUnsuccesfulReauctions() {
    return numUnsuccesfulReauctions;
  }

  @Override
  int getNumFailedReauctions() {
    return numFailedReauctions;
  }

  @Override
  public String toString() {
    return "AuctionStats{"
        + "numParcels=" + numParcels + ", "
        + "numReauctions=" + numReauctions + ", "
        + "numUnsuccesfulReauctions=" + numUnsuccesfulReauctions + ", "
        + "numFailedReauctions=" + numFailedReauctions
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ExperimentRunner.AuctionStats) {
      ExperimentRunner.AuctionStats that = (ExperimentRunner.AuctionStats) o;
      return (this.numParcels == that.getNumParcels())
           && (this.numReauctions == that.getNumReauctions())
           && (this.numUnsuccesfulReauctions == that.getNumUnsuccesfulReauctions())
           && (this.numFailedReauctions == that.getNumFailedReauctions());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = 1;
    h *= 1000003;
    h ^= this.numParcels;
    h *= 1000003;
    h ^= this.numReauctions;
    h *= 1000003;
    h ^= this.numUnsuccesfulReauctions;
    h *= 1000003;
    h ^= this.numFailedReauctions;
    return h;
  }

  private static final long serialVersionUID = -597628566631371202L;

}
