package inu.codin.codin.domain.lecture.repository.jpa;

import inu.codin.codin.domain.lecture.entity.Lecture;
import inu.codin.codin.domain.lecture.entity.Semester;
import inu.codin.codin.global.common.entity.Department;

import java.util.List;

public interface LectureSearchRepositoryCustom {
    List<Lecture> searchLecturesAtReview(Department department, Integer grade, Semester semester);
}
