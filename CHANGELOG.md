# Changelog

All notable changes to the TODO++ plugin will be documented in this file.

## [1.2.0] - 2026-02-15

### Added
- **Auto-Refresh**: TODO list now updates automatically when files are saved
- **Column Sorting**: Click any column header to sort (Priority sorts by High > Medium > Low)
- **Export Functionality**: Export your TODOs to CSV or Markdown files
- **New Toolbar**: Improved toolbar with export buttons

## [1.1.3] - 2026-02-15

### Fixed
- Real-time TODO updates: Scanner now reads from editor buffer (PSI) instead of disk
- TODOs update immediately when changed, even before saving the file
- "Scan Project" now picks up unsaved changes

## [1.1.2] - 2026-02-15

### Changed
- Removed `untilBuild` limit to support all current and future IDE versions
- Now compatible with Android Studio Panda (2025.3.1) and all future releases

## [1.1.1] - 2026-02-15

### Fixed
- Extended compatibility range to support IntelliJ IDEA 2025.3 and newer versions
- Updated `untilBuild` to 253.* for broader IDE version support

## [1.1.0] - 2026-02-15

### Added
- **Smart visual styling for incomplete TODOs**: TODOs without priority or assignee now appear in italic gray text, making them stand out
- **Enhanced statistics in status bar**: Shows count of TODOs missing priority or assignee (e.g., "⚠️ 12 need priority, 18 unassigned")
- **Android Studio compatibility**: Plugin now works in Android Studio and all other JetBrains IDEs
- **Improved cell renderer**: Better visual distinction between complete and incomplete TODOs across all columns

### Changed
- Priority column styling: Missing priorities show in italic gray instead of regular gray
- Assignee and Category columns: Empty values now styled with italic gray text
- Status bar: More informative messages showing filtering state and missing metadata alerts

### Documentation
- Updated README with v1.1.0 features
- Enhanced BUILD.md with marketplace publishing instructions
- Added comprehensive examples in documentation

## [1.0.0] - 2026-02-15

### Added
- 🎉 Initial release of TODO++
- Enhanced TODO syntax with assignee, priority, and category support
- Project-wide file scanner supporting 15+ programming languages
- Professional tool window with table view
- Color-coded priorities (RED=High, ORANGE=Medium, GREEN=Low)
- Comprehensive filtering system:
  - Filter by priority (HIGH/MEDIUM/LOW)
  - Filter by assignee (@username)
  - Filter by category (bug, feature, refactor, etc.)
  - Search in TODO descriptions
- Double-click navigation to jump to code
- Real-time statistics showing TODO breakdown
- Scan and refresh functionality
- Clear filters button

---

## Future Enhancements (Planned)

### [1.1.0] - Future
- Export TODOs to CSV/Markdown
- Custom priority levels
- Due dates support
- Team dashboard view
- Integration with issue trackers (JIRA, GitHub Issues)

### [1.2.0] - Future
- TODO tags (e.g., #urgent, #easy-fix)
- Estimated effort tracking
- Completion tracking
- TODO history/timeline

---

**Note**: This project follows [Semantic Versioning](https://semver.org/).
