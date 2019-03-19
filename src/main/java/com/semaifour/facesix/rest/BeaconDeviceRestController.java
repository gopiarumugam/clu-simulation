package com.semaifour.facesix.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.semaifour.facesix.web.WebController;

@RestController
@RequestMapping("/rest/beacon/device")
public class BeaconDeviceRestController extends WebController {


	static Logger LOG = LoggerFactory.getLogger(BeaconDeviceRestController.class.getName());

	
}
