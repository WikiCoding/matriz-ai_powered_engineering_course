package com.wikicoding.matrizbackend.user;

import com.wikicoding.matrizbackend.user.domain.User.ProvisioningStatus;
import com.wikicoding.matrizbackend.user.persistence.UserDataModel;
import com.wikicoding.matrizbackend.user.persistence.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Local profile creation invoked after Keycloak identity creation, without an HTTP self-call. */
@Service
public class ProfileProvisioning {
    private final UserRepository users;
    public ProfileProvisioning(UserRepository users) { this.users = users; }

    @Transactional
    public Profile create(String subject, String email, UUID teamId) {
        UUID id = UUID.randomUUID();
        users.saveAndFlush(new UserDataModel(id, subject, email, teamId,
                ProvisioningStatus.COMPLETED, UUID.randomUUID()));
        return new Profile(id, email);
    }
    public record Profile(UUID id, String email) {}
}
