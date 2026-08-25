package com.ukbank.fraudplatform.repository;

import com.ukbank.fraudplatform.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    Optional<Device> findByCustomerIdAndDeviceFingerprint(UUID customerId, String deviceFingerprint);
}
