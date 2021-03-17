package com.infy.order.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.infy.order.dto.BuyerDTO;
import com.infy.order.dto.CartDTO;
import com.infy.order.dto.OrderDTO;
import com.infy.order.dto.ProductDTO;
import com.infy.order.dto.ProductsOrderedDTO;
import com.infy.order.entity.Order;
import com.infy.order.entity.ProductsOrdered;
import com.infy.order.repository.OrderRepository;
import com.infy.order.service.OrderService;

@RestController
@CrossOrigin
public class OrderController {
	
	Logger logger = LoggerFactory.getLogger(this.getClass());
	
	
	@Autowired
	OrderService orderService;
	
	@Value("${user.url}")
	public String userUrl;
	
	@Value("${product.url}")
	public String productUrl;
	
	@Autowired
	Environment environment;
	
	@Autowired
	OrderRepository orderRepo;
	
	
////////////Fetching from OrderDetails table	
	
     // Fetches all order details
     @GetMapping(value = "/api/orders", produces = MediaType.APPLICATION_JSON_VALUE)
     public List<OrderDTO> getAllOrders() {
	     logger.info("Fetching all orders");
	     return orderService.getAllOrders();
     	}

     // Fetch plan details of a specific plan
     @GetMapping(value = "/api/orders/{orderid}", produces = MediaType.APPLICATION_JSON_VALUE)
     public ResponseEntity<OrderDTO> getSpecificOrders(@PathVariable Integer orderid) {
	     logger.info("Fetching details of Order {}", orderid);
	     ResponseEntity<OrderDTO> responceEntity;
			try {
				OrderDTO orderDTO=orderService.getSpecificOrder(orderid);
				responceEntity=new ResponseEntity<OrderDTO>(orderDTO, HttpStatus.OK);
			}
			catch(Exception e) {
				ResponseStatusException xyz =new ResponseStatusException(HttpStatus.BAD_REQUEST,environment.getProperty(e.getMessage()), e);
				throw xyz;
			}
			return responceEntity;
     	}

     // Create a new order
     @PostMapping(value = "/api/orders", produces = MediaType.APPLICATION_JSON_VALUE)
     public  String addOrderDetails(@RequestBody Order order) {
    	 logger.info("Adding orderDetails");
    	 orderService.addOrderDetails(order);
    	 String string="ADDED SUCCESSFULLY";
    		return string;
    	 //return orderService.getAllOrders();
     }

     
   //Place an Order i/p = buyerid, address within validations
 	@PostMapping(value="/api/placeOrder")
 	public ResponseEntity<String> placeOrder(@RequestBody OrderDTO orderDTO){
 		int flag=0;
 		RestTemplate restTemplate = new RestTemplate();
 		int buyerid = orderDTO.getBuyerid();
 		System.out.println("begore cart");
 		String carturl = userUrl+"getcart/{buyerid}";
 		System.out.println("after cart");
 		String producturl = productUrl+"productid/{prodid}";
 		System.out.println("after product");
 		CartDTO cartDTOs[] = restTemplate.getForObject(carturl, CartDTO[].class, buyerid);
 		double amount = 0.0;
 		for (CartDTO cartDTO : cartDTOs) {
 			int prodid = cartDTO.getProdid();
 			System.out.println(prodid);
 			ProductDTO productDTO = restTemplate.getForObject(producturl, ProductDTO.class, prodid);
 			int price = (int) productDTO.getPrice();
 			int quantity = cartDTO.getQuantity();
 			System.out.println("Quantity:"+quantity);
 			System.out.println("Stock:"+productDTO.getStock());
 			System.out.println("Amount:"+amount);
 			amount += (price*quantity);
 			if(quantity>=productDTO.getStock()) {
 				flag=1;
 				break;
 			}
 			if(orderDTO.getAddress().length()>=100) {
 				flag=2;
 				break;
 			}
 			
 		}
 			
 		BuyerDTO buyerDTO = restTemplate.getForObject(userUrl+"buyer/{buyerid}", BuyerDTO.class, buyerid);
 		System.out.println(buyerDTO.getRewardpoints());
 		double rewardpoints = buyerDTO.getRewardpoints();
 		
 		
 		if(rewardpoints>0) {
 			amount = amount - rewardpoints/4;
 			rewardpoints = 0;
 		}
 		System.out.println(amount);
 		ResponseEntity<String> response=null;
 		
 		//Save to Order details table
 		if(flag==0) {
 			OrderDTO neworderDTO = new OrderDTO();
 			neworderDTO.setBuyerid(orderDTO.getBuyerid());
 			System.out.println(orderDTO.getBuyerid());
 			neworderDTO.setAddress(orderDTO.getAddress());
 			System.out.println(orderDTO.getAddress());
 			neworderDTO.setAmount(amount);
 			neworderDTO.setDate(LocalDate.now());
 			neworderDTO.setStatus("Order Placed successfully");
 			int updatedRewards = (int) ((amount/100));
 			BuyerDTO buyerDTO1 = new BuyerDTO();
 			buyerDTO1.setRewardpoints(updatedRewards);
 			restTemplate.put(userUrl+"buyer/updaterewards/{buyerid}", buyerDTO1, buyerid);
 			Order order = neworderDTO.createEntity();
 			System.out.println(order.getAddress());
 			orderRepo.save(order);
 			response = new ResponseEntity<String>("Order placed successfully", HttpStatus.OK);
 			}
 		else if(flag==1) {
 			response = new ResponseEntity<String>("Ordered quantity is more than the stock available", HttpStatus.BAD_REQUEST);
 		}
 		else if(flag==2) {
 			response = new ResponseEntity<String>("Address should be less than 100 characters", HttpStatus.BAD_REQUEST);
 		}
 		
 		return response;
 	}


////////////Fetching from ProductsOrdered table	


