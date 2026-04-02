package com.parvez.blogs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration;

@SpringBootApplication(
		exclude = {MailSenderAutoConfiguration.class}
)
public class BlogPostApplication {

	public static void main(String[] args) {
		SpringApplication.run(BlogPostApplication.class, args);
		System.out.println("App is running!!!");
	}
}
