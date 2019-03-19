package com.semaifour.facesix.rest;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.semaifour.facesix.account.Customer;
import com.semaifour.facesix.account.CustomerService;
import com.semaifour.facesix.beacon.data.Beacon;
import com.semaifour.facesix.beacon.data.BeaconService;
import com.semaifour.facesix.data.mongo.beacondevice.BeaconDevice;
import com.semaifour.facesix.data.mongo.beacondevice.BeaconDeviceService;
import com.semaifour.facesix.data.site.Portion;
import com.semaifour.facesix.data.site.PortionService;
import com.semaifour.facesix.simulatedBeacon.BeaconAssociation;
import com.semaifour.facesix.simulatedBeacon.BeaconAssociationService;
import com.semaifour.facesix.util.CustomerUtils;
import com.semaifour.facesix.web.WebController;

import net.sf.json.JSONObject;

@RestController
@RequestMapping("/rest/simulation")
public class SimulationRestController extends WebController {

	static Logger LOG = LoggerFactory.getLogger(SimulationRestController.class.getName());
	
	@Autowired
	private BeaconDeviceService beaconDeviceService;
	
	@Autowired
	private CustomerUtils customerUtils;
	
	@Autowired
	private BeaconAssociationService beaconAssociationService;
	
	@Autowired
	private CustomerService customerService;

	@Autowired
	private BeaconService beaconService;
	
	@Autowired
	private PortionService portionService;
	
	@Autowired
	private GeoFinderRestController geoFinderRestController;

	DecimalFormat dformat 		   = new DecimalFormat("00000000");
	final static String parent     = "ble";
	final static String macDef 	   = "TEST";
	final static String tagType    = "Female";
	final static String tagModel   = "neck";
	final static String refTxpwr   = "-59";
	final static String scannerUid = "00:00:00:00:00:00";
	final static String typefs = "server";
	final static String status = "inactive";
	final static String deviceType = "receiver";
	final static String tagStatus  = "checkedout";
	
	final static String device_default_config = "{\"attributes\":[{\"type\":\"receiver\",\"proximity\":\"2.0\","
			+ "\"scanduration\":\"5.0\",\"ssid\":\"Locatum\",\"encryption\":\"wpa2-psk\","
			+ "\"key\":\"sudavasu\",\"batteryinterval\":\"12\",\"statusinterval\":\"15\","
			+ "\"tluinterval\":\"1\",\"loglevel\":\"all\",\"configuration\":\"trilateration\","
			+ "\"keepaliveinterval\":\"10\"},{\"diag_key\":\"\",\"diag_value\":\"\"}]}";
	
