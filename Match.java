package com.achadosedevolvidos.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lost_item_id", nullable = false)
    private Item lostItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "found_item_id", nullable = false)
    private Item foundItem;

    @Column(nullable = false)
    private Double score; // Porcentagem de 0 a 100%

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status = MatchStatus.PENDENTE;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum MatchStatus { PENDENTE, ACEITO, REJEITADO }
}
