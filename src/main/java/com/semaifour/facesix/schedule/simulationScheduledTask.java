package com.semaifour.facesix.schedule;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import com.semaifour.facesix.account.Customer;
import com.semaifour.facesix.account.CustomerService;
import com.semaifour.facesix.beacon.data.Beacon;
import com.semaifour.facesix.beacon.data.BeaconService;
import com.semaifour.facesix.boot.Application;
import com.semaifour.facesix.data.mongo.beacondevice.BeaconDeviceService;
import com.semaifour.facesix.data.site.Portion;
import com.semaifour.facesix.data.site.PortionService;
import com.semaifour.facesix.mqtt.DeviceEventPublisher;
import com.semaifour.facesix.simulatedBeacon.BeaconAssociation;
import com.semaifour.facesix.simulatedBeacon.BeaconAssociationService;
import com.semaifour.facesix.util.CustomerUtils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import scala.concurrent.forkjoin.ForkJoinPool;
import scala.concurrent.forkjoin.RecursiveTask;

@Controller
public class simulationScheduledTask extends RecursiveTask<Integer> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5071601145810944146L;

	@Autowired
	CustomerService customerService;
	
	@Autowired
	PortionService portionService;
	
	@Autowired
	BeaconDeviceService beaconDeviceService;
	
	@Autowired
	BeaconService beaconService;
	
	@Autowired
	DeviceEventPublisher mqttPublisher;
	
	@Autowired
	BeaconAssociationService beaconAssociationService;
	
	@Autowired
	CustomerUtils customerUtils;
	
	
	@Value("${facesix.simulationScheduledTask.enable}")
	private boolean simulation_enable;
	
	public final String classname = simulationScheduledTask.class.getName();
	Logger LOG = LoggerFactory.getLogger(classname);
	public final String simulation = "enable";
	public final String opcode = "current-location-update";
	public final String cx_state = "ACTIVE";
	DateFormat format =	new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	public static int count = 0;
	List<String> solution = Arrays.asList("GatewayFinder", "GeoFinder");
	
	String mqttMsgTemplate = "\"opcode\":\"{0}\", \"uid\":\"{1}\",\"spid\":\"{2}\""
						   + ",\"tag_count\":{3}, \"record_num\":{4},\"max_record\":{5},"
						   + "\"tag_list\":{6},\"server_send_ts\":\"{7}\"";


	ForkJoinPool forkJoinPool = null;
	
	boolean logenabled = false;
	String spid = null;
	String simulateVia = "mqtt";
	int threshold = 50;
	TimeZone timeZone = null;
	
	private void setSpid (String spid) {
		this.spid = spid;
	}
	
	private void setlog (boolean log) {
		this.logenabled = log;
	}
	
	private void setSimulationVia(String simulation){
		this.simulateVia = simulation;
	}
	
	private void setTagThreshold(int threshold){
		this.threshold = threshold;
	}
	
	private void setTimeZone(TimeZone timezone){
		this.timeZone = timezone;
	}
	
	//@Scheduled (fixedDelay=1000)
	public void simulationSchedule() throws InterruptedException {

		if (!simulation_enable) {
			 return;
		}
		
		//List<Customer> customerList = getCustomerService().findBySimulationSolutionAndState(simulation,solution,cx_state);
		List<Customer> customerList = customerService.findOneById("5a65cd7ddb9a525c12dd035e");
		List<String> cidList = new ArrayList<String>();
		Map<String,Boolean> enableLogs = new HashMap<String,Boolean>();
		Map<String,String> simulationVia = new HashMap<String,String>();
		Map<String,Integer> threshold = new HashMap<String,Integer>();
		Map<String,TimeZone> timezone = new HashMap<String,TimeZone>();
		String cid;
		Boolean logs;
		int num_of_forks =0;
		List<simulationScheduledTask> recursiveTasks = new ArrayList<simulationScheduledTask>();
		for (Customer cx : customerList) {
			cid = cx.getId();
			logs = cx.getLogs() == null || cx.getLogs().equals("false") ? false : true;
			cidList.add(cx.getId());
			enableLogs.put(cid, logs);
			simulationVia.put(cid, cx.getSimulationVia());
			threshold.put(cid, Integer.valueOf(cx.getThreshold()));
			timezone.put(cid, customerUtils.FetchTimeZone(cx.getTimezone()));
		}
		
		List<Portion> portionList = getPortionService().findByCids(cidList);
		int i = 0;
		forkJoinPool 	= new ForkJoinPool();
		for (Portion p : portionList) {
			simulationScheduledTask sst = new simulationScheduledTask();
			cid = p.getCid();
			sst.setSpid(p.getId());
			sst.setlog(enableLogs.get(cid));
			sst.setSimulationVia(simulationVia.get(cid));
			sst.setTagThreshold(threshold.get(cid));
			sst.setTimeZone(timezone.get(cid));
			recursiveTasks.add(sst);
			forkJoinPool.execute(sst);
			i++;
		}
		num_of_forks = i;
		i =0;
		do {
			if (recursiveTasks.get(i).isDone() 		          || 
				recursiveTasks.get(i).isCancelled()           ||
				recursiveTasks.get(i).isCompletedAbnormally() || 
				recursiveTasks.get(i).isCompletedNormally()) {
					//getCustomerUtils().logs(enablelog,classname,"Index Range> " + i);
					i++;
				}
			if (i == num_of_forks) {
				//getCustomerUtils().logs(enablelog,classname,"Index reaches Floor Count, Safe Exit" + i);
				break;
			}
		} while (num_of_forks >= i);
		
		forkJoinPool.shutdownNow();
		forkJoinPool.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
	}

	@Override
	protected Integer compute() {
		
		String spid = this.spid;
		boolean logenabled = this.logenabled;
		String simulateVia = this.simulateVia;
		int threshold = this.threshold;
		format.setTimeZone(this.timeZone);
		
		List<BeaconAssociation> associatedBeaconList = getBeaconAssociationService().findBySpid(spid);
		if(associatedBeaconList == null || associatedBeaconList.size()==0){
			return 0;
		}
		
		int tagsInFloor = associatedBeaconList.size();
		int max_record = tagsInFloor/threshold;
		String uid = "E8:C7:4F:08:00:07";
		
		if (tagsInFloor % threshold > 0) {
			max_record++;
		}
		
		int fromIndex = 0, toIndex = 0;
		
		for (int record_num = 1; record_num <= max_record; record_num++) {
			int tag_count = tagsInFloor - threshold > 0 ? threshold : tagsInFloor;
			tagsInFloor -= threshold;
			toIndex += tag_count;
			List<BeaconAssociation> subList = associatedBeaconList.subList(fromIndex, toIndex);
			fromIndex = toIndex;
			JSONArray tag_list = maketagList(subList);
			JSONObject message = new JSONObject();
			message.put("opcode", opcode);
			message.put("uid", uid);
			message.put("spid", spid);
			message.put("tag_count", tag_count);
			message.put("record_num", record_num);
			message.put("max_record", max_record);
			message.put("tag_list", tag_list);
			message.put("server_send_ts", format.format(new Date()));
			//testing
			simulateVia = "mqtt";
			switch (simulateVia) {
			case "mqtt":
				String msg = MessageFormat.format(mqttMsgTemplate,
						new Object[] { opcode, uid, spid, tag_count,
									   record_num, max_record, tag_list,format.format(new Date())
									  });
				getMqttPublisher().publish("{"+msg+"}",spid);
				break;
			case "rest":
				String inputLine = null;
				byte[] postData = message.toString().getBytes(StandardCharsets.UTF_8);
				int postDataLength = message.toString().length();

				String url = "http://cloud.qubercomm.com/facesix/rest/locatum/test/clu";

				URL obj;
				try {
					obj = new URL(url);
					HttpURLConnection con = (HttpURLConnection) obj.openConnection();
					con.setDoOutput( true );
					con.setInstanceFollowRedirects(false);
					con.setRequestMethod("POST");
					con.setRequestProperty("Content-Type", "text/plain");
					con.setRequestProperty( "charset", "utf-8");
					con.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));
					con.setUseCaches( false );
					con.getOutputStream().write(postData);
				   
					InputStream is   = con.getInputStream();
					
					if (is != null) {
						BufferedReader in 	= new BufferedReader( new InputStreamReader(is));
						inputLine 	= in.readLine();
						in.close();							
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			default:
				break;
			}
		}
		return 0;
	}
	
	private JSONArray maketagList(List<BeaconAssociation> subList) {
		JSONArray tag_list = new JSONArray();
		Random rand = new Random();
		for (BeaconAssociation b : subList) {

			JSONObject tagDetail = new JSONObject();
			JSONObject coordinate = new JSONObject();
			JSONObject receiverDetail = new JSONObject();
			JSONArray receiver_list = new JSONArray();

			int rssi = -59;
			double distance = 24.831335067749023;
			double accuracy = 0.25;
			double range = 24.831335067749023;

			String uid = b.getMacaddr();
			if(uid.equals("AC:23:3E:01:00:03")) {
				Beacon beacon = getBeaconService().findOneByMacaddr("AC:23:3E:01:00:03");
				if(System.currentTimeMillis() - beacon.getLastactive() < 180000) {
					continue;
				}
			}
			String dev_uid = b.getUid();
			double lat = Double.valueOf(b.getLat());
			double lon = Double.valueOf(b.getLon());

			/*if (rand.nextInt() % 2 == 0) {
				lat = lat + (rand.nextDouble() * 0.000001);
				lon = lon - (rand.nextDouble() * 0.000001);
			} else {
				lat = lat - (rand.nextDouble() * 0.000001);
				lon = lon + (rand.nextDouble() * 0.000001);
			}*/

			coordinate.put("latitude", lat);
			coordinate.put("longitude", lon);

			receiverDetail.put("uid", dev_uid);
			receiverDetail.put("rssi", rssi);
			receiverDetail.put("distance", distance);
			receiver_list.add(receiverDetail);

			tagDetail.put("uid", uid);
			tagDetail.put("coordinate", coordinate);
			tagDetail.put("accuracy", accuracy);
			tagDetail.put("range", range);
			tagDetail.put("sos_alert", 1);
			tagDetail.put("receiver_list", receiver_list);

			tag_list.add(tagDetail);
		}
		return tag_list;
	}

	public CustomerService getCustomerService() {
		if(customerService == null){
			customerService = Application.context.getBean(CustomerService.class);
		}
		return customerService;
	}
	public PortionService getPortionService() {
		if(portionService == null){
			portionService = Application.context.getBean(PortionService.class);
		}
		return portionService;
	}

	public BeaconDeviceService getBeaconDeviceService() {
		if (beaconDeviceService == null) {
			beaconDeviceService = Application.context.getBean(BeaconDeviceService.class);
		}
		return beaconDeviceService;
	}

	public BeaconService getBeaconService() {
		if (beaconService == null) {
			beaconService = Application.context.getBean(BeaconService.class);
		}
		return beaconService;
	}
	
	public DeviceEventPublisher getMqttPublisher() {
		if (mqttPublisher == null) {
			mqttPublisher = Application.context.getBean(DeviceEventPublisher.class);
		}
		return mqttPublisher;
	}
	
	public BeaconAssociationService getBeaconAssociationService() {
		if (beaconAssociationService == null) {
			beaconAssociationService = Application.context.getBean(BeaconAssociationService.class);
		}
		return beaconAssociationService;
	}
	
	public CustomerUtils getCustomerUtils() {
		if (customerUtils == null) {
			customerUtils = Application.context.getBean(CustomerUtils.class);
		}
		return customerUtils;
	}
}
