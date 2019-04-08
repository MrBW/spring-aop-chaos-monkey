package de.codecentric.spring.boot.chaos.monkey.assaults;

import de.codecentric.spring.boot.demo.chaos.monkey.ChaosDemoApplication;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;

/**
 * @author Benjamin Wilms
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ChaosDemoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"chaos.monkey.assaults.memoryAssaultActive=true", "spring.profiles" +
        ".active=chaos-monkey"})
public class MemoryAssaultIntegrationTest {

    @Autowired
    private MemoryAssault memoryAssault;

    @Test
    public void memoryAssault_configured() {

        assertNotNull(memoryAssault);
    }

    @Test
    @Ignore
    public void memoryAssault_runAttack() {

        memoryAssault.attack();
    }


}