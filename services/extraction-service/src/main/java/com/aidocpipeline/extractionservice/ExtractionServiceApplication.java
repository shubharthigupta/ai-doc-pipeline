package com.aidocpipeline.extractionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExtractionServiceApplication {

	public static void main(String[] args) {
		// Print actual heap size so we can confirm VM options worked
		long maxHeap = Runtime.getRuntime().maxMemory() / 1024 / 1024;
		System.out.println(">>> MAX HEAP: " + maxHeap + " MB");

		SpringApplication.run(ExtractionServiceApplication.class, args);
	}
}