package com.thilaga.fruitmachine;

import org.springframework.boot.SpringApplication;

public class TestFruitmachineApplication {

	public static void main(String[] args) {
		SpringApplication.from(FruitmachineApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
