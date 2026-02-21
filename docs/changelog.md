# Changelog

* 0.0.1: Initial release; most AdvantageScope panels supported, namely:
  * Graph
  * 2D field
  * 3D field
  * Table 
  * Stats
  * Mechanism
  * Points
  
* 0.0.2: Add `System.out` logging

* 0.0.3: First public release for beta testing

* 0.1.0-beta2:
  * FTC autolog default changed to opt-in (`PsiKitAutoLogSettings.enabledByDefault = false`).
  * Adding PsiKit no longer auto-instruments all OpModes by default.
  * Enable globally only when desired via `PsiKitAutoLogSettings.enabledByDefault = true`
    (or system property `psikit.autolog.enabled=true`), or opt in per OpMode with `@PsiKitAutoLog`.

* 0.1.0-beta3 (unreleased):
  * Added `FtcLogTuning.bulkOnlyLogging` to globally restrict FTC logging to bulk-backed paths.
  * `FtcLogTuning.prefetchBulkDataEachLoop` now defaults to `true` to isolate hub bulk transaction
    cost into `PsiKit/sessionTimes (us)/BulkPrefetchTotal` and per-hub
    `PsiKit/logTimes (us)/BulkPrefetch/<hub>`.
  * Defaults updated for low-overhead operation: `bulkOnlyLogging = true`, `logImu = false`.
  * Improved low-overhead sampling semantics for wrappers that can skip background reads
    (color/distance, IMU, voltage): values are written when freshly sampled and sampled flags are
    emitted for freshness.
  * Docs: added a dedicated FTC logging/tuning reference page and linked quick defaults from usage docs.
