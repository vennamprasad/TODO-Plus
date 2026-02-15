package com.example.demo

/**
 * Sample file for TODO++ plugin screenshots
 * This demonstrates all the features with realistic TODOs
 */
class UserService {
    
    // TODO(@john priority:high category:bug): Fix authentication token expiration issue
    fun login(username: String, password: String) {
        // Authentication logic here
    }
    
    // TODO(@sarah priority:high category:security): Implement rate limiting to prevent brute force attacks
    fun validatePassword(password: String): Boolean {
        return password.length >= 8
    }
    
    // TODO(@mike priority:medium category:feature): Add OAuth2 support for Google and GitHub login
    fun socialLogin(provider: String) {
        // Social login integration
    }
    
    // TODO(@alice priority:medium category:performance): Optimize database query - currently taking 2+ seconds
    fun getUserProfile(userId: Int) {
        // Load user profile from database
    }
    
    // TODO(@bob priority:low category:refactor): Extract user validation logic into separate validator class
    fun createUser(username: String, email: String) {
        // User creation logic
    }
    
    // TODO(priority:high category:bug): Memory leak in WebSocket connection - needs urgent fix
    fun connectWebSocket() {
        // WebSocket connection code
    }
    
    // TODO(@team priority:medium category:feature): Add email verification for new user registrations
    fun sendVerificationEmail(email: String) {
        // Email sending logic
    }
    
    // TODO: Add unit tests for all authentication methods
    fun logout() {
        // Logout logic
    }
    
    // TODO(@sarah priority:low category:documentation): Document the password reset flow in wiki
    fun resetPassword(email: String) {
        // Password reset logic
    }
    
    // TODO(@john priority:high category:feature): Implement two-factor authentication (2FA)
    fun enable2FA(userId: Int) {
        // 2FA setup code
    }
    
    // TODO(priority:medium category:refactor): Remove deprecated login methods before v2.0 release
    fun deprecatedLogin() {
        // Old login method
    }
    
    // TODO(@mike priority:low category:enhancement): Add remember me functionality
    fun rememberUser(userId: Int) {
        // Remember me logic
    }
    
    // TODO(@alice priority:high category:bug): Session timeout not working correctly on mobile devices
    fun checkSession() {
        // Session validation
    }
    
    // TODO(category:performance): Cache user permissions to reduce database calls
    fun getUserPermissions(userId: Int): List<String> {
        return emptyList()
    }
    
    // TODO(@bob priority:medium category:feature): Add support for magic link authentication
    fun sendMagicLink(email: String) {
        // Magic link generation
    }
}
