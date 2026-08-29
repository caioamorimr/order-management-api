package com.caioamorimr.ordermanagement.resources;

import com.caioamorimr.ordermanagement.dto.LoginRequest;
import com.caioamorimr.ordermanagement.dto.RefreshTokenRequest;
import com.caioamorimr.ordermanagement.dto.UserDTO;
import com.caioamorimr.ordermanagement.dto.UserInsertDTO;
import com.caioamorimr.ordermanagement.entities.User;
import com.caioamorimr.ordermanagement.repositories.UserRepository;
import com.caioamorimr.ordermanagement.security.JwtUtil;
import com.caioamorimr.ordermanagement.security.SecurityConfig;
import com.caioamorimr.ordermanagement.security.UserDetailsServiceImpl;
import com.caioamorimr.ordermanagement.services.RefreshTokenService;
import com.caioamorimr.ordermanagement.services.UserService;
import com.caioamorimr.ordermanagement.services.exceptions.InvalidRefreshTokenException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    private LoginRequest loginRequest;
    private UserInsertDTO registerDTO;
    private User user;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest("caio@email.com", "123456");

        registerDTO = new UserInsertDTO();
        registerDTO.setName("Caio");
        registerDTO.setEmail("caio@email.com");
        registerDTO.setPhone("988888888");
        registerDTO.setPassword("123456");

        user = new User(1L, "Caio", "caio@email.com", "988888888", "hashed_password");
    }

    @Test
    @DisplayName("POST /auth/login should return 200 with an access token and a refresh token when credentials are valid")
    void login_shouldReturn200_whenCredentialsAreValid() throws Exception {
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("caio@email.com");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtUtil.generateToken("caio@email.com")).thenReturn("fake-jwt-token");
        when(userRepository.findByEmail("caio@email.com")).thenReturn(Optional.of(user));
        when(refreshTokenService.issue(user)).thenReturn("fake-refresh-token");

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.refreshToken").value("fake-refresh-token"));
    }

    @Test
    @DisplayName("POST /auth/login should return 401 when credentials are invalid")
    void login_shouldReturn401_whenCredentialsAreInvalid() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login should return 422 when email is blank")
    void login_shouldReturn422_whenEmailIsBlank() throws Exception {
        LoginRequest invalid = new LoginRequest("", "123456");

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].fieldName").value("email"));
    }

    @Test
    @DisplayName("POST /auth/register should return 201 without requiring authentication")
    void register_shouldReturn201_whenPayloadIsValid() throws Exception {
        when(userService.insert(any(UserInsertDTO.class))).thenReturn(new UserDTO());

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /auth/register should return 422 when name is blank")
    void register_shouldReturn422_whenNameIsBlank() throws Exception {
        registerDTO.setName("");

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].fieldName").value("name"));
    }

    @Test
    @DisplayName("POST /auth/register should return 409 when the email is already registered")
    void register_shouldReturn409_whenEmailAlreadyExists() throws Exception {
        when(userService.insert(any(UserInsertDTO.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint \"tb_user_email_key\""));

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Data Integrity Violation"));
    }

    @Test
    @DisplayName("POST /auth/refresh should return 200 with a new token pair without requiring authentication")
    void refresh_shouldReturn200_whenRefreshTokenIsValid() throws Exception {
        when(refreshTokenService.rotate("old-refresh-token"))
                .thenReturn(new RefreshTokenService.RotationResult(user, "new-refresh-token"));
        when(jwtUtil.generateToken("caio@email.com")).thenReturn("new-jwt-token");

        RefreshTokenRequest dto = new RefreshTokenRequest("old-refresh-token");

        mockMvc.perform(post("/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-jwt-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    @DisplayName("POST /auth/refresh should return 401 when the refresh token is invalid, expired, or already used")
    void refresh_shouldReturn401_whenRefreshTokenIsInvalid() throws Exception {
        when(refreshTokenService.rotate("bad-token")).thenThrow(new InvalidRefreshTokenException());

        RefreshTokenRequest dto = new RefreshTokenRequest("bad-token");

        mockMvc.perform(post("/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid Refresh Token"));
    }

    @Test
    @DisplayName("POST /auth/refresh should return 422 when refreshToken is blank")
    void refresh_shouldReturn422_whenRefreshTokenIsBlank() throws Exception {
        RefreshTokenRequest dto = new RefreshTokenRequest("");

        mockMvc.perform(post("/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].fieldName").value("refreshToken"));
    }

    @Test
    @DisplayName("POST /auth/logout should return 204 without requiring authentication")
    void logout_shouldReturn204() throws Exception {
        RefreshTokenRequest dto = new RefreshTokenRequest("some-refresh-token");

        mockMvc.perform(post("/auth/logout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNoContent());
    }
}