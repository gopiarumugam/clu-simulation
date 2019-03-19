package com.semaifour.facesix.data.site;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface SiteDeviceRepository extends MongoRepository<SiteDevice, String> {
	
	@Query("{sid:?0}")
	public List<SiteDevice> findBySiteId(String siteId);

	public List<SiteDevice> findByPortionId(String portionId);

	public List<SiteDevice> findByDeviceType(String deviceType);

	public List<SiteDevice> findByDeviceState(String deviceState);

	public SiteDevice findByDeviceUid(String deviceUid);
}