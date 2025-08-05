package inu.codin.codin.domain.elasticsearch.convertor;

import inu.codin.codin.domain.elasticsearch.document.LectureDocument;
import inu.codin.codin.domain.lecture.entity.Lecture;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LectureDocumentConverter {

    /**
     * Converts a Lecture entity into a LectureDocument for Elasticsearch indexing.
     *
     * Maps relevant fields from the Lecture object to a new LectureDocument, converting enums and collections as needed for document storage.
     *
     * @param lecture the Lecture entity to convert
     * @return a LectureDocument representing the given Lecture
     */
    public LectureDocument convertToDocument(Lecture lecture) {
        return LectureDocument.builder()
                .id(lecture.getId())
                .lectureNm(lecture.getLectureNm())
                .grade(lecture.getGrade())
                .department(lecture.getDepartment().name())
                .starRating(lecture.getStarRating())
                .likes((long) lecture.getLikes())
                .hits((long) lecture.getHits())
                .professor(lecture.getProfessor())
                .type(lecture.getType().name())
                .lectureType(lecture.getLectureType())
                .evaluation(lecture.getEvaluation().name())
                .preCourses(lecture.getPreCourse() != null ? List.of(lecture.getPreCourse()) : List.of())
                .tags(lecture.getTags().stream()
                        .map(tag -> tag.getTag().getTagName())
                        .toList())
                .semesters(lecture.getSemester().stream()
                        .map(ls -> ls.getSemester().getString())
                        .toList())
                .build();
    }
}
