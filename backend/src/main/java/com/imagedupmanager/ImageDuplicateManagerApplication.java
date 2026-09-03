package com.imagedupmanager;

import com.imagedupmanager.config.FileSystemBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ImageDuplicateManagerApplication {

	public static void main(String[] args) {
		FileSystemBootstrap.ensureDataDirectories();
		SpringApplication.run(ImageDuplicateManagerApplication.class, args);
	}

}

