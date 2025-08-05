package inu.codin.codin.domain.lecture.repository;

import inu.codin.codin.domain.lecture.entity.Lecture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LectureRepository extends JpaRepository<Lecture, Long> {
    @Query("""
        SELECT l FROM Lecture l
        LEFT JOIN FETCH l.schedule
        LEFT JOIN FETCH l.tags
        WHERE l.id=:lectureId
    """)
    Optional<Lecture> findLectureWithScheduleAndTagsById(Long lectureId);

    /**
     * Retrieves a lecture by its ID, eagerly loading its associated semester and reviews.
     *
     * @param lectureId the ID of the lecture to retrieve
     * @return an {@code Optional} containing the lecture with its semester and reviews if found, or empty if not found
     */
    @Query("""
        SELECT l FROM Lecture l
        LEFT JOIN FETCH l.semester
        LEFT JOIN FETCH l.reviews
        WHERE l.id=:lectureId
    """)
    Optional<Lecture> findLectureWithSemesterAndReviewsById(Long lectureId);

    /**
     * Retrieves a paginated list of distinct lectures by their IDs, eagerly loading associated tags, semester, and schedule.
     *
     * @param lectureIds the list of lecture IDs to filter by
     * @return a page of lectures with their tags, semester, and schedule associations loaded
     */
    @Query("""
        SELECT DISTINCT l FROM Lecture l
        LEFT JOIN FETCH l.tags
        LEFT JOIN FETCH l.semester
        LEFT JOIN FETCH l.schedule
        WHERE l.id IN :lectureIds
    """)
    Page<Lecture> findAllWithAssociationsByIds(List<Long> lectureIds);

    /**
     * Retrieves a paginated list of all Lecture entities.
     *
     * @param pageable pagination and sorting information
     * @return a page of Lecture entities
     */
    @Query("""
        SELECT l FROM Lecture l
    """)
    Page<Lecture> findAllPaged(Pageable pageable);


    /**
     * Retrieves a lecture by its ID, eagerly loading its associated tags and reviews.
     *
     * @param lectureId the ID of the lecture to retrieve
     * @return an {@code Optional} containing the lecture with its tags and reviews if found, or empty if not found
     */
    @Query("""
        SELECT DISTINCT l FROM Lecture l
        LEFT JOIN FETCH l.tags
        LEFT JOIN FETCH l.reviews
        WHERE l.id=:lectureId
    """)
    Optional<Lecture> findLectureWithTagsAndReviewsById(Long lectureId);
}
