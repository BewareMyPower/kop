# Security

## KoP authentication

KoP support following SASL mechanisms:

- PLAIN
- OAUTHBEARER

You must configure it in broker's configuration file, take `PLAIN` for example:

```properties
saslAllowedMechanisms=PLAIN
```

## Enable authentication on Pulsar broker

### PLAIN mechanism

For PLAIN mechanism, the Kafka authentication is forwarded to Pulsar's JWT (Json Web Token) authentication, so you also need to configure the [JWT authentication](https://pulsar.apache.org/docs/en/security-jwt/).

```properties
# Configuration to enable authentication and authorization
authenticationEnabled=true
authorizationEnabled=true
authenticationProviders=org.apache.pulsar.broker.authentication.AuthenticationProviderToken

# Configuration to enable authentication between KoP and Pulsar broker
brokerClientAuthenticationPlugin=org.apache.pulsar.client.impl.auth.AuthenticationToken
brokerClientAuthenticationParameters=token:<token-of-super-user-role>
superUserRoles=<super-user-roles>

# If using secret key
tokenSecretKey=file:///path/to/secret.key
```

### OAUTHBEARER mechanism

For OAUTHBEARER mechanism, you can use `AuthenticationProviderToken` or your own authentication provider to process the access token from OAuth 2.0 server.

What's different from PLAIN mechanism is that you must specify the [AuthenticateCallbackHandler](http://kafka.apache.org/20/javadoc/index.html?org/apache/kafka/common/security/auth/AuthenticateCallbackHandler.html) and its related config file.

KoP provides a builtin `AuthenticateCallbackHandler` that uses Pulsar's authenticate provider for authentication.

```properties
# Use the KoP's builtin handler here
kopOauth2AuthenticateCallbackHandler=io.streamnative.pulsar.handlers.kop.security.oauth.OauthValidatorCallbackHandler
# The Java properties config file of OauthValidatorCallbackHandler
kopOauth2ConfigFile=conf/kop-handler.properties
```

In the config file, you need to specify the validate method, which is what the provider's `getAuthMethodName()` returns. If you're using `AuthenticationProviderToken`, since`AuthenticationProviderToken#getAuthMethodName()` returns `token`, you need to configure as below.

```properties
oauth.validate.method=token
```

