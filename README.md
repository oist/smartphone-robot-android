# OIST Smartphone Robot Android Framework

[![](https://jitpack.io/v/oist/smartphone-robot-android.svg)](https://jitpack.io/#oist/smartphone-robot-android)

This repository contains the Android side of the OIST Smartphone Robot project:
the shared `abcvlib` Android library, demo app modules, build configuration, and
documentation for creating robot-control apps.

> **This fork, branch `dreamer-bridge`**, adds the `dreamerBridge` app, which
> trains DreamerV3 on the robot: the policy runs on the phone and a learner on
> a GPU streams it new weights. Setup and usage:
> [ROBOT.md in the dreamerv3 repo](https://github.com/Alex-Vasilache/dreamerv3/blob/smartphone-robot-bridge/ROBOT.md);
> app internals: [docs/dreamer-bridge.md](docs/dreamer-bridge.md).

Android apps handle phone-side sensing, user interaction, app-level control
logic, and communication with the robot hardware. For the full hardware,
firmware, model, and Android project context, see the
[Smartphone Robot wiki](https://github.com/oist/smartphone-robot/wiki).

## Table Of Contents

- [Getting Started](docs/getting-started.md): first run with the `backAndForth`
  demo app.
- [Demo Apps](docs/demo-apps.md): overview of the included demo apps.
- [Build Your Own App](docs/build-your-own-app.md): choose how to build a
  robot-control app from this project.
- [Contributing](docs/contributing.md): contributor and maintainer references.

## Repository Layout

- `apps/`: Android demo app modules.
- `libs/abcvlib/`: shared Android framework/library code used by app modules.
