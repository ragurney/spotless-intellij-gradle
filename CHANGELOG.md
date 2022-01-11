<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# spotless Changelog

## [Unreleased]

## [1.0.3]
* Add a link to the GitHub repository in the plugin description
* Add the `--no-configuration-cache` option to the `spotlessApply` task execution to address issue #18

## [1.0.2]
- Updated version range lower bound to 211

## [1.0.1]
- Update README for a more helpful plugin description

## [1.0.0]
- Initial scaffold created from IntelliJ Platform Plugin Template
- Implemented initial version of spotlessApply action which executes the gradle task in the background asynchronously
- Users can now execute spotlessApply for their current file by selecting Code > Reformat Code with Spotless