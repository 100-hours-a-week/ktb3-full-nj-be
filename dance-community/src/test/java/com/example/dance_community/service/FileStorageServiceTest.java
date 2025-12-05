package com.example.dance_community.service;

import com.example.dance_community.config.FileProperties;
import com.example.dance_community.enums.ImageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient; // lenient 확인

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    private FileStorageService fileStorageService;

    @Mock
    private FileProperties fileProperties;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // lenient()를 써서 사용되지 않는 설정 경고 무시
        lenient().when(fileProperties.getBaseDir()).thenReturn(tempDir.toString());
        fileStorageService = new FileStorageService(fileProperties);
    }

    @Test
    @DisplayName("이미지 저장 성공")
    void saveImage_Success() {
        String filename = "test.jpg";
        MockMultipartFile file = new MockMultipartFile("image", filename, "image/jpeg", "content".getBytes());

        String savedPath = fileStorageService.saveImage(file, ImageType.POST);

        assertThat(savedPath).contains(filename);
    }

    @Test
    @DisplayName("이미지 저장 실패 - 빈 파일")
    void saveImage_Fail_Empty_Post() {
        MockMultipartFile emptyFile = new MockMultipartFile("image", new byte[0]);

        assertThrows(IllegalArgumentException.class, () ->
                fileStorageService.saveImage(emptyFile, ImageType.POST)
        );
    }

    @Test
    @DisplayName("파일 삭제 성공")
    void deleteFile_Success() throws IOException {
        // given: 임시 파일 생성
        Path filePath = tempDir.resolve("delete_me.jpg");
        Files.createFile(filePath);

        // 파일이 생성되었는지 먼저 확인
        assertThat(Files.exists(filePath)).isTrue();

        // when: 삭제 시도 (절대 경로를 문자열로 변환)
        fileStorageService.deleteFile(filePath.toAbsolutePath().toString());

        // then: 파일이 지워졌는지 확인
        assertThat(Files.exists(filePath)).isFalse();
    }

    @Test
    @DisplayName("파일 삭제 - 파일이 존재하지 않아도 에러 안 남")
    void deleteFile_NotExists() {
        // given
        String nonExistentPath = "/path/to/nothing.jpg";

        // when & then (예외가 발생하지 않아야 함)
        fileStorageService.deleteFile(nonExistentPath);
    }

    @Test
    @DisplayName("파일 삭제 - 경로가 Null이거나 빈 문자열일 때 무시")
    void deleteFile_NullOrEmpty() {
        // when & then
        fileStorageService.deleteFile(null);
        fileStorageService.deleteFile("");
    }

    // --- [추가] 이미지 업데이트 로직 (processImageUpdate) ---

    // 테스트를 위한 가짜 엔티티 (ImageHolder 구현)
    static class FakeEntity implements com.example.dance_community.entity.ImageHolder {
        private List<String> images = new ArrayList<>();

        public FakeEntity(List<String> images) { this.images = new ArrayList<>(images); }

        @Override public List<String> getImages() { return images; }
        @Override public void updateImages(List<String> images) { this.images = images; }
    }

    @Test
    @DisplayName("이미지 업데이트 - 기존 이미지 삭제 및 새 목록 반영")
    void processImageUpdate_Success() throws IOException {
        // given
        // 1. 기존 파일 생성 (삭제되는지 확인용)
        Path oldFile = tempDir.resolve("old.jpg");
        Files.createFile(oldFile);
        String oldPath = oldFile.toAbsolutePath().toString();

        // 2. 엔티티 설정 (기존 이미지 보유)
        FakeEntity entity = new FakeEntity(List.of(oldPath));

        // 3. 요청 데이터 (기존 이미지는 목록에서 제외 -> 삭제되어야 함, 새 이미지는 없음)
        List<String> newImages = new ArrayList<>(); // 새 이미지 없음
        List<String> keepImages = new ArrayList<>(); // 유지할 이미지 없음 (즉, 다 삭제)

        // when
        fileStorageService.processImageUpdate(entity, newImages, keepImages);

        // then
        // 1. 파일이 실제로 삭제되었는지 확인
        assertThat(Files.exists(oldFile)).isFalse();

        // 2. 엔티티 리스트가 비워졌는지 확인
        assertThat(entity.getImages()).isEmpty();
    }

    @Test
    @DisplayName("이미지 업데이트 - 일부 유지 및 추가")
    void processImageUpdate_KeepAndAdd() throws IOException {
        // given
        Path keepFile = tempDir.resolve("keep.jpg");
        Files.createFile(keepFile);
        String keepPath = keepFile.toAbsolutePath().toString();

        FakeEntity entity = new FakeEntity(List.of(keepPath));

        List<String> newImages = List.of("new1.jpg", "new2.jpg");
        List<String> keepImages = List.of(keepPath); // 유지

        // when
        fileStorageService.processImageUpdate(entity, newImages, keepImages);

        // then
        assertThat(Files.exists(keepFile)).isTrue(); // 유지된 파일은 삭제되지 않음
        assertThat(entity.getImages()).hasSize(3); // keep(1) + new(2)
        assertThat(entity.getImages()).contains("new1.jpg", "new2.jpg");
    }
}