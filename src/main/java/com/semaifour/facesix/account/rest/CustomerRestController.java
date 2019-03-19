package com.semaifour.facesix.account.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.semaifour.facesix.account.Customer;
import com.semaifour.facesix.account.CustomerService;
import com.semaifour.facesix.account.Privilege;
import com.semaifour.facesix.account.PrivilegeService;
import com.semaifour.facesix.web.WebController;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

@RestController
@RequestMapping("/rest/customer")
public class CustomerRestController extends WebController {

	Logger LOG = LoggerFactory.getLogger(CustomerRestController.class.getName());

	@Autowired
	CustomerService customerService;
	
	@Autowired
	PrivilegeService privilegeService;

	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public @ResponseBody JSONObject listData(HttpServletRequest request, HttpServletResponse response) {

		JSONObject jsonList = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		
		try {

			boolean privFlag = privilegeService.hasPrivilege(request.getSession().getId(), Privilege.CUST_WRITE);
			
			String simulation 	= "true";
			String venueType 	= "Locatum";
			String status 		= "ACTIVE";
			
			if (privFlag) {
				Iterable<Customer>  customerList = customerService.findBySimulationCustomerList(simulation, venueType, status);
				if (customerList != null) {
					for (Customer customer : customerList) {
						
						LOG.info("simulation Status " +simulation + " venueType " +venueType + " status " +status);

						simulation = (simulation == null) ? "false" : simulation;
						
						if (simulation.equals("true") && "Locatum".equals(venueType)) {
							JSONObject json = new JSONObject();
							json.put("id", 			 customer.getId());
							json.put("customerName", customer.getCustomerName());
							jsonArray.add(json);
						}
					}
				}
			}
			jsonList.put("customer", jsonArray);
		} catch (Exception e) {
			e.printStackTrace();
			LOG.error("Customer details getting error ", e);
		}
		return jsonList;
	}

}