See [AuthenticationProvider](https://pulsar.apache.org/docs/en/security-extending/#proxybroker-authentication-plugin) for details.

## Enable authentication on Kafka client

### PLAIN mechanism

To forward your credentials, `SASL-PLAIN` is used on the Kafka client side. The two important settings are `username` and `password`:

* The `username` of Kafka JAAS is the `tenant/namespace`, in which Kafka’s topics are stored in Pulsar.

  For example, `public/default`.

* The `password` must be your token authentication parameters from Pulsar.

  For example, `token:xxx`.

  The token can be created by Pulsar tokens tools. The role is the `subject` for token. It is embedded in the created token, and the broker can get `role` by parsing this token.

You can use the following code to enable SASL-PLAIN through jaas.

```java
String tenant = "public/default";
String password = "token:xxx";

String jaasTemplate = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";";
String jaasCfg = String.format(jaasTemplate, tenant, password);
props.put("sasl.jaas.config", jaasCfg);

props.put("security.protocol", "SASL_PLAINTEXT");
props.put("sasl.mechanism", "PLAIN");
```

Kafka consumers and Kafka producers can use the props to connect to brokers.

### OAUTHBEARER mechanism

Kafka client uses its own login callback handler to get access token from an OAuth 2.0 server, see [login callback handler for token retrieval](https://docs.confluent.io/platform/current/kafka/authentication_sasl/authentication_sasl_oauth.html#login-callback-handler-for-token-retrievalKoP).

KoP provides a builtin callback handler. You can install it to your local Maven repository by

```bash
$ mvn clean install -pl oauth-client -DskipTests
```

Then add following dependency to your `pom.xml`

```xml
    <dependency>
      <groupId>io.streamnative.pulsar.handlers</groupId>
      <artifactId>oauth-client</artifactId>
      <version>2.8.0-SNAPSHOT</version>
      <exclusions>
        <exclusion>
          <groupId>org.apache.logging.log4j</groupId>
          <artifactId>log4j-slf4j-impl</artifactId>
        </exclusion>
      </exclusions>
    </dependency>

    <!-- KoP's login callback handler has a pulsar-client dependency -->
    <dependency>
      <groupId>org.apache.pulsar</groupId>
      <artifactId>pulsar-client</artifactId>
      <version>2.8.0-SNAPSHOT</version>
    </dependency>
```

> NOTE: You need to replace the `2.8.0-SNAPSHOT` to the actual version.

In your client code, you need to configure your producer or consumer with following properties

```properties
sasl.login.callback.handler.class=io.streamnative.pulsar.handlers.kop.security.oauth.OauthLoginCallbackHandler
security.protocol=SASL_PLAINTEXT
sasl.mechanism=OAUTHBEARER
sasl.jaas.config=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule \
   required oauth.issuer.url="..."\
   oauth.credentials.url="..."\
   oauth.audience="...";
```

As we can see, there're some required configs that are marked with `...` in `sasl.jaas.config`. They are from the [Pulsar client's credentials](http://pulsar.apache.org/docs/en/security-oauth2/#client-credentials).

| Kafka config name       | Pulsar config name |
| ----------------------- | ------------------ |
| `oauth.issuer.url`      | `issuerUrl`        |
| `oauth.credentials.url` | `privateKey`       |
| `oauth.audience`        | `audience`         |

## SSL connection

KoP supports the following configuration types for Kafka listeners:
- PLAINTEXT
- SSL

**Example**

```shell
listeners=PLAINTEXT://localhost:9092,SSL://localhost:9093
```

> **Tip**
> For how to configure SSL keys, see [Kafka SSL](https://kafka.apache.org/documentation/#security_ssl).

The following example shows how to connect KoP through SSL.

1. Create SSL related keys.

    This example creates the related CA and JKS files.

    ```shell
    #!/bin/bash
    #Step 1
    keytool -keystore server.keystore.jks -alias localhost -validity 365 -keyalg RSA -genkey
    #Step 2
    openssl req -new -x509 -keyout ca-key -out ca-cert -days 365
    keytool -keystore server.truststore.jks -alias CARoot -import -file ca-cert
    keytool -keystore client.truststore.jks -alias CARoot -import -file ca-cert
    #Step 3
    keytool -keystore server.keystore.jks -alias localhost -certreq -file cert-file
    openssl x509 -req -CA ca-cert -CAkey ca-key -in cert-file -out cert-signed -days 365 -CAcreateserial -passin pass:test1234
    keytool -keystore server.keystore.jks -alias CARoot -import -file ca-cert
    keytool -keystore server.keystore.jks -alias localhost -import -file cert-signed
    ```

2. Configure the KoP broker.

    In the StreamNative Platform configuration file (`${PLATFORM_HOME}/etc/pulsar/broker.conf` or `${PLATFORM_HOME}/etc/pulsar/standalone.conf`), add the related configurations that using the jks configurations created in Step 1:

    ```shell
    listeners=PLAINTEXT://localhost:9092,SSL://localhost:9093

    kopSslKeystoreLocation=/Users/kop/server.keystore.jks
    kopSslKeystorePassword=test1234
    kopSslKeyPassword=test1234
    kopSslTruststoreLocation=/Users/kop/server.truststore.jks
    kopSslTruststorePassword=test1234
    ```

3. Configure the Kafka client.

    (1) Prepare a file named `client-ssl.properties`. The file contains the following information.

    ```shell
    security.protocol=SSL
    ssl.truststore.location=client.truststore.jks
    ssl.truststore.password=test1234
    ssl.endpoint.identification.algorithm=
    ```

    (2) Verify the console-producer and the console-consumer.

    ```shell
    kafka-console-producer.sh --broker-list localhost:9093 --topic test --producer.config client-ssl.properties
    kafka-console-consumer.sh --bootstrap-server localhost:9093 --topic test --consumer.config client-ssl.properties
    ```

    > **Tip**
    > For more information, see [Configure Kafka client](https://kafka.apache.org/documentation/#security_configclients).

