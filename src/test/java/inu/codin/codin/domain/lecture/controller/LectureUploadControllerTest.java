package inu.codin.codin.domain.lecture.controller;

import inu.codin.codin.domain.lecture.dto.MetaMode;
import inu.codin.codin.domain.lecture.service.LectureUploadService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test stack: JUnit 5 (Jupiter) + Spring Boot Test (@WebMvcTest) + MockMvc + Mockito + spring-security-test.
 * Focus: LectureUploadController endpoints and behavior added in the PR.
 */
@WebMvcTest(controllers = LectureUploadController.class)
@Import(LectureUploadControllerTest.MethodSecurityConfig.class)
class LectureUploadControllerTest {

    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityConfig {
        // Uses default ROLE_ prefix; note controller uses hasAnyRole('ROLE_ADMIN','ROLE_MANAGER'),
        // hence tests use roles="ROLE_ADMIN"/"ROLE_MANAGER" to match (Spring adds ROLE_ prefix).
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LectureUploadService lectureUploadService;

    @Test
    @DisplayName("POST /upload as ROLE_ADMIN: returns 201 and calls service with uploaded file")
    @WithMockUser(roles = "ROLE_ADMIN")
    void uploadNewSemesterLectures_asAdmin_returns201_andCallsService() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "excelFile",
                "24-1.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3}
        );

        doNothing().when(lectureUploadService).uploadNewSemesterLectures(any());

        mockMvc.perform(multipart("/upload").file(file).contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message", containsString("24-1.xlsx")))
                .andExpect(jsonPath("$.message", containsString("강의 내역 업로드")));

        ArgumentCaptor<org.springframework.web.multipart.MultipartFile> captor =
                ArgumentCaptor.forClass(org.springframework.web.multipart.MultipartFile.class);
        verify(lectureUploadService, times(1)).uploadNewSemesterLectures(captor.capture());
        org.springframework.web.multipart.MultipartFile passed = captor.getValue();
        // Verify the same filename reaches the service
        org.junit.jupiter.api.Assertions.assertEquals("24-1.xlsx", passed.getOriginalFilename());
    }

    @Test
    @DisplayName("POST /upload/rooms as ROLE_MANAGER: returns 201 and calls service with uploaded file")
    @WithMockUser(roles = "ROLE_MANAGER")
    void uploadNewSemesterRooms_asManager_returns201_andCallsService() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "excelFile",
                "24-2.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{9, 8, 7}
        );

        doNothing().when(lectureUploadService).uploadNewSemesterRooms(any());

        mockMvc.perform(multipart("/upload/rooms").file(file).contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message", containsString("24-2.xlsx")))
                .andExpect(jsonPath("$.message", containsString("강의실 현황 업데이트")));

        verify(lectureUploadService, times(1)).uploadNewSemesterRooms(any());
    }

    @Test
    @DisplayName("POST /upload/meta default mode: ALL (no mode param) -> 201 and service called with MetaMode.ALL")
    @WithMockUser(roles = "ROLE_ADMIN")
    void uploadLectureMeta_defaultMode_callsServiceWithALL() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "excelFile",
                "info_25_2_meta.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy".getBytes()
        );

        doNothing().when(lectureUploadService).uploadLectureMeta(any(), any());

        mockMvc.perform(multipart("/upload/meta").file(file).contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message", containsString("info_25_2_meta.xlsx")))
                .andExpect(jsonPath("$.message", containsString("메타데이터")));

        verify(lectureUploadService, times(1)).uploadLectureMeta(any(), eq(MetaMode.ALL));
    }

    @Test
    @DisplayName("POST /upload/meta with mode=TAGS -> 201 and service called with MetaMode.TAGS")
    @WithMockUser(roles = "ROLE_ADMIN")
    void uploadLectureMeta_withTags_callsServiceWithTAGS() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "excelFile",
                "info_25_2_meta.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{0x1}
        );

        doNothing().when(lectureUploadService).uploadLectureMeta(any(), any());

        mockMvc.perform(
                        multipart("/upload/meta")
                                .file(file)
                                .param("mode", "TAGS")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message", containsString("메타데이터")));

        verify(lectureUploadService, times(1)).uploadLectureMeta(any(), eq(MetaMode.TAGS));
    }

    @Test
    @DisplayName("POST /upload without authentication -> 401 Unauthorized")
    void uploadNewSemesterLectures_unauthenticated_returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "excelFile", "24-1.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1}
        );

        mockMvc.perform(multipart("/upload").file(file).contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /upload as ROLE_USER (insufficient) -> 403 Forbidden")
    @WithMockUser(roles = "USER")
    void uploadNewSemesterLectures_insufficientRole_returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "excelFile", "24-1.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1}
        );

        mockMvc.perform(multipart("/upload").file(file).contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /upload/rooms missing file parameter -> 400 Bad Request and service not called")
    @WithMockUser(roles = "ROLE_ADMIN")
    void uploadNewSemesterRooms_missingFile_returns400() throws Exception {
        mockMvc.perform(multipart("/upload/rooms").contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        verify(lectureUploadService, never()).uploadNewSemesterRooms(any());
    }

    @Test
    @DisplayName("POST /upload/meta with invalid mode value -> 400 Bad Request and service not called")
    @WithMockUser(roles = "ROLE_ADMIN")
    void uploadLectureMeta_invalidMode_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "excelFile", "info_25_2_meta.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{2,3}
        );

        mockMvc.perform(
                        multipart("/upload/meta")
                                .file(file)
                                .param("mode", "INVALID")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andExpect(status().isBadRequest());

        verify(lectureUploadService, never()).uploadLectureMeta(any(), any());
    }
}