package io.study.studyup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.Assert;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
class StudyupApplicationTests {

	@Autowired
	private MockMvc mockMvc;


	@Test
	@WithMockUser(username = "basatg1", roles = {"USER"})
	public void testCreateStudyGroup() throws Exception {
		// Creating object to store body message key-value pairs
		Object randomObj = new Object() {
			public final String groupname = "study5ever";
			public final String subject = "MATH 1300";
			public final String description = "We meet MWF @7pm in Buttrick";
		};

		ObjectMapper objectMapper = new ObjectMapper();
		String json = objectMapper.writeValueAsString(randomObj);

		// Testing post request to create a new user
		String result = mockMvc.perform(MockMvcRequestBuilders.post("/create-group")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json)
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		Assert.isTrue(result.equals("<h2><center>Created New StudyUp Group Successfully!</center></h2>"));
	}

}
