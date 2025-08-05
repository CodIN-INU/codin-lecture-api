package inu.codin.codin.domain.lecture.service;

import inu.codin.codin.domain.lecture.entity.Lecture;
import inu.codin.codin.domain.lecture.exception.LectureErrorCode;
import inu.codin.codin.domain.lecture.exception.LectureException;
import inu.codin.codin.domain.lecture.repository.LectureRepository;
import inu.codin.codin.domain.review.entity.Review;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ai.openai.OpenAiChatModel;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LectureSummarizationService {

    private final OpenAiChatModel chatModel;
    private final LectureRepository lectureRepository;

    /**
     * Generates and saves an AI-based summary for the specified lecture using its metadata, tags, and reviews.
     *
     * Retrieves the lecture by ID, validates the presence and content of reviews, constructs a prompt with lecture details,
     * and requests a summary from the AI chat model. If successful, updates the lecture with the generated summary.
     * Skips summarization if there are no reviews or valid review content. Throws a {@code LectureException} if the lecture
     * is not found or if AI summary generation fails.
     *
     * @param lectureId the ID of the lecture to summarize
     */
    @Transactional
    public void summarizeLecture(Long lectureId) {
        Lecture lecture = lectureRepository.findLectureWithTagsAndReviewsById(lectureId)
                .orElseThrow(() -> new LectureException(LectureErrorCode.LECTURE_NOT_FOUND));

        // 리뷰가 없거나 최소 조건 만족 예외 처리
        if (lecture.getReviews().isEmpty()) {
            log.info("강의에 리뷰가 없어 AI 요약을 생략, lectureId:{}", lectureId);
            return;
        }

        // 리뷰들을 하나의 문자열화
        String reviewsText = lecture.getReviews().stream()
                .map(Review::getContent)
                .collect(Collectors.joining("\n"));

        if (reviewsText.trim().isEmpty()) {
            log.info("유효한 리뷰 내용이 없어 AI 요약을 생략, lectureId:{}", lectureId);
            return;
        }

        // 강의 태그 메타데이터 문자열화
        String tags = lecture.getTags().stream()
                .map(t -> t.getTag().getTagName())
                .collect(Collectors.joining(", "));

        // Prompt 생성
        Prompt prompt = new Prompt(
                List.of(
                        new SystemMessage("당신은 친절한 대학 강의 큐레이션 도우미입니다."),
                        new UserMessage(buildPrompt(lecture, reviewsText, tags))
                )
        );

        // ChatModel 호출 -> AI 요약문 반환
        try {
            ChatResponse response = chatModel.call(prompt);
            String aiSummary = response.getResult().getOutput().getText();

            if (aiSummary == null || aiSummary.trim().isEmpty()) {
                log.warn("강의 ID {}에 대한 AI 요약이 비어있습니다.", lectureId);
                return;
            }
            // AI 요약문 저장
            lecture.updateAiSummary(aiSummary);
        } catch (Exception e) {
            log.error("강의 ID {}의 AI 요약 생성 중 오류 발생", lectureId, e);
            throw new LectureException(LectureErrorCode.AI_SUMMARY_GENERATION_FAILED);
        }
    }

    /**
     * Constructs a formatted prompt string containing lecture details, tags, syllabus, and reviews for AI-based summarization.
     *
     * @param lecture the lecture entity containing metadata and syllabus information
     * @param reviews concatenated review content for the lecture
     * @param tags comma-separated lecture tags
     * @return a multi-line prompt instructing the AI to summarize the lecture in a Q&A format within 700 characters
     */
    private String buildPrompt(Lecture lecture, String reviews, String tags) {
        return """
            아래 정보를 토대로 “질문: 답변” 형태로 총 700자 이내로 요약해 주세요.

            【교과목명】%s
            【학년】%d학년
            【학점】%d학점
            【교수】%s
            【학과】%s
            【수업 유형】%s
            【수업 방식】%s
            【평가 방식】%s
            【태그】%s
            【강의 계획서】%s

            【수강 후기】
            %s

            1. 이 수업, 뭘 배우나요?
            2. 수업 난이도는?
            3. 누가 들으면 좋을까요?
            4. 수업 방식은?
            5. 내가 얻을 수 있는 것은?
            """
                .formatted(
                        lecture.getLectureNm(),
                        lecture.getGrade(),
                        lecture.getCredit(),
                        lecture.getProfessor(),
                        lecture.getDepartment(),
                        lecture.getType(),
                        lecture.getLectureType(),
                        lecture.getEvaluation(),
                        tags.isEmpty() ? "태그 없음" : tags,
                        lecture.getSyllabus() == null ? "강의 계획서 없음" : lecture.getSyllabus(),
                        reviews
                );
    }
}
