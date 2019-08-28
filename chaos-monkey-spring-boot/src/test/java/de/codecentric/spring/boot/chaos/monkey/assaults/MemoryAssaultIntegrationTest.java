package de.codecentric.spring.boot.chaos.monkey.assaults;

import de.codecentric.spring.boot.chaos.monkey.HumanReadableSize;
import de.codecentric.spring.boot.chaos.monkey.configuration.AssaultProperties;
import de.codecentric.spring.boot.chaos.monkey.configuration.ChaosMonkeySettings;
import de.codecentric.spring.boot.chaos.monkey.endpoints.AssaultPropertiesUpdate;
import de.codecentric.spring.boot.demo.chaos.monkey.ChaosDemoApplication;
import lombok.extern.java.Log;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import javax.validation.constraints.NotNull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Benjamin Wilms
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ChaosDemoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "management.endpoints.web.exposure.include=chaosmonkey",
        "management.endpoints.enabled-by-default=true",
        "chaos.monkey.assaults.memoryActive=true", "chaos.monkey.assaults.memoryFillTargetFraction=0.90",
        "chaos.monkey.assaults.memoryMillisecondsWaitNextIncrease=100",
        "chaos.monkey.assaults.memoryFillIncrementFraction=0.9",
        "chaos.monkey.assaults.memoryMillisecondsHoldFilledMemory=5000",
        "spring.profiles.active=chaos-monkey"})
public class MemoryAssaultIntegrationTest {
    @LocalServerPort
    private int serverPort;

    private String baseUrl;

    @Autowired
    private TestRestTemplate restTemplate;


    @Autowired
    private MemoryAssault memoryAssault;

    @Autowired
    private ChaosMonkeySettings settings;

    @NotNull
    private boolean isMemoryAssaultActiveOrignal;

    @NotNull
    private double memoryFillTargetFraction;

    @Before
    public void setUp() {
        isMemoryAssaultActiveOrignal = settings.getAssaultProperties().isMemoryActive();
        memoryFillTargetFraction =
                settings.getAssaultProperties().getMemoryFillTargetFraction();
        baseUrl = "http://localhost:" + this.serverPort + "/actuator/chaosmonkey";
    }

    @After
    public void tearDown() {
        settings.getAssaultProperties().setMemoryActive(isMemoryAssaultActiveOrignal);
    }

    @Test
    public void memoryAssault_configured() {
        assertNotNull(memoryAssault);
        assertTrue(memoryAssault.isActive());
    }

    @Test
    public void memoryAssault_runAttack() {
        Runtime rt = Runtime.getRuntime();
        long start = System.nanoTime();

        Thread backgroundThread = new Thread(memoryAssault::attack);
        backgroundThread.start();

        // make sure we timeout if we never reach the target fill fraction
        while (System.nanoTime() - start < TimeUnit.SECONDS.toNanos(20)) {
            // if we reach target approximately (memory filled up
            // correctly) we can return (test is successful)
            double target = rt.maxMemory() * memoryFillTargetFraction;
            if (isInRange(rt.totalMemory(), target, 0.1)) {
                return;
            }
        }
        // if timeout reached
        fail("Memory did not fill up in time. Filled " + HumanReadableSize.inMegabytes(rt.totalMemory())
                + " MB but should have filled "
                + HumanReadableSize.inMegabytes(rt.maxMemory() * memoryFillTargetFraction) + " MB");
    }

    /**
     * Checks if `value` is in range of designated `target`, depending on
     * given `deviationFactor`
     *
     * @param value - value to check if its in range
     * @param deviationFactor - factor in percentage (10% = 0.1)
     * @param target - value is in range with this
     * @return true if in range
     */
    private boolean isInRange(double value, double target, double deviationFactor) {
        double deviation = target * deviationFactor;
        double lowerBoundary = Math.max(target - deviation, 0);
        double upperBoundary = Math.max(target + deviation,
                Runtime.getRuntime().maxMemory());

        return value >= lowerBoundary && value <= upperBoundary;
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Test
    public void runAndAbortAttack() throws Throwable {
        AssaultPropertiesUpdate assaultProperties = new AssaultPropertiesUpdate();
        assaultProperties.setMemoryActive(false);

        Runtime rt = Runtime.getRuntime();
        long usedMemoryBeforeAttack = rt.totalMemory() - rt.freeMemory();
        long start = System.nanoTime();

        Thread backgroundThread = new Thread(memoryAssault::attack);
        backgroundThread.start();
        Thread.sleep(1000);
        long usedMemoryDuringAttack = rt.totalMemory() - rt.freeMemory();

        assertTrue(usedMemoryBeforeAttack <= usedMemoryDuringAttack);

        ResponseEntity<String> result =
                restTemplate.postForEntity(baseUrl + "/assaults", assaultProperties,
                        String.class);
        assertEquals(200, result.getStatusCodeValue());


        while (backgroundThread.isAlive() && System.nanoTime() - start < TimeUnit.SECONDS.toNanos(20)) {
            // wait for thread to finish gracefully or time out
        }

        assertFalse("Assault is still running", backgroundThread.isAlive());

        long usedMemoryAfterAttack = rt.totalMemory() - rt.freeMemory();
        // garbage collection should have ran by now
        assertTrue("Used more memory after attack than before. Used: " + HumanReadableSize.inMegabytes(usedMemoryAfterAttack) + " but should been lower than " + HumanReadableSize.inMegabytes(usedMemoryBeforeAttack),
                usedMemoryAfterAttack <= usedMemoryBeforeAttack);
    }

    @Test
    public void allowInterruptionOfAssaultDuringHoldPeriod() throws Throwable {
        Runtime rt = Runtime.getRuntime();
        long start = System.nanoTime();
        double targetFraction = 0.25;

        AtomicBoolean stillActive = new AtomicBoolean(true);
        AssaultProperties originalAssaultProperties = settings.getAssaultProperties();
        AssaultProperties mockAssaultConfig = mock(AssaultProperties.class);
        when(mockAssaultConfig.getMemoryFillTargetFraction()).thenReturn(targetFraction);
        when(mockAssaultConfig.getMemoryMillisecondsHoldFilledMemory()).thenReturn(10000);
        when(mockAssaultConfig.getMemoryMillisecondsWaitNextIncrease()).thenReturn(100);
        when(mockAssaultConfig.isMemoryActive()).thenReturn(true);

        try {
            Thread backgroundThread = new Thread(memoryAssault::attack);
            backgroundThread.start();

            outer:
            {
                double fillTargetMemory = rt.maxMemory() * targetFraction;
                while (System.nanoTime() - start < TimeUnit.SECONDS.toNanos(20)) {
                    long totalMemoryDuringAttack = rt.totalMemory();
                    if (isInRange(totalMemoryDuringAttack, fillTargetMemory, 0.1)) {
                        break outer;
                    }
                }

                fail("Memory did not fill up in time. Filled " + HumanReadableSize.inMegabytes(rt.totalMemory())
                        + " MB but should have filled "
                        + HumanReadableSize.inMegabytes(fillTargetMemory) + " MB");
            }

            AssaultPropertiesUpdate assaultProperties = new AssaultPropertiesUpdate();
            assaultProperties.setMemoryActive(false);

            stillActive.set(false);
            ResponseEntity<String> result =
                    restTemplate.postForEntity(baseUrl + "/assaults", assaultProperties,
                            String.class);
            assertEquals(200, result.getStatusCodeValue());

            backgroundThread.join(7500);
            assertFalse(backgroundThread.isAlive());
        } finally {
            settings.setAssaultProperties(originalAssaultProperties);
        }
    }

}
