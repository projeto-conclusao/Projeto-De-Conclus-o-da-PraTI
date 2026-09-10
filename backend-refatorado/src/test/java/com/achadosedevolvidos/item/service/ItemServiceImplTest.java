package com.achadosedevolvidos.item.service;

import com.achadosedevolvidos.category.model.Category;
import com.achadosedevolvidos.category.repository.CategoryRepository;
import com.achadosedevolvidos.item.dto.CreateItemRequest;
import com.achadosedevolvidos.item.dto.ItemResponse;
import com.achadosedevolvidos.item.event.ItemCreatedEvent;
import com.achadosedevolvidos.item.mapper.ItemMapper;
import com.achadosedevolvidos.item.model.Item;
import com.achadosedevolvidos.item.repository.ItemRepository;
import com.achadosedevolvidos.shared.exception.AppException;
import com.achadosedevolvidos.user.model.User;
import com.achadosedevolvidos.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários (Mockito) do ItemServiceImpl. O ponto mais importante coberto
 * aqui é o desacoplamento com o módulo match: criar um item deve publicar um
 * {@link ItemCreatedEvent} e nada mais — o service não deve conhecer MatchService.
 */
@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ItemServiceImpl itemService;

    private User user;
    private Category category;

    @BeforeEach
    void setUp() {
        itemService = new ItemServiceImpl(itemRepository, categoryRepository, userRepository, new ItemMapper(), eventPublisher);

        user = User.builder().id(UUID.randomUUID()).name("Ana").email("ana@teste.com")
                .role(User.Role.USER).provider(User.AuthProvider.LOCAL).build();
        category = Category.builder().id(UUID.randomUUID()).name("Eletrônicos").build();
    }

    @Test
    void sunnyDay_deveCriarItemEPublicarEventoDeCriacao() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
            Item item = invocation.getArgument(0);
            item.setId(UUID.randomUUID());
            item.setCreatedAt(LocalDateTime.now());
            return item;
        });

        CreateItemRequest request = new CreateItemRequest(
                Item.ItemType.PERDIDO, category.getId(), "Carteira preta", "Perdida no ônibus",
                "Carteira preta perdida", "Terminal Central", -23.55, -46.63, LocalDateTime.now(), List.of("http://img/1.png")
        );

        ItemResponse response = itemService.createAndAnalyze(user.getId(), request);

        assertThat(response.title()).isEqualTo("Carteira preta");
        assertThat(response.userId()).isEqualTo(user.getId());
        assertThat(response.categoryId()).isEqualTo(category.getId());
        assertThat(response.shortDescription()).isEqualTo("Carteira preta perdida");
        assertThat(response.images()).hasSize(1);

        ArgumentCaptor<ItemCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ItemCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().itemId()).isEqualTo(response.id());
    }

    @Test
    void rainyDay_deveLancarUnauthorizedQuandoUsuarioNaoExiste() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        CreateItemRequest request = validRequest();

        assertThatThrownBy(() -> itemService.createAndAnalyze(user.getId(), request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);

        verify(itemRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rainyDay_deveLancarBadRequestQuandoCategoriaNaoExiste() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.empty());

        CreateItemRequest request = validRequest();

        assertThatThrownBy(() -> itemService.createAndAnalyze(user.getId(), request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);

        verify(itemRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void sunnyDay_deveEncontrarItemPorId() {
        Item item = Item.builder().id(UUID.randomUUID()).user(user).category(category)
                .type(Item.ItemType.ENCONTRADO).title("Chaveiro").eventDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now()).build();
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        ItemResponse response = itemService.findById(item.getId());

        assertThat(response.id()).isEqualTo(item.getId());
        assertThat(response.title()).isEqualTo("Chaveiro");
    }

    @Test
    void rainyDay_deveLancarNotFoundQuandoItemNaoExiste() {
        UUID idInexistente = UUID.randomUUID();
        when(itemRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.findById(idInexistente))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    private CreateItemRequest validRequest() {
        return new CreateItemRequest(
                Item.ItemType.PERDIDO, category.getId(), "Carteira preta", "Perdida no ônibus",
                "Carteira preta perdida", null, -23.55, -46.63, LocalDateTime.now(), null
        );
    }
}
