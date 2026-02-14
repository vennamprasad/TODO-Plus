# Building the Plugin

## For Development

### Run in Sandbox IDE
```bash
./gradlew runIde
```
This launches a development instance of IntelliJ with your plugin installed.

### Run Tests
```bash
./gradlew test
```

### Clean Build
```bash
./gradlew clean build
```

## For Distribution

### Build Plugin Package
```bash
# Make sure no IDE instance is running first!
./gradlew buildPlugin
```

The plugin will be built to:
```
build/distributions/TODO-Plus-1.0.0.zip
```

### Install Locally
1. Build the plugin (see above)
2. Open IntelliJ IDEA
3. Go to `Settings/Preferences → Plugins`
4. Click the ⚙️ (gear icon)
5. Select "Install Plugin from Disk..."
6. Choose `build/distributions/TODO-Plus-1.0.0.zip`
7. Restart IntelliJ

## Publishing to JetBrains Marketplace

### Prerequisites
- JetBrains account
- Plugin verified and tested
- Screenshots and marketing materials ready

### Steps
1. Build the plugin: `./gradlew buildPlugin`
2. Go to https://plugins.jetbrains.com/
3. Sign in with your JetBrains account
4. Click "Upload Plugin"
5. Fill in plugin details:
   - Name: TODO++
   - Category: Code Editing
   - Supported products: IntelliJ IDEA
6. Upload the `.zip` file
7. Add screenshots and description
8. Submit for review

### After Approval
- Plugin will be available in the marketplace
- Users can install via: `Settings → Plugins → Marketplace → Search "TODO++"`

## Version Updates

Update version in `build.gradle.kts`:
```kotlin
version = "1.1.0"
```

Update `CHANGELOG.md` with changes, then rebuild.
