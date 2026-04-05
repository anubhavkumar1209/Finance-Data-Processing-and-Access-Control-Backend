package com.financedashboard.config;

import com.financedashboard.entity.AccountStatus;
import com.financedashboard.entity.Role;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.auth.registration")
public class SelfRegistrationProperties {

    private Role defaultRole = Role.VIEWER;

    private AccountStatus defaultStatus = AccountStatus.ACTIVE;

    private List<Role> allowedRoles = new ArrayList<>(List.of(Role.VIEWER));

    private List<AccountStatus> allowedStatuses = new ArrayList<>(List.of(AccountStatus.ACTIVE));
}
