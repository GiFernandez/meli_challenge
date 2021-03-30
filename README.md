## Meli Challenge by Gisela Fernandez


### Requisitos
JDK 15


### Clonar proyecto
```
git clone https://github.com/GiFernandez/meli_challenge.git
```

### Ejercicio 1

#### Compilación

Para compilar se debe ejecutar desde línea de comando lo siguiente: 

Windows
```
javac -cp ".;./jars/*" EjercicioUno.java 
```

Unix
```
javac -cp ".:./jars/*" EjercicioUno.java 
```

#### Ejecución

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

El archivo de salida se consulta desde `data/output/output_1.csv`


#### Explicación genérica del programa

1. Al iniciar, el script evalúa el entorno definido por parámetro, leyendo el primer parámetro enviado. Esto se guarda en una variable global `ENVIRONMENT` que será utilizada a lo largo del programa. En caso de error se muestra mensaje en consola. 
2. Consultamos y guardamos en un variable las monedas de cada país a través de la función `getMonedas` que será utilizado en la construcción de la data de salida (pto 4).
3. Leemos el archivo de entrada `orders.csv` que contiene las órdenes a procesar.
4. Iteramos cada orden para consultar la información solicitada en los distintos recursos, utilizando la fuente correspondiente según el ambiente:
- Ambiente `dev`: se utilizan las respuestas mock de los servicios guardados localmente para cada orden. Para este ejemplo, se crearon órdenes con información falsa que permita testear distintos escenarios de prueba. 
- Ambiente `prod`:  se interactúa con los servicios productivos de la API de ML según el access token enviado por parámetro. Se obtienen los json para cada orden a través de requests a la API correspondiente (Se hace el request a la API de orders, a la API de shipments, a la API de shipment-payment, a la API de shipment-carrier). En caso de error se muestra mensaje en consola. 
Independientemente que la consulta sea vía mock o vía api, los datos se guardan en una única Collection `jsonOrders`, procesando los datos en los atributos necesarios para generar el csv output solicitado. 
5. Escritura del archivo csv de salida: se recorre la Collection `jsonOrders` que contiene toda la información solicitada y se imprime en un csv `output_1.csv` dando por finalizado el proceso. 


#### Cambios posibles en el programa:

1.	En lugar de guardar en una collection la información de todas las orders para luego guardarlas todas juntas en el csv, se podría ir escribiendo en el csv a medida que se consulta cada order.
2.	Se podría tener un archivo de entrada mock que sea distinto al archivo de entrada objetivo para consultar una cantidad menor de órdenes en caso que la lista objetivo sea muy larga. 


#### Consideraciones:

