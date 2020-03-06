# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.0.4] - Unreleased
### Added
- Statement `timestamp` and `stored` are parsed and indexed as `:statement/timestamp-inst` and `:statement/stored-inst`

### Removed
- Timestamp strings are no longer indexed

## [0.0.3] - 2020-02-24
### Added
- DAVE Analysis entity
- Analysis Editor
- Datalog parsing and query for xAPI
- Vega input specification in Analysis Editor
- Makefile for easier building w/o clojure syntax
- CI via travis.org
- DAVE Primitives Specification
- `vega-tooltip` plugin for Vega renderer
- Learning Path algorithm
- Common xAPI Statement parsers

### Changed
- Changed DAVE navigation to use Analysis under Workbook instead of Question/visualization
- Removed Wizard flow
- Run headless cljs tests via nodejs
- Documentation split up into stand alone .tex and .pdf files which are required within main.tex
  - docs/algorithms/master.<tex + pdf> => docs/main.<tex + pdf>

### Removed
- "Functions"
- "Question" and "Visualization" entities

### Fixed
- Tooltips now display properly

## [0.0.2] - 2019-05-31
### Added
- Navigation footer

### Changed
- Improved, centralized edit forms
- Simplify object navigation
- Various copy changes
- Algorithm Doc Updates
- Wizard navigation UX improvements

### Fixed
- Visualization sizing issues

## [0.0.1] - 2019-03-29
### Added
- DAVE Wizard for guided workbook creation
- Live LRS connection functionality
- Implemented visualization
- Implemented browser data persistence
- Implemented workbook UI + navigation scheme
- Function implementations
- Reducible function protocol + Schema
- Reference implementation of pre-alpha algorithms
- Manual creation of appropriate xAPI dataset for Pre-Alpha functionality
- Defined data model for DAVE UI
- Defined Dave UI general states and navigation based on data model

[Unreleased]: https://github.com/yetanalytics/dave/compare/v0.0.2...HEAD
[0.0.2]: https://github.com/yetanalytics/dave/compare/v0.0.1...v0.0.2
[0.0.1]: https://github.com/yetanalytics/dave/releases/tag/v0.0.1
