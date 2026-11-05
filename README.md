# ConfigCat IntelliJ Plugin
![Build](https://github.com/configcat/intellij-plugin/actions/workflows/build.yml/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/26096-configcat-feature-flags.svg)](https://plugins.jetbrains.com/plugin/26096-configcat-feature-flags)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/26096-configcat-feature-flags.svg)](https://plugins.jetbrains.com/plugin/26096-configcat-feature-flags)

<!-- Plugin description -->
[ConfigCat IntelliJ Plugin](https://plugins.jetbrains.com/plugin/26096-configcat-feature-flags) to manage feature flags from JetBrains/IntelliJ IDEs.

Connect your Product and Config to your IDE. Find your Feature Flag's usages in your code easily.

## About ConfigCat
ConfigCat is a hosted feature flag service: https://configcat.com. Manage feature toggles across frontend, backend, mobile, and desktop apps. Alternative to LaunchDarkly. Management app + feature flag SDKs.

## Feature overview

- View all of your Products, Configs and Feature Flags.
- Create Configs and Feature Flags within JetBrains/IntelliJ IDE.
- Open your Configs and Feature Flags on the ConfigCat Dashboard.
- Copy a Feature Flag's key to the clipboard.
- Find your Feature Flag's usages in your code.

<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "ConfigCat Feature Flags"</kbd> >
  <kbd>Install</kbd>
  
- Manually:

  Download the [latest release](https://github.com/configcat/intellij-plugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Development

The plugin compiles against IntelliJ IDEA Community 2024.1 with Java 17 bytecode.
Its declared compatibility range is 2024.1 through 2026.2.

- `.\gradlew.bat runIde` starts the baseline 2024.1 IDE.
- `.\gradlew.bat runIde2026_2` starts IntelliJ IDEA 2026.2 using its bundled JetBrains Runtime, independently of the compilation SDK.
- `.\gradlew.bat runIdeForUiTests` starts the 2026.2 runtime with the Robot Server plugin.
- `.\gradlew.bat test` runs against the 2024.1 compilation SDK.
- `.\gradlew.bat verifyPlugin` checks the plugin against both versions.

The newer runtime version is configured by `runtimePlatformVersion` in `gradle.properties`.
JCEF is supplied by the platform in 2024.1 and by the optional Web Browser (JCEF) plugin in 2026.2.
Without JCEF, web views display an explanatory message instead of preventing the plugin from loading.
