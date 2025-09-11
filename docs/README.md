# Psi Kit

[AdvantageKit](https://docs.advantagekit.org) is a logging and replay framework for FRC, as well as a server to provide [AdvantageScope](https://docs.advantagescope.com) with live data. Psi Kit removes the dependency on FRC tooling, allowing it to be used in FTC (or any other Java project).

### Current Features

* 90% of AdvantageScope features (see a [comparison to FTC Dashboard](compare.md)), including:
  - Timeline scrubbing 
  - Line graph
  - 2D field
  - 3D field
  - Detailed tables of values
  - Statistical analysis of values
  - Mechanism view
  - 2D drawing of arbitrary points
  - Metadata

* For a complete overview of features, check out the [AdvantageScope docs](https://docs.advantagescope.org/category/tab-reference). Unsupported features are:
  - Swerve view
  - Live tuning
  - Log file review
  - Joysticks

* Some features should work, but are untested. these are: 
  - Comparison to a video of the match

> Note that most of the abilities that advantage kit offers in the non-live aspect are coming soon.

### Coming Soon
* Live tuning of variables
* Log files for review of what happened after the fact
* Log replay, allowing you to **[log new data](https://docs.advantagekit.org/getting-started/what-is-advantagekit/example-output-logging)** that you didn't record while the robot was running
* Kotlin tooling to take advantage of the language's expressiveness

### Support
If you have questions, feel free to ping `@Avery | 3825 | PsiKit` in the [Unofficial FTC Discord](https://discord.gg/ftc)

### PRs are Welcome and Accepted. 
If there are any features you think should come sooner, please feel free to open a PR. 

### Next, read the [Installation Guide](installing.md)