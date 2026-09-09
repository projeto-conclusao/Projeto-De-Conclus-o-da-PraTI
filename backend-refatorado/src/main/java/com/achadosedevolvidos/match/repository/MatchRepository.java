package com.achadosedevolvidos.match.repository;

import com.achadosedevolvidos.match.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MatchRepository extends JpaRepository<Match, UUID> {

    List<Match> findByLostItemIdOrFoundItemId(UUID lostItemId, UUID foundItemId);
}
