package com.example.demo;

import com.sforce.soap.metadata.Connector;
import com.sforce.soap.metadata.DescribeMetadataObject;
import com.sforce.soap.metadata.DescribeMetadataResult;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.LoginResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringSfApiApplication {

	@Value("${SF_USERNAME}")
	private String SF_USERNAME;

	@Value("${SF_PASSWORD}")
	private String SF_PASSWORD;

	@Value("${SF_API_VER}")
	public int SF_API_VER;

	@Value("${SF_API_END}")
	private String SF_API_END;

	public static MetadataConnection connection = null;


	public static void main(String[] args) {
		SpringApplication.run(SpringSfApiApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx){
		return args -> {
			initializer(SF_API_END, SF_USERNAME, SF_PASSWORD, SF_API_VER);
		};
	}

	public static void initializer(String apiEnd, String username, String password, int apiVersion) throws ConnectionException {
		ConnectorConfig config = new ConnectorConfig();

		config.setServiceEndpoint(apiEnd);
		config.setAuthEndpoint(apiEnd);
		config.setManualLogin(true);

		LoginResult loginResult = new PartnerConnection(config).login(username, password);

		connection = getMetadataConnection(loginResult);

		DescribeMetadataResult metadataResult =  connection.describeMetadata(apiVersion);

		System.out.println("Here are all the metadata objects");

		for (DescribeMetadataObject obj : metadataResult.getMetadataObjects()){
			System.out.println(obj.getXmlName());
		}


	}


	private static MetadataConnection getMetadataConnection(LoginResult loginResult) throws ConnectionException {
		ConnectorConfig loginConfig = new ConnectorConfig();
		loginConfig.setSessionId(loginResult.getSessionId());
		loginConfig.setServiceEndpoint(loginResult.getMetadataServerUrl());

		return new MetadataConnection(loginConfig);
	}

}
