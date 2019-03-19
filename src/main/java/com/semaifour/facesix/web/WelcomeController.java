package com.semaifour.facesix.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.graylog2.rest.models.system.sessions.requests.SessionCreateRequest;
import org.graylog2.rest.models.system.sessions.responses.SessionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.itextpdf.text.Font;
import com.semaifour.facesix.account.Customer;
import com.semaifour.facesix.account.CustomerService;
import com.semaifour.facesix.data.account.UserAccount;
import com.semaifour.facesix.data.account.UserAccountService;
import com.semaifour.facesix.data.graylog.GraylogRestClient;
import com.semaifour.facesix.data.site.Site;
import com.semaifour.facesix.data.site.SiteService;
import com.semaifour.facesix.domain.Message;
import com.semaifour.facesix.session.Contactus;
import com.semaifour.facesix.util.SessionUtil;

/**
 * 
 * Welcome Controller for the webapp
 * 
 * @author mjs
 *
 */
@Controller
public class WelcomeController extends WebController {

	static Font smallBold = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);

	static Logger LOG = LoggerFactory.getLogger(WelcomeController.class.getName());

	@Autowired
	com.semaifour.facesix.util.CustomerUtils CustomerUtils;

	@Autowired
	UserAccountService userAccountService;

	@Autowired
	CustomerService customerService;

	@Autowired
	ResourceLoader resourceLoader;

	@Autowired
	AccountWebController accountWebController;
	
	@Autowired
	SiteService service;

	@Value("${facesix.cloud.version}")
	private String cloud_version;

	@RequestMapping(value = { "", "/" })
	public String root(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) {
		if (SessionUtil.isAuthorized(request.getSession())) {
			String c = _CCC.properties.getProperty("facesix.path2home");
			if (c != null) {
				try {
					response.sendRedirect(c);
				} catch (Exception e) {
					LOG.warn("Failed to redirect to path2home :" + c);
					return _CCC.pages.getPage("facesix.home", "home");
				}
			} else {
				return _CCC.pages.getPage("facesix.home", "home");
			}
		} else {
			HttpSession session = request.getSession();
			String message = (String) session.getAttribute("message");
			if (message != null && !message.isEmpty()) {
				model.put("messages", message);
			}
			session.invalidate();
			return _CCC.pages.getPage("facesix.login", "login");
		}
		return _CCC.pages.getPage("facesix.home", "home");
	}

	public String error(Map<String, Object> model) {
		return _CCC.pages.getPage("facesix.error", "error");
	}



	@RequestMapping("/forgot")
	public String fotgot(Map<String, Object> model, @RequestParam(value = "u", required = false) String user,
			@RequestParam(value = "p", required = false) String pwd,
			@RequestParam(value = "re", required = false) String rem,
			@RequestParam(value = "fgt", required = false) String fgt) {

		Contactus contact = new Contactus();
		model.put("fsobject", contact);
		model.put("Contactus", TAB_HIGHLIGHTER);
		return _CCC.pages.getPage("facesix.forgot", "forgot");
	}

	@RequestMapping("/reset")
	public String reset(Map<String, Object> model, @RequestParam(value = "id", required = true) String id,
			@RequestParam(value = "token", required = true) String token) {

		UserAccount user = userAccountService.findById(id);

		String message = "Invalid User";
		Date now = new Date();

		boolean restStatus = user.getResetStatus();

		if (restStatus) {
			message = "Link has either expired or it has been already used";
		}

		boolean notExpried = withinTimeLimit(user.getResetTime(), now.getTime());

		if (user != null && restStatus && notExpried) {
			Contactus contact = new Contactus();
			model.put("fsobject", contact);
			model.put("uid", id);
			model.put("token", token);
			return _CCC.pages.getPage("facesix.reset", "reset");
		} else {
			model.put("reason", message);
			return _CCC.pages.getPage("facesix.notFound", "notFound");
		}
	}

	@RequestMapping(value = "/welcome", method = RequestMethod.GET)
	public String welcome(Map<String, Object> model, @RequestParam(value = "u", required = false) String user,
			@RequestParam(value = "p", required = false) String pwd,
			@RequestParam(value = "re", required = false) String rem,
			@RequestParam(value = "fgt", required = false) String fgt,
			@RequestParam(value = "url", required = false) String pref_url, HttpServletRequest request,
			HttpServletResponse response) {

		if (SessionUtil.isAuthorized(request.getSession())) {
			// return resolveWelcomePage(request, response);
			return _CCC.pages.getPage("facesix.home", "home");
		} else {
			if (!StringUtils.isEmpty(user) && !StringUtils.isEmpty(pwd)) {
				try {
					if (login(request, response, user, pwd)) {

						if (rem != null && rem.equals("on")) {
							Cookie c = new Cookie("userid", request.getSession().getId());
							c.setDomain(request.getServerName());
							c.setPath(request.getContextPath());
							c.setMaxAge(365 * 24 * 60 * 60);
							response.addCookie(c);
						}
						UserAccount account = userAccountService.findOneByUid(user);
						Boolean flag = true;
						Boolean customizedLogin = true;
						if (account != null) {
							Customer cust = customerService.findById(account.getCustomerId());
							if (cust != null) {
								if (account.getVersion() == null && (pref_url != null && !pref_url.isEmpty())) {
									if (!pref_url.equalsIgnoreCase(cust.getCustomerName())) {
										customizedLogin = false;
									}
								}
								LOG.info("cust.getStatus is :" + cust.getStatus() + " account.getStatus is : "
										+ account.getStatus());
								if (cust.getStatus().equalsIgnoreCase("inactive")
										|| account.getStatus().equalsIgnoreCase("inactive")) {
									// if customer or user is inactive then
									// redirect to login page.
									model.put("messages", "Your account is not active at present");
									request.getSession().invalidate();
									flag = false;

								}

								if (pref_url != null && !pref_url.isEmpty()) {

									if (cust.getVersion() != null && !cust.getPreferedUrlName().equals(pref_url)) {
										// new customer using wrong url
										LOG.info(" url is not matching .....");
										LOG.info(" cust.getPreferedUrlName() " + cust.getPreferedUrlName()
												+ " pref_url " + pref_url);
										model.put("messages", "You are not allowed to access this link");
										request.getSession().invalidate();
										flag = false;

									} else if (!cust.getPreferedUrlName().equals(pref_url)) {
										// any existing customer using wrong url
										LOG.info(" not authorized to use customized login");
										model.put("messages", "You are not allowed to access this link");
										request.getSession().invalidate();
										flag = false;
									}
								}
							}

							if (account.getRole().equals("superadmin") && pref_url != null && !pref_url.isEmpty()) {
								if (customizedLogin) {
									LOG.info(" You are superadmin and pref url is " + pref_url);
									model.put("messages", "You are not allowed to access this link");
									request.getSession().invalidate();
									flag = false;
								}
							}

							if (!account.getRole().equals("superadmin") && (pref_url == null || pref_url.isEmpty())) {
								if (customizedLogin) {
									LOG.info("You are not superadmin and you are using superadmin portal!!!!!!!");
									model.put("messages", "You are not allowed to access this link");
									request.getSession().invalidate();
									flag = false;
								}
							}
						}
						if (flag) {
							LOG.info("User successfully logged in with id :" + user);
							account.setCount(account.getCount() + 1);
							userAccountService.saveContact(account);
							if (request.getSession().getAttribute("ORG_REQUEST_URI") != null) {
								String uri = (String) request.getSession().getAttribute("ORG_REQUEST_URI");
								request.getSession().removeAttribute("ORG_REQUEST_URI");
								try {
									response.sendRedirect(uri);
								} catch (IOException e) {
									return _CCC.pages.getPage("facesix.home", "home");
								}
							}
						}

					} else {
						model.put("messages", "Invalid username or password, please try again.");
						request.getSession().invalidate();
					}
				} catch (IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}

			if (pref_url != null && !pref_url.isEmpty()) {
				Customer cx = customerService.findByPreferredUrl(pref_url);
				if (cx != null) {
					model.put("customer", cx);
				}
			}
			return _CCC.pages.getPage("facesix.login", "login");
		}
	}

	public boolean login(HttpServletRequest request, HttpServletResponse response, String user, String pwd)
			throws IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
		return slogin(user, pwd) || glogin(user, pwd, request, response);
	}

	public boolean slogin(String user, String pwd)
			throws IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
		if (_CCC.properties.isconfig("facesix.admin.user", user)
				&& _CCC.properties.isconfig("facesix.admin.secret", _CCC.cryptor.encrypt(pwd))) {
			return true;
		} else {
			return false;
		}
	}

	public boolean glogin(String user, String pwd, HttpServletRequest request, HttpServletResponse response) {
		try {
			GraylogRestClient graylogRestClient = new GraylogRestClient(_CCC.graylog.getRestUrl());
			SessionCreateRequest scr = SessionCreateRequest.create(user, pwd, request.getRemoteAddr());
			ResponseEntity<SessionResponse> resp = graylogRestClient.invoke(HttpMethod.POST, "/system/sessions", scr,
					SessionResponse.class);
			if (resp.getStatusCode() == HttpStatus.OK && resp.getBody().sessionId() != null) {
				SessionUtil.authorizeSession(request.getSession(), user);
			}
		} catch (Throwable t) {
			LOG.warn("Login attempt failed with user :" + user, t);
			return false;
		}
		return true;
	}

	@RequestMapping("/goodbye")
	public String goodbye(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) {
		Cookie[] cookies = request.getCookies();
		for (int i = 0; i < cookies.length; i++) {
			Cookie c = cookies[i];

			if (c.getName().equals("userid")) {
				if (c.getValue().equals(request.getSession().getId())) {
					c.setMaxAge(0);
					c.setValue(null);
					response.addCookie(c);
				}
			}
		}

		UserAccount account = userAccountService.findOneByEmail(SessionUtil.currentUser(request.getSession()));

		if (account != null) {
			long count = account.getCount();
			count = count - 1;
			if (count > 0) {
				account.setCount(count);
			} else {
				account.setCount(0);
			}
			userAccountService.saveContact(account);
		}
		request.getSession().invalidate();

		return _CCC.pages.getPage("facesix.login", "login");
	}

	@RequestMapping("/theme")
	public String theme(Map<String, Object> model) {
		return "theme";
	}

	@RequestMapping("/page/{page}/{op}")
	public String page(Map<String, Object> model, @PathVariable("page") String page, @PathVariable("op") String op,
			HttpServletRequest request, HttpServletResponse response) {
		if (SessionUtil.isAuthorized(request.getSession())) {
			if (op != null) {
				return page + "-" + op;
			} else {
				return page;
			}
		} else {
			return _CCC.pages.getPage("facesix.login", "login");
		}
	}

	@RequestMapping("/page/{page}")
	public String page(Map<String, Object> model, @PathVariable("page") String page, HttpServletRequest request,
			HttpServletResponse response) {
		return page(model, page, null, request, response);
	}

	@RequestMapping("/page")
	public String page1(Map<String, Object> model, @RequestParam("page") String page, HttpServletRequest request,
			HttpServletResponse response) {
		return page(model, page, null, request, response);
	}

	@RequestMapping("/template/**")
	public String template(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) {
		// return root(model, request, response);
		String path = request.getRequestURI();
		path = path.substring(path.indexOf("/template/") + 10);
		return page(model, path, request, response);
	}


	@RequestMapping(value = "/changepwd", method = RequestMethod.POST)
	public String changepwd(Map<String, Object> model, @ModelAttribute Contactus contact,
			@RequestParam(value = "id", required = false) String id,
			@RequestParam(value = "token", required = false) String token, HttpServletRequest request,
			HttpServletResponse response) {
		try {

			LOG.info("Id" + id);
			LOG.info("Token" + token);

			UserAccount user = userAccountService.findById(id);

			Customer cx = null;
			String preferred_url = null;

			if (user != null && user.getResetStatus()) {
				if (user.getToken().equals(token)) {

					if (user.getCustomerId() != null) {
						cx = customerService.findById(user.getCustomerId());
						if (cx != null) {
							if (cx.getPreferedUrlName() != null && !cx.getPreferedUrlName().isEmpty()) {
								preferred_url = cx.getPreferedUrlName();
							}
						}

					}
					accountWebController.setpasswd(model, user.getUid(), contact.getName());
					user.setResetStatus(false);
					user.setPassword(_CCC.cryptor.encrypt(contact.getName()));
					user = userAccountService.save(user, preferred_url);
				}

				HttpSession session = request.getSession(true);
				session.setAttribute("message", model.get("messages"));
				String str = "";

				if (preferred_url != null && !preferred_url.isEmpty()) {
					str = "/simulate/" + preferred_url;
				} else {
					str = "/simulate/";
				}
				response.sendRedirect(str);
			}

			LOG.info("Reset Notification:Change Password");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return _CCC.pages.getPage("facesix.login", "login");
	}

	@RequestMapping("/simulation")
	public String list(Map<String, Object> model, @RequestParam(value = "cid", required = true) String cid,
			HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		sessionCache.clearAttribute(request.getSession(), "sid", "suid", "spid", "spuid", "cid");

		List<Site> fsobjects = service.findByCustomerId(cid);

		String sid = null ;
		
		if (fsobjects != null && fsobjects.size() > 0) {
			Site site = fsobjects.get(0);
			sid = site.getId();
		}

		super.prepare(model, request, response);
		model.put("fsobjects", fsobjects);

		if (cid != null) {
			SessionUtil.setCurrentSiteCustomerId(request.getSession(), cid);
			model.put("cid", cid);
		}
		if (sid != null) {
			SessionUtil.setCurrentSite(request.getSession(), sid);
			model.put("sid", sid);
		}

		model.put("GatewayFinder", CustomerUtils.GatewayFinder(cid));
		model.put("GeoFinder", CustomerUtils.GeoFinder(cid));

		return _CCC.pages.getPage("facesix.simulation", "simulation");
	}

	private boolean withinTimeLimit(long resetTime, long time) {
		long max_diff = 30 * 60 * 1000; // 30 minutes (min * sec * millisec)
		long diff = time - resetTime;
		if (diff <= max_diff) {
			return true;
		}
		return false;
	}
}