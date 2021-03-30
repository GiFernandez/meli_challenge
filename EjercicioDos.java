import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class EjercicioDos {

	static String ENVIRONMENT = "dev"; // dev or prod
	static String API_SHIPMENTS_SITE = "https://api.mercadolibre.com/shipments/";
	static String OUTPUT_FILE_PATH = "data/output/output_2.csv";
	static String ACCESS_TOKEN;
	
	public static void main(String[] args) throws Exception {
		
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
		
		// Se obtienen los envios a buscar
		String[] shipments;
		System.out.println("INFO: Obteniendo los envios a consultar...");
		shipments = getShipments(new FileReader("data/sources/shipments.csv"));
		
		Set<JSONObject> jsonShipments = new HashSet<JSONObject>();
		JSONObject currentShipment;
		JSONObject currentShipmentLeadTime;
		
		// Si el ambiente es dev, se utilizan las respuestas mock de los servicios
    	if (ENVIRONMENT.equals("dev")) {
    		
    		// Se obtienen los json mock guardados localmente para cada envio
    		for (int i = 0; i < shipments.length; i++) {
	    		
    			System.out.println("INFO: Procesando el envio " + shipments[i] + "...");
	    		
    			// Se obtiene el json mock del envio
	    		currentShipment = (JSONObject) getJSONObjectOrArray(
	    				new FileReader("data/mock/shipments/" + shipments[i] + ".json"));
	    		
	    		// Se obtiene el json mock de las estimaciones de entrega del envio
	    		currentShipmentLeadTime = (JSONObject) getJSONObjectOrArray(
	    				new FileReader("data/mock/shipments/lead_time/" + shipments[i] + ".json"));
	 
	    		jsonShipments.add(getRelevantShipmentFieldsAsJSONObj(currentShipment, currentShipmentLeadTime));	    		
	    	} 	
	    	
		} else {
			// Si el ambiente es prod, se interactua con los servicios productivos de la API de ML
			
			// Se obtienen los json para cada envio a traves de requests a la API correspondiente
			for (int i = 0; i < shipments.length; i++) {
	    		
				System.out.println("INFO: Procesando el envio " + shipments[i] + "...");

		        // Se hace el request a la API de shipments
				currentShipment = (JSONObject) getJSONFromAPI(API_SHIPMENTS_SITE + shipments[i], args[1]);
		        
				// Se hace el request a la API de shipments-lead_time
				currentShipmentLeadTime = (JSONObject) getJSONFromAPI(API_SHIPMENTS_SITE + shipments[i] + "/lead_time", args[1]);

		        jsonShipments.add(getRelevantShipmentFieldsAsJSONObj(currentShipment, currentShipmentLeadTime));
		        
	    	}		        
		}
    	
    	writeOutputFile(jsonShipments);
    	
    	System.out.println("\nEjecucion finalizada. Presione Enter para salir.");
    	System.in.read();
	}
	
	/**
	* Devuelve los IDs de los envios en un array de tipo String, a partir de 
	* un documento de tipo csv.
	*
	* @param fr  El documento con los envios a leer, con formato csv.
	*/
	public static String[] getShipments(FileReader fr) throws Exception {
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
	* Devuelve un objeto de tipo JSONObject (a partir de los objetos del envio y
	* de las estimaciones de entrega del envio) que unicamente contiene los 
	* campos necesarios para imprimir el resultado.  
	*
	* @param shipment  El objeto de tipo JSONObject del envio
	* @param shipmentLeadTime  El objeto de tipo JSONObject las estimaciones de
	* entrega del envio
	*/
	public static JSONObject getRelevantShipmentFieldsAsJSONObj(JSONObject shipment, JSONObject shipmentLeadTime) throws Exception {
		
		JSONObject jsonOutput = new JSONObject();
		jsonOutput.put("id", shipment.get("id"));
		jsonOutput.put("status", shipment.get("status"));
		jsonOutput.put("substatus", shipment.get("substatus"));
		jsonOutput.put("logistic_type", shipment.get("logistic_type"));
		jsonOutput.put("source", buildAddress((JSONObject) shipment.get("sender_address")));
		jsonOutput.put("date_delivered", getDeliveryDate(shipment));
		
		
		jsonOutput.put("estimated_delivery_final", 
				((JSONObject) shipmentLeadTime.get("estimated_delivery_final")).get("date"));
		jsonOutput.put("estimated_delivery_extended", 
				((JSONObject) shipmentLeadTime.get("estimated_delivery_extended")).get("date"));
		jsonOutput.put("estimated_delivery_date", getEstimatedDeliveryDate(
				(JSONObject) shipmentLeadTime.get("estimated_delivery_time")));
		
		jsonOutput.put("deliveryDelay", getDeliveryDelay(getEstimatedDeliveryDate(
				(JSONObject) shipmentLeadTime.get("estimated_delivery_time"))
				, getDeliveryDate(shipment)));

		if(getDeliveryDate(shipment).isEmpty()) {
			jsonOutput.put("wasDeliveryOnTime", "N/A");
		} else {
			jsonOutput.put("wasDeliveryOnTime", wasDeliveryOnTime(jsonOutput.get("deliveryDelay").toString()));	
		}
		
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
	* Devuelve un objeto de tipo String, con "Si" o "No" a partir del delay entre
	* la fecha de entrega real y la fecha de entrega estimada.
	*
	* @param delay  El objeto de tipo String con la demora entre la fecha de entrega real
	* y la fecha de entrega estimada.
	* 
	* @return <li> "Si"	Si la demora es un objeto vacio
	* 		<li> "N/A"	Si la demora es el String "N/A", que se devuelve en los casos en
	* que el envio no fue entregado, o que el atributo <code>"type"</code> del envio tenia
	* los valores <code>unknown</code> o <code>unknown_frame</code>
	*/
	public static String wasDeliveryOnTime(String delay) {
		
		if(delay.equals("N/A")) {
			return "Si";
		}
		
		return "No"; 
	}
	
	/**
	* Devuelve un objeto de tipo String  con la demora entre la fecha de entrega del
	* envio y la primera ventana de entrega estimada. Tambien puede devolver "N/A"
	* si el envio no fue entregado, o si el atributo <code>"type"</code> del envio
	* tuviera los valores <code>unknown</code> o <code>unknown_frame</code>.
	*
	* @param estimatedDeliveryDate  El objeto de tipo String con la fecha de entrega 
	* estimada.
	* @param deliveryDate	El objeto de tipo String con la fecha de entrega.
	* 
	* @return <li> HH:mm:ss	Un objeto de tipo String con el formato HH:mm:ss, con la 
	* demora en horas, minutos y segundos.
	* 		<li> "N/A"	Si el envio no fue entregado, o si el envio fue entregado
	* antes de la fecha de entrega estimada.
	*/
	public static String getDeliveryDelay(String estimatedDeliveryDate, String deliveryDate) throws Exception {
		
		LocalDateTime estimatedDate = null;
		LocalDateTime dateDelivered = null;
		LocalDateTime delay = null;
		
		long hours;
		long minutes;
		long seconds;
		
		if (estimatedDeliveryDate.isEmpty()) {
			throw new Exception("ERROR: La fecha estimada de entrega no puede estar vacia");
		}
		
		if (deliveryDate.isEmpty()) {
			return "N/A";
		} else {
			estimatedDate = LocalDateTime.parse(estimatedDeliveryDate, DateTimeFormatter.ISO_DATE_TIME); 
			dateDelivered = LocalDateTime.parse(deliveryDate, DateTimeFormatter.ISO_DATE_TIME);
		}
		
		if (dateDelivered.isBefore(estimatedDate) || dateDelivered.equals(estimatedDate)) {
			return "N/A";
		}

		delay = estimatedDate;
		
		hours = delay.until(dateDelivered, ChronoUnit.HOURS);
		delay = delay.plusHours(hours);
		minutes = delay.until(dateDelivered, ChronoUnit.MINUTES);
		delay = delay.plusMinutes(minutes);
		seconds = delay.until(dateDelivered, ChronoUnit.SECONDS);
		
		return String.valueOf(hours) + ":" + String.valueOf(minutes) + ":" + String.valueOf(seconds);
	}
	
	/**
	* Devuelve un objeto de tipo String con la fecha de entrega del producto con
	* formato <code>ISO_DATE_TIME</code>.
	*
	* @param obj  El objeto de tipo JSONObject con la informacion del envio.
	*/
	public static String getDeliveryDate(JSONObject obj) {
		
		String dateDelivered;
		
		if (((JSONObject) obj.get("status_history")).get("date_delivered") != null) {
			dateDelivered = ((JSONObject) obj.get("status_history")).get("date_delivered").toString();
		} else {
			return "";
		}
		
		return dateDelivered;
	}
	
	/**
	* Devuelve un objeto de tipo String con la fecha estimada de entrega del 
	* producto con formato <code>ISO_DATE_TIME</code>.
	*
	* @param obj  El objeto de tipo JSONObject con la informacion del envio.
	*/
	public static String getEstimatedDeliveryDate(JSONObject obj) {
		
		if (obj.get("type").toString().equals("known") || obj.get("type").toString().
				equals("known_frame")) {
			if (obj.get("type").toString().equals("known_frame")) {
				return ((JSONObject)obj.get("offset")).get("date").toString(); 
			} else if (obj.get("type").toString().equals("known")) {
				return obj.get("date").toString(); 
			}
		}
		
		return "";
	}
	
	
	/**
	* Genera un archivo de tipo .csv, que se guarda en la ruta establecida
	* por la variable <code>OUTPUT_FILE_PATH</code>, con la informacion de
	* cada uno de los envios.
	*
	* @param jsonShipments  El Set con objetos de tipo JSONObject, donde cada
	* uno representa un envio.
	*/
	public static void writeOutputFile(Set<JSONObject> jsonShipments) throws Exception {
		
		FileWriter csvWriter = new FileWriter(OUTPUT_FILE_PATH);
		
		System.out.println("INFO: Generando el archivo resultante...");
		
		// Se agregan los headers del archivo
		csvWriter.append("Shipment,,,,,,Estimacion de entrega (de mayor a menor en eficiencia),,,,\n");
		csvWriter.append("ID,Estado actual,Sub-estado actual,Tipo de logistica,Origen,Fecha de Entrega,Estimacion 1,"
				+ "Estimacion 2,Estimacion 3,Llego a tiempo?,Delay respecto de la primera ventana\n");
		
		// Se agrega la informacion de cada envio en una misma fila
		for (JSONObject jsonShipment : jsonShipments) {
			
			csvWriter.append(jsonShipment.get("id").toString());
			csvWriter.append(",");
			csvWriter.append(jsonShipment.get("status").toString());
			csvWriter.append(",");
			csvWriter.append(jsonShipment.get("substatus").toString());
			csvWriter.append(",");
			csvWriter.append(jsonShipment.get("logistic_type").toString());
			csvWriter.append(",");
			csvWriter.append(jsonShipment.get("source").toString());
			csvWriter.append(",");
			csvWriter.append(jsonShipment.get("date_delivered").toString());
			csvWriter.append(",");
			csvWriter.append(jsonShipment.get("estimated_delivery_date").toString());
			csvWriter.append(",");
			csvWriter.append(jsonShipment.get("estimated_delivery_extended").toString());
			csvWriter.append(",");
			csvWriter.append(jsonShipment.get("estimated_delivery_final").toString());
			csvWriter.append(",");
			csvWriter.append(jsonShipment.get("wasDeliveryOnTime").toString());
			csvWriter.append(",");
			csvWriter.append(jsonShipment.get("deliveryDelay").toString());
			csvWriter.append("\n");
					
		}
		
		csvWriter.flush();
		csvWriter.close();
		
		System.out.println("INFO: Archivo generado con exito.");
				
	}
}
