package inu.codin.codin.domain.lecture.entity;

import inu.codin.codin.domain.lecture.converter.EvaluationConverter;
import inu.codin.codin.domain.lecture.converter.TypeConverter;
import inu.codin.codin.domain.review.entity.Review;
import inu.codin.codin.global.common.entity.Department;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.util.List;
import java.util.Set;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicUpdate
public class Lecture {

    // todo: 강의계획서 내용, 강의 계획서 AI 요약 컬럼 추가

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String lectureNm;                                   //교과목명
    private int grade;                                          //학년 (0 : 전학년)
    private int credit;                                         //학점
    private String professor;                                   //교수명

    @Enumerated(EnumType.STRING)
    private Department department;                              //학과 (OTHERS : 교양)

    @Convert(converter = TypeConverter.class)
    private Type type;                                          //수업 유형(전공핵심, 전공선택..)
    private String lectureType;                                 //수업 방식(강의(이론), 온오프라인혼합형..)

    @Convert(converter = EvaluationConverter.class)
    private Evaluation evaluation;                              //평가 방식(상대평가, 절대평가, 이수)
    private String preCourse;                                   //사전 과목 //todo List로 관리 피룡
    private double starRating;                                  //과목 평점
    private int likes;                                          //좋아요 수
    private int hits;                                           //조회 수

    /** 강의 계획서 */
    @Column(columnDefinition = "TEXT")
    private String syllabus;

    /** 교과목 정보 + 강의 계획서 + 리뷰 -> AI 요약 */
    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "emotion_id")
    private Emotion emotion;                                    //수강 후기의 평점 분포도

    @OneToMany(mappedBy = "lecture", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<LectureSemester> semester;                      //과목이 진행된 학기

    @OneToMany(mappedBy = "lecture", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<LectureTag> tags;                               //과목의 키워드

    @OneToMany(mappedBy = "lecture", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<LectureSchedule> schedule;                      //과목 시간표

    @OneToMany(mappedBy = "lecture", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews;                               //수강 후기

    public void increaseLikes() { this.likes++; }

    public void decreaseLikes() { this.likes--; }

    public void increaseHits(){
        this.hits++;
    }

    public void updateReviewRating(double starRating, Emotion emotion) {
        this.starRating = starRating;
        this.emotion = emotion;
    }

    /**
     * Sets or updates the emotion associated with this lecture.
     *
     * @param emotion the Emotion entity representing the lecture's rating distribution
     */
    public void assignEmotion(Emotion emotion) {
        this.emotion = emotion;
    }

    /**
     * Updates the AI-generated summary for the lecture.
     *
     * @param aiSummary the new AI-generated summary text to set
     */
    public void updateAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }
}
