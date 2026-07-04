package com.asknehru.backend;

import com.asknehru.auth.model.Role;
import com.asknehru.auth.model.UserAccount;
import com.asknehru.auth.model.UserRole;
import com.asknehru.auth.repository.RoleRepository;
import com.asknehru.auth.repository.UserAccountRepository;
import com.asknehru.auth.repository.UserRoleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        public org.springframework.ai.vectorstore.VectorStore vectorStore() {
            return org.mockito.Mockito.mock(org.springframework.ai.vectorstore.VectorStore.class);
        }

        @org.springframework.context.annotation.Bean
        public org.springframework.kafka.core.KafkaTemplate kafkaTemplate() {
            return org.mockito.Mockito.mock(org.springframework.kafka.core.KafkaTemplate.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

        @Autowired
        private UserAccountRepository userAccountRepository;

        @Autowired
        private RoleRepository roleRepository;

        @Autowired
        private UserRoleRepository userRoleRepository;

    @Test
    void registerLoginRefreshMeAndLogoutFlow() throws Exception {
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "email", "integration-user@asknehru.com",
                "password", "Password@123",
                "fullName", "Integration User"
        ));

        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode registerJson = objectMapper.readTree(registerResponse);
        String accessToken = registerJson.path("data").path("accessToken").asText();
        String refreshToken = registerJson.path("data").path("refreshToken").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        String meResponse = mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode meJson = objectMapper.readTree(meResponse);
        assertThat(meJson.path("data").path("email").asText()).isEqualTo("integration-user@asknehru.com");

        String refreshBody = objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken));
        String refreshResponse = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode refreshJson = objectMapper.readTree(refreshResponse);
        String rotatedAccessToken = refreshJson.path("data").path("accessToken").asText();
        String rotatedRefreshToken = refreshJson.path("data").path("refreshToken").asText();
        assertThat(rotatedAccessToken).isNotBlank();
        assertThat(rotatedRefreshToken).isNotBlank();
        assertThat(rotatedRefreshToken).isNotEqualTo(refreshToken);

        String logoutBody = objectMapper.writeValueAsString(Map.of("refreshToken", rotatedRefreshToken));
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + rotatedAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutBody))
                .andExpect(status().isOk());
    }

    @Test
    void duplicateRegisterReturnsConflict() throws Exception {
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "email", "duplicate@asknehru.com",
                "password", "Password@123",
                "fullName", "Duplicate User"
        ));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isConflict());
    }

    @Test
    void disabledAccountCannotLogin() throws Exception {
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "email", "disabled@asknehru.com",
                "password", "Password@123",
                "fullName", "Disabled User"
        ));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk());

        UserAccount user = userAccountRepository.findByEmail("disabled@asknehru.com").orElseThrow();
        user.setEnabled(false);
        userAccountRepository.save(user);

        String loginBody = objectMapper.writeValueAsString(Map.of(
                "email", "disabled@asknehru.com",
                "password", "Password@123"
        ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminPingForbiddenForUserRole() throws Exception {
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "email", "normal-user@asknehru.com",
                "password", "Password@123",
                "fullName", "Normal User"
        ));

        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = objectMapper.readTree(registerResponse)
                .path("data")
                .path("accessToken")
                .asText();

        mockMvc.perform(get("/api/auth/admin/ping")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPingAllowedForAdminRole() throws Exception {
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "email", "admin-user@asknehru.com",
                "password", "Password@123",
                "fullName", "Admin User"
        ));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk());

        UserAccount user = userAccountRepository.findByEmail("admin-user@asknehru.com").orElseThrow();
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ROLE_ADMIN");
                    return roleRepository.save(role);
                });

        if (!userRoleRepository.existsByUserIdAndRoleId(user.getId(), adminRole.getId())) {
            UserRole adminMapping = new UserRole();
            adminMapping.setUserId(user.getId());
            adminMapping.setRoleId(adminRole.getId());
            userRoleRepository.save(adminMapping);
        }

        String loginBody = objectMapper.writeValueAsString(Map.of(
                "email", "admin-user@asknehru.com",
                "password", "Password@123"
        ));

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = objectMapper.readTree(loginResponse)
                .path("data")
                .path("accessToken")
                .asText();

        mockMvc.perform(get("/api/auth/admin/ping")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void usersModuleCrudFlow() throws Exception {
        String token = registerAndGetAccessToken("users-admin@asknehru.com", "Users Admin");

        String createUserBody = objectMapper.writeValueAsString(Map.of(
                "username", "api-user-1",
                "email", "api-user-1@asknehru.com",
                "password", "Password@123"
        ));

        String createResponse = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createJson = objectMapper.readTree(createResponse);
        int createdUserId = createJson.path("id").asInt();
        assertThat(createdUserId).isPositive();

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        String updateBody = objectMapper.writeValueAsString(Map.of(
                "username", "api-user-1-updated"
        ));

        mockMvc.perform(put("/api/users/{id}", createdUserId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk());
    }



    @Test
    void uploadImageAndAudioReturnStoredUrls() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "picture.png",
                "image/png",
                "image-bytes".getBytes()
        );

        String imageResponse = mockMvc.perform(multipart("/api/upload/image")
                        .file(imageFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode imageJson = objectMapper.readTree(imageResponse);
        assertThat(imageJson.path("filename").asText()).isNotBlank();
        assertThat(imageJson.path("url").asText()).contains("/media/yoga-poses/");

        MockMultipartFile audioFile = new MockMultipartFile(
                "file",
                "sound.mp3",
                "audio/mpeg",
                "audio-bytes".getBytes()
        );

        String audioResponse = mockMvc.perform(multipart("/api/upload/audio")
                        .file(audioFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode audioJson = objectMapper.readTree(audioResponse);
        assertThat(audioJson.path("filename").asText()).isNotBlank();
        assertThat(audioJson.path("url").asText()).contains("/media/yoga-audio/");
    }

    @Test
    void uploadImageRejectsNonImageContentType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "note.txt",
                "text/plain",
                "plain text".getBytes()
        );

        mockMvc.perform(multipart("/api/upload/image")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    void yogaPosesCreateAndSearchFlow() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of(
                "yogaName", "Trikonasana",
                "yogaNameEnglish", "Triangle Pose",
                "blogContent", "Stretch and balance",
                "audioURL", "https://api.asknehru.com/media/yoga-audio/sample.mp3",
                "videoURL", "https://api.asknehru.com/media/yoga-video/sample.mp4",
                "imageURL", "https://api.asknehru.com/media/yoga-poses/sample.png",
                "category", "standing"
        ));

        mockMvc.perform(post("/api/yoga/poses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/yoga/poses/search")
                        .param("yogaName", "Triko"))
                .andExpect(status().isOk());
    }

    private String registerAndGetAccessToken(String email, String fullName) throws Exception {
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", "Password@123",
                "fullName", fullName
        ));

        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(registerResponse).path("data").path("accessToken").asText();
    }
}
