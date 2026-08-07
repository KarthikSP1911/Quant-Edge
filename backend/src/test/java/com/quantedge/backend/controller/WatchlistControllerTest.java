package com.quantedge.backend.controller;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quantedge.backend.entity.Company;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.enums.AuthProvider;
import com.quantedge.backend.enums.Role;
import com.quantedge.backend.repository.CompanyRepository;
import com.quantedge.backend.repository.UserRepository;
import com.quantedge.backend.repository.WatchlistRepository;
import com.quantedge.backend.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WatchlistControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private WatchlistRepository watchlistRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private Company apple;
    private String validToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        watchlistRepository.deleteAll();
        userRepository.deleteAll();
        companyRepository.deleteAll();

        apple = companyRepository.save(Company.builder()
                .symbol("AAPL")
                .name("Apple Inc.")
                .sector("Technology")
                .industry("Consumer Electronics")
                .exchange("NASDAQ")
                .build());

        User user = userRepository.save(User.builder()
                .name("Watchlist Test User")
                .email("watchlist@example.com")
                .passwordHash(passwordEncoder.encode("password"))
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .build());

        validToken = jwtService.generateAccessToken(user);
    }

    @AfterEach
    void tearDown() {
        watchlistRepository.deleteAll();
        userRepository.deleteAll();
        companyRepository.deleteAll();
    }

    @Test
    void addingASymbolPersistsAWatchlistEntry() throws Exception {
        mockMvc.perform(post("/api/watchlist/AAPL").header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(watchlistRepository.findAll())
                .hasSize(1);
    }

    @Test
    void addingTheSameSymbolTwiceIsIdempotent() throws Exception {
        mockMvc.perform(post("/api/watchlist/AAPL").header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/watchlist/AAPL").header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(watchlistRepository.findAll())
                .hasSize(1);
    }

    @Test
    void removingASymbolDeletesTheWatchlistEntry() throws Exception {
        mockMvc.perform(post("/api/watchlist/AAPL").header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/watchlist/AAPL").header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(watchlistRepository.findAll())
                .isEmpty();
    }

    @Test
    void unknownSymbolReturns404() throws Exception {
        mockMvc.perform(post("/api/watchlist/NOPE").header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void watchlistGraphQlQueryReturnsAddedCompany() throws Exception {
        mockMvc.perform(post("/api/watchlist/AAPL").header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNoContent());

        String graphQlRequest = "{\"query\":\"{ watchlist { company { symbol } } }\"}";

        mockMvc.perform(post("/graphql")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(graphQlRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.watchlist[0].company.symbol").value("AAPL"));
    }
}
