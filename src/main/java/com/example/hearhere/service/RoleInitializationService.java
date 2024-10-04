package com.example.hearhere.service;

import com.example.hearhere.entity.Role;
import com.example.hearhere.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class RoleInitializationService {

    private final RoleRepository roleRepository;

    public RoleInitializationService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @PostConstruct
    public void initializeRoles() {
        if (roleRepository.findByName("ROLE_USER").isEmpty()) {
            Role roleUser = new Role();
            roleUser.setName("ROLE_USER");
            roleRepository.save(roleUser);
        }
        if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
            Role roleAdmin = new Role();
            roleAdmin.setName("ROLE_ADMIN");
            roleRepository.save(roleAdmin);
        }
    }
}
