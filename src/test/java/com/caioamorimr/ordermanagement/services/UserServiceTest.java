package com.caioamorimr.ordermanagement.services;

import com.caioamorimr.ordermanagement.dto.UserDTO;
import com.caioamorimr.ordermanagement.dto.UserInsertDTO;
import com.caioamorimr.ordermanagement.dto.UserUpdateDTO;
import com.caioamorimr.ordermanagement.entities.User;
import com.caioamorimr.ordermanagement.repositories.UserRepository;
import com.caioamorimr.ordermanagement.services.exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private User user;
    private UserInsertDTO insertDTO;
    private UserUpdateDTO updateDTO;

    @BeforeEach
    void setUp() {
        user = new User(1L, "Caio", "caio@email.com", "988888888", "hashed_password");

        insertDTO = new UserInsertDTO();
        insertDTO.setName("Caio");
        insertDTO.setEmail("caio@email.com");
        insertDTO.setPhone("988888888");
        insertDTO.setPassword("123456");

        updateDTO = new UserUpdateDTO();
        updateDTO.setName("Caio Updated");
        updateDTO.setEmail("caio.updated@email.com");
        updateDTO.setPhone("977777777");
    }

    @Test
    @DisplayName("findAll should return a paginated page of UserDTOs")
    void findAll_shouldReturnPageOfUserDTOs() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user)));

        Page<UserDTO> result = userService.findAll(pageable);

        assertThat(result).isNotEmpty();
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("caio@email.com");
        // Senha nunca deve aparecer no DTO
        verify(userRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("findById should return UserDTO when user exists")
    void findById_shouldReturnUserDTO_whenUserExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserDTO result = userService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Caio");
    }

    @Test
    @DisplayName("findById should throw ResourceNotFoundException when user does not exist")
    void findById_shouldThrowResourceNotFoundException_whenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("insert should encode password and persist user")
    void insert_shouldEncodePasswordAndPersistUser() {
        when(passwordEncoder.encode("123456")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDTO result = userService.insert(insertDTO);

        assertThat(result.getEmail()).isEqualTo("caio@email.com");
        verify(passwordEncoder).encode("123456");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("delete should throw ResourceNotFoundException when user does not exist")
    void delete_shouldThrowResourceNotFoundException_whenUserNotFound() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("update should throw ResourceNotFoundException when user does not exist")
    void update_shouldThrowResourceNotFoundException_whenUserNotFound() {
        when(userRepository.getReferenceById(99L)).thenThrow(EntityNotFoundException.class);

        assertThatThrownBy(() -> userService.update(99L, updateDTO))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
