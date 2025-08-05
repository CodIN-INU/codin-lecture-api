package inu.codin.codin.domain.review.repository;

import inu.codin.codin.domain.review.entity.UserReviewStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserReviewStatsRepository extends JpaRepository<UserReviewStats, Long> {
    /**
 * Retrieves the user review statistics associated with the specified user ID.
 *
 * @param userId the unique identifier of the user
 * @return an Optional containing the UserReviewStats if found, or an empty Optional if not present
 */
Optional<UserReviewStats> findByUserId(String userId);
}
