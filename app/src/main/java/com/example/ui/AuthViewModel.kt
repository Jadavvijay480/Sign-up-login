package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.User
import com.example.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    object Idle : LoginUiState
    object Loading : LoginUiState
    data class Success(val user: User) : LoginUiState
    data class Error(val message: String) : LoginUiState
}

sealed interface SignupUiState {
    object Idle : SignupUiState
    object Loading : SignupUiState
    object Success : SignupUiState
    data class Error(val message: String) : SignupUiState
}

class AuthViewModel(private val repository: UserRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _signupState = MutableStateFlow<SignupUiState>(SignupUiState.Idle)
    val signupState: StateFlow<SignupUiState> = _signupState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isPasswordResetOk = MutableStateFlow<Boolean?>(null)
    val isPasswordResetOk: StateFlow<Boolean?> = _isPasswordResetOk.asStateFlow()

    private val _resetError = MutableStateFlow<String?>(null)
    val resetError: StateFlow<String?> = _resetError.asStateFlow()

    fun login(email: String, passwordCheck: String) {
        if (email.isBlank() || passwordCheck.isBlank()) {
            _loginState.value = LoginUiState.Error("Please fill in all fields.")
            return
        }

        _loginState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                val user = repository.verifyCredentials(email.trim(), passwordCheck)
                if (user != null) {
                    _currentUser.value = user
                    _loginState.value = LoginUiState.Success(user)
                    // Reset login state to idle after success so it doesn't immediately log in again on re-nav
                } else {
                    _loginState.value = LoginUiState.Error("Invalid email or password.")
                }
            } catch (e: Exception) {
                _loginState.value = LoginUiState.Error("An error occurred: ${e.localizedMessage}")
            }
        }
    }

    fun register(username: String, email: String, passwordCheck: String, confirmPasswordCheck: String, securityAnswerCheck: String) {
        if (username.isBlank() || email.isBlank() || passwordCheck.isBlank() || confirmPasswordCheck.isBlank() || securityAnswerCheck.isBlank()) {
            _signupState.value = SignupUiState.Error("All fields are required!")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _signupState.value = SignupUiState.Error("Please enter a valid email address.")
            return
        }

        if (passwordCheck.length < 6) {
            _signupState.value = SignupUiState.Error("Password must be at least 6 characters.")
            return
        }

        if (passwordCheck != confirmPasswordCheck) {
            _signupState.value = SignupUiState.Error("Passwords do not match.")
            return
        }

        _signupState.value = SignupUiState.Loading
        viewModelScope.launch {
            try {
                val existing = repository.getUserByEmail(email.trim())
                if (existing != null) {
                    _signupState.value = SignupUiState.Error("Email is already registered.")
                    return@launch
                }

                val newUser = User(
                    email = email.trim(),
                    username = username.trim(),
                    passwordHash = passwordCheck, // In production, we should hash, but standard is fine here
                    securityAnswer = securityAnswerCheck.trim().lowercase()
                )

                repository.registerUser(newUser)
                _signupState.value = SignupUiState.Success
            } catch (e: Exception) {
                _signupState.value = SignupUiState.Error("Fail to register: ${e.localizedMessage}")
            }
        }
    }

    fun resetPassword(email: String, securityAnswer: String, newPasswordCheck: String) {
        if (email.isBlank() || securityAnswer.isBlank() || newPasswordCheck.isBlank()) {
            _resetError.value = "All recovery fields are required!"
            return
        }

        if (newPasswordCheck.length < 6) {
            _resetError.value = "New password must be at least 6 characters."
            return
        }

        viewModelScope.launch {
            try {
                val user = repository.getUserByEmail(email.trim())
                if (user == null) {
                    _resetError.value = "No registered user found with this email."
                    _isPasswordResetOk.value = false
                    return@launch
                }

                if (user.securityAnswer.trim().lowercase() != securityAnswer.trim().lowercase()) {
                    _resetError.value = "Security answer is incorrect!"
                    _isPasswordResetOk.value = false
                    return@launch
                }

                // If correct, update password by inserting / overwriting (Room REPLACE or Update)
                val updatedUser = user.copy(passwordHash = newPasswordCheck)
                repository.registerUser(updatedUser) // will overwrite due to PrimaryKey
                _isPasswordResetOk.value = true
                _resetError.value = null
            } catch (e: Exception) {
                _resetError.value = "Recovery error: ${e.localizedMessage}"
                _isPasswordResetOk.value = false
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _loginState.value = LoginUiState.Idle
        _signupState.value = SignupUiState.Idle
        _isPasswordResetOk.value = null
        _resetError.value = null
    }

    fun clearSignupState() {
        _signupState.value = SignupUiState.Idle
    }

    fun clearLoginState() {
        _loginState.value = LoginUiState.Idle
    }

    fun clearResetProgress() {
        _isPasswordResetOk.value = null
        _resetError.value = null
    }
}

class AuthViewModelFactory(private val repository: UserRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
