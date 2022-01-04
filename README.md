# Spotless Intellij Gradle

![Build](https://github.com/ragurney/spotless/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/18321.svg)](https://plugins.jetbrains.com/plugin/18321)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/18321.svg)](https://plugins.jetbrains.com/plugin/18321)

<!-- Plugin description -->
An IntelliJ plugin to allow running the [spotless](https://github.com/diffplug/spotless) gradle task
from within the IDE on the current file selected in the editor. 

You may find the spotless action via <kbd>Code</kbd> > <kbd>Reformat Code with Spotless</kbd>.

![spotlessdemo](https://user-images.githubusercontent.com/15261525/147841908-d5cc3bda-56c8-4cbd-ba29-13ebe29f6a1d.gif)
<!-- Plugin description end -->

## Features
* `spotlessApply` can be run on the current file via <kbd>Code</kbd> > <kbd>Reformat Code with Spotless</kbd>.
  You may also assign a keyboard shortcut to this action for convenience.

## Installation
>**NOTE:** Before using this extension, ensure you've [configured Spotless](https://github.com/diffplug/spotless/tree/master/plugin-gradle)
correctly in your Gradle build file. (Run ./gradlew spotlessDiagnose to prepare & validate Spotless.)

### Using IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Spotless Gradle"</kbd> >
  <kbd>Install Plugin</kbd>

### Manually:

  Download the [latest release](https://github.com/ragurney/spotless/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## How it Works
This plugin runs the `spotlessApply` Gradle task on the current file using the [Spotless IDE hook](https://github.com/diffplug/spotless/blob/main/plugin-gradle/IDE_HOOK.md). 
  
## Release Notes
See [CHANGELOG.md](CHANGELOG.md)

## License
See [License](LICENSE)
