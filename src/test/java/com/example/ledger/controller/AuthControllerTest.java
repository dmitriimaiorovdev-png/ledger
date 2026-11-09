package com.example.ledger.controller;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.example.firstpetprojectexchanger.dto.AuthResponse;
import org.example.firstpetprojectexchanger.dto.LoginRequest;
import org.example.firstpetprojectexchanger.dto.RefreshRequest;
import org.example.firstpetprojectexchanger.dto.RegisterRequest;
import org.example.firstpetprojectexchanger.dto.UserDto;
import org.example.firstpetprojectexchanger.model.User;
import org.example.firstpetprojectexchanger.security.JwtAuthenticationFilter;
import org.example.firstpetprojectexchanger.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(//этим мы убираем лишние моки, которые я закомментил
        controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@Import(AuthControllerTest.TestConfig.class)
class AuthControllerTest {

	
	@TestConfiguration
	static class TestConfig implements WebMvcConfigurer {
	    @Override
	    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
	        resolvers.add(new AuthenticationPrincipalArgumentResolver());
	    }
	}
	/*
	@MockitoBean
	private UserRepository userRepository;
	
	@MockitoBean
	private JwtService jwtService;
	*/
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("Успешная регистрация пользователя")
    void register_Success() throws Exception {

        // Arrange
    	RegisterRequest request = new RegisterRequest(
    	        "Ivan",
    	        "Ivanov",
    	        "ivan@test.ru",
    	        "Ivanov123"
    	);

        AuthResponse response = new AuthResponse(
                "access-token",
                "Bearer",
                3600L,
                "refresh-token",
                null
        );

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));

        // Verify
        verify(authService, times(1))
                .register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Успешная авторизация пользователя")
    void login_Success() throws Exception {

        // Arrange
        LoginRequest request = new LoginRequest(
                "vlad@mail.ru",
                "A5536dxVSCsc"
        );

        AuthResponse response = new AuthResponse(
                "access-token",
                "Bearer",
                3600L,
                "refresh-token",
                null
        );

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));

        // Verify
        verify(authService, times(1))
                .login(any(LoginRequest.class));
    }


    @Test
    @DisplayName("Успешное обновление пользователя")
    void refresh_Success() throws Exception {

        // Arrange
        RefreshRequest request = new RefreshRequest(
                "123321"
        );

        AuthResponse response = new AuthResponse(
                "access-token",
                "Bearer",
                3600L,
                "refresh-token",
                null
        );

        when(authService.refresh(any(String.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));

        // Verify
        verify(authService, times(1))
                .refresh(any(String.class));
    }


   

    	@Test
    	@DisplayName("Успешное получение текущего пользователя")
    	void me_Success() throws Exception {

    	    // Arrange
    	    User user = new User();
    	    user.setId(1L);

    	    UserDto response = new UserDto(
    	            1L,
    	            "vlad@test.ru",
    	            "Vlad",
    	            "Ivanov",
    	            "USER",
    	            "ACTIVE",
    	            Instant.now()
    	    );

    	    when(authService.me(1L)).thenReturn(response);

    	    UsernamePasswordAuthenticationToken auth =
    	            new UsernamePasswordAuthenticationToken(
    	                    user,
    	                    null,
    	                    List.of()
    	            );

    	    SecurityContext context = SecurityContextHolder.createEmptyContext();
    	    context.setAuthentication(auth);
    	    SecurityContextHolder.setContext(context);

    	   // try {
    	        // Act
    	        mockMvc.perform(get("/api/auth/me"))
    	                .andDo(print())
    	                .andExpect(status().isOk())
    	                .andExpect(jsonPath("$.id").value(1))
    	                .andExpect(jsonPath("$.email").value("vlad@test.ru"));

    	        // Verify
    	        verify(authService).me(1L);

    	  //  } finally {
    	     //   SecurityContextHolder.clearContext();
    	   // }
    	}
    
}
