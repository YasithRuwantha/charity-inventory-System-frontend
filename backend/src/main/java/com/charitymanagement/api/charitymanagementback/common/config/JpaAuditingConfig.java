package com.charitymanagement.api.charitymanagementback.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Powers the {@code @CreatedDate}/{@code @LastModifiedDate} columns on BaseEntity. */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
