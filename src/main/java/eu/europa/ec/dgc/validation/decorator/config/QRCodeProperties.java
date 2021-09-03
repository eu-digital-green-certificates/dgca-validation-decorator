package eu.europa.ec.dgc.validation.decorator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "identity.qrcode")
public class QRCodeProperties {

	private String protocol;

    private String protocolVersion;	
}
