
package com.semaifour.facesix.data.mongo.beacondevice;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.semaifour.facesix.beacon.data.BeaconService;
import com.semaifour.facesix.data.site.Portion;
import com.semaifour.facesix.data.site.PortionService;
import com.semaifour.facesix.rest.GeoFinderRestController;

@Service
public class BeaconDeviceService {

	static Logger LOG = LoggerFactory.getLogger(BeaconDeviceService.class.getName());

	@Autowired
	private BeaconDeviceRepository repository;

	@Autowired
	BeaconDeviceService beaconDeviceService;
	
	@Autowired
	GeoFinderRestController geoFinderRestController;

	String mqttMsgTemplate = " \"opcode\":\"{0}\", \"uid\":\"{1}\",\"type\":\"{2}\",\"cid\":\"{3}\","
			+ " \"sid\":\"{4}\",\"spid\":\"{5}\",\"ip\":\"{6}\", \"serverip\":\"{7}\", "
			+ " \"solution\":\"{8}\",\"tagthreshold\":\"{9}\", \"conf\":{10}, \"tunnelip\":\"{11}\"";

	@Autowired
	PortionService portionService;

	public BeaconDeviceService() {
	}

	public Page<BeaconDevice> findAll(Pageable pageable) {
		return repository.findAll(pageable);
	}

	public List<BeaconDevice> findByName(String name) {
		return repository.findByName(name);
	}

	public List<BeaconDevice> findByUid(String uid) {
		return repository.findByUid(uid);
	}

	public BeaconDevice findOneByName(String name) {
		List<BeaconDevice> list = findByName(name);
		if (list != null & list.size() > 0)
			return list.get(0);
		return null;
	}

	public BeaconDevice findOneByUid(String uid) {
		List<BeaconDevice> list = findByUid(uid);
		if (list != null & list.size() > 0) {
			BeaconDevice bdev = list.get(0);
			if (uid.equalsIgnoreCase(bdev.getUid())) {
				return bdev;
			}

		}
		return null;
	}

	public BeaconDevice findByUidAndCid(String uid, String cid) {
		List<BeaconDevice> devicelist = repository.findByUidAndCid(QueryParser.escape(uid), cid);
		if (devicelist != null && devicelist.size() > 0) {
			BeaconDevice device = devicelist.get(0);
			if (device.getUid().equalsIgnoreCase(uid)) {
				return device;
			}
		}
		return null;
	}

	public BeaconDevice findOneByUuid(String uid) {
		return repository.findByUuid(uid);
	}

	public BeaconDevice findById(String id) {
		return repository.findOne(id);
	}

	public boolean exists(String id) {
		return repository.exists(id);
	}

	public boolean exists(String uid, String name) {
		if (findOneByUid(uid) != null)
			return true;
		if (findOneByName(name) != null)
			return true;
		return false;
	}

	public void deleteAll() {
		LOG.info(" Beacon Device deleted All successfully :");
		repository.deleteAll();
	}

	public void delete(String id) {
		LOG.info(" Beacon Device deleted successfully :" + id);
		repository.delete(id);
	}

	public void delete(BeaconDevice device) {
		LOG.info(" Beacon Device deleted successfully :" + device.getId());
		repository.delete(device);
	}

	public long count() {
		return repository.count();
	}

	public List<BeaconDevice> findBySid(String sid) {
		return repository.findBySid(sid);
	}

	public List<BeaconDevice> findBySpid(String spid) {
		return repository.findBySpid(spid);
	}

	public List<BeaconDevice> findByCid(String cid) {
		return repository.findByCid(cid);
	}

	public BeaconDevice save(BeaconDevice device) {
		return save(device, true);
	}

	public BeaconDevice save(BeaconDevice device, boolean notify) {
		device = repository.save(device);
		if (device.getPkid() == null) {
			device.setPkid(device.getId());
		}
		device = repository.save(device);
		
	    LOG.info(" Beacon Device saved successfully :" + device.getId());
		return device;
	}

	

	public List<BeaconDevice> findByCidAndType(String cid, String deviceType) {
		return repository.findByCidAndType(cid, deviceType);
	}

	

	public BeaconDevice findByUidAndCidAndType(String uid, String cid, String deviceType) {
		return repository.findByUidAndCidAndType(uid, cid, deviceType);
	}
}
