package com.qualcomm.hardware.limelightvision;

import org.json.JSONObject;

/**
 * Internal helper for PsiKit replay.
 *
 * <p>Lives in the Limelight package so it can access protected/package-private constructors.
 */
public final class PsiKitLimelightJsonFactory {
  private PsiKitLimelightJsonFactory() {}

  public static LLResult resultFromJson(String json, long controlHubTimestampMs) {
    if (json == null || json.trim().isEmpty()) return null;
    try {
      LLResult r = new LLResult(new JSONObject(json));
      // package-private in LLResult; accessible here
      r.setControlHubTimeStamp(controlHubTimestampMs);
      return r;
    } catch (Throwable t) {
      return null;
    }
  }

  public static LLStatus statusFromJson(String json) {
    if (json == null || json.trim().isEmpty()) return new LLStatus();
    try {
      return new LLStatus(new JSONObject(json));
    } catch (Throwable t) {
      return new LLStatus();
    }
  }
}
