package pe.smartcash.cash;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CashApplication {

  public static void main(String[] args) {
    SpringApplication.run(CashApplication.class, args);
  }

}
