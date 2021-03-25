import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.FileWriter;
import java.io.FileReader;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EjercicioUno {

	static String ENVIRONMENT = "dev"; // dev or prod
	static String ACCESS_TOKEN;
	static String API_ORDERS_SITE = "https://api.mercadolibre.com/orders/";
	static String API_SHIPMENTS_SITE = "https://api.mercadolibre.com/shipments/";
	static String API_CURRENCIES_SITE = "https://api.mercadolibre.com/currencies/";
	static String OUTPUT_FILE_PATH = "data/output/output_1.csv";
	static JSONArray CURRENCIES = new JSONArray();

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception{
		
		// Se establece el ambiente de ejecucion
		if (args.length == 0 || !(args[0].equals("dev") || args[0].equals("prod"))) {
			System.out.println("ERROR: Debe definir el ambiente de ejecucion en los argumentos.");
    		return;
		} else {
			if(args[0].equals("prod")){
	    		ENVIRONMENT = "prod";
	    		System.out.println("INFO: Establecido el ambiente a produccion.");
	    	} else {
	    		System.out.println("INFO: Establecido el ambiente a desarrollo.");
	    	}
		}
		
		if ( ENVIRONMENT.equals("prod")){
			if( args.length == 2 ){
				ACCESS_TOKEN = args[1];		
			} else {
				System.out.println("ERROR: Debe ingresar un Access Token");
				return;
			}
		}
		
    	
		// Se obtienen las monedas que maneja MercadoLibre
		getMonedas();
		
		// Se obtienen las ordenes a buscar
		String[] orders;
		System.out.println("INFO: Obteniendo las ordenes a consultar...");
		orders = getOrders(new FileReader("data/sources/orders.csv")); // orders.csv es el posta
		
		Set<JSONObject> jsonOrders = new HashSet<JSONObject>();
		JSONObject currentOrder;
		JSONObject currentShipment;
		JSONArray currentShipmentPayments;
		JSONObject currentShipmentCarrier;
		//JSONObject currentShupmentLeadTime;
				
		// Si el ambiente es dev, se utilizan las respuestas mock de los servicios
    	if (ENVIRONMENT.equals("dev")) {
    		
    		// Se obtienen los json mock guardados localmente para cada orden
    		for (int i = 0; i < orders.length; i++) {
	    		
    			System.out.println("INFO: Procesando la orden " + orders[i] + "...");
	    		
    			currentOrder = (JSONObject) getJSONObjectOrArray(new FileReader("data/mock/orders/" + orders[i] + ".json"));	    		
	    		currentShipment = (JSONObject) getJSONObjectOrArray(new FileReader("data/mock/shipments/" + ((JSONObject) currentOrder.get("shipping")).get("id") + ".json"));
	    		currentShipmentPayments = (JSONArray) getJSONObjectOrArray(new FileReader("data/mock/shipments/payments/" + ((JSONObject) currentOrder.get("shipping")).get("id") + ".json"));
	    		currentShipmentCarrier = (JSONObject) getJSONObjectOrArray(new FileReader("data/mock/shipments/carrier/" + ((JSONObject) currentOrder.get("shipping")).get("id") + ".json"));
	    		//currentShupmentLeadTime = (JSONObject) getJSONObjectOrArray(new FileReader("data/mock/shipments/carrier/" + ((JSONObject) currentOrder.get("shipping")).get("id") + ".json"));
	    		
	    		jsonOrders.add(getRelevantOrderFieldsAsJSONObj(currentOrder, currentShipment, currentShipmentPayments, currentShipmentCarrier));	    		
	    	} 	
	    	
		} else {
			// Si el ambiente es prod, se interactua con los servicios productivos de la API de ML
			
			// Se obtienen los json para cada orden a traves de requests a la API correspondiente
			for (int i = 0; i < orders.length; i++) {
	    		
				System.out.println("INFO: Procesando la orden " + orders[i] + "...");
				
				// Se hace el request a la API de orders
				currentOrder = (JSONObject) getJSONFromAPI(API_ORDERS_SITE + orders[i], ACCESS_TOKEN);

		        // Se hace el request a la API de shipments
				currentShipment = (JSONObject) getJSONFromAPI(API_SHIPMENTS_SITE + ((JSONObject) currentOrder.get("shipping")).get("id").toString(), ACCESS_TOKEN);
		        		        
		        // Se hace el request a la API de shipment-payment
				currentShipmentPayments = (JSONArray) getJSONFromAPI(API_SHIPMENTS_SITE + 
						((JSONObject) currentOrder.get("shipping")).get("id").toString() + "/payments", ACCESS_TOKEN);
		        
		        // Se hace el request a la API de shipment-carrier
				currentShipmentCarrier = (JSONObject) getJSONFromAPI(API_SHIPMENTS_SITE +
						((JSONObject) currentOrder.get("shipping")).get("id").toString() + "/carrier", ACCESS_TOKEN);

		        jsonOrders.add(getRelevantOrderFieldsAsJSONObj(currentOrder, currentShipment, currentShipmentPayments, currentShipmentCarrier));
		        
	    	}		        
		}
    	
    	writeOutputFile(jsonOrders);
    	
    	System.out.println("\nEjecucion finalizada. Presione Enter para salir.");
    	System.in.read();
    }
	
	
	/**
	* Carga el objeto <code>currencies</code> de tipo JSONArray con las monedas
	* utilizadas por MercadoLibre. Si la variable <code>ENVIRONMENT</code> tiene 
	* valor <code>"dev"</code>, las toma a partir de un archivo local; si tiene
	* valor <code>"prod"</code>, realiza el request a la API correspondiente.
	*/
	public static void getMonedas() throws Exception {
		if(ENVIRONMENT.equals("prod")){
    		CURRENCIES = (JSONArray) getJSONFromAPI(API_CURRENCIES_SITE, "");
    	} else {
    		CURRENCIES = (JSONArray) getJSONObjectOrArray(new FileReader("data/mock/currencies.json"));
    	}
	}
	
	/**
	* Devuelve un objeto de tipo String con el smbolo de la moneda
	* correspondiente.
	*
	* @param currencyId  El ID de la moneda a buscar.
	*/
	public static String getCurrencySymbol(String currencyId) throws Exception {
		Object symbol = ((JSONObject) CURRENCIES.stream().filter(currency -> ((JSONObject) currency).get("id").
				toString().equals(currencyId)).findFirst().get()).get("symbol");
		if(symbol != null) {
			return symbol.toString();
		} else {
			throw new Exception("ERROR: No se encontro la moneda con el ID especificado.");
		}
	}
	
	/**
	* Devuelve un objeto de tipo String con la descripcion de la moneda
	* correspondiente.
	*
	* @param currencyId  El ID de la moneda a buscar.
	*/
	public static String getCurrencyDesc(String currencyId) throws Exception {
		Object symbol = ((JSONObject) CURRENCIES.stream().filter(currency -> ((JSONObject) currency).get("id").
				toString().equals(currencyId)).findFirst().get()).get("description");
		if(symbol != null) {
			return symbol.toString();
		} else {
			throw new Exception("ERROR: No se encontro la moneda con el ID especificado.");
		}
	}
	
	/**
	* Devuelve los IDs de las ordenes en un array de tipo String, a partir de 
	* un documento de tipo csv.
	*
	* @param fr  El documento con las ordenes a leer, con formato csv.
	*/
	public static String[] getOrders(FileReader fr) throws Exception {
		List<String> res = new ArrayList<String>();
		String row;
		
		BufferedReader csvReader = new BufferedReader(fr);
    	csvReader.readLine();
    	
    	while ((row = csvReader.readLine()) != null) {
    	    res.add(row); 	    
    	}
    	csvReader.close();
    	
    	return res.toArray(new String[res.size()]);
	}

	/**
	* Devuelve un objeto generico, con formato JSONObject o JSONArray, segun
	* corresponda, a partir de un objeto generico que contiene el json a leer.
	*
	* @param json  El objeto generico a leer. Puede ser de tipo String o de 
	* tipo FileReader.
	*/
	public static Object getJSONObjectOrArray(Object json) throws Exception {
		
		JSONObject jsonObjectOutput = new JSONObject();
		JSONArray jsonArrayOutput = new JSONArray();
		JSONParser parser = new JSONParser();
		Object obj;
		
		if(json.getClass().equals(String.class)) {
			 obj = parser.parse((String) json);
		} else if(json.getClass().equals(FileReader.class)) {
			obj = parser.parse((FileReader) json);
		} else {
			throw new Exception("ERROR: El objeto enviado no tiene el formato esperado.");
		}
		
		if((obj.toString()).charAt(0) == '['){
			JSONArray jsonObject = (JSONArray) obj;
			jsonArrayOutput = jsonObject;
			return jsonArrayOutput;
		} else {
			JSONObject jsonObject = (JSONObject) obj;
			jsonObjectOutput = jsonObject;	
			return jsonObjectOutput;
		}
	}
	
	/**
	* Devuelve un objeto generico, con formato JSONObject o JSONArray, segun
	* corresponda, a partir del resultado de un request a una API determinada.
	*
	* @param site  La direccion de la API a consultar.
	* @param accessToken  El codigo de autorizacion para acceder a la API a
	* consultar; puede quedar en blanco si la API no requiere autorizacion.
	*/
	public static Object getJSONFromAPI(String site, String accessToken) throws Exception {
		
		Object result;
		
		URL url = new URL(site);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "application/json");
		if (!accessToken.isEmpty()) {
			conn.setRequestProperty("Authorization", "Bearer " + accessToken);	
		}
		
        System.out.println("INFO: Enviando request a " + url.getHost() + url.getPath() + "...");
        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("ERROR: Fallo la conexion con la url '" + url.getHost() + url.getPath() + "'. "
            		+ "Codigo de error HTTP: " + conn.getResponseCode() + " - " + conn.getResponseMessage());
        }
        
        BufferedReader brOrder = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        
        result = getJSONObjectOrArray(brOrder.readLine());
        conn.disconnect();
        
        return result;
	}
	
	/**
	* Devuelve un objeto de tipo JSONObject (a partir de los objetos de la orden,
	* del envio, de los pagos del envio y del proveedor del envio) que unicamente
	* contiene los campos necesarios para imprimir el resultado.  
	*
	* @param order  El objeto de tipo JSONObject de la orden.
	* @param shipment  El objeto de tipo JSONObject del envio.
	* @param shipmentPayments  El objeto de tipo JSONObject con los pagos del
	* envio.
	* @param shipmentCarrier  El objeto de tipo JSONObject con la informacion
	* del proveedor del envio.
	*/
	public static JSONObject getRelevantOrderFieldsAsJSONObj(JSONObject order, JSONObject shipment, JSONArray shipmentPayments, JSONObject shipmentCarrier) throws Exception {
		
		JSONObject jsonOutput = new JSONObject();
		jsonOutput.put("id", order.get("id"));
		
		JSONArray orderItems = (JSONArray) order.get("order_items");
		
		JSONObject currentOrderItem;
		JSONObject currentItem;
		String itemsDescription = "";
		
		for (Object orderItem : orderItems) {
			
			String description;
			currentOrderItem = (JSONObject) orderItem;
			currentItem = (JSONObject) currentOrderItem.get("item");
			
			description = currentItem.get("title").toString();
			
			JSONArray variations = (JSONArray) currentItem.get("variation_attributes");
			
			if (!variations.isEmpty()) {
				for (Object variation : variations) {
					description += " - " + ((JSONObject) variation).get("name") + " " + ((JSONObject) variation).get("value_name"); // PROBAR
				}
			}
			
			description += " - " + getCurrencySymbol(currentOrderItem.get("currency_id").toString());
			description += currentOrderItem.get("unit_price").toString();
			
			itemsDescription += description;
		}
		
		jsonOutput.put("items", itemsDescription);
		jsonOutput.put("total_amount", order.get("total_amount"));
		jsonOutput.put("currency_id", getCurrencyDesc(order.get("currency_id").toString()));
		jsonOutput.put("shipping_id", ((JSONObject) order.get("shipping")).get("id"));
		jsonOutput.put("logistic_type", shipment.get("logistic_type"));
		jsonOutput.put("source", buildAddress((JSONObject) shipment.get("sender_address")));
		if (shipment.get("agency") != null) {
			jsonOutput.put("destination", "ID Agencia:" + ((JSONObject) shipment.get("agency")).get("agency_id") 
					+ "; ID Carrier: " + ((JSONObject) shipment.get("agency")).get("carrier_id"));
		} else {
			jsonOutput.put("destination", buildAddress((JSONObject) shipment.get("receiver_address")));
		}
		jsonOutput.put("shipping_payments", buildPayments(shipmentPayments,
				((JSONObject) order.get("buyer")).get("id").toString(),
				((JSONObject) order.get("seller")).get("id").toString()));
		jsonOutput.put("carrier", shipmentCarrier.get("name"));
		jsonOutput.put("estimatedDeliveryStart", ((JSONObject) ((JSONObject) shipment.get("shipping_option")).
				get("estimated_delivery_time")).get("date").toString());
		if (((JSONObject) ((JSONObject) (((JSONObject) shipment.
				get("shipping_option")).get("estimated_delivery_time"))).get("offset")).get("date") != null) {
			jsonOutput.put("estimatedDeliveryEnd", ((JSONObject) ((JSONObject) (((JSONObject) shipment.
					get("shipping_option")).get("estimated_delivery_time"))).get("offset")).get("date").toString());
		} else {
			jsonOutput.put("estimatedDeliveryEnd", "");
		}
		jsonOutput.put("actualDelivery", ((JSONObject) shipment.get("status_history")).get("date_delivered"));
		jsonOutput.put("status", shipment.get("status").toString());
		jsonOutput.put("isShipmentOnTime", isShipmentOnTime((JSONObject) shipment.get("shipping_option"), 
				((JSONObject) shipment.get("status_history")).get("date_delivered")));
				
		return jsonOutput;
	}
	
	/**
	* Devuelve un objeto de tipo String a partir de un objeto de tipo JSONObject,
	* con la direccion consolidada.  
	*
	* @param obj  El objeto de tipo JSONObject con la informacion del domicilio.
	*/
	public static String buildAddress(JSONObject obj) {
		
		String address;
		
		address = obj.get("address_line").toString();
		address += " (" + obj.get("zip_code").toString() + ")";
		address += " " + ((JSONObject) obj.get("city")).get("name").toString() + " -";
		address += " " + ((JSONObject) obj.get("state")).get("name").toString() + " -";
		address += " " + ((JSONObject) obj.get("country")).get("name").toString();
		
		return address;
	}
	
	/**
	* Devuelve un objeto de tipo String a partir de un objeto de tipo JSONArray con
	* los pagos del envio, y dos objetos de tipo String con los IDs del vendedor y
	* del comprador
	*
	* @param arr  El objeto de tipo JSONArray con la informacion de los pagos del
	* envio.
	* @param buyerId  El objeto de tipo String con el ID del comprador.
	* @param sellerId  El objeto de tipo String con el ID del vendedor.
	*/
	public static String buildPayments(JSONArray arr, String buyerId, String sellerId) {
		
		String payments = "";
		
		for (Object payment : arr) {
			JSONObject paymentJson = (JSONObject) payment;
			if (paymentJson.get("user_id").toString().equals(buyerId)) {
				payments += "Comprador: " + paymentJson.get("amount") + " (" + paymentJson.get("status") + ")";
			} else if (paymentJson.get("user_id").toString().equals(sellerId)) {
				payments += "Vendedor: " + paymentJson.get("amount") + " (" + paymentJson.get("status") + ")";
			}
			
			payments += "  ";
		}
					
		return payments.substring(0, payments.length()-1);
	}
	
	/**
	* Devuelve un String, evaluando si el envio se entrego antes de la fecha limite de entrega.
	* Si el tipo de estimacion de entrega es <code>known</code>, se compara contra la fecha de
	* entrega; si el tipo de estimacion de entrega es <code>known_frame</code>, se compara contra
	* la fecha final del rango de fechas; y si el tipo de estimacion de entrega es 
	* <code>unknown</code> o <code>unknown_frame</code>, o si la fecha de entrega es <code>null</code>,
	* no se compara y se devuelve la respuesta correspondiente.
	* 
	* @param obj  El objeto de tipo JSONObject con la informacion del shipment_option.
	* @param dateDelivered  El objeto de tipo String con la fecha de entrega.
	* 
	* @return <code>"Si"</code>, si el envio fue entregado antes de la fecha limite
	* @return <code>"No"</code>, si el envio fue entregado despues de la fecha limite
	* @return <code>"N/A"</code>, si no se puede determinar, ya sea por no tener fechas
	* estimadas de entrega, o por no haber sido entregada aun la orden.
	*/
	public static String isShipmentOnTime(JSONObject obj, Object dateDelivered) {
		
		String shippingType;
		LocalDateTime estimatedDeliveryDateLimit;
		LocalDateTime deliveryDate;
		
		if (dateDelivered == null) {
			return "N/A";
		} else {
			deliveryDate = LocalDateTime.parse(dateDelivered.toString(), DateTimeFormatter.ISO_DATE_TIME);
		}
		
		shippingType = ((JSONObject) obj.get("estimated_delivery_time")).get("type").toString();
		
		if (shippingType.equals("known")) {
			estimatedDeliveryDateLimit = LocalDateTime.parse(((JSONObject) obj.
					get("estimated_delivery_time")).get("date").toString(), DateTimeFormatter.ISO_DATE_TIME);
		} else if (shippingType.equals("known_frame")) {
			estimatedDeliveryDateLimit = LocalDateTime.parse(((JSONObject) ((JSONObject) obj.
					get("estimated_delivery_time")).get("offset")).get("date").toString(), DateTimeFormatter.ISO_DATE_TIME);
		} else {
			return "N/A";
		}
		
		if (deliveryDate.isBefore(estimatedDeliveryDateLimit) || deliveryDate.isEqual(estimatedDeliveryDateLimit)) {
			return "Si";
		}
		
		return "No";
	}
	
	/**
	* Genera un archivo de tipo .csv, que se guarda en la ruta establecida
	* por la variable <code>OUTPUT_FILE_PATH</code>, con la informacion de
	* cada una de las ordenes.
	*
	* @param jsonOrders  El Set con objetos de tipo JSONObject, donde cada
	* uno representa una orden.
	*/
	public static void writeOutputFile(Set<JSONObject> jsonOrders) throws Exception {
		
		FileWriter csvWriter = new FileWriter(OUTPUT_FILE_PATH);
		
		System.out.println("INFO: Generando el archivo resultante...");
		
		// Se agregan los headers del archivo
		csvWriter.append("Order,,Payment,,Envio\n");
		csvWriter.append("ID,Productos,Monto total,Moneda,Logistica,Origen,"
				+ "Destino (Agencia o domicilio),Pagos del envio,Proveedor,Fecha de entrega estimada (Minima),"
				+ "Fecha de entrega estimada (Maxima),Fecha de entrega real,Estado,En tiempo y forma?\n");
		
		// Se agrega la informacion de cada orden en una misma fila
		for (JSONObject jsonOrder : jsonOrders) {
			
			csvWriter.append(jsonOrder.get("id").toString());
			csvWriter.append(",");
			csvWriter.append(jsonOrder.get("items").toString());
			csvWriter.append(",");
			csvWriter.append(jsonOrder.get("total_amount").toString());
			csvWriter.append(",");
			csvWriter.append(jsonOrder.get("currency_id").toString());
			csvWriter.append(",");
			csvWriter.append(jsonOrder.get("logistic_type").toString());
			csvWriter.append(",");
			csvWriter.append(jsonOrder.get("source").toString());
			csvWriter.append(",");
			csvWriter.append(jsonOrder.get("destination").toString());
			csvWriter.append(",");
			csvWriter.append(jsonOrder.get("shipping_payments").toString());
			csvWriter.append(",");
			csvWriter.append(jsonOrder.get("carrier").toString());
			csvWriter.append(",");
			csvWriter.append(jsonOrder.get("estimatedDeliveryStart").toString());
			csvWriter.append(",");
			csvWriter.append(jsonOrder.get("estimatedDeliveryEnd").toString());
			csvWriter.append(",");
			if (jsonOrder.get("actualDelivery") != null) {
				csvWriter.append(jsonOrder.get("actualDelivery").toString());
			} else {
				csvWriter.append("");
			}
			csvWriter.append(",");
			csvWriter.append(jsonOrder.get("status").toString());
			csvWriter.append(",");
			csvWriter.append(jsonOrder.get("isShipmentOnTime").toString());
			csvWriter.append("\n");
					
		}
		
		csvWriter.flush();
		csvWriter.close();
		
		System.out.println("INFO: Archivo generado con exito.");
				
	}
}