	@RequestMapping(value = "/simulateTagDetails", method = RequestMethod.GET)
	public boolean simulateTagDetails(
			@RequestParam("cid") String cid,
			@RequestParam("simulateVia") String simulateVia,
			@RequestParam("tagCount") int tagCount,
			@RequestParam("deviceCount") int deviceCount,
			@RequestParam("maxCount") int maxCount) {
		
		boolean result = true;

		try {
			
			Customer customer = customerService.findById(cid);

			if (customer != null) {
				this.simulateTags(cid, tagCount);
				this.simulateDevices(cid, deviceCount);
				this.associationTagsForCustomer(customer);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			result = false;
		}
		LOG.info("simulation is enabled for cid " + cid);

		return result;
	}
	
	/**
	 * Used to generate tags
	 * @param cid
	 * @param tagCount
	 * @return
	 */
	
	public List<Beacon> simulateTags(String cid, int tagCount) {
		
		List<Beacon> stimulatedBeacons = new ArrayList<Beacon>();
		
		Random rand = new Random(); 
		
		Beacon addBeacon = null;
		
		List<Beacon> alreadyAvailableTags = (List<Beacon>) beaconService.getSavedBeaconByCidAndStatus(cid, tagStatus);
		int availableTagCount = alreadyAvailableTags.size();
		
		if(availableTagCount >= tagCount) {
			int removeBeacon = availableTagCount - tagCount;
			if(removeBeacon != 0){
				List<Beacon> deleteBeacons = alreadyAvailableTags.subList(removeBeacon, availableTagCount);
				beaconService.delete(deleteBeacons);
			}
			stimulatedBeacons  = alreadyAvailableTags.subList(0, tagCount);
			return stimulatedBeacons;
		} else {
			stimulatedBeacons.addAll(alreadyAvailableTags);
			tagCount -= availableTagCount;
		}
		
		for (int i = 0; i < tagCount; i++) {
			
			String mac = generateMacAddress(macDef,rand,dformat);
			
			String assignedTo = mac.replaceAll(":", "").toUpperCase();
			
			addBeacon = beaconService.checkout(mac, assignedTo, tagType, cid,
									"qubertag", "1000", "4",tagModel, refTxpwr, scannerUid, "simulatedTag", null);
			
			stimulatedBeacons.add(addBeacon);
		}
		return stimulatedBeacons;
	}

	/**
	 * Used to generate devices
	 * @param cid
	 * @param deviceCount
	 * @return
	 */
	
	public List<BeaconDevice> simulateDevices(String cid, int deviceCount) {
		
		List<BeaconDevice> simulatedDevices = new ArrayList<BeaconDevice>();
		Random rand = new Random();
		
		List<BeaconDevice> availableDevices = beaconDeviceService.findByCidAndType(cid,deviceType);
		
		if (availableDevices != null) {
			
			int availDeviceCount = availableDevices.size();
			
			if (availDeviceCount > deviceCount) {
				
				int deleteDevices = availDeviceCount - deviceCount;
				
				List<BeaconDevice> deleteSublist = availableDevices.subList(0, deleteDevices);
				for (BeaconDevice beaconDevice : deleteSublist) {
					beaconDeviceService.delete(beaconDevice);
				}
				
			} else if (availDeviceCount < deviceCount) {
				deviceCount -= availDeviceCount;
			} else {
				for (BeaconDevice device : availableDevices) {
					simulatedDevices.add(device);
				}
			}
		}

		List<Portion> availableFloors = portionService.findByCid(cid);
		
		if (availableFloors != null && !availableFloors.isEmpty()) {
		
			int floorCount 		   = availableFloors.size();
			int devicesInEachFloor = deviceCount / floorCount;
			
			for (int i = 0; i < floorCount; i++) {
				
				Portion portion 	= availableFloors.get(i);
				
				if (portion != null) {
				
					String spid = portion.getId();
					String sid 	= portion.getSiteId();
					int width 	= portion.getWidth();
					int height 	= portion.getHeight();
					
					int deviceInThisFloor = devicesInEachFloor;
					if (i == floorCount - 1) {
						deviceInThisFloor = deviceCount;
					}
					deviceCount -= deviceInThisFloor;
					if (deviceInThisFloor > 0) {
						simulatedDevices.addAll(createDevices(cid, sid, spid, deviceInThisFloor, width, height, rand));
					}
				}
				
			}
		}
		return simulatedDevices;
	}

	public List<BeaconDevice> createDevices(String cid,String sid,String spid, int deviceCount, int width, int height, Random rand) {

		DecimalFormat dformat = new DecimalFormat("000000000000");
		
		List<BeaconDevice> createdDevices = new ArrayList<BeaconDevice>();
		
		for (int i = 0; i < deviceCount; i++) {
			
			int x = rand.nextInt(width);
			int y = rand.nextInt(height);
			
			String uid 	 = this.generateMacAddress("", rand, dformat);
			String alias = uid.replaceAll(":", "");
			
			
			BeaconDevice beacondevice = beaconDeviceService.findOneByUid(uid);
			
			if (beacondevice == null) {
				
				beacondevice = new BeaconDevice();
				
				beacondevice.setCreatedBy("simulation");
				beacondevice.setUid(uid);
				beacondevice.setCid(cid);
				beacondevice.setSid(sid);
				beacondevice.setSpid(spid);
				beacondevice.setName(alias);
				beacondevice.setTypefs(typefs);
				beacondevice.setStatus(BeaconDevice.STATUS.CONFIGURED.name());
				beacondevice.setState(BeaconDevice.STATE.inactive.name());
				beacondevice.setTemplate(device_default_config);
				beacondevice.setConf(device_default_config);
				beacondevice.setModifiedBy("simulation");
				beacondevice.setType(deviceType);
				beacondevice.setIp("0.0.0.0");
				beacondevice.setKeepAliveInterval("10");
				beacondevice.setTlu(1);
				beacondevice.setXposition(String.valueOf(x));
				beacondevice.setYposition(String.valueOf(y));
				beacondevice.setParent(parent);
				beacondevice.setSource("qubercomm");
				
				beacondevice = beaconDeviceService.save(beacondevice, false);
				
				geoFinderRestController.Pixel2Coordinate(createdDevices,spid, uid, String.valueOf(x), String.valueOf(y));
				
			} else {
				LOG.info("Duplicate simulation uid " +uid);
			}
			
			
		}
		return createdDevices;
	}
	
	public void associationTagsForCustomer(Customer customer) {
		
		try {
			
			Random rand = new Random();
			
			final String cid  = customer.getId();

			/*
			 *  For customer demo 
			 *  
			 * List<String> macaddrs = Arrays.asList("TE:ST:00:86:86:71","TE:ST:00:22:37:78","TE:ST:00:64:89:98");
			// beaconList = beaconService.findByMacaddrs(macaddrs);
			 * 
			 */
			
			Collection<Beacon> beaconList = beaconService.getSavedBeaconByCidAndStatus(cid, tagStatus);
			
			
			List<BeaconAssociation> oldData = beaconAssociationService.findByCid(cid);
			if (oldData != null && oldData.size() > 0) {
				beaconAssociationService.deleteAssociatedList(oldData);
			}
			
			final List<BeaconDevice> deviceList = beaconDeviceService.findByCidAndType(cid, deviceType);
			
			if (beaconList == null || beaconList.isEmpty() || deviceList == null || deviceList.isEmpty()) {
				return;
			}

			final int num_of_devices = deviceList.size();
			
			beaconList.parallelStream().forEach(beacon -> {
				
				try {
				
					JSONParser parse = new JSONParser();

					String macaddr = beacon.getMacaddr();
					
					String lat = null;
					String lon = null;
					String sid = null;
					String spid = null;
					String devId = null;

					BeaconAssociation associatedBeacon = beaconAssociationService.findByCidAndMacaddr(cid, macaddr);

					if (associatedBeacon == null || !associatedBeacon.getMacaddr().equals("TE:ST:00:86:86:71")) {
						
						if(associatedBeacon == null) {
							associatedBeacon = new BeaconAssociation();
							associatedBeacon.setMacaddr(macaddr);
						}
						
						List<String> latLong = associatedBeacon.getLatlonList();
						
						if(latLong == null || latLong.isEmpty()) {
							
							double fromX = 0.0d;
							double toX = 0.0d;
							double fromY = 0.0d;
							double toY = 0.0d;

							String from_dev;
							String to_dev;
							String fromUid;
							String toUid;
							
							boolean first = false;
							
							List<String> latLonList = new ArrayList<String>();
							
							if(associatedBeacon.getTo_dev() == null || associatedBeacon.getTo_dev().isEmpty()) {
								first = true;
								// set new from device
								int index = rand.nextInt(num_of_devices);
								BeaconDevice chosenDevice = deviceList.get(index);
								String pixelResult = chosenDevice.getPixelresult();
								devId = chosenDevice.getUid();
								
								if (pixelResult != null) {
									org.json.simple.JSONObject result = (org.json.simple.JSONObject) parse.parse(pixelResult);
									org.json.simple.JSONArray latAndLon = (org.json.simple.JSONArray) result.get("result");

									result = (org.json.simple.JSONObject) latAndLon.get(0);
									lat = (String) result.get("latitude");
									lon = (String) result.get("longitude");
									
									fromX = Double.valueOf(result.get("x").toString()); 
									fromY = Double.valueOf(result.get("y").toString());
								}
								
								
								fromUid = devId;
								from_dev = "{\"sid\":\""+sid+"\",\"spid\":\""+spid+"\",\"uid\":\""+fromUid+"\""
										+ ",\"lat\":"+lat+",\"lon\":"+lon+",\"x\":"+fromX+",\"y\":"+fromY+"}";
								
							} else {
								// from device = to device
								from_dev = associatedBeacon.getTo_dev();
								JSONParser parser = new JSONParser();
								org.json.simple.JSONObject json = (org.json.simple.JSONObject) parser.parse(from_dev);
								fromX = Double.valueOf(json.get("x").toString());
								fromY = Double.valueOf(json.get("y").toString());
								fromUid = json.get("uid").toString();
							}
							
							associatedBeacon.setFrom_dev(from_dev);
							// select new to device
							BeaconDevice chosenDevice = null;
							int index = 0;
							do {
								index = rand.nextInt(num_of_devices);
								chosenDevice = deviceList.get(index);
							} while(chosenDevice.getUid().equals(fromUid));
							
							String pixelResult = chosenDevice.getPixelresult();
							devId = chosenDevice.getUid();
							sid = chosenDevice.getSid();
							spid = chosenDevice.getSpid();
							
							String tolat = "0.000000";
							String tolon = "0.000000";
							
							if (pixelResult != null) {
								org.json.simple.JSONObject result = (org.json.simple.JSONObject) parse.parse(pixelResult);
								org.json.simple.JSONArray latAndLon = (org.json.simple.JSONArray) result.get("result");

								result = (org.json.simple.JSONObject) latAndLon.get(0);
								 tolat = (String) result.get("latitude");
								 tolon = (String) result.get("longitude");
								
								toX = Double.valueOf(result.get("x").toString()); 
								toY = Double.valueOf(result.get("y").toString());
							}
							
							toUid = devId;
							to_dev = "{\"sid\":\""+sid+"\",\"spid\":\""+spid+"\",\"uid\":\""+toUid+"\""
									+ ",\"lat\":"+tolat+",\"lon\":"+tolon+",\"x\":"+toX+",\"y\":"+toY+"}";
							associatedBeacon.setTo_dev(to_dev);
							
							// get midpoint of it
							double midptx = (fromX + toX) / 2;
							double midpty = (fromY + toY) / 2;
							JSONObject coord = geoFinderRestController.simulatePixels2Coordinate(spid, String.valueOf(midptx), String.valueOf(midpty));
							
							if (coord != null) {
								String midlat = coord.getString("latitude");
								String midlon = coord.getString("longitude");
								if(!first) {
									lat = midlat;
									lon = midlon;
									devId = associatedBeacon.getUid();
									sid = associatedBeacon.getSid();
									spid = associatedBeacon.getSpid();
									latLonList = Arrays.asList(tolat+","+tolon);
								} else {
									latLonList = Arrays.asList(midlat+","+midlon,tolat+","+tolon);
								}
								associatedBeacon.setLatlonList(latLonList);
							}
							
						} else {
							// use exiting values , remove points from the list
							String todev = associatedBeacon.getTo_dev();
							JSONParser parser = new JSONParser();
							if (todev != null) {
								org.json.simple.JSONObject json = (org.json.simple.JSONObject) parser.parse(todev);
								List<String> latLonList = associatedBeacon.getLatlonList();
								String latlon = latLonList.remove(0);
								associatedBeacon.setLatlonList(latLonList);
								String[] latLon = latlon.split(",");
								lat = latLon[0];
								lon = latLon[1];
								
								String jsonlat = json.get("lat").toString();
								String jsonlon = json.get("lon").toString();
								if(jsonlat.equals(lat) && jsonlon.equals(lon)) {
									devId = json.get("uid").toString();
									sid = json.get("sid").toString();
									spid = json.get("spid").toString();
								} else {
									devId = associatedBeacon.getUid();
									sid = associatedBeacon.getSid();
									spid = associatedBeacon.getSpid();
								}
							}
							
							
						}
						
					}  else {
						
						devId = associatedBeacon.getUid();
						sid = associatedBeacon.getSid();
						spid = associatedBeacon.getSpid();
						int height = associatedBeacon.getHeight();
						int width = associatedBeacon.getWidth();
						if(height == 0 && width == 0) {
							Portion p = portionService.findById(spid);
							height = p.getHeight();
							width = p.getWidth();
							associatedBeacon.setHeight(height);
							associatedBeacon.setWidth(width);
						}
						String x = associatedBeacon.getX();
						String y = associatedBeacon.getY();
						
						if (x == null || y == null) {
							x = "0";
							y = "0";
						} else {
							int xpos = Integer.valueOf(x);
							int ypos = Integer.valueOf(y);
							if (xpos < (width - 40)) {
								xpos += 40;
							} else if (xpos <= width && ypos < (height - 40)) {
								ypos += 40;
							} else {
								xpos = 0;
								ypos = 0;
							}
							x = String.valueOf(xpos);
							y = String.valueOf(ypos);
						}
						
						JSONObject result = geoFinderRestController.simulatePixels2Coordinate(spid, x, y);
						
						if(result != null && !result.isEmpty()) {
							lat = result.getString("latitude");
							lon = result.getString("longitude");
						} else  {
							lat = associatedBeacon.getLat();
							lon = associatedBeacon.getLon();
						}
						associatedBeacon.setX(x);
						associatedBeacon.setY(y);
					}

					associatedBeacon.setCid(cid);
					associatedBeacon.setSid(sid);
					associatedBeacon.setSpid(spid);
					associatedBeacon.setLat(lat);
					associatedBeacon.setUid(devId);
					associatedBeacon.setLon(lon);
					
					beaconAssociationService.save(associatedBeacon);
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	public String generateMacAddress(String macDef, Random rand, DecimalFormat dformat) {
		
		int randomNumber = rand.nextInt(999999);
		String mac 		 = dformat.format(randomNumber);

		mac = macDef + mac;
		
		mac = mac.substring(0, 2) + ":" + mac.substring(2, 4) + ":" + mac.substring(4, 6)+":" 
			+ mac.substring(6, 8) + ":" + mac.substring(8, 10) + ":" + mac.substring(10, 12);
		
		Beacon beacon = beaconService.findOneByMacaddr(mac);
		
		if(beacon == null){
			return mac;
		} else {
			return generateMacAddress(macDef, rand, dformat);
		}
	}
}
