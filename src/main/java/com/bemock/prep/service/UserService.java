package com.bemock.prep.service;

import com.bemock.prep.dto.UserResponse;
import com.bemock.prep.exception.ResourceNotFoundException;
import com.bemock.prep.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserResponse get(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public List<UserResponse> list() {
        return userRepository.findAll(Sort.by("id")).stream().map(UserResponse::from).toList();
    }
}
