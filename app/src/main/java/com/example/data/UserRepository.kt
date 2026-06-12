package com.example.data

class UserRepository(private val userDao: UserDao) {

    suspend fun getUserByEmail(email: String): User? {
        return userDao.getUserByEmail(email)
    }

    suspend fun registerUser(user: User): Long {
        return userDao.insertUser(user)
    }

    suspend fun verifyCredentials(email: String, passwordToCheck: String): User? {
        val user = userDao.getUserByEmail(email)
        if (user != null && user.passwordHash == passwordToCheck) {
            return user
        }
        return null
    }

    suspend fun getUserCount(): Int {
        return userDao.getUserCount()
    }
}