1.	No fue posible consultar las APIs productivas de las órdenes dadas con el access token generado con mi usuario ya que no tengo permisos para poder hacerlo. 
2.	La información de las APIs en la web de developers de Mercado Libre, no me muestra exactamente lo mismo que utilizando los user de test, por esto es que en varios escenarios tengo dudas si la info es correcta.
3.	Para transformar el valor de la moneda, utilicé una API (https://api.mercadolibre.com/currencies/) no mencionada en el challenge, pero la encontré en la documentación de Mercado Libre (https://developers.mercadolibre.com.ar/es_ar/ubicacion-y-monedas#close)
4.	Para acotar el alcance del desarrollo, el script no contempla la posibilidad de que el envío esté a cargo del vendedor (`custom`), ni de que el comprador pueda acordar el envió con el vendedor. Documentación consultada: https://developers.mercadolibre.com.ar/es_ar/envio-de-productos
5. 	Para acotar el alcance del desarrollo, el script no contempla los escenarios en los que el tipo estimación de entrega sea `unknown` o `unknown_frame`, ya que no permitiría la comparación con la fecha de entrega.
6.	Para el punto donde se solicita el Origen, se muestra directamente el Sender Address, en vez de mostrar si corresponde al vendedor o depósitos de Mercado Libre, de acuerdo a lo acordado.


### Ejercicio 2

#### Compilación

Para compilar se debe ejecutar desde línea de comando lo siguiente: 

Windows
```
javac -cp ".;./jars/*" EjercicioDos.java 
```

Unix
```
javac -cp ".:./jars/*" EjercicioDos.java 
```

#### Ejecución

La ejecución del script espera recibir 2 parámetros obligatorios:
1.	**Environment:** Identifica el entornos de ejecución. Puede ser `dev` o `prod`. En el entorno `dev` ejecutará tomando como fuente de datos los archivos mocks, y en entorno prod, ejecutará tomando como fuente las apis productivas. 
2.	**Access Token:** el access token necesario para la autenticación productiva de las apis. Solo se utiliza en entorno `prod`.

Windows
```
java -cp ".;./jars/*" EjercicioDos {environment} {acceso_token}
```
Unix
```
java -cp ".:./jars/*" EjercicioDos {environment} {acceso_token}
```

El archivo de salida se consulta desde `data/output/output_2.csv`


#### Explicación genérica del programa

1. Al iniciar, el script evalúa el entorno definido por parámetro, leyendo el primer parámetro enviado. Esto se guarda en una variable global `ENVIRONMENT` que será utilizada a lo largo del programa. En caso de error se muestra mensaje en consola. 
2. Leemos el archivo de entrada `shipments.csv` que contiene los envíos a procesar.
3. Iteramos cada orden para consultar la información solicitada en los distintos recursos, utilizando la fuente correspondiente según el ambiente:
- Ambiente `dev`: se utilizan las respuestas mock de los servicios guardados localmente para cada envío. Para este ejemplo, se crearon envíos con información falsa que permita testear distintos escenarios de prueba. 
- Ambiente `prod`:  se interactúa con los servicios productivos de la API de ML según el access token enviado por parámetro. Se obtienen los json para cada envío a través de requests a la API correspondiente (Se hace el request a la API de shipments y a la API de shipments-lead_time). En caso de error se muestra mensaje en consola. 
Independientemente que la consulta sea vía mock o vía api, los datos se guardan en una única Collection `jsonShipments`, procesando los datos en los atributos necesarios para generar el csv output solicitado. 
4. Escritura del archivo csv de salida: se recorre la Collection `jsonShipments` que contiene toda la información solicitada y se imprime en un csv `output_2.csv` dando por finalizado el proceso. 


#### Cambios posibles en el programa:

1.	En lugar de guardar en una collection la información de todas los envíos para luego guardarlas todas juntas en el csv, se podría ir escribiendo en el csv a medida que se consulta cada envío.
2.	Se podría tener un archivo de entrada mock que sea distinto al archivo de entrada objetivo para consultar una cantidad menor de envíos en caso que la lista objetivo sea muy larga. 


#### Consideraciones:

1.	No fue posible consultar las APIs productivas de los envíos dados con el access token generado con mi usuario ya que no tengo permisos para poder hacerlo. 
2.	La información de las APIs en la web de developers de Mercado Libre, no me muestra exactamente lo mismo que utilizando los user de test, por esto es que en varios escenarios tengo dudas si la info es correcta.
3.	Para acotar el alcance del desarrollo, el script no contempla la posibilidad de que el envío esté a cargo del vendedor (`custom`), ni de que el comprador pueda acordar el envió con el vendedor. Documentación consultada: https://developers.mercadolibre.com.ar/es_ar/envio-de-productos
4. 	Para acotar el alcance del desarrollo, el script no contempla los escenarios en los que el tipo estimación de entrega sea `unknown` o `unknown_frame`, ya que no permitiría la comparación con la fecha de entrega.
5.	Para el punto donde se solicita el Origen, se muestra el Sender Address, en lugar de mostrar si corresponde al vendedor o depósitos de Mercado Libre, de acuerdo a lo acordado.


#### Notas sobre funciones para ambos ejercicios:

- `getMonedas`: Carga el objeto `currencies` de tipo `JSONArray` con las monedas utilizadas por MercadoLibre. Si la variable `ENVIRONMENT` tiene valor `dev`, las toma a partir de un archivo local `data/mock/currencies.json`; si tiene valor `prod`, realiza el request a la API correspondiente (https://api.mercadolibre.com/currencies/)

- `getCurrencySymbol`: Devuelve un objeto de tipo String con el símbolo de la moneda correspondiente. 
Parameters: `currencyId` El ID de la moneda a buscar

- `getCurrencyDesc`: Devuelve un objeto de tipo String con la descripción de la moneda correspondiente. 
Parameters: `currencyId`  El ID de la moneda a buscar.
 
- `getOrders`: Devuelve los IDs de las órdenes en un array de tipo String, a partir de un documento de tipo csv. 
Parameters: `fr` El documento con las órdenes a leer, con formato csv

- `getJSONFromAPI`: es una función genérica que nos permite consultar cualquier API de Mercado Libre según el ejercicio. Dado que hay APIs que retornan Arrays y otras que retornan Objetos, es que esta función permite devolver el tipo de dato correspondiente según la API (array u objeto). Esto lo resuelve con la función `getJSONObjectOrArray`
Parameters: `site`  La dirección de la API a consultar. `accessToken` El código de autorización para acceder a la API a consultar; puede quedar en blanco si la API no requiere autorización.

- `getJSONObjectOrArray`: Devuelve un objeto genérico, con formato JSONObject o JSONArray, según corresponda, a partir de un objeto genérico que contiene el json a leer. 
Parameters: `json` El objeto genérico a leer. Puede ser de tipo String o de tipo FileReader.

- `getRelevantOrderFieldsAsJSONObj`: Devuelve un objeto de tipo JSONObject (a partir de los objetos de la orden, del envío, de los pagos del envío y del proveedor del envío) que únicamente contiene los campos necesarios para imprimir el resultado. 
Parameters: `order` El objeto de tipo JSONObject de la orden. `shipment` El objeto de tipo JSONObject del envío. `shipmentPayments` El objeto de tipo JSONObject con los pagos del envío. `shipmentCarrier` El objeto de tipo JSONObject con la información del proveedor del envío.

- `buidAddress`: Devuelve un objeto de tipo String a partir de un objeto de tipo JSONObject, con la dirección consolidada.  
Parameters: `obj`  El objeto de tipo JSONObject con la información del domicilio.

- `buildPayments`: Devuelve un objeto de tipo String a partir de un objeto de tipo JSONArray con los pagos del envío, y dos objetos de tipo String con los IDs del vendedor y del comprador
Parameters: `arr`  El objeto de tipo JSONArray con la información de los pagos del envío. `buyerId`  El objeto de tipo String con el ID del comprador. `sellerId`  El objeto de tipo String con el ID del vendedor.

- `writeOutputFile`: Genera un archivo de tipo .csv, que se guarda en la ruta establecida por la variable `OUTPUT_FILE_PATH`, con la información de cada una de las órdenes.
Parameters: `jsonOrders` La Collection con objetos de tipo `JSONObject`, donde cada uno representa una orden.

- `getShipments`: Devuelve los IDs de los envíos en un array de tipo String, a partir de un documento de tipo csv. Parameters: `fr` El documento con los envíos a leer, con formato csv

- `getRelevantShipmentFieldsAsJSONObj`: Devuelve un objeto de tipo JSONObject (a partir de los objetos del envío y de las estimaciones de entrega del envío) que únicamente contiene los campos necesarios para imprimir el resultado. 
Parameters: `shipment` El objeto de tipo JSONObject del envío. `shipmentLeadTime` El objeto de tipo JSONObject con las estimaciones de entrega del envío.

- `getDeliveryDate`: Devuelve un objeto de tipo String con la fecha de entrega del producto con formato `ISO_DATE_TIME`. 
Parameters: `obj`  El objeto de tipo JSONObject con la información del envío.

- `getEstimatedDeliveryDate`: Devuelve un objeto de tipo String con la fecha estimada de entrega del producto con formato `ISO_DATE_TIME`. 
Parameters: `obj`  El objeto de tipo JSONObject con la información del envío.

- `getDeliveryDelay`: Devuelve un objeto de tipo String con la demora entre la fecha de entrega del envío y la primera ventana de entrega estimada. También puede devolver "N/A" si el envío no fue entregado, o si el atributo `"type"` del envío tuviera los valores `unknown` o `unknown_frame`.
Parameters: `estimatedDeliveryDate`  El objeto de tipo String con la fecha de entrega estimada. `deliveryDate`	El objeto de tipo String con la fecha de entrega.

- `wasDeliveryOnTime`: Devuelve un objeto de tipo String, con "Si" o "No" a partir del delay entre la fecha de entrega real y la fecha de entrega estimada.
Parameters: `delay`  El objeto de tipo String con la demora entre la fecha de entrega real y la fecha de entrega estimada.
