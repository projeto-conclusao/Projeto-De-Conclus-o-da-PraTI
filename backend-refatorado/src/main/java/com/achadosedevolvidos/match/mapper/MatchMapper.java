package com.achadosedevolvidos.match.mapper;

import com.achadosedevolvidos.match.dto.MatchResponse;
import com.achadosedevolvidos.match.model.Match;
import org.springframework.stereotype.Component;

@Component
public class MatchMapper {

    public MatchResponse toResponse(Match match) {
        return new MatchResponse(
                match.getId(),
                match.getLostItem().getId(),
                match.getLostItem().getTitle(),
                match.getFoundItem().getId(),
                match.getFoundItem().getTitle(),
                match.getScore(),
                match.getStatus(),
                match.getCreatedAt()
        );
    }
}
