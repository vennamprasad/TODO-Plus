package com.example.demo

/**
 * Sample file to demonstrate TODO++ parsing
 */
class UserService {

    // TODO: Add input validation
    fun createUser(name: String) {
        println("Creating user: $name")
    }

    // TODO(@john priority:high category:bug): Fix authentication logic
    fun authenticate(username: String, password: String): Boolean {
        return true // FIXME: Implement proper auth
    }

    // TODO(@sarah priority:medium category:feature): Add password reset
    fun resetPassword(email: String) {
        // Implementation needed
    }

    // TODO(@team priority:low category:refactor): Clean up this method
    fun getUserProfile(userId: Int) {
        // TODO(category:performance): Optimize database query
        println("Fetching user $userId")
    }

    // TODO(priority:high): Add rate limiting
    fun processRequest() {
        // Critical security feature
    }

    // TODO(@mike category:documentation): Add API docs
    fun getPublicApi() {
        // Public endpoints
    }
}
