# Psi Kit

[advantage kit](https://docs.advantagekit.org) is a logging and replay framework for FRC, as well as a server to provide [advantage scope](https://docs.advantagescope.com) with live data. Psi Kit removes the dependency on FRC tooling, allowing it to be used in FTC, or any other java project.

### Current Features

* 90% of advantage scope features (see a [comparison to ftc dash](compare.md)), including:
  - timeline scrubbing 
  - line graph
  - 2d field
  - 3d field
  - detailed tables of values
  - statistical analysis of values
  - mechanism view
  - 2d drawing of arbitrary points
  - metadata

* for a complete overview of features, check out the [advantage scope docs](https://docs.advantagescope.org/category/tab-reference). unsupported features are:
  - swerve view
  - live tuning
  - log file review
  - joysticks

* some features should work, but are untested. these are: 
  - console
  - comparison to a video of the match

> note that most of the abilities that advantage kit offers in the non-live aspect are coming soon.

### Coming Soon
* log files for review of what happened after the fact
* log replay, allowing you to **[log new data](https://docs.advantagekit.org/getting-started/what-is-advantagekit/example-output-logging)** that you didn't record while the robot was running
* kotlin tooling to take advantage of the language's expresiveness

### Support
if you have questions, feel free to ping `@Avery | 3825 | PsiKit` in the [unofficial FTC discord](https://discord.gg/ftc)

### PRs are Welcome and Accepted. 
if there are any features you think should come sooner, please feel free to open a PR. 

### read the &nbsp; [installation guide](installing.md)