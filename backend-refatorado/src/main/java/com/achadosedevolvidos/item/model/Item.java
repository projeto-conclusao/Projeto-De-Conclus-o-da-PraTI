package com.achadosedevolvidos.item.model;

import com.achadosedevolvidos.category.model.Category;
import com.achadosedevolvidos.shared.model.BaseEntity;
import com.achadosedevolvidos.user.model.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "items")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Item extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ItemStatus status = ItemStatus.ANALISANDO;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "short_description", length = 100, nullable = false)
    private String shortDescription;

    private String locationText;
    private Double latitude;
    private Double longitude;

    private LocalDateTime eventDate;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemImage> images = new ArrayList<>();

    public enum ItemType { PERDIDO, ENCONTRADO }

    public enum ItemStatus { ANALISANDO, PROCURANDO, POSSIVEL_MATCH, RESOLVIDO, INATIVO }
}
