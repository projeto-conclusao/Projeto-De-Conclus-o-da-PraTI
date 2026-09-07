package com.achadosedevolvidos.chat.model;

import com.achadosedevolvidos.match.model.Match;
import com.achadosedevolvidos.shared.model.BaseEntity;
import com.achadosedevolvidos.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * O "conversationId" citado no protótipo original do ChatController é, aqui, o
 * próprio id do Match: cada match tem exatamente uma conversa entre os dois donos
 * de item envolvidos. createdAt (herdado de BaseEntity) faz o papel de "sentAt".
 */
@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Message extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
}
