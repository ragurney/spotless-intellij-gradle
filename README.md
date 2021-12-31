# Spotless Intellij Gradle

![Build](https://github.com/ragurney/spotless/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)

<!-- Plugin description -->
An IntelliJ plugin to allow running the [spotless](https://github.com/diffplug/spotless) gradle task
from within the IDE.
<!-- Plugin description end -->

## Features
* `spotlessApply` can be run on the current file via <kbd>Code</kbd> > <kbd>Reformat Code with Spotless</kbd>.
  You may also assign a keyboard shortcut to this action for convenience.

## Installation
>**NOTE:** Before using this extension, ensure you've [configured Spotless](https://github.com/diffplug/spotless/tree/master/plugin-gradle)
correctly in your Gradle build file. (Run ./gradlew spotlessDiagnose to prepare & validate Spotless.)

- Using IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "spotless"</kbd> >
  <kbd>Install Plugin</kbd>

- Manually:

  Download the [latest release](https://github.com/ragurney/spotless/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>