     // Fetches all productOrdered details
     @GetMapping(value = "/api/prodorders", produces = MediaType.APPLICATION_JSON_VALUE)
     public List<ProductsOrderedDTO> getAllProductsOrdered() {
    	 	logger.info("Fetching all ProductsOrdered");
    	 	return orderService.getAllProductsOrdered();
     	}
     
   //Fetch all productsOrdered in a particular OrderId
 	@GetMapping(value="/api/getProducts/{orderid}")
 	public List<ProductsOrderedDTO> getProducts(@PathVariable Integer orderid)
 	{
 		logger.info("Fetching all products ordered for id {}",orderid);
 		return orderService.getProductsOrdered(orderid);
 		
 	}
 	
 	//Fetch Order details including list of all products ordered in that particular order
 		@GetMapping(value="/api/orderdetails/{orderid}")
 		public OrderDTO getOrderDetails(@PathVariable Integer orderid) 
 		{
 			logger.info("Fetching order details for id {}",orderid);
 			return orderService.getOrderDetails(orderid);
 			
 		}

     @PostMapping(value = "/api/prodorders", produces = MediaType.APPLICATION_JSON_VALUE)
     public  String addProductOrderedDetails(@RequestBody ProductsOrdered order) {
    	 	logger.info("Adding orderDetails");
    	 	orderService.addProductOrderedDetails(order);
    	 	String string="ADDED SUCCESSFULLY";
    	 		return string;
     	}
     
   //reorder an order
 	@PostMapping(value="/api/reOrder/{buyerid}/{orderid}")
 	public String reorder(@PathVariable Integer buyerid,@PathVariable Integer orderid)
 	{
 		return orderService.reOrder(orderid, buyerid);
 	}
     
     
     @GetMapping(value = "/api/vieworders/{productId}", produces = MediaType.APPLICATION_JSON_VALUE)
 	public ResponseEntity<List<OrderDTO>> getSpecificViewOrder (@PathVariable Integer prodid) {
 		logger.info("Fetching details of order {}");
 		ResponseEntity<List<OrderDTO>> responceEntity=null;
 		try {
 		List<ProductsOrderedDTO> poductsOrderedDTO =orderService.getAllViewProdsOrdered(prodid);
 		List<OrderDTO> orderDTOs = new ArrayList<>();
 		for(ProductsOrderedDTO product:poductsOrderedDTO) {
 			OrderDTO orderDTO=orderService.getSpecificOrder(product.getOrderid());
 			if(orderDTO!=null) {
 			orderDTOs.add(orderDTO);
 			}
 		}
 		responceEntity=new ResponseEntity<List<OrderDTO>>(orderDTOs, HttpStatus.OK);
 		}
 		catch(Exception e) {
 			ResponseStatusException rsc =new ResponseStatusException(HttpStatus.BAD_REQUEST,environment.getProperty(e.getMessage()), e);
 			throw rsc;
 		}
 		return responceEntity;
 	}
  	
       
}
