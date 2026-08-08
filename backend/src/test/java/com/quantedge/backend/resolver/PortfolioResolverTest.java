package com.quantedge.backend.resolver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.quantedge.backend.dto.response.CompanyResponse;
import com.quantedge.backend.dto.response.PortfolioPositionResponse;
import com.quantedge.backend.dto.response.PortfolioSummaryResponse;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.enums.AuthProvider;
import com.quantedge.backend.enums.Role;
import com.quantedge.backend.repository.UserRepository;
import com.quantedge.backend.security.JwtService;
import com.quantedge.backend.service.DashboardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PortfolioResolverTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private DashboardService dashboardService;

    private MockMvc mockMvc;
    private String validToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        userRepository.deleteAll();
        User user = userRepository.save(User.builder()
                .name("Portfolio Test User")
                .email("portfolio-resolver@example.com")
                .passwordHash(passwordEncoder.encode("password"))
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .build());

        validToken = jwtService.generateAccessToken(user);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void portfolioQueryReturnsPositionsAndAccountValue() throws Exception {
        CompanyResponse company = new CompanyResponse(
                UUID.randomUUID(), "AAPL", "Apple Inc.", "Technology", "Consumer Electronics", null, null, "NASDAQ");
        PortfolioPositionResponse position = new PortfolioPositionResponse(
                company,
                10,
                new BigDecimal("100.00"),
                new BigDecimal("120.00"),
                new BigDecimal("118.00"),
                new BigDecimal("1.6949"),
                new BigDecimal("1200.00"),
                new BigDecimal("200.00"),
                new BigDecimal("20.0000"));
        PortfolioSummaryResponse summary = new PortfolioSummaryResponse(
                new BigDecimal("500.00"), List.of(position), new BigDecimal("1200.00"), new BigDecimal("1700.00"));
        when(dashboardService.getPortfolioSummary(any())).thenReturn(summary);

        String graphQlRequest =
                "{\"query\":\"{ portfolio { totalAccountValue positions { company { symbol } changePercent } } }\"}";

        mockMvc.perform(post("/graphql")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(graphQlRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.portfolio.totalAccountValue").value(1700.0))
                .andExpect(
                        jsonPath("$.data.portfolio.positions[0].company.symbol").value("AAPL"));
    }
}
