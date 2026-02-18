# TODO++ - Enhanced TODO Management for IntelliJ IDEA

[![Build](https://img.shields.io/badge/build-passing-brightgreen)]() 
[![Version](https://img.shields.io/badge/version-1.0.0-blue)]()
[![License](https://img.shields.io/badge/license-MIT-green)]()

Take your TODO comments to the next level! TODO++ adds powerful features to manage, organize, and track TODOs across your entire project.

## ✨ Features

### 🎯 Enhanced TODO Syntax
Assign TODOs to team members, set priorities, add due dates, and track issues:

```java
// TODO(@john priority:high category:bug): Fix memory leak
// TODO(due:2024-03-20): API migration deadline
// TODO(issue:PROJ-123): Linked to Jira issue
// TODO(risk:high estimate:2d): Custom tags for better tracking
```

### 📅 Due Dates & Overdue Alerts
- **Track Deadlines**: Use `due:YYYY-MM-DD`, `due:today`, or `due:tomorrow`.
- **Visual Alerts**: Overdue items are highlighted in **RED**. Items due soon are **ORANGE**.
- **Sorting**: Sort the TODO list by due date to see what's urgent.

### 🔗 Issue Tracker Integration
- **Link Issues**: Add `issue:ID` (e.g., `issue:PROJ-123`) to your TODOs.
- **Auto-Detection**: Configure regex patterns (e.g., `[A-Z]+-\d+`) to automatically detect issue IDs in descriptions.
- **Python Support**: Now supports Python (`#`) and SQL (`--`) comment styles.
- **Quick Access**: Right-click any TODO to "Open in Issue Tracker" (Jira, GitHub, GitLab, etc.).

### 🎨 Custom Priorities & Colors
- **Default Priorities**: 🔴 High, 🟠 Medium, 🟢 Low.
- **Customizable**: Add your own priorities (e.g., "Critical", "Optional") in **Settings > Tools > TODO++**.
- **Colors**: Assign custom colors to each priority level.

### 🏷️ Custom Tags (Key-Value Pairs)
- **Arbitrary Metadata**: Add any `key:value` pair to your TODOs.
- **Quoted Values**: Support for tags with spaces: `client:"Acme Corp"`, `msg:"fix later"`.
- **Examples**: `risk:low`, `estimate:4h`, `reviewer:@alice`.
- **Filtering**: Search for `risk:high` to find specific tasks.

### 🔍 Smart Project Scanning
- Automatically scans **15+ programming languages** (Java, Kotlin, JavaScript, Python, SQL, Lua, Shell, and more)
- Real-time statistics showing TODO breakdown by priority
- Fast and efficient file indexing

### 🎨 Visual Organization
- **Color-coded priorities**: 🔴 RED (High), 🟠 ORANGE (Medium), 🟢 GREEN (Low)
- Clean table view with sortable columns
- Professional UI integrated into IntelliJ's tool window system

### 🔎 Powerful Filtering
- **Priority**: Filter by specific priority levels.
- **Assignee**: See what's assigned to team members.
- **Category**: Focus on bugs, features, or refactor tasks.
- **Deep Search**: Search by description or specific tags (e.g., `risk:high`).
- **One-click clear**: Reset all filters instantly.

### 🚀 Quick Navigation
- **Double-click** any TODO to jump straight to that line in your code
- No more hunting through files!


### 📦 Installation

### From Marketplace (Coming Soon)
1. Open IntelliJ IDEA
2. Go to `Settings/Preferences → Plugins → Marketplace`
3. Search for **"TODO++"**
4. Click **Install**

### Manual Installation
1. Download the latest release from [Releases](../../releases)
2. Open IntelliJ IDEA
3. Go to `Settings/Preferences → Plugins → ⚙️ (gear icon) → Install Plugin from Disk...`
4. Select the downloaded `.zip` file (e.g., `TODO-Plus-1.6.4.zip`)
5. Restart IntelliJ IDEA (Required)

### Build from Source
```bash
git clone https://github.com/yourusername/TODO-plus.git
cd TODO-plus
./gradlew buildPlugin
```

Find the plugin in `build/distributions/TODO-Plus-1.6.4.zip`

## 🎯 Quick Start

1. **Open TODO++ Tool Window**
   - Click **"TODO++"** button at the bottom of your IDE
   - Or: `View → Tool Windows → TODO++`

2. **Scan Your Project**
   - Click **"🔍 Scan Project"** button
   - Watch your TODOs appear instantly

3. **Navigate to Code**
   - Double-click any TODO to jump to that line

4. **Use Filters**
   - Filter by priority: Select from dropdown
   - Filter by assignee: Type `@john`
   - Filter by category: Type `bug`
   - Search: Type any text to search descriptions

## 📖 Usage Examples

### Basic TODOs

```kotlin
// TODO: Add input validation
```

### Priority-based

```kotlin
// TODO(priority:high): Fix critical security issue
// TODO(priority:medium): Improve performance
// TODO(priority:low): Add code comments
```

### Team Assignment

```kotlin
// TODO(@alice): Review this implementation
// TODO(@bob): Update documentation
// TODO(@team): Discuss architecture
```

### Categorized

```kotlin
// TODO(category:bug): Memory leak in connection pool
// TODO(category:feature): Add dark mode support
// TODO(category:refactor): Extract duplicate code
// TODO(category:performance): Optimize database queries
```

### Full Format

```kotlin
// TODO(@john priority:high category:bug): Fix authentication race condition
```

## ⚙️ Supported Languages

Java • Kotlin • JavaScript • TypeScript • Python • Go • Rust • C/C++ • C# • Swift • Ruby • PHP • Scala • Groovy • HTML • XML • SQL • Shell • Lua

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🔒 Privacy & Legal

- **Privacy Policy**: [PRIVACY.md](PRIVACY.md) - We don't collect any data
- **Copyright Notice**: [COPYRIGHT.md](COPYRIGHT.md) - Legal attributions

## 🙏 Acknowledgments

Built with the IntelliJ Platform SDK

---

**Made with ❤️ for developers who love organized code**

**Copyright © 2026 Vennam Prasad**

