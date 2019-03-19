package com.semaifour.facesix.simulatedBeacon;

import java.util.List;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BeaconAssociationService {
	private static String classname = BeaconAssociationService.class.getName();

	Logger LOG = LoggerFactory.getLogger(classname);

	@Autowired(required = false)
	private BeaconAssociationRepository repository;
	
	public void save(BeaconAssociation associatedBeacon) {
		repository.save(associatedBeacon);
	}
	
	public void delete(BeaconAssociation associatedBeacon) {
		repository.delete(associatedBeacon);
	}
	
	public void deleteAssociatedList(List<BeaconAssociation> associatedBeacon) {
		repository.delete(associatedBeacon);
	}

	public BeaconAssociation findByCidAndMacaddr(String cid,String macaddr) {
		return repository.findByCidAndMacaddr(cid,macaddr);
	}

	public List<BeaconAssociation> findByCid(String cid) {
		return repository.findByCid(cid);
	}
	public List<BeaconAssociation> findBySid(String sid) {
		return repository.findBySpid(sid);
	}
	public List<BeaconAssociation> findBySpid(String spid) {
		return repository.findBySpid(spid);
	}
	public List<BeaconAssociation> findByUid(String uid) {
		return repository.findByUid(QueryParser.escape(uid));
	}
}
