## Meli Challenge by Gisela Fernandez


### Requisitos
JDK 15


### Clonar proyecto
```
git clone https://github.com/GiFernandez/meli_challenge.git
```

### Compilación
Para compilar se debe ejecutar desde línea de comando lo siguiente: 

Windows
```
javac -cp ".;./jars/*" EjercicioUno.java 
```

Unix
```
javac -cp ".:./jars/*" EjercicioUno.java 
```


### Ejecución
La ejecución del script espera recibir 2 parámetros obligatorios:
1.	**Environment:** Identifica el entornos de ejecución. Puede ser `dev` o `prod`. En el entorno `dev` ejecutará tomando como fuente de datos los archivos mocks, y en entorno prod, ejecutará tomando como fuente las apis productivas. 
2.	**Access Token:** el access token necesario para la autenticación productiva de las apis. Solo se utiliza en entorno `prod`.

Windows
```
java -cp ".;./jars/*" EjercicioUno {environment} {acceso_token}
```
Unix
```
java -cp ".:./jars/*" EjercicioUno {environment} {acceso_token}
```

### Explicación genérica del programa

1. Al iniciar, el script evalúa el entorno definido por parámetro, leyendo el primer parámetro enviado. Esto se guarda en una variable global `ENVIRONMENT` que será utilizada a lo largo del programa. En caso de error se muestra mensaje en consola. 

2. Consultamos y guardamos en un variable las monedas de cada país a través de la función `getMonedas` que será utilizado en la construcción de la data de salida (pto 4).

3. Leemos el archivo de entrada `orders.csv` que contiene las órdenes a procesar.

4. Iteramos cada orden para consultar la información solicitada en los distintos recursos, utilizando la fuente correspondiente según el ambiente:
- Ambiente `dev`: se utilizan las respuestas mock de los servicios guardados localmente para cada orden. Para este ejemplo, se crearon órdenes con información falsa que permita testear distintos escenarios de prueba. 
- Ambiente `prod`:  se interactúa con los servicios productivos de la API de ML según el access token enviado por parámetro. Se obtienen los json para cada orden a través de requests a la API correspondiente (Se hace el request a la API de orders, a la API de shipments, a la API de shipment-payment, a la API de shipment-carrier). En caso de error se muestra mensaje en consola. 
Independientemente que la consulta sea vía mock o vía api, los datos se guardan en una única Collection `jsonOrders`, procesando los datos en los atributos necesarios para generar el csv output solicitado. 

5. Escritura del archivo csv de salida: se recorre la Collection `jsonOrder` que contiene toda la información solicitada y se imprime en un csv `output_1.csv` dando por finalizado el proceso. 



### Cambios posibles en el programa:


1.	En lugar de guardar en una collection la información de todas las orders para luego guaradarlas todas juntas en el csv, se podría ir escribiendo en el csv a medida que se consulta cada order.
2.	Se podria tener un archivo de entrada mock que sea distinto al archivo de entrada objetivo para consultar una cantidad menor de órdenes en caso que la lista objetivo sea muy larga. 


### Consideraciones:


1.	No fue posible consultar las APIs productivas de las órdenes dadas con el access token generado con mi usuario ya que no tengo permisos para poder hacerlo. 
2.	La información de las APIs en la web de developers de Mercado Libre, no me muestra exactamente lo mismo que utilizando los user de test, por esto es que en varios escenarios tengo dudas si la info es correcta.
3.	Para transformar el valor de la moneda, utilicé una API (https://api.mercadolibre.com/currencies/) no mencionada en el challenge, pero la encontré en la documentación de Mercado Libre (https://developers.mercadolibre.com.ar/es_ar/ubicacion-y-monedas#close)
4.	Para acotar el alcance del desarrollo, el script no contempla la posibilidad de que el envío esté a cargo del vendedor (custom), ni de que el comprador pueda acordar el envió con el vendedor. Documentación consultada: https://developers.mercadolibre.com.ar/es_ar/envio-de-productos

### Notas sobre funciones:
- `getMonedas`: Carga el objeto `currencies` de tipo `JSONArray` con las monedas utilizadas por MercadoLibre. Si la variable `ENVIRONMENT` tiene valor `dev`, las toma a partir de un archivo local `data/mock/currencies.json`; si tiene valor `prod`, realiza el request a la API correspondiente (https://api.mercadolibre.com/currencies/)

- `getCurrencySymbol`: Devuelve un objeto de tipo String con el símbolo de la moneda correspondiente. Parameters: `currencyId` El ID de la moneda a buscar

- `getCurrencyDesc`: Devuelve un objeto de tipo String con la descripción de la moneda correspondiente. Parameters: `currencyId`  El ID de la moneda a buscar.
 
- `getOrders`: Devuelve los IDs de las órdenes en un array de tipo String, a partir de un documento de tipo csv. Parameters: `fr` El documento con las órdenes a leer, con formato csv

- `getJSONFromAPI`: es una función genérica que nos permite consultar cualquier API de Mercado Libre según el ejercicio. Dado que hay APIs que retornan Arrays y otras que retornan Objetos, es que esta función permite devolver el tipo de dato correspondiente según la API (array u objeto). Esto lo resuelve con la función `getJSONObjectOrArray`

- `getJSONObjectOrArray`: Devuelve un objeto genérico, con formato JSONObject o JSONArray, según corresponda, a partir de un objeto genérico que contiene el json a leer. 
Parameters: `json` El objeto genérico a leer. Puede ser de tipo String o de tipo FileReader.

- `getRelevantOrderFieldsAsJSONObj`: Devuelve un objeto de tipo JSONObject (a partir de los objetos de la orden, del envío, de los pagos del envío y del proveedor del envío) que únicamente contiene los campos necesarios para imprimir el resultado. 
Parameters: `order` El objeto de tipo JSONObject de la orden. `shipment` El objeto de tipo JSONObject del envío. `shipmentPayments` El objeto de tipo JSONObject con los pagos del envío. `shipmentCarrier` El objeto de tipo JSONObject con la información del proveedor del envío.

-  `buidAddress`: Devuelve un objeto de tipo String a partir de un objeto de tipo JSONObject, con la dirección consolidada.  
Parameters: `obj`  El objeto de tipo JSONObject con la información del domicilio.

- `buildPayments`: Devuelve un objeto de tipo String a partir de un objeto de tipo JSONArray con los pagos del envío, y dos objetos de tipo String con los IDs del vendedor y del comprador
Parameters: `arr`  El objeto de tipo JSONArray con la información de los pagos del envío. `buyerId`  El objeto de tipo String con el ID del comprador. `sellerId`  El objeto de tipo String con el ID del vendedor.

-  `writeOutputFile`: Genera un archivo de tipo .csv, que se guarda en la ruta establecida por la variable `OUTPUT_FILE_PATH`, con la información de cada una de las órdenes.
Parameters: `jsonOrders` El Collection con objetos de tipo `JSONObject`, donde cada uno representa una orden.
