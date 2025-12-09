package com.example.dance_community.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommentRequest {
    @NotBlank(message = "댓글 미입력")
    @Size(max = 500, message = "댓글은 500자 이내로 작성해주세요.")
    private String content;

    Long postId;
    Long eventId;
}