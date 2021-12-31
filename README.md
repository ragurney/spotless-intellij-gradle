# spotless-intellij-gradle
A plugin for IntelliJ to allow running the [spotless](https://github.com/diffplug/spotless) gradle task 
from within the IDE
![Build](https://github.com/ragurney/spotless/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "spotless"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/ragurney/spotless/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>
  
## How to Use
`spotlessApply` can be run on the current file via <kbd>Code</kbd> > <kbd>Reformat Code with Spotless</kbd>.
You may also assign a keyboard shortcut to this action for convenience.
