package com.haizad.ticketwise;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = { "gemini.api-key=", "admin.email=", "admin.password=" })
class TicketwiseApplicationTests {

	@Test
	void contextLoads() {
	}

}
