package inu.codin.codin.domain.lecture.repository.jpa;

import inu.codin.codin.domain.lecture.entity.Emotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmotionRepository extends JpaRepository<Emotion, Long> {
}
