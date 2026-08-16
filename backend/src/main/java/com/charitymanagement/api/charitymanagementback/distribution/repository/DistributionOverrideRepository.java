package com.charitymanagement.api.charitymanagementback.distribution.repository;

import com.charitymanagement.api.charitymanagementback.distribution.entity.DistributionOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DistributionOverrideRepository extends JpaRepository<DistributionOverride, Long> {

    List<DistributionOverride> findByDistributionRequestIdOrderByCreatedAtDesc(Long distributionRequestId);

    List<DistributionOverride> findByBeneficiaryIdOrderByCreatedAtDesc(Long beneficiaryId);
}
